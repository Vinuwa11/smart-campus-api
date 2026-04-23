package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final DataStore store = DataStore.getInstance();
    private final String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public Response getReadings() {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            Map<String, String> err = new HashMap<String, String>();
            err.put("error", "Sensor '" + sensorId + "' not found.");
            return Response.status(404).entity(err).build();
        }
        List<SensorReading> readings = store.getSensorReadings().get(sensorId);
        if (readings == null) readings = new ArrayList<SensorReading>();
        return Response.ok(readings).build();
    }

    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            Map<String, String> err = new HashMap<String, String>();
            err.put("error", "Sensor '" + sensorId + "' not found.");
            return Response.status(404).entity(err).build();
        }
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            Map<String, Object> err = new HashMap<String, Object>();
            err.put("status", 403);
            err.put("error", "Forbidden");
            err.put("message", "Sensor '" + sensorId + "' is currently in MAINTENANCE " +
                    "and cannot accept new readings.");
            return Response.status(403).entity(err).build();
        }
        if (reading.getId() == null || reading.getId().trim().isEmpty()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }
        List<SensorReading> list = store.getSensorReadings().get(sensorId);
        if (list == null) {
            list = new ArrayList<SensorReading>();
            store.getSensorReadings().put(sensorId, list);
        }
        list.add(reading);
        sensor.setCurrentValue(reading.getValue());
        return Response.status(201).entity(reading).build();
    }
}
