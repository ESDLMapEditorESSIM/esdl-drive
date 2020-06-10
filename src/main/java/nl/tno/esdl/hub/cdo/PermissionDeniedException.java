package nl.tno.esdl.hub.cdo;

public class PermissionDeniedException extends Exception {
	private static final long serialVersionUID = 300769009167117865L;

	public PermissionDeniedException(String reason) {
		super(reason);
	}
}
