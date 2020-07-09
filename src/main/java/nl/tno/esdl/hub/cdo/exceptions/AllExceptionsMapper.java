package nl.tno.esdl.hub.cdo.exceptions;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

//@Provider
public class AllExceptionsMapper implements ExceptionMapper<Exception>{

	@Override
	public Response toResponse(Exception exception) {
		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(exception.getMessage()).build();
	}
/*
 * @Provider
public class EntityNotFoundMapper
     implements ExceptionMapper<EntityNotFoundException> {

   public Response toResponse(EntityNotFoundException e) {
      return Response.status(Response.Status.NOT_FOUND).build();
   }
}
 * 
 */
}
