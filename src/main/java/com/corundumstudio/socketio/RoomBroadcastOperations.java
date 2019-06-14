/**
 * Copyright 2012 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio;

import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.store.StoreFactory;
import com.corundumstudio.socketio.store.pubsub.DispatchMessage;
import com.corundumstudio.socketio.store.pubsub.PubSubType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is a fix for the default socket.io room broadcast operations. This fixes
 * the send function from sending the message to everyone that is in any room that
 * the client is in, essentially it actually uses the room instead of ignoring it.
 */
public class RoomBroadcastOperations extends BroadcastOperations {
  private final List<String> rooms;
  private final StoreFactory storeFactory;

  public RoomBroadcastOperations(SocketIOServer server, StoreFactory storeFactory, String... rooms) {
    super(
      Arrays.stream(rooms)
        .map(room -> server.getRoomOperations(room)
          .getClients().toArray(new SocketIOClient[0]))
        .flatMap(Arrays::stream)
        .distinct()
        .collect(Collectors.toList()),
      storeFactory
    );

    this.storeFactory = storeFactory;
    this.rooms = Arrays.asList(rooms);
  }

  @Override
  public void send(Packet packet) {
    getClients().forEach(c -> c.send(packet));

    // Do it only to the specific rooms, and not all of the rooms that the user is in,
    // like the default implementation does.
    rooms.forEach(room ->
      storeFactory.pubSubStore().publish(
        PubSubType.DISPATCH,
        new DispatchMessage(room, packet, "")
      ));
  }
}
