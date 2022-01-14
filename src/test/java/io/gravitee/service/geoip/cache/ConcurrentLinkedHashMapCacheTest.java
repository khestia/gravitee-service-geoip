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

import static java.lang.String.format;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;

import io.gravitee.service.geoip.utils.InetAddresses;
import io.vertx.core.json.JsonObject;
import java.net.InetAddress;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConcurrentLinkedHashMapCacheTest {

    private GeoIpCache cache;
    private int capacity;

    @Before
    public void setUp() {
        capacity = 5;
        cache = new GeoIpCache(5);
    }

    @Test
    public void mustLimitCache_with_5_capacity() {
        int previousValue = -1;
        for (int i = 0; i < 100; i++) {
            final InetAddress ip = InetAddresses.forString(format("%d.%d.%d.%d", i, i, i, i));
            this.cache.put(ip, new JsonObject());
            cache.get(ip);
            if (cache.getCache().size() < capacity) {
                assertEquals(cache.getCache().size(), i + 1);
            } else {
                assertFalse(cache.getCache().containsKey(previousValue));
            }
            previousValue = i;
        }
    }
}
