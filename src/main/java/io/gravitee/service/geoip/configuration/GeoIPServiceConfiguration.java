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

package io.gravitee.service.geoip.configuration;

import io.gravitee.service.geoip.cache.GeoIpCache;
import io.gravitee.service.geoip.service.DatabaseReaderService;
import io.gravitee.service.geoip.service.DatabaseReaderServiceImpl;
import io.gravitee.service.geoip.service.DatabaseReaderWatcherService;
import io.gravitee.service.geoip.service.GeoIpFinderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class GeoIPServiceConfiguration {

    @Value("${geoip.database.city.filename:#{null}}")
    private String filename;

    @Value("${geoip.database.city.watch:false}")
    private boolean watch;

    @Value("${geoip.database.city.cache.capacity:4096}")
    private int cityCacheCapacity;

    @Bean
    public GeoIpCache geoIpCache() {
        return new GeoIpCache(cityCacheCapacity < 1 ? 4096 : cityCacheCapacity);
    }

    @Bean
    public DatabaseReaderService databaseReaderService() {
        return new DatabaseReaderServiceImpl();
    }

    @Bean
    public GeoIpFinderService geoIpFinderService() {
        return new GeoIpFinderService();
    }

    @Bean
    public DatabaseReaderWatcherService databaseReaderCronService(
        GeoIpCache geoIpCache,
        GeoIpFinderService geoIpFinderService,
        DatabaseReaderService databaseReaderService
    ) {
        return new DatabaseReaderWatcherService(databaseReaderService, geoIpFinderService, geoIpCache, filename).start(watch);
    }
}
