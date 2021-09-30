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

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GeoIPServiceTest {

    public static final String GRAVITEE_IO_WEBSITE_IP = "75.2.70.75";
    private static GeoIPService processor;
    private static Vertx vertx;

    @BeforeClass
    public static void beforeClass() throws Exception {
        vertx = Vertx.vertx();
        processor = new GeoIPService(vertx);
        processor.start();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        processor.stop();
        vertx.close();
    }

    @Before
    public void setUp() {
    }

    @Test
    public void shouldProcessEvent_noGeoIp() {
        Future<Message<JsonObject>> messageFuture = getMessageFuture(null);

        assertTrue(messageFuture.failed());
        assertTrue(messageFuture.cause() instanceof ReplyException);
        assertEquals(messageFuture.cause().getMessage(), "Unexpected error while resolving IP {null}");
    }

    @Test
    public void shouldProcessEvent_withGeoIp_missingProperty() {
        Future<Message<JsonObject>> messageFuture = getMessageFuture("");

        assertTrue(messageFuture.failed());
        assertTrue(messageFuture.cause() instanceof ReplyException);
        assertEquals(messageFuture.cause().getMessage(), "Unexpected error while resolving IP {}");
    }

    @Test
    public void shouldProcessEvent_withGeoIp_notAnIp() {
        Future<Message<JsonObject>> messageFuture = getMessageFuture("gravitee.io");

        assertTrue(messageFuture.failed());
        assertTrue(messageFuture.cause() instanceof ReplyException);
        assertEquals(messageFuture.cause().getLocalizedMessage(), "Unexpected error while resolving IP {gravitee.io}");
    }

    @Test
    public void shouldProcessEvent_withGeoIp_wrongIp() {
        Future<Message<JsonObject>> messageFuture = getMessageFuture("127.0.0.1");

        assertTrue(messageFuture.failed());
        assertTrue(messageFuture.cause() instanceof ReplyException);
        assertEquals(messageFuture.cause().getMessage(), "The address 127.0.0.1 is not in the database.");
    }

    @Test
    public void shouldProcessEvent_withGeoIp_ipProperty() {
        Future<Message<JsonObject>> messageFuture = getMessageFuture(GRAVITEE_IO_WEBSITE_IP);

        assertTrue(messageFuture.succeeded());

        final JsonObject body = messageFuture.result().body();
        assertNotNull(body);
        assertEquals(body.getString("country_iso_code"), "US");
        assertEquals(body.getString("country_name"), "United States");
        assertEquals(body.getString("continent_name"), "North America");
        assertEquals(body.getString("region_name"), null);
        assertEquals(body.getString("city_name"), null);
        assertEquals(body.getString("timezone"), "America/Chicago");
        assertEquals(body.getDouble("lat"), 37.751, 0);
        assertEquals(body.getDouble("lon"), -97.822, 0);
    }

    private Future<Message<JsonObject>> getMessageFuture(String ipMessage) {
        var messageFuture = vertx.eventBus().<JsonObject>request(GeoIPService.GEOIP_SERVICE, ipMessage);
        while (!messageFuture.isComplete()) ;
        return messageFuture;
    }
}
