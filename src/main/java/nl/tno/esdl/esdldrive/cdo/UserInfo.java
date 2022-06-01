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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.security.openidconnect.token.IdToken;

/**
 * Immutable class that creates user login info from a JWT token. Is created on every request, currently.
 * (should be added to the session)
 * @author werkmane
 *
 */
public class UserInfo {
	
	private static final String USER_NAME_CLAIM = "email";
	private static final String FULL_NAME_CLAIM = "name";
	private static final String USER_GROUP_PATH_CLAIM = "user_group_path";
	
	
	
	/**
	 * Associated Json token:<br/>
	 *  
	 *	 "resource_access": {
	 *	    "cdo-mondaine": {
	 *	      "roles": [
	 *	        "esdl-drive-admin", # can access everything
	 *			"edr-public-write"  # can write to edr public
	 *	      ]
	 *	    },
	 *	  } ...
	 * 
	 */
	private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
	private static final String CLIENT_ID = "cdo-mondaine";
	private static final String RESOURCE_ACCESS_ROLES_SUB_CLAIM = "roles";
	private static final String ESDL_DRIVE_ADMIN_ROLE = "esdl-drive-admin";
	private static final String ESDL_DRIVE_EDR_PUBLIC_WRITE_ROLE = "edr-public-write";
	
	/**
	 * List of folders that are always read-only<br/>
	 *   "esdl-drive-read-only": [
     *		"/Projects/Shared Models"
  	 *	 ]
	 */
	private static final String ESDL_DRIVE_READ_ONLY_CLAIM = "esdl-drive-read-only";
	
	/**
	 * List of folders that are writable for this user.<br/>
	 * "esdl-drive-write": [
     * 		"/Projects/development",
     *  	"/Projects/Shared Models"
  	 * ]
	 */
	private static final String ESDL_DRIVE_WRITE_CLAIM = "esdl-drive-write";
	
	/**
	 * Lists default paths that are always accessible without authentication
	 */
	private final static Set<String> predefined_read_only_paths = Set.of(AccessController.EDR_PUBLIC_FOLDER_PATH);
	
	private final String userId;
	private final String userName;
	private final String fullName;
	private final Set<String> keyCloackGroups; // is used to check if it is a configured folder in keycloak, that users cannot remove
	private final Set<String> writable_paths = new HashSet<String>();
	private final Set<String> read_only_paths = new HashSet<String>(predefined_read_only_paths);
	private final boolean admin;
	private final boolean edrPublicWrite;
	private final boolean publicUser;
	
	/**
	 * 
	 * Access to folders is based on data in the JWT token. <br/>
	 * Basic rule: Group membership means R+W access to that folder. Group membership is stored in the 'group_path' claim.<br/>
	 * 1. If the user is added to a group in Keycloak, it has automatically write access to that group. 
	 * 	  ESDL Drive makes sure these folders exists when setting up a session to the Drive.<br/>
	 * 2. Groups in Keycloak can have the attribute of 'esdl-drive-read-only' with the value of the full path of that group (e.g. /Projects/Shared_read_only_folder).
	 * 	  If a user is member of this group, it's write-access is revoked. This way it is possible to create read-only folders in Keycloak (albeit a bit hacky, ACLs would be nicer)<br/>
	 * 3. A user can have additional write access to a folder (e.g. for disabling the 'esdl-drive-read-only' claim) by setting the user attribute 'esdl-drive-write'.
	 *    This gives the user direct write access to that folder (even without being a group member!) and makes it possible to give
	 *    some users write access to a folder and others not.<br/>
	 *    
	 * 	  To add multiple folders to a users' "esdl-drive-write" attribute separate the folder paths by '##' (keycloak's way of doing this).
	 * 
	 *  <p>
	 *  The idea is that option 2 does not happen a lot, and that option 3 is only for exceptional circumstances.
	 *  </p>
	 *    
	 * 
	 * @param jwtToken
	 * @throws InvalidLoginInformationException
	 */
	
