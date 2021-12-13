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
import io.gravitee.service.geoip.cache.GeoIpCache;
import io.gravitee.service.geoip.service.DatabaseReaderServiceImpl;
import io.gravitee.service.geoip.service.DatabaseReaderWatcherService;
import io.gravitee.service.geoip.service.GeoIpFinderService;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static io.gravitee.service.geoip.service.DatabaseReaderService.CITY_DB_TYPE;
import static io.gravitee.service.geoip.service.DatabaseReaderService.DATABASES_GEO_LITE_2_CITY_MMDB;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

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
        final DatabaseReaderServiceImpl databaseReaderService = new DatabaseReaderServiceImpl();
        databaseReaderService.put(CITY_DB_TYPE, getDatasourceReader());

        var databaseReaderWatcherService = mock(DatabaseReaderWatcherService.class);
        doNothing().when(databaseReaderWatcherService).close();

        processor = new GeoIPService(vertx, databaseReaderService, new GeoIpFinderService(), new GeoIpCache(5), databaseReaderWatcherService);
        processor.start();
    }

    private static DatabaseReader getDatasourceReader() throws IOException {
        return new DatabaseReader.Builder(GeoIPServiceTest.class.getResourceAsStream(DATABASES_GEO_LITE_2_CITY_MMDB)).build();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        processor.stop();
        vertx.close();
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
        assertNull(body.getString("region_name"));
        assertNull(body.getString("city_name"));
        assertEquals(body.getString("timezone"), "America/Chicago");
        assertEquals(body.getDouble("lat"), 37.751, 0);
        assertEquals(body.getDouble("lon"), -97.822, 0);

        //Hits the cache
        messageFuture = getMessageFuture(GRAVITEE_IO_WEBSITE_IP);
        final JsonObject body2 = messageFuture.result().body();

        assertEquals(body, body2);
    }

    private Future<Message<JsonObject>> getMessageFuture(String ipMessage) {
        var messageFuture = vertx.eventBus().<JsonObject>request(GeoIPService.GEOIP_SERVICE, ipMessage);
        while (!messageFuture.isComplete()) ;
        return messageFuture;
    }
}
