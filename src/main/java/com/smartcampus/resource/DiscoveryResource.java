package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;


@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> meta = new LinkedHashMap<String, Object>();
        meta.put("api", "Smart Campus Sensor & Room Management API");
        meta.put("version", "1.0");
        meta.put("description", "RESTful API for managing campus rooms and IoT sensors.");
        meta.put("contact", "admin@smartcampus.westminster.ac.uk");

        Map<String, String> links = new LinkedHashMap<String, String>();
        links.put("self", "/api/v1");
        links.put("rooms", "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");

        meta.put("resources", links);
        return Response.ok(meta).build();
    }

    
    @GET
    @Path("/api/v1/info")
    public Response info() {
        return discover();
    }
}
