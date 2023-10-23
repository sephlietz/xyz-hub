/*
 * Copyright (C) 2017-2023 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */
package com.here.naksha.lib.hub;

import static org.junit.jupiter.api.Assertions.*;

import com.here.naksha.lib.core.NakshaAdminCollection;
import com.here.naksha.lib.core.NakshaContext;
import com.here.naksha.lib.core.models.naksha.Storage;
import com.here.naksha.lib.core.models.storage.ErrorResult;
import com.here.naksha.lib.core.models.storage.ReadFeatures;
import com.here.naksha.lib.core.models.storage.ReadResult;
import com.here.naksha.lib.core.models.storage.Result;
import com.here.naksha.lib.core.storage.IReadSession;
import com.here.naksha.lib.core.util.json.Json;
import com.here.naksha.lib.core.view.ViewSerialize;
import com.here.naksha.lib.psql.PsqlConfig;
import com.here.naksha.lib.psql.PsqlConfigBuilder;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

class NakshaHubTest {

  static final String TEST_DATA_FOLDER = "src/test/resources/unit_test_data/";
  static NakshaHub hub;

  // TODO HP : Re-enable after NakshaHub initialization code starts working (dependency on psql module)
  @BeforeAll
  static void prepare() {
    String dbUrl = System.getenv("TEST_NAKSHA_PSQL_URL");
    String password = System.getenv("TEST_NAKSHA_PSQL_PASS");
    if (password == null) password = "password";
    if (dbUrl == null)
      dbUrl = "jdbc:postgresql://localhost/postgres?user=postgres&password=" + password
          + "&schema=naksha_test_hub";
    final PsqlConfig psqlCfg = new PsqlConfigBuilder()
        .withAppName(NakshaHubConfig.defaultAppName())
        .parseUrl(dbUrl)
        .build();
    hub = new NakshaHub(psqlCfg, null, null);
  }

  private String readTestFile(final String filePath) throws Exception {
    return new String(Files.readAllBytes(Paths.get(TEST_DATA_FOLDER + filePath)));
  }

  // @Test
  void tc0001_testGetStorages() throws Exception {
    // TODO: test hub methods
    // 1. Load test data
    final String expectedBodyPart = readTestFile("TC0001_getStorages/body_part.json");
    // Create new NakshaContext
    final NakshaContext ctx = new NakshaContext();
    // Create ReadFeatures Request to read all storages from Admin DB
    final ReadFeatures readFeaturesReq = new ReadFeatures(NakshaAdminCollection.STORAGES);
    // Submit request to NH Space Storage
    try (final IReadSession reader = hub.getSpaceStorage().newReadSession(ctx, false)) {
      final Result result = reader.execute(readFeaturesReq);
      if (result == null) {
        fail("Storage read result is null!");
      } else if (result instanceof ErrorResult er) {
        fail("Exception reading storages " + er);
      } else if (result instanceof ReadResult<?> rr) {
        final ReadResult<Storage> storages = rr.withFeatureType(Storage.class);
        String storagesJson = null;
        try (final Json json = Json.get()) {
          storagesJson = json.writer(ViewSerialize.Storage.class)
              .forType(Storage.class)
              .writeValueAsString(storages);
        }
        JSONAssert.assertEquals(
            "Expecting default psql Storage", expectedBodyPart, storagesJson, JSONCompareMode.LENIENT);
      } else {
        fail("Unexpected result while reading storages : " + result.getClass());
      }
    }
  }

  @AfterAll
  static void close() throws InterruptedException {
    // TODO: Find a way to gracefully shutdown the hub
  }
}
