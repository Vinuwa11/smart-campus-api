package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.model.Room;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("/api/v1/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    @GET
    public Response getAllRooms() {
        return Response.ok(store.getRooms().values()).build();
    }

    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().trim().isEmpty()) {
            Map<String, String> err = new HashMap<String, String>();
            err.put("error", "Room ID is required.");
            return Response.status(400).entity(err).build();
        }
        if (store.getRooms().containsKey(room.getId())) {
            Map<String, String> err = new HashMap<String, String>();
            err.put("error", "Room with ID '" + room.getId() + "' already exists.");
            return Response.status(409).entity(err).build();
        }
        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<String>());
        }
        store.getRooms().put(room.getId(), room);
        return Response.status(201).entity(room).build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            Map<String, String> err = new HashMap<String, String>();
            err.put("error", "Room '" + roomId + "' not found.");
            return Response.status(404).entity(err).build();
        }
        return Response.ok(room).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return Response.noContent().build();
        }
        if (!room.getSensorIds().isEmpty()) {
            // Return JSON 409 directly - no exception throwing
            Map<String, Object> err = new HashMap<String, Object>();
            err.put("status", 409);
            err.put("error", "Conflict");
            err.put("message", "Cannot delete room '" + roomId + "': it still has " +
                    room.getSensorIds().size() + " sensor(s) assigned. " +
                    "Remove all sensors before deleting this room.");
            return Response.status(409).entity(err).build();
        }
        store.getRooms().remove(roomId);
        return Response.noContent().build();
    }
}
