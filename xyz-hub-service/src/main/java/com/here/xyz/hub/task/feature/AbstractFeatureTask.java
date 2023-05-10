/*
 * Copyright (C) 2017-2022 HERE Europe B.V.
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

package com.here.xyz.hub.task.feature;

import com.here.xyz.INaksha;
import com.here.xyz.events.Event;
import com.here.xyz.exceptions.ParameterError;
import com.here.xyz.hub.rest.ApiParam;
import com.here.xyz.hub.rest.ApiResponseType;
import com.here.xyz.hub.task.NakshaTask;
import com.here.xyz.models.hub.Space;
import com.here.xyz.responses.XyzResponse;
import io.vertx.ext.web.RoutingContext;
import org.jetbrains.annotations.NotNull;

/**
 * All tasks related to features in a space.
 */
public abstract class AbstractFeatureTask<EVENT extends Event> extends NakshaTask<EVENT> {

  protected AbstractFeatureTask(@NotNull EVENT event) {
    super(event);
  }

  @Override
  protected void initEventFromRoutingContext(@NotNull RoutingContext routingContext, @NotNull ApiResponseType responseType)
      throws ParameterError {
    super.initEventFromRoutingContext(routingContext, responseType);
    if (routingContext.pathParam(ApiParam.Path.SPACE_ID) == null) {
      throw new ParameterError("Missing space path parameter");
    }
    final @NotNull String spaceId = routingContext.pathParam(ApiParam.Path.SPACE_ID);
    if (spaceId == null) {
      throw new ParameterError("Missing space path parameter");
    }
    final Space space = INaksha.instance.get().getSpaceById(spaceId);
    if (space == null) {
      throw new ParameterError("Unknown space " + spaceId);
    }
    event.setSpace(space);
    requestMatrix.readFeatures(space);
  }


  @Override
  protected @NotNull XyzResponse execute() throws Exception {
    final Space space = event.getSpace();
    if (space == null) {
      throw new ParameterError("Missing space for spaceId "+event.getStreamId());
    }
    pipeline.addSpaceHandler(space);
    return sendAuthorizedEvent(event, requestMatrix());
  }
}