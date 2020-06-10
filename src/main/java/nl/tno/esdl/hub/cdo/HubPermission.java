package nl.tno.esdl.hub.cdo;

public class HubPermission {
	private final String path;
	private final boolean writable;
	private final String userGroup;
	private final boolean accessible;
	public HubPermission(String path, String userGroup, boolean writable, boolean accessible) {
		this.path = path;
		this.userGroup = userGroup;
		this.writable = writable;
		this.accessible = accessible;
	}
	
	public String getPath() {
		return path;
	}
	public boolean isWritable() {
		return writable;
	}
	public String getUserGroup() {
		return userGroup;
	}
	public boolean isAccessible() {
		return accessible;
	}
}