/**
 *  This work is based on original code developed and copyrighted by TNO 2020. 
 *  Subsequent contributions are licensed to you by the developers of such code and are
 *  made available to the Project under one or several contributor license agreements.
 *
 *  This work is licensed to you under the Apache License, Version 2.0.
 *  You may obtain a copy of the license at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Contributors:
 *      TNO         - Initial implementation
 *  Manager:
 *      TNO
 */

package nl.tno.esdl.esdldrive.cdo.exceptions;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;

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
