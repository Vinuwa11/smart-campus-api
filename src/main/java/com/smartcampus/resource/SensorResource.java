package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("/api/v1/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    @GET
    public Response getSensors(@QueryParam("type") String type) {
        Collection<Sensor> all = store.getSensors().values();
        if (type != null && !type.trim().isEmpty()) {
            List<Sensor> filtered = new ArrayList<Sensor>();
            for (Sensor s : all) {
                if (s.getType().equalsIgnoreCase(type)) filtered.add(s);
            }
            return Response.ok(filtered).build();
        }
        return Response.ok(all).build();
    }

    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().trim().isEmpty()) {
            Map<String, String> err = new HashMap<String, String>();
            err.put("error", "Sensor ID is required.");
            return Response.status(400).entity(err).build();
        }
        if (store.getSensors().containsKey(sensor.getId())) {
            Map<String, String> err = new HashMap<String, String>();
            err.put("error", "Sensor '" + sensor.getId() + "' already exists.");
            return Response.status(409).entity(err).build();
        }
        if (sensor.getRoomId() == null || !store.getRooms().containsKey(sensor.getRoomId())) {
            Map<String, Object> err = new HashMap<String, Object>();
            err.put("status", 422);
            err.put("error", "Unprocessable Entity");
            err.put("message", "Cannot register sensor: roomId '" + sensor.getRoomId() +
                    "' does not exist in the system.");
            return Response.status(422).entity(err).build();
        }
        if (sensor.getStatus() == null) sensor.setStatus("ACTIVE");
        store.getSensors().put(sensor.getId(), sensor);
        store.getSensorReadings().put(sensor.getId(), new ArrayList<SensorReading>());
        store.getRooms().get(sensor.getRoomId()).getSensorIds().add(sensor.getId());
        return Response.status(201).entity(sensor).build();
    }

    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            Map<String, String> err = new HashMap<String, String>();
            err.put("error", "Sensor '" + sensorId + "' not found.");
            return Response.status(404).entity(err).build();
        }
        return Response.ok(sensor).build();
    }

    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().remove(sensorId);
        if (sensor != null) {
            Room room = store.getRooms().get(sensor.getRoomId());
            if (room != null) room.getSensorIds().remove(sensorId);
            store.getSensorReadings().remove(sensorId);
        }
        return Response.noContent().build();
    }

    @PUT
    @Path("/{sensorId}")
    public Response updateSensor(@PathParam("sensorId") String sensorId, Sensor updated) {
        Sensor existing = store.getSensors().get(sensorId);
        if (existing == null) {
            Map<String, String> err = new HashMap<String, String>();
            err.put("error", "Sensor '" + sensorId + "' not found.");
            return Response.status(404).entity(err).build();
        }
        if (updated.getType() != null) existing.setType(updated.getType());
        if (updated.getStatus() != null) existing.setStatus(updated.getStatus());
        existing.setCurrentValue(updated.getCurrentValue());
        return Response.ok(existing).build();
    }

    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
