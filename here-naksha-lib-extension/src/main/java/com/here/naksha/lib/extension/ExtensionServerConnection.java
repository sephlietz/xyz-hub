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
package com.here.naksha.lib.extension;

import com.here.naksha.lib.core.IEventContext;
import com.here.naksha.lib.core.IEventHandler;
import com.here.naksha.lib.core.NakshaContext;
import com.here.naksha.lib.core.extension.NakshaExtSocket;
import com.here.naksha.lib.core.extension.messages.ExtensionMessage;
import com.here.naksha.lib.core.extension.messages.ProcessEventMsg;
import com.here.naksha.lib.core.extension.messages.ResponseMsg;
import com.here.naksha.lib.core.extension.messages.SendUpstreamMsg;
import com.here.naksha.lib.core.models.payload.Event;
import com.here.naksha.lib.core.models.payload.XyzResponse;
import com.here.naksha.lib.core.models.payload.responses.ErrorResponse;
import com.here.naksha.lib.core.models.payload.responses.XyzError;
import java.io.IOException;
import java.net.Socket;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

class ExtensionServerConnection extends Thread implements IEventContext {
  private final NakshaExtSocket nakshaExtSocket;

  ExtensionServerConnection(Socket socket) throws IOException {
    nakshaExtSocket = new NakshaExtSocket(socket);
    start();
  }

  @Override
  public void run() {
    final ExtensionMessage msg;
    try {
      msg = nakshaExtSocket.readMessage();
      if (msg instanceof ProcessEventMsg processEvent) {
        event = processEvent.event;
        NakshaContext.currentLogger().info(String.format("Handling event with streamID %s",event.getStreamId()));
        final IEventHandler handler =
            Objects.requireNonNull(processEvent.connector).newInstance();
        final XyzResponse response = handler.processEvent(this);
        nakshaExtSocket.sendMessage(new ResponseMsg(response));
      }
    } catch (Exception e) {
      NakshaContext.currentLogger().error(e.toString());
      e.printStackTrace();
    } finally {
      nakshaExtSocket.close();
    }
  }

  private Event event;

  /**
   * Returns the current event of the underlying pipeline.
   *
   * @return the current event of the underlying pipeline.
   */
  @Override
  public @NotNull Event getEvent() {
    return event;
  }

  /**
   * Replace the event in the pipeline, returning the old even.
   *
   * @param event The new event.
   * @return The previous event.
   */
  @Override
  public @NotNull Event setEvent(@NotNull Event event) {
    var oldEvent = this.event;
    this.event = event;
    return oldEvent;
  }

  /**
   * Send the event upstream to the next event handler. If no further handler is available, the default implementation at the end of each
   * pipeline will return a not implemented error response.
   *
   * @return the generated response.
   */
  @Override
  public @NotNull XyzResponse sendUpstream() {
    final SendUpstreamMsg sendUpstream = new SendUpstreamMsg(event);
    try {
      nakshaExtSocket.sendMessage(sendUpstream);
      final ExtensionMessage message = nakshaExtSocket.readMessage();
      if (message instanceof ResponseMsg responseMsg) {
        return responseMsg.response;
      }
      return new ErrorResponse()
          .withError(XyzError.EXCEPTION)
          .withErrorMessage("Received invalid message from Naksha.")
          .withStreamId(event.getStreamId());
    } catch (IOException e) {
      return new ErrorResponse()
          .withError(XyzError.EXCEPTION)
          .withErrorMessage("Received invalid message from Naksha.")
          .withStreamId(event.getStreamId());
    }
  }
}
