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
import java.io.IOException;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface DatabaseReaderService {
    String CITY_DB_TYPE = "GeoLite2-City";
    String DATABASES_GEO_LITE_2_CITY_MMDB = "/databases/GeoLite2-City.mmdb";

    void put(String key, DatabaseReader value);

    DatabaseReader get(String key);

    void close() throws IOException;
}
