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

package nl.tno.esdl.esdldrive.cdo.revisions;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.emf.cdo.CDOObjectHistory;
import org.eclipse.emf.cdo.common.commit.CDOCommitInfo;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.ibm.websphere.security.openidconnect.PropagationHelper;

import nl.tno.esdl.esdldrive.cdo.AccessController;
import nl.tno.esdl.esdldrive.cdo.CDOManager;
import nl.tno.esdl.esdldrive.cdo.DrivePermission;
import nl.tno.esdl.esdldrive.cdo.UserInfo;
import nl.tno.esdl.esdldrive.cdo.UserInfo.InvalidLoginInformationException;


@RequestScoped
@Path("/revision")
public class CDORevisionResource {

	Logger log = Logger.getLogger(this.getClass().getName());

	@Inject
	CDOManager cdoManager;

	@Path("/{resourceURL:.+}")
	@RolesAllowed("cdo_read")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(description = "Lists the history and commits for a specific resource")
	@Tag(ref = "ESDL Drive")
	public Response listHistory(@PathParam("resourceURL") String resourceURL) {
		if (!resourceURL.startsWith("/")) {
			resourceURL = "/" + resourceURL;
		}
		UserInfo userInfo;
		try {
			userInfo = AccessController.userInfoFromToken(PropagationHelper.getIdToken());
		} catch (InvalidLoginInformationException e) {
			return Response.serverError().status(Status.NETWORK_AUTHENTICATION_REQUIRED).entity(e.getMessage()).build();
		}
		
		DrivePermission permission = AccessController.getPermission(resourceURL, userInfo);
		if (!permission.isAccessible()) {
			return Response.serverError().status(Status.FORBIDDEN).entity("Access denied").build();
		}
		
		
		log.info("listHistory() ResourceURL: " + resourceURL);
		JsonObject jsonObject = null;
		try {
			CDOResource cdoResource = cdoManager.getResource(userInfo, resourceURL);
			CDOObjectHistory cdoHistory = cdoResource.cdoHistory();
			//cdoHistory.getManager().addCommitInfoHandler(new TextCommitInfoLog(System.out));
			
			cdoHistory.triggerLoad();
			while(cdoHistory.isLoading()) {
				Thread.sleep(10);
			}
			
			JsonObjectBuilder returnObject = Json.createObjectBuilder();
			JsonArrayBuilder array = Json.createArrayBuilder();
			returnObject.add("path", cdoResource.getPath());
			returnObject.add("version", cdoResource.cdoRevision().getVersion());
			
			for (CDOCommitInfo info: cdoHistory.getElements()) {
				//System.out.println(info);
				JsonObjectBuilder commit = Json.createObjectBuilder();
				commit
					.add("user", info.getUserID()==null?"Unknown user":info.getUserID())
					.add("message", info.getComment()==null?"No commit message":info.getComment())
					.add("timestamp", info.getTimeStamp())
					.add("local_time", LocalDateTime.ofInstant(Instant.ofEpochMilli(info.getTimeStamp()), ZoneId.systemDefault()).toString())
					.add("branch", info.getBranch()==null?"":info.getBranch().getName());
				array.add(commit);
				
			}
			returnObject.add("commits", array);
			jsonObject = returnObject.build();
			
			//resourceResult.close();
			cdoResource.cdoView().close();
			
		} catch(Exception e) {
			log.severe("Error: " + e);
			//e.printStackTrace();
			return Response.status(Status.NOT_FOUND).entity("Failed to find the resource:" + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		}
		
		return Response.ok(jsonObject).build();
	}
	
}
