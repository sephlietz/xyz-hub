package com.here.xyz.hub.task.connector;

import com.here.xyz.INaksha;
import com.here.xyz.exceptions.ParameterError;
import com.here.xyz.hub.auth.JWTPayload;
import com.here.xyz.hub.auth.XyzHubActionMatrix;
import com.here.xyz.hub.events.GetConnectorsByIdEvent;
import com.here.xyz.hub.rest.ApiResponseType;
import com.here.xyz.models.geojson.implementation.FeatureCollection;
import com.here.xyz.models.hub.Connector;
import com.here.xyz.responses.XyzError;
import com.here.xyz.responses.XyzResponse;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GetConnectorsByIdTask extends AbstractConnectorTask<GetConnectorsByIdEvent> {

  public GetConnectorsByIdTask() {
    super(new GetConnectorsByIdEvent());
  }

  @Override
  public void initEventFromRoutingContext(@NotNull RoutingContext routingContext, @NotNull ApiResponseType responseType) throws ParameterError {
    super.initEventFromRoutingContext(routingContext, responseType);
    assert queryParameters != null;
    final List<@NotNull String> ids = queryParameters.getIds();
    if (ids != null) {
      event.ids.addAll(ids);
    }
  }

  private static boolean returnConnector(@NotNull Connector connector, @NotNull XyzHubActionMatrix rightsMatrix) {
    final XyzHubActionMatrix requestMatrix = new XyzHubActionMatrix();
    requestMatrix.readConnector(connector);
    return rightsMatrix.matches(requestMatrix);
  }

  @Override
  protected @NotNull XyzResponse execute() throws Throwable {
    final JWTPayload jwt = getJwt();
    if (jwt == null || jwt.anonymous) {
      return errorResponse(XyzError.UNAUTHORIZED, "Missing JWT token");
    }
    final XyzHubActionMatrix rightsMatrix = jwt.getXyzHubMatrix();
    if (rightsMatrix == null) {
      return errorResponse(XyzError.FORBIDDEN, "Missing access rights in JWT token");
    }
    final FeatureCollection collection = new FeatureCollection();
    final INaksha naksha = INaksha.instance.get();
    if (event.ids.size() == 0) {
      // The client wants to read all connectors it has access to.
      for (final @NotNull Connector connector : naksha.getConnectors()) {
        if (returnConnector(connector, rightsMatrix)) {
          collection.getFeatures().add(connector);
        }
      }
    } else {
      for (final @NotNull String id : event.ids) {
        final Connector connector = naksha.getConnectorById(id);
        if (connector != null && returnConnector(connector, rightsMatrix)) {
          collection.getFeatures().add(connector);
        }
      }
    }
    return collection;
  }
}
