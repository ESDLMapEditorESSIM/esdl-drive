package nl.tno.esdl.esdldrive.edr;

import java.util.List;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.eclipse.emf.cdo.common.util.CDOResourceNodeNotFoundException;
import org.eclipse.microprofile.openapi.annotations.Operation;

import nl.tno.esdl.esdldrive.browser.BrowserController;
import nl.tno.esdl.esdldrive.browser.BrowserController.BrowseOperation;
import nl.tno.esdl.esdldrive.browser.BrowserNode;
import nl.tno.esdl.esdldrive.cdo.CDOManager;

@RequestScoped
@Path("/edr")
public class EDRResource {

	public static final String EDR_FOLDER_NAME = "Energy Data Repository";
	Logger log = Logger.getLogger(this.getClass().getName());
	
	@Inject
	CDOManager cdoManager;
	
	@GET
	@Path("/browse")
	@ApplicationScoped
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "browse the public EDR")
	public Response browsePublicEDR( 
    		@Context SecurityContext securityContext, 
    		@QueryParam("operation") @DefaultValue("none") BrowseOperation operation,
    		@QueryParam("id") String id) {
		
		
		
		if (!id.startsWith("/")) id = "/" + id;
		if (!id.toLowerCase().startsWith("/public")) {
			return Response.status(Status.UNAUTHORIZED).entity("{\"error\": \"Not a public folder\"}").build();
		}
		
		String path = "/" + EDR_FOLDER_NAME + id; 
		
		PublicUserInfo userInfo = new PublicUserInfo();
		
		switch(operation) {
			case get_node:
				try {
					List<BrowserNode> folderContents = cdoManager.getFolderContents(userInfo, path);
					JsonArrayBuilder arrayBuilder = BrowserController.buildFolderContents(folderContents);
					return Response.ok(arrayBuilder.build()).build();
				} catch (CDOResourceNodeNotFoundException e) {
					return Response.status(Status.NOT_FOUND).entity(getErrorAsJson("Not found: " + id)).build();
				}
			case get_content:
				break;
			default:
				return Response.status(Status.METHOD_NOT_ALLOWED)
						.entity(getErrorAsJson("Unsupported operation: " + operation))
						.build();
			
		}
		
		
		return Response.ok(operation).build();
	}
	
	private JsonObject getErrorAsJson(String reason) {
		return Json.createObjectBuilder()
				.add("error", reason)
				.build();
	}
}
