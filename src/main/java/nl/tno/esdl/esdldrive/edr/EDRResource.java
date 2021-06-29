package nl.tno.esdl.esdldrive.edr;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.jms.IllegalStateException;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.eclipse.emf.cdo.common.commit.CDOCommitInfo;
import org.eclipse.emf.cdo.common.util.CDOResourceNodeNotFoundException;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.util.CommitException;
import org.eclipse.emf.cdo.util.ConcurrentAccessException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameters;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import com.ibm.websphere.security.openidconnect.PropagationHelper;

import nl.tno.edr.EnergyDataDescription;
import nl.tno.esdl.esdldrive.browser.BrowserController;
import nl.tno.esdl.esdldrive.browser.BrowserController.BrowseOperation;
import nl.tno.esdl.esdldrive.browser.BrowserNode;
import nl.tno.esdl.esdldrive.cdo.AccessController;
import nl.tno.esdl.esdldrive.cdo.CDOManager;
import nl.tno.esdl.esdldrive.cdo.ResourceResult;
import nl.tno.esdl.esdldrive.cdo.UserInfo;
import nl.tno.esdl.esdldrive.cdo.UserInfo.InvalidLoginInformationException;
import nl.tno.esdl.esdldrive.cdo.exceptions.PermissionDeniedException;
import nl.tno.esdl.esdldrive.edr.query.EDRQueryResult;

@RequestScoped
@Path("/edr")
public class EDRResource {
	
	public static final String EDR_FOLDER_NAME = "edr";
	public static final String EDR_PUBLIC_FOLDER_PATH = "/" + EDR_FOLDER_NAME + UserInfo.PUBLIC_FOLDER;
	Logger log = Logger.getLogger(this.getClass().getName());
	
	@Inject
	CDOManager cdoManager;
		
