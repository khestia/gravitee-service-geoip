/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gravitee.service.geoip.service;

import static com.sun.nio.file.SensitivityWatchEventModifier.HIGH;
import static io.gravitee.service.geoip.service.DatabaseReaderService.CITY_DB_TYPE;
import static io.gravitee.service.geoip.service.DatabaseReaderService.DATABASES_GEO_LITE_2_CITY_MMDB;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.DatabaseReader.Builder;
import io.gravitee.service.geoip.cache.GeoIpCache;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchService;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DatabaseReaderWatcherService implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseReaderWatcherService.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final DatabaseReaderService databaseReaderService;
    private final String cityNameDatabase;
    private final GeoIpFinderService geoIpFinderService;
    private final GeoIpCache cache;
    private boolean started;

    public DatabaseReaderWatcherService(
        DatabaseReaderService databaseReaderService,
        GeoIpFinderService geoIpFinderService,
        GeoIpCache cache,
        String cityNameDatabase
    ) {
        this.cache = cache;
        this.databaseReaderService = databaseReaderService;
        this.geoIpFinderService = geoIpFinderService;
        this.cityNameDatabase = cityNameDatabase;
    }

    @Override
    public void run() {
        try {
            WatchService watcherService = FileSystems.getDefault().newWatchService();
            final File file = new File(cityNameDatabase);
            Path path = file.toPath();
            Path directory = path.getParent();
            directory.register(watcherService, new Kind[] { ENTRY_MODIFY }, HIGH);
            while (started) {
                var watchKey = watcherService.poll(200, TimeUnit.MILLISECONDS);
                if (nonNull(watchKey)) {
                    watchKey
                        .pollEvents()
                        .stream()
                        .map(watchEvent -> ((WatchEvent<Path>) watchEvent).context().getFileName())
                        .filter(path.getFileName()::equals)
                        .findAny()
                        .ifPresent(__ -> loadDatabase(cityNameDatabase, CITY_DB_TYPE, DATABASES_GEO_LITE_2_CITY_MMDB));
                    if (!watchKey.reset()) {
                        throw new InterruptedException("watchKey could not reset");
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Password dictionary watcher stopped. Reason:", e);
        }
    }

    public DatabaseReaderWatcherService start(boolean watch) {
        started = true;
        if (nonNull(cityNameDatabase)) {
            executor.submit(() -> loadDatabase(cityNameDatabase, CITY_DB_TYPE, DATABASES_GEO_LITE_2_CITY_MMDB));
            if (watch) {
                executor.submit(this);
            }
        } else {
            executor.submit(this.loadEmbeddedDefaultReader(CITY_DB_TYPE, DATABASES_GEO_LITE_2_CITY_MMDB));
        }
        return this;
    }

    private void loadDatabase(String databaseName, String cityDbType, String databaseClasspathName) {
        LOG.info("Loading {} database", cityDbType);
        var optionalReader = loadReader(databaseName, false);
        optionalReader.ifPresentOrElse(
            // If present we load the new reader
            refreshAndLoadDatabase(cityDbType),
            // Unless there is no reader present (we might have a working reader before) we load the embedded db
            loadEmbeddedDefaultReader(cityDbType, databaseClasspathName)
        );
    }

    private Consumer<DatabaseReader> refreshAndLoadDatabase(String cityDbType) {
        return reader -> {
            databaseReaderService.put(cityDbType, reader);
            // We refresh only if there was data before
            var oldReader = databaseReaderService.get(cityDbType);
            if (nonNull(oldReader)) {
                refreshDataIfNecessary(reader);
            }
            LOG.info("{} database loaded", cityDbType);
        };
    }

    private void refreshDataIfNecessary(DatabaseReader reader) {
        final Set<InetAddress> inetAddresses = this.cache.getCache().keySet();
        inetAddresses.forEach(ip -> {
            try {
                this.cache.put(ip, this.geoIpFinderService.retrieveCityGeoData(ip, reader));
            } catch (Exception e) {
                LOG.error("Could not refresh entry {}, reason:", ip, e);
            }
        });
    }

    private Runnable loadEmbeddedDefaultReader(String cityDbType, String databaseClasspathName) {
        return () -> {
            // We load only once the embedded data
            // we don't want to override if the previous databaseReader worked
            if (isNull(databaseReaderService.get(cityDbType))) {
                var optionalDefaultReader = loadReader(databaseClasspathName, true);
                if (optionalDefaultReader.isPresent()) {
                    LOG.info("Fallback to {} embedded database", CITY_DB_TYPE);
                    databaseReaderService.put(cityDbType, optionalDefaultReader.get());
                    LOG.info("{} embedded database loaded", cityDbType);
                }
            }
        };
    }

    private Optional<DatabaseReader> loadReader(String filename, boolean isClasspath) {
        try {
            var inputStream = isClasspath ? this.getClass().getResourceAsStream(filename) : new FileInputStream(filename);
            return Optional.of(new Builder(inputStream).build());
        } catch (IOException e) {
            LOG.error("An unexpected error has occurred", e);
        }
        return Optional.empty();
    }

    public void close() {
        LOG.info("Closing {} ", this.getClass().getName());
        started = false;
        executor.shutdown();
        LOG.info("{} closed", this.getClass().getName());
    }
}
