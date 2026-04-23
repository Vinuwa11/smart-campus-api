package com.smartcampus.exception;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Logger;

// ── 409 Conflict: room still has sensors ─────────────────────────────────────
@Provider
class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {
    @Override
    public Response toResponse(RoomNotEmptyException ex) {
        ErrorResponse body = new ErrorResponse(409, "Conflict",
                "Cannot delete room '" + ex.getRoomId() + "': it still has sensors assigned. " +
                "Reassign or remove all sensors before deleting this room.");
        return Response.status(409)
                .header("Content-Type", "application/json")
                .entity(body)
                .build();
    }
}

// ── 422 Unprocessable Entity ──────────────────────────────────────────────────
@Provider
class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {
    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        ErrorResponse body = new ErrorResponse(422, "Unprocessable Entity", ex.getMessage());
        return Response.status(422)
                .header("Content-Type", "application/json")
                .entity(body)
                .build();
    }
}

// ── 403 Forbidden: sensor in MAINTENANCE ─────────────────────────────────────
@Provider
class SensorUnavailableExceptionMapper
        implements ExceptionMapper<SensorUnavailableException> {
    @Override
    public Response toResponse(SensorUnavailableException ex) {
        ErrorResponse body = new ErrorResponse(403, "Forbidden", ex.getMessage());
        return Response.status(403)
                .header("Content-Type", "application/json")
                .entity(body)
                .build();
    }
}

// ── 404 Not Found ─────────────────────────────────────────────────────────────
@Provider
class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {
    @Override
    public Response toResponse(NotFoundException ex) {
        ErrorResponse body = new ErrorResponse(404, "Not Found",
                ex.getMessage() != null ? ex.getMessage() : "Resource not found.");
        return Response.status(404)
                .header("Content-Type", "application/json")
                .entity(body)
                .build();
    }
}

// ── WebApplicationException (catch Jersey built-in exceptions) ────────────────
@Provider
class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {
    @Override
    public Response toResponse(WebApplicationException ex) {
        int status = ex.getResponse().getStatus();
        ErrorResponse body = new ErrorResponse(status, "Error",
                ex.getMessage() != null ? ex.getMessage() : "Request failed.");
        return Response.status(status)
                .header("Content-Type", "application/json")
                .entity(body)
                .build();
    }
}

// ── 500 Global safety net ─────────────────────────────────────────────────────
@Provider
class GlobalExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {
        LOGGER.severe("Unexpected error: " + ex.getClass().getName() + " - " + ex.getMessage());
        ErrorResponse body = new ErrorResponse(500, "Internal Server Error",
                "An unexpected error occurred. Please contact the system administrator.");
        return Response.status(500)
                .header("Content-Type", "application/json")
                .entity(body)
                .build();
    }
}
