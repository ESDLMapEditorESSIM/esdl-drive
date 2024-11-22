package nl.tno.esdl.esdldrive.cdo.exceptions;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

public class MergeConfictException extends WebApplicationException {
	private static final long serialVersionUID = 1L;
	
	public MergeConfictException(String reason) {
		super(reason, Status.CONFLICT);
	}
	
}
