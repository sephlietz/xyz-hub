package com.here.xyz.hub.task.feature;

import com.here.xyz.events.feature.IterateFeaturesEvent;
import com.here.naksha.lib.core.exceptions.ParameterError;
import com.here.xyz.hub.rest.ApiResponseType;
import io.vertx.ext.web.RoutingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IterateFeaturesTask extends AbstractSearchForFeaturesTask<IterateFeaturesEvent> {

  public IterateFeaturesTask(@Nullable String streamId) {
    super(streamId);
  }

  @Override
  public @NotNull IterateFeaturesEvent createEvent() {
    return new IterateFeaturesEvent();
  }

  @Override
  public void initEventFromRoutingContext(@NotNull RoutingContext routingContext, @NotNull ApiResponseType responseType) throws ParameterError {
    super.initEventFromRoutingContext(routingContext, responseType);

    assert queryParameters != null;
    event.setForce2D(queryParameters.getForce2D());

    /*

     try {
      final boolean skipCache = Query.getBoolean(context, SKIP_CACHE, false);
      final boolean force2D = Query.getBoolean(context, FORCE_2D, false);
      Integer version = Query.getInteger(context, Query.VERSION, null);
      final SpaceContext spaceContext = getSpaceContext(context);
      final String author = Query.getString(context, Query.AUTHOR, null);

      List<String> sort = Query.getSort(context);
      PropertyQueryOr propertiesQuery = Query.getPropertiesQuery(context);
      String handle = Query.getString(context, Query.HANDLE, null);
      Integer[] part = Query.getPart(context);

      if (sort != null || propertiesQuery != null || part != null || (handle != null && handle.startsWith("h07~"))) {
        IterateFeaturesEvent event = new IterateFeaturesEvent();
        event.withLimit(getLimit(context))
            .withForce2D(force2D)
            .withTags(Query.getTags(context))
            .withPropertiesQuery(propertiesQuery)
            .withSelection(Query.getSelection(context))
            .withSort(sort)
            .withPart(part)
            .withHandle(handle)
            .withContext(spaceContext)
            .withAuthor(author);

        final SearchQuery task = new SearchQuery(event, context, ApiResponseType.FEATURE_COLLECTION, skipCache);
        task.execute(this::sendResponse, this::sendErrorResponse);
        return;
      }

      IterateFeaturesEvent event = new IterateFeaturesEvent()
          .withLimit(getLimit(context))
          .withForce2D(force2D)
          .withTags(Query.getTags(context))
          .withSelection(Query.getSelection(context))
          .withV(version)
          .withHandle(Query.getString(context, Query.HANDLE, null))
          .withContext(spaceContext);

      final IterateQuery task = new IterateQuery(event, context, ApiResponseType.FEATURE_COLLECTION, skipCache);
      task.execute(this::sendResponse, this::sendErrorResponse);
    } catch (HttpException e) {
      sendErrorResponse(context, e);
    }

     */

    // TODO: Do we need downward compatibility?
    // Integer version = Query.getInteger(context, Query.VERSION, null);
    // final SpaceContext spaceContext = getSpaceContext(context);
    // final String author = Query.getString(context, Query.AUTHOR, null);
  }
}