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

import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.*;
import io.gravitee.common.service.AbstractService;
import io.gravitee.service.geoip.utils.InetAddresses;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.InetAddress;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GeoIPService extends AbstractService<GeoIPService> {

    private final Logger logger = LoggerFactory.getLogger(GeoIPService.class);

    public final static String GEOIP_SERVICE = "service:geoip";
    private static final String CITY_DB_TYPE = "GeoLite2-City";

    private final Map<String, DatabaseReader> readers = new HashMap<>();
    private MessageConsumer<String> consumer;

    private final Vertx vertx;

    @Autowired
    public GeoIPService(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    protected String name() {
        return "GeoIP Resolver Service";
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        readers.put(CITY_DB_TYPE, new DatabaseReader
                .Builder(this.getClass().getResourceAsStream("/databases/GeoLite2-City.mmdb"))
                .withCache(new CHMCache())
                .build());

        consumer = vertx.eventBus().consumer(GEOIP_SERVICE, message -> {
            try {
                InetAddress ipAddress = InetAddresses.forString(message.body());
                JsonObject geoData = retrieveCityGeoData(ipAddress);

                message.reply(geoData);
            } catch (AddressNotFoundException anfe) {
                // Silent exception to avoid unnecessary logs
                message.fail(-1, anfe.getMessage());
            } catch (Exception ex) {
                logger.error("Unexpected error while resolving IP: {}", message.body(), ex);
                message.fail(-1, "Unexpected error while resolving IP: {}");
            }
        });
    }

    private JsonObject retrieveCityGeoData(InetAddress ipAddress) throws IOException, GeoIp2Exception {
        JsonObject geo = new JsonObject();

        CityResponse response = readers.get(CITY_DB_TYPE).city(ipAddress);

        Country country = response.getCountry();
        City city = response.getCity();
        Location location = response.getLocation();
        Continent continent = response.getContinent();
        Subdivision subdivision = response.getMostSpecificSubdivision();

        for (Property property : Property.ALL_CITY_PROPERTIES) {
            switch (property) {
                case COUNTRY_ISO_CODE:
                    geo.put("country_iso_code", country.getIsoCode());
                    break;
                case COUNTRY_NAME:
                    geo.put("country_name", country.getName());
                    break;
                case CONTINENT_NAME:
                    geo.put("continent_name", continent.getName());
                    break;
                case REGION_NAME:
                    geo.put("region_name", subdivision.getName());
                    break;
                case CITY_NAME:
                    geo.put("city_name", city.getName());
                    break;
                case TIMEZONE:
                    geo.put("timezone", location.getTimeZone());
                    break;
                case LOCATION:
                    geo.put("lat", location.getLatitude());
                    geo.put("lon", location.getLongitude());
                    break;
            }
        }
        return geo;
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        if (consumer != null) {
            consumer.unregister();
        }

        readers.forEach((databaseType, databaseReader) -> {
            try {
                databaseReader.close();
            } catch (IOException ioe) {
                logger.error("Unexpected error while closing GeoIP database", ioe);
            }
        });
    }

    enum Property {

        IP,
        COUNTRY_ISO_CODE,
        COUNTRY_NAME,
        CONTINENT_NAME,
        REGION_NAME,
        CITY_NAME,
        TIMEZONE,
        LOCATION;

        static final EnumSet<Property> ALL_CITY_PROPERTIES = EnumSet.allOf(Property.class);
    }
}
