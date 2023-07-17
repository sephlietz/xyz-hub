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
package com.here.naksha.lib.core.storage;

import com.here.naksha.lib.core.lambdas.Pe1;
import com.here.naksha.lib.core.models.TxSignalSet;
import java.io.Closeable;
import org.jetbrains.annotations.NotNull;

/** Storage API to gain access to storages. */
public interface IStorage extends Closeable {
  // TODO: - Add transaction log access.
  //       - Add history access.

  /**
   * Perform maintenance tasks, for example garbage collect features that are older than the set
   * {@link CollectionInfo#getMaxAge()}. This task is at least called ones every 12 hours. It is
   * guaranteed that this is only executed on one Naksha instances at a given time, so there is no
   * concurrent execution.
   *
   * @throws Exception If any error occurred.
   */
  void maintain() throws Exception;

  /**
   * Create default transaction settings.
   * @return New transaction settings.
   */
  @NotNull
  ITransactionSettings createSettings();

  /**
   * Opens a read-only transaction, preferably from a replication node; if no replication node is available, then returns a transaction to
   * the master node.
   *
   * @return the read transaction.
   * @throws Exception If any error occurred.
   */
  default @NotNull IReadTransaction openReplicationTransaction() throws Exception {
    return openReplicationTransaction(createSettings());
  }

  /**
   * Opens a read-only transaction, preferably from a replication node; if no replication node is available, then returns a transaction to
   * the master node.
   *
   * @param settings Optional settings for the transaction.
   * @return the read transaction.
   * @throws Exception If any error occurred.
   */
  @NotNull
  IReadTransaction openReplicationTransaction(@NotNull ITransactionSettings settings) throws Exception;

  /**
   * Opens a read/write transaction to the master node.
   *
   * @return The mutator transaction.
   * @throws Exception If any error occurred.
   */
  default @NotNull IMasterTransaction openMasterTransaction() throws Exception {
    return openMasterTransaction(createSettings());
  }

  /**
   * Opens a read/write transaction to the master node.
   *
   * @param settings Optional settings for the transaction.
   * @return The mutator transaction.
   * @throws Exception If any error occurred.
   */
  @NotNull
  IMasterTransaction openMasterTransaction(@NotNull ITransactionSettings settings) throws Exception;

  /**
   * Add a listener to be called, when something changes in the storage.
   *
   * @param listener The change listener to invoke, receiving the transaction set. If the listener
   *     throws an exception, it should be called again after some time.
   * @throws Exception If any error occurred.
   */
  void addListener(@NotNull Pe1<@NotNull TxSignalSet> listener) throws Exception;

  /**
   * Remove the given listener.
   *
   * @param listener the change listener to remove.
   * @return {@code true} if the listener was removed; {@code false} otherwise.
   */
  boolean removeListener(@NotNull Pe1<@NotNull TxSignalSet> listener);

  /**
   * Closes the storage, may block for cleanup work.
   */
  void close();
}
