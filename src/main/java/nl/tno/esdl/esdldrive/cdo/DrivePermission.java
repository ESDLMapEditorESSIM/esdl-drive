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

package nl.tno.esdl.esdldrive.cdo;

public class DrivePermission {
	private final String path;
	private final boolean writable;
	private final String userGroup;
	private final boolean accessible;
	
	// 'deletable' is used for folders that are keycloak groups:
	// you can create subfolders and files, but not delete the folder
	// so these folders are writable, but cannot be deleted or renamed
	private final boolean deletable;
	
	public DrivePermission(String path, String userGroup, boolean writable, boolean accessible) {
		this.path = path;
		this.userGroup = userGroup;
		this.writable = writable;
		this.accessible = accessible;
		this.deletable = writable;
	}

	public DrivePermission(String path, String userGroup, boolean writable, boolean accessible, boolean deletable) {
		this.path = path;
		this.userGroup = userGroup;
		this.writable = writable;
		this.accessible = accessible;
		this.deletable = deletable;
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
	
	/**
	 * @return true if this folder can be deleted (false for keycloak groups)
	 */
	public boolean isDeletable() {
		return deletable;
	}

	@Override
	public String toString() {
		return "DrivePermission [path=" + path + ", writable=" + writable + ", userGroup=" + userGroup + ", accessible="
				+ accessible + "]";
	}
	
	
}