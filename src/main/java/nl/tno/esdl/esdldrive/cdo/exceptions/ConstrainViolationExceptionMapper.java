package nl.tno.esdl.esdldrive.cdo.exceptions;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;


@Provider
public class ConstrainViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException>{
	/**
	 * Called when validation of Java Beans fails
	 * See e.g. {@see EnergyDataDescriptionJSON} 
	 */

    @Override
    public Response toResponse(ConstraintViolationException e) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(prepareMessage(e))
                .build();
    }
    
    
    private String prepareMessage(ConstraintViolationException exception) {
        String msg = "";
        for (ConstraintViolation<?> cv : exception.getConstraintViolations()) {
            msg+=cv.getPropertyPath()+" "+cv.getMessage()+"\n";
        }
        return msg;
    }

}