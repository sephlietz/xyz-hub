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
package com.here.naksha.app.service.http.tasks;

import static com.here.naksha.app.service.http.apis.ApiParams.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.here.naksha.app.service.http.NakshaHttpVerticle;
import com.here.naksha.app.service.models.FeatureCollectionRequest;
import com.here.naksha.lib.core.INaksha;
import com.here.naksha.lib.core.NakshaContext;
import com.here.naksha.lib.core.models.XyzError;
import com.here.naksha.lib.core.models.geojson.implementation.XyzFeature;
import com.here.naksha.lib.core.models.payload.XyzResponse;
import com.here.naksha.lib.core.models.payload.events.QueryParameterList;
import com.here.naksha.lib.core.models.storage.Result;
import com.here.naksha.lib.core.models.storage.WriteXyzFeatures;
import com.here.naksha.lib.core.util.json.Json;
import com.here.naksha.lib.core.util.storage.RequestHelper;
import com.here.naksha.lib.core.view.ViewDeserialize;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriteFeatureApiTask<T extends XyzResponse> extends AbstractApiTask<XyzResponse> {

  private static final Logger logger = LoggerFactory.getLogger(WriteFeatureApiTask.class);
  private final @NotNull WriteFeatureApiReqType reqType;

  public enum WriteFeatureApiReqType {
    CREATE_FEATURES,
    UPSERT_FEATURES,
    UPDATE_BY_ID,
    DELETE_FEATURES,
    DELETE_BY_ID
  }

  public WriteFeatureApiTask(
      final @NotNull WriteFeatureApiReqType reqType,
      final @NotNull NakshaHttpVerticle verticle,
      final @NotNull INaksha nakshaHub,
      final @NotNull RoutingContext routingContext,
      final @NotNull NakshaContext nakshaContext) {
    super(verticle, nakshaHub, routingContext, nakshaContext);
    this.reqType = reqType;
  }

  /**
   * Initializes this task.
   */
  @Override
  protected void init() {}

  /**
   * Execute this task.
   *
   * @return the response.
   */
  @Override
  protected @NotNull XyzResponse execute() {
    logger.info("Received Http request {}", this.reqType);
    // Custom execute logic to process input API request based on reqType
    try {
      return switch (this.reqType) {
        case CREATE_FEATURES -> executeCreateFeatures();
        case UPSERT_FEATURES -> executeUpsertFeatures();
        case UPDATE_BY_ID -> executeUpdateFeature();
        case DELETE_FEATURES -> executeDeleteFeatures();
        case DELETE_BY_ID -> executeDeleteFeature();
        default -> executeUnsupported();
      };
    } catch (Exception ex) {
      // unexpected exception
      logger.error("Exception processing Http request. ", ex);
      return verticle.sendErrorResponse(
          routingContext, XyzError.EXCEPTION, "Internal error : " + ex.getMessage());
    }
  }

  private @NotNull XyzResponse executeCreateFeatures() throws Exception {
    // Deserialize input request
    final FeatureCollectionRequest collectionRequest = featuresFromRequestBody();
    final List<XyzFeature> features = (List<XyzFeature>) collectionRequest.getFeatures();
    if (features.isEmpty()) {
      return verticle.sendErrorResponse(routingContext, XyzError.ILLEGAL_ARGUMENT, "Can't create empty features");
    }

    // Parse API parameters
    final String spaceId = pathParam(routingContext, SPACE_ID);
    final QueryParameterList queryParams = queryParamsFromRequest(routingContext);
    final String prefixId = extractSpecificParam(queryParams, PREFIX_ID);
    final List<String> addTags = extractSpecificParamList(queryParams, ADD_TAGS);
    final List<String> removeTags = extractSpecificParamList(queryParams, REMOVE_TAGS);

    // Validate parameters
    if (spaceId == null || spaceId.isEmpty()) {
      return verticle.sendErrorResponse(routingContext, XyzError.ILLEGAL_ARGUMENT, "Missing spaceId parameter");
    }

    // as applicable, modify features based on parameters supplied

    for (final XyzFeature feature : features) {
      feature.setIdPrefix(prefixId);
      addTagsToFeature(feature, addTags);
      removeTagsFromFeature(feature, removeTags);
    }

    final WriteXyzFeatures wrRequest = RequestHelper.createFeaturesRequest(spaceId, features);

    // Forward request to NH Space Storage writer instance
    final Result wrResult = executeWriteRequestFromSpaceStorage(wrRequest);
    // transform WriteResult to Http FeatureCollection response
    return transformWriteResultToXyzCollectionResponse(wrResult, XyzFeature.class);
  }

  private @NotNull XyzResponse executeUpsertFeatures() throws Exception {
    // Deserialize input request
    final FeatureCollectionRequest collectionRequest = featuresFromRequestBody();
    final List<XyzFeature> features = (List<XyzFeature>) collectionRequest.getFeatures();
    if (features.isEmpty()) {
      return verticle.sendErrorResponse(routingContext, XyzError.ILLEGAL_ARGUMENT, "Can't update empty features");
    }

    // Parse API parameters
    final String spaceId = pathParam(routingContext, SPACE_ID);
    final QueryParameterList queryParams = queryParamsFromRequest(routingContext);
    final List<String> addTags = extractSpecificParamList(queryParams, ADD_TAGS);
    final List<String> removeTags = extractSpecificParamList(queryParams, REMOVE_TAGS);

    // Validate parameters
    if (spaceId == null || spaceId.isEmpty()) {
      return verticle.sendErrorResponse(routingContext, XyzError.ILLEGAL_ARGUMENT, "Missing spaceId parameter");
    }

    // as applicable, modify features based on parameters supplied
    for (final XyzFeature feature : features) {
      addTagsToFeature(feature, addTags);
      removeTagsFromFeature(feature, removeTags);
    }
    final WriteXyzFeatures wrRequest = RequestHelper.upsertFeaturesRequest(spaceId, features);

    // Forward request to NH Space Storage writer instance
    final Result wrResult = executeWriteRequestFromSpaceStorage(wrRequest);
    // transform WriteResult to Http FeatureCollection response
    return transformWriteResultToXyzCollectionResponse(wrResult, XyzFeature.class);
  }

  private @NotNull XyzResponse executeUpdateFeature() throws Exception {
    // Deserialize input request
    final XyzFeature feature = singleFeatureFromRequestBody();

    // Parse API parameters
    final String spaceId = pathParam(routingContext, SPACE_ID);
    final String featureId = pathParam(routingContext, FEATURE_ID);

    final QueryParameterList queryParams = queryParamsFromRequest(routingContext);
    final List<String> addTags = extractSpecificParamList(queryParams, ADD_TAGS);
    final List<String> removeTags = extractSpecificParamList(queryParams, REMOVE_TAGS);

    // Validate parameters
    if (spaceId == null || spaceId.isEmpty()) {
      return verticle.sendErrorResponse(routingContext, XyzError.ILLEGAL_ARGUMENT, "Missing spaceId parameter");
    }
    if (featureId == null || featureId.isEmpty()) {
      return verticle.sendErrorResponse(routingContext, XyzError.ILLEGAL_ARGUMENT, "Missing featureId parameter");
    }
    if (!featureId.equals(feature.getId())) {
      return verticle.sendErrorResponse(
          routingContext,
          XyzError.ILLEGAL_ARGUMENT,
          "URI path parameter featureId is not the same as id in feature request body.");
    }

    // as applicable, modify features based on parameters supplied
    addTagsToFeature(feature, addTags);
    removeTagsFromFeature(feature, removeTags);

    final WriteXyzFeatures wrRequest = RequestHelper.updateFeatureRequest(spaceId, feature);

    // Forward request to NH Space Storage writer instance
    final Result wrResult = executeWriteRequestFromSpaceStorage(wrRequest);
    // transform WriteResult to Http FeatureCollection response
    return transformWriteResultToXyzFeatureResponse(wrResult, XyzFeature.class);
  }

  private @NotNull XyzResponse executeDeleteFeatures() {
    // Deserialize input request
    final List<String> features = extractSpecificParamList(routingContext, FEATURE_IDS);
    if (features == null || features.isEmpty()) {
      return verticle.sendErrorResponse(
          routingContext, XyzError.ILLEGAL_ARGUMENT, "Missing feature id parameter");
    }

    // Parse API parameters
    final String spaceId = pathParam(routingContext, SPACE_ID);

    // Validate parameters
    if (spaceId == null || spaceId.isEmpty()) {
      return verticle.sendErrorResponse(routingContext, XyzError.ILLEGAL_ARGUMENT, "Missing spaceId parameter");
    }

    final WriteXyzFeatures wrRequest = RequestHelper.deleteFeaturesRequest(spaceId, features);

    // Forward request to NH Space Storage writer instance
    final Result wrResult = executeWriteRequestFromSpaceStorage(wrRequest);
    // transform WriteResult to Http FeatureCollection response
    return transformWriteResultToXyzCollectionResponse(wrResult, XyzFeature.class);
  }

  private @NotNull XyzResponse executeDeleteFeature() throws Exception {
    // Parse API parameters
    final String spaceId = pathParam(routingContext, SPACE_ID);
    final String featureId = pathParam(routingContext, FEATURE_ID);

    // Validate parameters
    if (spaceId == null || spaceId.isEmpty()) {
      return verticle.sendErrorResponse(routingContext, XyzError.ILLEGAL_ARGUMENT, "Missing spaceId parameter");
    }
    if (featureId == null || featureId.isEmpty()) {
      return verticle.sendErrorResponse(routingContext, XyzError.ILLEGAL_ARGUMENT, "Missing featureId parameter");
    }

    final WriteXyzFeatures wrRequest = RequestHelper.deleteFeatureRequest(spaceId, featureId);

    // Forward request to NH Space Storage writer instance
    final Result wrResult = executeWriteRequestFromSpaceStorage(wrRequest);
    // transform WriteResult to Http FeatureCollection response
    return transformWriteResultToXyzFeatureResponse(wrResult, XyzFeature.class);
  }

  private @NotNull FeatureCollectionRequest featuresFromRequestBody() throws JsonProcessingException {
    try (final Json json = Json.get()) {
      final String bodyJson = routingContext.body().asString();
      return json.reader(ViewDeserialize.User.class)
          .forType(FeatureCollectionRequest.class)
          .readValue(bodyJson);
    }
  }

  private @NotNull XyzFeature singleFeatureFromRequestBody() throws JsonProcessingException {
    try (final Json json = Json.get()) {
      final String bodyJson = routingContext.body().asString();
      return json.reader(ViewDeserialize.User.class)
          .forType(XyzFeature.class)
          .readValue(bodyJson);
    }
  }

  private void addTagsToFeature(XyzFeature feature, List<String> addTags) {
    if (addTags != null) {
      feature.getProperties().getXyzNamespace().addTags(addTags, true);
    }
  }

  private void removeTagsFromFeature(XyzFeature feature, List<String> removeTags) {
    if (removeTags != null) {
      feature.getProperties().getXyzNamespace().removeTags(removeTags, true);
    }
  }
}
