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

package io.gravitee.service.geoip.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vertx.core.json.JsonObject;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GeoIpCache {

    private final Cache<InetAddress, JsonObject> cache;

    public GeoIpCache(int capacity) {
        cache = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.HOURS).maximumSize(capacity).build();
    }

    public JsonObject get(InetAddress ip) {
        return cache.getIfPresent(ip);
    }

    public void put(InetAddress ip, JsonObject geoIp) {
        cache.put(ip, geoIp);
    }

    public Map<InetAddress, JsonObject> getCache() {
        return cache.asMap();
    }
}
