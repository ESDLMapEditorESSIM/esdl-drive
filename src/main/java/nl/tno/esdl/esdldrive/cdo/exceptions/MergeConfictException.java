package nl.tno.esdl.esdldrive.cdo.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

public class MergeConfictException extends WebApplicationException {
	private static final long serialVersionUID = 1L;
	
	public MergeConfictException(String reason) {
		super(reason, Status.CONFLICT);
	}
	
}
