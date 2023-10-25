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
package com.here.naksha.lib.hub.storages;

import com.here.naksha.lib.core.NakshaContext;
import com.here.naksha.lib.core.storage.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NHAdminStorage implements IStorage {

  /** Singleton instance of physical admin storage implementation */
  protected final @NotNull IStorage psqlStorage;

  public NHAdminStorage(final @NotNull IStorage psqlStorage) {
    this.psqlStorage = psqlStorage;
  }

  @Override
  public @NotNull IWriteSession newWriteSession(@Nullable NakshaContext context, boolean useMaster) {
    return new NHAdminStorageWriter(this.psqlStorage.newWriteSession(context, useMaster));
  }

  @Override
  public @NotNull IReadSession newReadSession(@Nullable NakshaContext context, boolean useMaster) {
    return new NHAdminStorageReader(this.psqlStorage.newReadSession(context, useMaster));
  }

  /**
   * Initializes the storage, create the transaction table, install needed scripts and extensions.
   */
  @Override
  public void initStorage() {
    this.psqlStorage.initStorage();
  }

  /**
   * Starts the maintainer thread that will take about history garbage collection, sequencing and other background jobs.
   */
  @Override
  public void startMaintainer() {
    this.psqlStorage.startMaintainer();
  }

  /**
   * Blocking call to perform maintenance tasks right now. One-time maintenance.
   */
  @Override
  public void maintainNow() {
    this.psqlStorage.maintainNow();
  }

  /**
   * Stops the maintainer thread.
   */
  @Override
  public void stopMaintainer() {
    this.psqlStorage.stopMaintainer();
  }
}
