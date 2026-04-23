package com.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    public static final String BASE_URI = "http://0.0.0.0:8080/";

    public static HttpServer startServer() {
        final ResourceConfig rc = new ResourceConfig()
                .packages(
                    "com.smartcampus.resource",
                    "com.smartcampus.exception",
                    "com.smartcampus.filter"
                )
                .register(JacksonFeature.class);
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }

    public static void main(String[] args) throws IOException {
        final HttpServer server = startServer();
        LOGGER.info("================================================");
        LOGGER.info("Smart Campus API started.");
        LOGGER.info("Discovery: http://localhost:8080/api/v1");
        LOGGER.info("Rooms:     http://localhost:8080/api/v1/rooms");
        LOGGER.info("Sensors:   http://localhost:8080/api/v1/sensors");
        LOGGER.info("Press ENTER to stop the server.");
        LOGGER.info("================================================");
        System.in.read();
        server.shutdownNow();
    }
}
