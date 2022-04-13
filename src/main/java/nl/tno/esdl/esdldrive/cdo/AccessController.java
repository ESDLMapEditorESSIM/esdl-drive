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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ibm.websphere.security.openidconnect.token.IdToken;

import nl.tno.esdl.esdldrive.cdo.UserInfo.InvalidLoginInformationException;

/**
 * Defines access control mechanisms for the ESDL Drive
 * This is based on the Keycloack user_group_path claim, the groups a user belongs to.
 * 
 * @author werkmane
 *
 */
public class AccessController {
	
	
	public static final String PERSONAL_FOLDER_PREFIX = "/Users/";
	public static final String DRIVE_PUBLIC_FOLDER = "/Public";
	public static final String PUBLIC_FOLDER = DRIVE_PUBLIC_FOLDER + "/";
	public static final String EDR_FOLDER_NAME = "edr";
	public static final String EDR_PUBLIC_FOLDER_PATH = "/" + EDR_FOLDER_NAME + PUBLIC_FOLDER;
	
	// specific case for /edr/Public, if you have the role of edr-public-write then you can write to the EDR public folder
	public static List<String> readOnlyPaths = Arrays.asList(EDR_PUBLIC_FOLDER_PATH); 
	
	public static UserInfo userInfoFromToken(IdToken jwtToken) throws InvalidLoginInformationException {
		return new UserInfo(jwtToken);
	}
	
	
	public static DrivePermission getPermission(String path, UserInfo userInfo) {
		// Iterate over all groups and their access
		// Groups in the token provide read/write access
		// Groups in readOnlyPath only provide read-only access unless the user has edr-public-write role 
		// and the path equals /edr/Public (special case)
		Set<String> readOnlyFolders = userInfo.getReadOnlyFolders();
		Set<String> completeROfolderList = new HashSet<String>();
		completeROfolderList.addAll(readOnlyPaths);
		completeROfolderList.addAll(readOnlyFolders);
		for (String readOnlyPath: completeROfolderList) {
			if (path.startsWith(readOnlyPath)) {
				if (userInfo.hasPublicEDRWriteAccess() && path.startsWith(EDR_PUBLIC_FOLDER_PATH)) {  // /edr/Public/Test vs /edr/Public/
					// correct role to write to the EDR
					return new DrivePermission(path, readOnlyPath, true, true);
				} else {
					// make read only path only accessible, not writable
					return new DrivePermission(path, readOnlyPath, false, true);
				}
			}
		}

		Set<String> readWriteFolders = userInfo.getReadWriteFolders();
		for (String folder: readWriteFolders) {
			if (path.equals(folder) && userInfo.getKeyCloakGroups().contains(path)) {
				// path is same as folder, so this is a group defined in keycloak: no ability to remove this folder.
				return new DrivePermission(path, folder, true, true, false);
			} else if (path.startsWith(folder)) {
				return new DrivePermission(path, folder, true, true);
			} else if (folder.startsWith(path)) {
				// upper folders are not writable, but accessible
				return new DrivePermission(path, folder, false, true);
				
			}
		}
		return new DrivePermission(path, null, false, false);
	}
	
	
	/**
	 * Checks if the user represented by UserInfo has access to the folder represented by path.
	 * UserInfo.getGroups() is used to find out which groups a user can access
	 * @param path
	 * @param userInfo
	 * @return True if the user has access, False otherwise
	 */
	public static boolean canAccess(String path, UserInfo userInfo) {
		// path = /Users/ewoud.werkman@tno.nl/Vesta resultaten/vesta_output_warmtekeuze_per_buurt.esdl
		// path = /Projects/Mondaine/Gooi en Vechtstreek/Test_energy_system.esdl
		// path = /Organisations/TNO/MCS/Veluwe als PV park.esdl
		Set<String> accessGroups = userInfo.getReadWriteFolders();
		
		Set<String> allFolders = Stream.of(accessGroups, userInfo.getReadOnlyFolders())
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());
		
		for (String folder: allFolders) {
			//System.out.println("Checking " + path + " against " + group);
			if (path.startsWith(folder) || folder.startsWith(path)) {
				return true;
			}
		}
		
		return false;
	}

}
