package nl.tno.esdl.esdldrive.cdo.exceptions;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ServerExceptionMapper implements ExceptionMapper<WebApplicationException> {
    @Override
    public Response toResponse(WebApplicationException exception) {
        String message = exception.getMessage();
        Response response = exception.getResponse();
        Status status = response.getStatusInfo().toEnum();

        JsonObject errorEntity = Json.createObjectBuilder()
		.add("error", message)
		.add("status", status.toString())
		.build();
        
        return Response.status(status)
          .entity(errorEntity)
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
}