	@GET
	@Path("/browse")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Browse the public EDR")
	@Parameters({
    	@Parameter(allowEmptyValue = false, name = "operation", in = ParameterIn.QUERY, description = ">The operation to execute\nOperation:\\n * `get_node` - Gets a listing of a folder \\n * `get_content` - Get a content description of a file", 
    			schema = @Schema(enumeration = {"get_node", "move_node", "copy_node", "delete_node", "create_node", "rename_node", "get_content", "get_revisions"}, 
    			description = "Operation:\n * `get_node` - Gets a listing of a folder \n * `get_content` - Get a content description of a file"), 
    			example = "get_node"),
    	@Parameter(allowEmptyValue = false, name = "id", in = ParameterIn.QUERY, description = "The path or file to get information from", schema = @Schema(type = SchemaType.STRING), example= "/Public"),
    	@Parameter(allowEmptyValue = true, name = "text", in = ParameterIn.QUERY, description = "In case of renaming (rename_node), its new name", schema = @Schema(type = SchemaType.STRING)),
    	@Parameter(allowEmptyValue = true, name = "type", in = ParameterIn.QUERY, description = "In case of creating new nodes (create_node), the type of node (e.g. folder)", schema = @Schema(type = SchemaType.STRING)),
    	@Parameter(allowEmptyValue = true, name = "parent", in = ParameterIn.QUERY, description = "In case of copying or moving files or folders (copy_node or move_node), the path of the parent folder where it needs to move/copy", schema = @Schema(type = SchemaType.STRING))
    })
    @APIResponses({
    	@APIResponse(responseCode = "202", description = "JSON response for the query", content = @Content(mediaType = "application/json")),
		@APIResponse(responseCode = "400", description = "Insufficient access to browse the database"),
		@APIResponse(responseCode = "500", description = "Some internal server error")
    })
	 public Response browsePublicEDR( 
    		@Context SecurityContext securityContext, 
    		@QueryParam("operation") @DefaultValue("none") BrowseOperation operation,
    		@QueryParam("id") String id) {
		
		
		
		// allow only browsing from /public
		if (!id.startsWith("/")) id = "/" + id;
		if (!id.toLowerCase().startsWith("/public")) {
			return Response.status(Status.UNAUTHORIZED).entity(getErrorAsJson("Not a public folder")).build();
		}
		
		String path = "/" + EDR_FOLDER_NAME + id; 
		
		PublicUserInfo userInfo = new PublicUserInfo();
		System.out.println("Browse public EDR: " + path);
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
				try {
					Map<String, Object> contentSummary = cdoManager.getContentSummary(userInfo, path);
					JsonObjectBuilder contentBuilder = Json.createObjectBuilder();
					for (Entry<String, Object> entry: contentSummary.entrySet()) {
						BrowserController.convertToActualType(contentBuilder, entry.getKey(), entry.getValue());
					}
					JsonObject jsonObject = contentBuilder.build();
					//System.out.println(jsonObject);
					return Response.ok(jsonObject).build();
				} catch (PermissionDeniedException e) {
					return Response.serverError().status(Status.FORBIDDEN).entity(getErrorAsJson("Access denied")).build();
				} catch (CDOResourceNodeNotFoundException e) {
					return Response.serverError().status(Status.NOT_FOUND).entity(getErrorAsJson(id + "not found")).build();
				}
			default:
				return Response.status(Status.METHOD_NOT_ALLOWED)
						.entity(getErrorAsJson("Unsupported operation: " + operation))
						.build();
			
		}
	}
	
	@GET
	@Path("/{resourceURL:.+}")
	@Produces(MediaType.APPLICATION_XML)
	@Operation(summary="Retrieve a file from the public Energy Data Repository")
	@Parameters({
		@Parameter(allowEmptyValue = false, name = "resourceURL", in = ParameterIn.PATH, description = "The path and filename of the resource (URL-encoded if using spaces)", example = "/Public/ETM/ETM WindTurbine.esdl"),
	})
	public Response getPublicEDRESDL(@PathParam("resourceURL") String resourceURL) {
		String path = new String(resourceURL);
		if (!resourceURL.startsWith("/")) {
			path = "/" + resourceURL;
		}
		
		path = "/" + EDR_FOLDER_NAME + path;
		System.out.println("PUBLIC EDR Retrieving " + path);
		
		String cachedModel = cdoManager.getCachedModelXML(path);
		if (cachedModel != null /*&& revision == null && !nocache*/) {
			log.fine("PUBLIC getResource(): Retrieving model from cache");
			System.out.println("PUBLIC EDR getResource(): Retrieving model from cache: " + path);
			return Response.ok(cachedModel).build();
		}
		try {
			ResourceResult resourceResult = cdoManager.getPublicResource(path);
			CDOResource cdoResource = resourceResult.getCDOResource();
			String xmlString = cdoManager.convertToXMLString(cdoResource);
			//log.info("Retrieved: " + xmlString);
			if (xmlString == null) {
				return Response.status(Status.NOT_FOUND).entity("Failed to find the resource").type(MediaType.TEXT_PLAIN).build();
			}
			resourceResult.getView().getSession().close();
			
			// currently only cache the latest version - not revisions
			cdoManager.setCache(path, xmlString, cdoResource);
			Date lastModified = resourceResult.getLastModified();
			return Response.ok(xmlString).lastModified(lastModified).build();
		} catch (Exception e) {
			log.severe("Error: " + e);
			e.printStackTrace();
			return Response.status(Status.NOT_FOUND).entity("Failed to find the resource at " + resourceURL).type(MediaType.TEXT_PLAIN).build();
		}
	}
	
	@GET
	@Path("/search")
	@Operation(summary="Search for a certain ESDL type in the public Energy Data Repository", deprecated = true)
	@Produces(MediaType.APPLICATION_JSON)
	@Deprecated	
	public Response search(@QueryParam("type") SearchESDLType type) {
		
		PublicUserInfo userInfo = new PublicUserInfo();
		
		List<EDRQueryResult> result = cdoManager.query(type, userInfo);
				
		return Response.ok(result).build();
	}
	
	
	@GET
	@Path("/query")
	@Operation(summary="Query EnergyData Descriptions in the database")
	@Parameters({
		@Parameter(name = "path", in = ParameterIn.QUERY, description = "Retrieve a specific EDD based on the path of the description (id field), all filters are ignored"),
		@Parameter(name = "title", in = ParameterIn.QUERY, description = "Filter based on title"),
		@Parameter(name = "tag", in = ParameterIn.QUERY, description = "Filter based on a tag"),
		@Parameter(name = "esdlType", in = ParameterIn.QUERY, description = "Filter based on the ESDL type contained in the Energy Data Description"),
		@Parameter(name = "addImageData", in = ParameterIn.QUERY, description = "Add image data to the result, if available", schema = @Schema(defaultValue = "false", implementation = boolean.class)),
		@Parameter(name = "addESDL", in = ParameterIn.QUERY, description = "Add ESDL content to the result", schema = @Schema(defaultValue = "false", implementation = boolean.class)),
		@Parameter(name = "maxResults", in = ParameterIn.QUERY, description = "Amount of results to return, default 25, a value of -1 removes this limit", schema = @Schema(defaultValue = "25", minimum = "-1", implementation = int.class)),
		@Parameter(name = "offset", in = ParameterIn.QUERY, description = "If more than 25 results, use offset to start at the 26th result for the next 25 results", schema = @Schema(minimum = "0", implementation = int.class)),
		@Parameter(name = "order", in = ParameterIn.QUERY, description = "Sort order, defaults to the order the descriptions are saved, ASC means ascending order on Title of the description", schema = @Schema(defaultValue = "DEFAULT", implementation = SortOrder.class)),
	})
	@APIResponses({
		@APIResponse(description = "Returns zero or more EnergyDataDescriptions", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EnergyDataDescriptionJSON.class)))
	})
	
	@Produces(MediaType.APPLICATION_JSON)
	public Response query(
			@QueryParam("path") String path, 
			@QueryParam("title") String title, 
			@QueryParam("tag") String tag,  
			@QueryParam("esdlType") String esdlType,
			@QueryParam("addImageData") @DefaultValue("false") final Boolean addImageData,
			@QueryParam("addESDL") @DefaultValue("false") final Boolean addESDL,
			@QueryParam("maxResults") @DefaultValue("25") int maxResults, 
			@QueryParam("offset") @DefaultValue("0") int offset,
			@QueryParam("order") @DefaultValue("DEFAULT") SortOrder order) {
		PublicUserInfo userInfo = new PublicUserInfo();
		
		if (path != null && !path.isEmpty()) {
			if (!path.startsWith("/")) path = "/" + path;
			try {
				ResourceResult publicResource = cdoManager.getPublicResource(path);
				CDOResource cdoResource = publicResource.getCDOResource();
				EObject eObject = cdoResource.getContents().get(0);
				if (eObject instanceof EnergyDataDescription) {
					EnergyDataDescription energyDataDescription = (EnergyDataDescription) eObject;
					EnergyDataDescriptionJSON dataDescriptionJSON = EnergyDataDescriptionJSON.fromEDD(energyDataDescription, addImageData, addESDL);
					return Response.ok(dataDescriptionJSON).build();
				} else {
					return Response.serverError().entity(getErrorAsJson("Not an EnergyDataDescription")).build();
				}
				
			} catch (PermissionDeniedException e) {
				log.severe(e.getMessage());
				e.printStackTrace();
			}
			return Response.status(Status.BAD_REQUEST).entity(getErrorAsJson("Cannot get EDD from path " + path)).build();
		} else {
			
			HashMap<String, String> filter = new HashMap<>();
			if (title != null && !title.isEmpty()) filter.put("title", title);
			if (tag != null && !tag.isEmpty()) filter.put("tags", tag);
			if (maxResults >= 0) filter.put("limit", String.valueOf(maxResults));
			if (offset > 0) filter.put("offset", String.valueOf(offset));
			if (order != null && order != SortOrder.DEFAULT) filter.put("order", order.name());
			if (esdlType != null && !esdlType.isEmpty()) filter.put("esdlType", esdlType);
			System.out.println("filter: " + filter);
			List<EnergyDataDescription> queryEDD = cdoManager.queryEDD(userInfo, filter);
			System.out.println(queryEDD);
			List<EnergyDataDescriptionJSON> result = queryEDD.stream().map(edd -> EnergyDataDescriptionJSON.fromEDD(edd, addImageData, addESDL)).collect(Collectors.toList());
			
		
			return Response.ok(result).build();
		}
	}
	
	
	
	//@RolesAllowed("cdo_read")
	@POST
	@Path("/upload/{resourceURL:.+}")
	@Operation(summary="Upload a new Energy Data Description to the EDR")
	@Parameters({
		@Parameter(allowEmptyValue = false, name = "resourceURL", in = ParameterIn.PATH, description = "The path and filename of the resource (URL-encoded if using spaces) for upload", example = "/edr/Public/ETM/ETM WindTurbine.esdl"),
	})
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@APIResponses( {
		@APIResponse(responseCode = "201", description = "Upload successful, created resource at resourceURL"),
		@APIResponse(responseCode = "403", description = "Access denied, provide proper credentials")
	})
	public Response upload(EnergyDataDescriptionJSON eddJson, @PathParam("resourceURL") String resourceURL) {
		String path = new String(resourceURL);
		if (path.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity(getErrorAsJson("Missing path and filename of this EDD in EDR. Put it after the url /upload/<path to store>")).build();
		}
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		// handle security here
//		PublicUserInfo userInfo = new PublicUserInfo();
		
		String commitMessage = null; // TODO: fixme
		UserInfo userInfo;
		try {
			userInfo = AccessController.userInfoFromToken(PropagationHelper.getIdToken());
		} catch (InvalidLoginInformationException e) {
			return Response.status(Status.FORBIDDEN).entity(e.getMessage()).build();
		}
		System.out.println("EDR upload: " + resourceURL);
		
		try {
			EnergyDataDescription edd = eddJson.toEDD();
			Resource resource;
			resource = new XMLResourceImpl();
			resource.getContents().add(edd);
			CDOCommitInfo commitInfo = cdoManager.storeResource(userInfo, path, resource, commitMessage, false);
			System.out.println(commitInfo);
		} catch (IOException e) {
			log.severe(e.getMessage());
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).entity(getErrorAsJson("Error parsing input:" + e.getMessage())).build();
		} catch (ConcurrentAccessException | IllegalStateException | TimeoutException |InterruptedException e) {
			log.severe(e.getMessage());
			e.printStackTrace();
			return Response.serverError().entity(getErrorAsJson("Internal server error" + e.getMessage())).build();
		} catch (CommitException e) {
			log.severe(e.getMessage());
			e.printStackTrace();
			return Response.serverError().entity(getErrorAsJson("Error storing to EDR:"+e.getMessage())).build();
		} catch (PermissionDeniedException e) {
			return Response.status(Status.FORBIDDEN).entity(getErrorAsJson("Access denied: " + e.getMessage())).build();
		}
		
		//return Response.ok().build();
		return Response.created(URI.create(path)).build();
	}
	
	
	
	private JsonObject getErrorAsJson(String reason) {
		return Json.createObjectBuilder()
				.add("error", reason)
				.build();
	}
}
