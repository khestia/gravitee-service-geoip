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

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.*;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.net.InetAddress;
import java.util.EnumSet;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GeoIpFinderService {

    public JsonObject retrieveCityGeoData(InetAddress ipAddress, DatabaseReader databaseReader) throws IOException, GeoIp2Exception {
        JsonObject geo = new JsonObject();

        CityResponse response = databaseReader.city(ipAddress);

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
