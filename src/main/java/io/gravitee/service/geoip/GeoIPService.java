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
package io.gravitee.service.geoip;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import io.gravitee.common.service.AbstractService;
import io.gravitee.service.geoip.cache.GeoIpCache;
import io.gravitee.service.geoip.service.DatabaseReaderService;
import io.gravitee.service.geoip.service.DatabaseReaderWatcherService;
import io.gravitee.service.geoip.service.GeoIpFinderService;
import io.gravitee.service.geoip.utils.InetAddresses;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.InetAddress;

import static java.util.Objects.isNull;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GeoIPService extends AbstractService<GeoIPService> {

    private static final String GEO_IP_RESOLVER_SERVICE = "GeoIP Resolver Service";
    private final Logger logger = LoggerFactory.getLogger(GeoIPService.class);

    public final static String GEOIP_SERVICE = "service:geoip";
    private static final String CITY_DB_TYPE = "GeoLite2-City";

    private final DatabaseReaderService databaseReaderService;
    private final GeoIpFinderService geoIPFinderService;
    private final GeoIpCache cache;
    private final DatabaseReaderWatcherService databaseReaderWatcherService;

    private MessageConsumer<String> consumer;

    private final Vertx vertx;

    @Autowired
    public GeoIPService(Vertx vertx,
                        DatabaseReaderService databaseReaderService,
                        GeoIpFinderService geoIPFinderService,
                        GeoIpCache cache,
                        DatabaseReaderWatcherService databaseReaderWatcherService
    ) {
        this.vertx = vertx;
        this.geoIPFinderService = geoIPFinderService;
        this.databaseReaderService = databaseReaderService;
        this.cache = cache;
        this.databaseReaderWatcherService = databaseReaderWatcherService;
    }

    @Override
    protected String name() {
        return GEO_IP_RESOLVER_SERVICE;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        consumer = vertx.eventBus().consumer(GEOIP_SERVICE, message -> {
            try {
                InetAddress ipAddress = InetAddresses.forString(message.body());
                final DatabaseReader databaseReader = databaseReaderService.get(CITY_DB_TYPE);

                if (isNull(databaseReader)) {
                    throw new GeoIp2Exception("Database " + CITY_DB_TYPE + " not loaded");
                }

                JsonObject geoData = cache.get(ipAddress);
                if (geoData == null) {
                    geoData = geoIPFinderService.retrieveCityGeoData(ipAddress, databaseReader);
                    cache.put(ipAddress, geoData);
                }

                message.reply(geoData);
            } catch (AddressNotFoundException anfe) {
                // Silent exception to avoid unnecessary logs
                message.fail(-1, anfe.getMessage());
            } catch (Exception ex) {
                logger.error("Unexpected error while resolving IP: {}", message.body(), ex);
                message.fail(-1, "Unexpected error while resolving IP {" + message.body() + "}");
            }
        });
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (consumer != null) {
            consumer.unregister();
        }
        databaseReaderService.close();
        databaseReaderWatcherService.close();
    }
}
