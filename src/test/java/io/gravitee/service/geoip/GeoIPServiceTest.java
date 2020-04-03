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

import org.junit.Before;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GeoIPServiceTest {

    private GeoIPService processor = new GeoIPService();

    @Before
    public void setUp() throws Exception {
    //    processor.start();
    }

    /*
    @Test
    public void shouldProcessEvent_noGeoIp() {
        Event event = Event
                .now()
                .type("TEST_CASE")
                .property("property-1", "value")
                .build();

        processor.process(event);

        Assert.assertNotNull(event.properties());
        Assert.assertNotNull(event.properties().get("property-1"));
    }

    @Test
    public void shouldProcessEvent_withGeoIp_missingProperty() {
        Event event = Event
                .now()
                .type("TEST_CASE")
                .property("property-1", "value")
                .context("geoip", "ip")
                .build();

        processor.process(event);

        Assert.assertNotNull(event.properties());
    }
    */

    @Test
    public void shouldProcessEvent_withGeoIp_ipProperty() {

        /*
        processor.resolve( "216.58.206.227");

        Assert.assertNotNull(event.properties());
        Assert.assertEquals("216.58.206.227", event.properties().get("ip"));
        Assert.assertEquals("US", event.properties().get("ip.country_iso_code"));
         */
    }
}
