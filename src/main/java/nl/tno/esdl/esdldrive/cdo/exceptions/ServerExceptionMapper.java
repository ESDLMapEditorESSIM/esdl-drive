package nl.tno.esdl.esdldrive.cdo.exceptions;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

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