	@SuppressWarnings("unchecked")
	public UserInfo(IdToken jwtToken) throws InvalidLoginInformationException {
		try {
			if (jwtToken == null)
				throw new InvalidLoginInformationException("No Bearer token supplied");
			this.userId = jwtToken.getSubject();
			this.userName = (String)jwtToken.getClaim(USER_NAME_CLAIM);
			this.fullName = (String)jwtToken.getClaim(FULL_NAME_CLAIM); 
			this.admin  = userHasESDLDriveRole(jwtToken, ESDL_DRIVE_ADMIN_ROLE);
			this.publicUser = false;
			this.edrPublicWrite = userHasESDLDriveRole(jwtToken, ESDL_DRIVE_EDR_PUBLIC_WRITE_ROLE);
			
			// add personal folder to writable paths
			writable_paths.add(AccessController.PERSONAL_FOLDER_PATH + this.userName);
			// give logged in users write access to /Public/
			writable_paths.add(AccessController.PUBLIC_FOLDER_PATH); 
			
			if (jwtToken.getClaim(USER_GROUP_PATH_CLAIM) != null && jwtToken.getClaim(USER_GROUP_PATH_CLAIM) instanceof List) {
				writable_paths.addAll((List<String>)jwtToken.getClaim(USER_GROUP_PATH_CLAIM));
				keyCloackGroups = Collections.unmodifiableSet(writable_paths);
			} else {
				throw new InvalidLoginInformationException(USER_GROUP_PATH_CLAIM + " is not present in the JWT token");
			}
					
			// check for read-only folders
			// if RO folder is also in writable_path: remove it, as default is R+W, but this claim can remove the +W from it
			if (jwtToken.getClaim(ESDL_DRIVE_READ_ONLY_CLAIM) != null && jwtToken.getClaim(ESDL_DRIVE_READ_ONLY_CLAIM) instanceof List) {
				read_only_paths.addAll((List<String>)jwtToken.getClaim(ESDL_DRIVE_READ_ONLY_CLAIM));
				//System.out.println("Detected read-only paths: " + read_only_paths);
			}
			
			for (String ro_path: read_only_paths) {
				if (writable_paths.contains(ro_path)) {
					writable_paths.remove(ro_path);
					//System.out.println("removing ro-path from write paths " + ro_path);
				}
			}
			// if subsequently Write access is granted setting the user's attribute esdl-drive-write to a folder path, 
			// add this to the writable_paths
			if (jwtToken.getClaim(ESDL_DRIVE_WRITE_CLAIM) != null && jwtToken.getClaim(ESDL_DRIVE_WRITE_CLAIM) instanceof List) {
				for (String rw_path : (List<String>)jwtToken.getClaim(ESDL_DRIVE_WRITE_CLAIM)) {
					if (read_only_paths.contains(rw_path)) {
						read_only_paths.remove(rw_path);
						//System.out.println("removing " + rw_path + " from RO paths, as user has write attribite");
					}
					writable_paths.add(rw_path);
					//System.out.println("Adding additional write path: " + rw_path);
				}
			}
			
			
			Iterator<String> iterator = writable_paths.iterator();
			// clean up empty paths (with bad configuration in keycloak)
			while (iterator.hasNext()) {
				String path = iterator.next();
				if (path.isEmpty()) iterator.remove();
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new InvalidLoginInformationException(e);
		}

	}
	
	protected UserInfo() {
		this.admin = false;
		this.userId = "public";
		this.userName = "publicUser";
		this.fullName = "Public User";
		this.publicUser = true;
		this.edrPublicWrite = false;
		// ESDL Drive public folder is writable by everybody, e.g. to share stuff if you have a login. Add folder without trailing '/'
		//this.writable_paths.add(AccessController.PUBLIC_FOLDER_PATH); // means also read-access
		this.read_only_paths.add(AccessController.PUBLIC_FOLDER_PATH);
		this.keyCloackGroups = Collections.unmodifiableSet(writable_paths);
	}
	
	
	
	/**
	 * Parses the following jwt json:
	 *    "resource_access": {
	 *	    "cdo-mondaine": {
	 *	      "roles": [
	 *	        "esdl-drive-admin",
	 *			"edr-public_write"
	 *	      ]
	 *	    },
	 *	  } ...
	 * 
	 * 
	 * @param jwtToken
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean userHasESDLDriveRole(IdToken jwtToken, String role) {
		Map<String, Object> resource_access = (Map<String, Object>)jwtToken.getClaim(RESOURCE_ACCESS_CLAIM);
		if (resource_access != null) {
			Map<String, Object> mondaine_cdo_client_claims = (Map<String, Object>)resource_access.get(CLIENT_ID);
			if (mondaine_cdo_client_claims != null) {
				List<String> mondaine_cdo_client_roles = (List<String>)mondaine_cdo_client_claims.get(RESOURCE_ACCESS_ROLES_SUB_CLAIM);
				if (mondaine_cdo_client_roles != null) { 
					return mondaine_cdo_client_roles.contains(role);
				}
			}
				
		}
		return false;
	}

	@Override
	public String toString() {
		return "UserInfo(userName=" + userName + ", fullName=" + fullName + ", write_folders=" + writable_paths + 
				", readonly_folders=" + read_only_paths + ", isAdmin=" + admin + ", edrPublicWrite=" + 
				edrPublicWrite + ", publicUser=" + publicUser + ", userId=" + userId + ")";
	}

	public String getUserName() {
		return userName;
	}

	public String getFullName() {
		return fullName;
	}

	public Set<String> getReadOnlyFolders() {
		return read_only_paths;
	}
	
	public Set<String> getReadWriteFolders() {
		return writable_paths;
	}

	public Set<String> getKeyCloakGroups() {
		return keyCloackGroups;
	}

	public String getUserId() {
		return userId;
	}
	
	public boolean isAdmin() {
		return admin;
	}

	public boolean hasPublicEDRWriteAccess() {
		return edrPublicWrite;
	}
	
	public boolean isPublic() {
		return publicUser;
	}

	
	public class InvalidLoginInformationException extends Exception {
		private static final long serialVersionUID = 1L;
		public InvalidLoginInformationException(Exception ex) {
			super(ex);
		}
		
		public InvalidLoginInformationException(String reason) {
			super(reason);
		}
	}

}
