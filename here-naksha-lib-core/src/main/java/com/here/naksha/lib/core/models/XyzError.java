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
package com.here.naksha.lib.core.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An enumeration of all possible error codes that can happen while processing a request.
 */
@JsonFormat(shape = Shape.STRING)
public enum XyzError {

  /**
   * An unexpected error (not further specified) happened while processing the request.
   *
   * <p>This can result in a 502 Bad Gateway.
   */
  EXCEPTION("Exception"),

  /**
   * An event that was sent to the connector failed, because the connector cannot process it.
   *
   * <p>This will result in an 501 Not Implemented response.
   */
  NOT_IMPLEMENTED("NotImplemented"),

  /**
   * A conflict occurred when updating a feature.
   *
   * <p>This will result in an 409 Conflict response.
   */
  CONFLICT("Conflict"),

  /**
   * Indicates an authorization error.
   *
   * <p>This will result in a 401 Unauthorized response.
   */
  UNAUTHORIZED("Unauthorized"),

  /**
   * Indicates an authorization error.
   *
   * <p>This will result in an 403 Forbidden response.
   */
  FORBIDDEN("Forbidden"),

  /**
   * The connector cannot handle the request due to a processing limitation in an upstream service or a database.
   *
   * <p>This will result in an 429 Too Many Requests response.
   */
  TOO_MANY_REQUESTS("TooManyRequests"),

  /**
   * A provided argument is invalid or missing.
   *
   * <p>This will lead to a HTTP 400 Bad Request response.
   */
  ILLEGAL_ARGUMENT("IllegalArgument"),

  /**
   * Any service or remote function required to process the request was not reachable.
   *
   * <p>This will result in a 502 Bad Gateway response.
   */
  BAD_GATEWAY("BadGateway"),

  /**
   * The request was aborted due to a timeout.
   *
   * <p>This will result in a HTTP 504 Gateway Timeout response.
   */
  TIMEOUT("Timeout"),

  /**
   * The request was aborted due to PayloadToLarge.
   *
   * <p>This will result in a HTTP 513 response.
   */
  PAYLOAD_TO_LARGE("PayloadToLarge");

  /**
   * The error code.
   */
  @JsonValue
  public final @NotNull String value;

  /**
   * Create a new error code.
   *
   * @param error the error code value.
   */
  XyzError(@NotNull String error) {
    value = error;
  }

  /**
   * Returns the enumeration item for the given value or null, when the given string is no valid error code.
   *
   * @param error the error code as string.
   * @return the error code as enumeration value.
   */
  public static @Nullable XyzError forValue(@Nullable String error) {
    if (error == null) {
      return null;
    }
    for (final XyzError e : XyzError.values()) {
      if (e.value.equalsIgnoreCase(error) || e.name().equalsIgnoreCase(error)) {
        return e;
      }
    }
    return null;
  }

  /**
   * Returns the enumeration item for the given value or null, when the given string is no valid error code.
   *
   * @param error        the error code as string.
   * @param defaultValue the default error to return, when the given error is unknown.
   * @return the error code as enumeration value.
   */
  public static @NotNull XyzError forValue(@Nullable String error, @NotNull XyzError defaultValue) {
    if (error == null) {
      return defaultValue;
    }
    for (final XyzError e : XyzError.values()) {
      if (e.value.equalsIgnoreCase(error) || e.name().equalsIgnoreCase(error)) {
        return e;
      }
    }
    return defaultValue;
  }

  @Override
  public String toString() {
    return "XyzError{" + "value='" + value + '\'' + '}';
  }
}
