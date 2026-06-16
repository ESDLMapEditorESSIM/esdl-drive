package nl.tno.esdl.esdldrive.edr;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.emf.cdo.common.commit.CDOCommitInfo;
import org.eclipse.emf.cdo.common.util.CDOResourceNodeNotFoundException;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.eresource.CDOResourceFolder;
import org.eclipse.emf.cdo.eresource.CDOResourceNode;
import org.eclipse.emf.cdo.eresource.EresourcePackage;
import org.eclipse.emf.cdo.util.CommitException;
import org.eclipse.emf.cdo.util.ConcurrentAccessException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClassifier;
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
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.ibm.websphere.security.openidconnect.PropagationHelper;

import edd.EddFactory;
import edd.EddPackage;
import edd.EnergyDataDescription;
import edd.ResourceMetaData;
import esdl.EsdlPackage;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
import nl.tno.esdl.esdldrive.browser.BrowserController;
import nl.tno.esdl.esdldrive.browser.BrowserController.BrowseOperation;
import nl.tno.esdl.esdldrive.browser.BrowserNode;
import nl.tno.esdl.esdldrive.cdo.AccessController;
import nl.tno.esdl.esdldrive.cdo.CDOManager;
import nl.tno.esdl.esdldrive.cdo.UserInfo;
import nl.tno.esdl.esdldrive.cdo.UserInfo.InvalidLoginInformationException;
import nl.tno.esdl.esdldrive.cdo.exceptions.PermissionDeniedException;
import nl.tno.esdl.esdldrive.edr.query.EDRQueryResult;
import nl.tno.esdl.esdldrive.util.JSONUtil;

@RequestScoped
@Path("/edr")
public class EDRResource {
	
	
	Logger log = Logger.getLogger(this.getClass().getName());
	
	@Inject
	CDOManager cdoManager;
	
	@Context 
	private HttpServletRequest request;
	
		
	@GET
	@Path("/browse")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Browse the public EDR")
	@Tag(ref = "EDR")
	@Parameters({
    	@Parameter(allowEmptyValue = false, name = "operation", in = ParameterIn.QUERY, description = "The operation to execute<br>Operation: <br> * `get_node` - Gets a listing of a folder <br> * `get_content` - Get a content description of a file", 
    			schema = @Schema(enumeration = {"get_node", "get_content"}, 
    			description = "Operation:\n * `get_node` - Gets a listing of a folder \n * `get_content` - Get a content description of a file"), 
    			example = "get_node"),
    	@Parameter(allowEmptyValue = true, name = "depth", in = ParameterIn.QUERY, description = "In case of listing folders (get_node), it will retrieve this amount of levels in the hierarchy. Default 1", schema = @Schema(type = SchemaType.STRING)),
    	@Parameter(allowEmptyValue = false, name = "id", in = ParameterIn.QUERY, description = "The path or file to get information from", schema = @Schema(type = SchemaType.STRING), example= "/edr/Public"),
    	//@Parameter(allowEmptyValue = true, name = "text", in = ParameterIn.QUERY, description = "In case of renaming (rename_node), its new name", schema = @Schema(type = SchemaType.STRING)),
    	//@Parameter(allowEmptyValue = true, name = "type", in = ParameterIn.QUERY, description = "In case of creating new nodes (create_node), the type of node (e.g. folder)", schema = @Schema(type = SchemaType.STRING)),
    	//@Parameter(allowEmptyValue = true, name = "parent", in = ParameterIn.QUERY, description = "In case of copying or moving files or folders (copy_node or move_node), the path of the parent folder where it needs to move/copy", schema = @Schema(type = SchemaType.STRING))
    })
    @APIResponses({
    	@APIResponse(responseCode = "202", description = "JSON response for the query", content = @Content(mediaType = "application/json")),
		@APIResponse(responseCode = "400", description = "Insufficient access to browse the database"),
		@APIResponse(responseCode = "500", description = "Some internal server error")
    })
	 public Response browsePublicEDR( 
    		@Context SecurityContext securityContext, 
    		@QueryParam("operation") @DefaultValue("none") BrowseOperation operation,
    		@QueryParam("depth") @DefaultValue("1") int depth,
    		@QueryParam("id") String id) {
		
		
		
		// allow only browsing from /edr/public
		if (!id.startsWith("/")) id = "/" + id;
		if (!id.toLowerCase().startsWith("/edr/public")) {
			return Response.status(Status.UNAUTHORIZED).entity(JSONUtil.getErrorAsJson("Not a public folder")).build();
		}		 
		String path = URI.decode(id);
		
		PublicUserInfo userInfo = new PublicUserInfo();
		System.out.println("Browse public EDR: " + path);
		switch(operation) {
			case get_node:
				try {
					List<BrowserNode> folderContents = cdoManager.getFolderContents(userInfo, path, depth);
					JsonArrayBuilder arrayBuilder = BrowserController.buildFolderContents(folderContents);
					return Response.ok(arrayBuilder.build()).build();
				} catch (CDOResourceNodeNotFoundException e) {
					return Response.status(Status.NOT_FOUND).entity(JSONUtil.getErrorAsJson("Not found: " + id)).build();
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
					return Response.serverError().status(Status.FORBIDDEN).entity(JSONUtil.getErrorAsJson("Access denied")).build();
				} catch (CDOResourceNodeNotFoundException e) {
					return Response.serverError().status(Status.NOT_FOUND).entity(JSONUtil.getErrorAsJson(id + "not found")).build();
				}
			default:
				return Response.status(Status.METHOD_NOT_ALLOWED)
						.entity(JSONUtil.getErrorAsJson("Unsupported operation: " + operation))
						.build();
			
		}
	}
	
	@GET
	@Path("/{resourceURL:.+}")
	@Produces(MediaType.APPLICATION_XML)
	@Operation(summary="Retrieve a file from the public Energy Data Repository")
	@Parameters({
		@Parameter(allowEmptyValue = false, name = "resourceURL", in = ParameterIn.PATH, description = "The path and filename of the resource (URL-encoded if using spaces)", example = "/edr/Public/Assets/ETM/ETM WindTurbine.edd"),
	})
	@Tag(ref = "EDR")
	public Response getPublicEDRESDL(@PathParam("resourceURL") String resourceURL) {
		String path = URI.decode(resourceURL);
		if (!path.startsWith("/")) {
			path = "/" + resourceURL;
		}
		// add /edr in front if resourceURL does not start with it (as endpoint is /store/edr
		if (!path.startsWith("/" + AccessController.EDR_FOLDER_NAME)) {
			path = "/" + AccessController.EDR_FOLDER_NAME + path;
		}
		
		PublicUserInfo userInfo = new PublicUserInfo();
		
		System.out.println("PUBLIC EDR Retrieving " + path);
		
		String cachedModel = cdoManager.getCachedModelXML(path);
		if (cachedModel != null /*&& revision == null && !nocache*/) {
			log.fine("PUBLIC getResource(): Retrieving model from cache");
			System.out.println("PUBLIC EDR getResource(): Retrieving model from cache: " + path);
			return Response.ok(cachedModel).build();
		}
		try {
			CDOResource cdoResource = cdoManager.getPublicResource(userInfo, path);
			String xmlString = cdoManager.convertToXMLString(cdoResource);
			//log.info("Retrieved: " + xmlString);
			if (xmlString == null) {
				return Response.status(Status.NOT_FOUND).entity("Failed to find the resource").type(MediaType.TEXT_PLAIN).build();
			}
			
			// currently only cache the latest version - not revisions
			cdoManager.setCache(path, xmlString, cdoResource);
			Date lastModified = new Date(cdoResource.cdoRevision(true).getTimeStamp());
			cdoResource.cdoView().close();
			return Response.ok(xmlString).lastModified(lastModified).build();
		} catch (Exception e) {
			log.severe("Error: " + e);
			e.printStackTrace();
			return Response.status(Status.NOT_FOUND).entity("Failed to find the resource at " + resourceURL).type(MediaType.TEXT_PLAIN).build();
		}
	}
	
	@GET
	@Path("/search")
	@Tag(ref = "EDR")
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
	@Tag(ref = "EDR")
	@Parameters({
		@Parameter(name = "path", in = ParameterIn.QUERY, description = "Retrieve a specific EDD based on the path of the description (id field), or filter on a specific path in the EDR (e.g. /edr/Public/Assets)"),
		@Parameter(name = "title", in = ParameterIn.QUERY, description = "Filter based on title"),
		@Parameter(name = "tag", in = ParameterIn.QUERY, description = "Filter based on tags, multiple tags can be specified for filtering using boolean OR (e.g. a union of EDDs that have these tags)"),
		@Parameter(name = "esdlType", in = ParameterIn.QUERY, description = "Filter based on the ESDL type contained in the Energy Data Description"),
		@Parameter(name = "addImageData", in = ParameterIn.QUERY, description = "Add image data to the result, if available", schema = @Schema(defaultValue = "false", implementation = boolean.class)),
		@Parameter(name = "addESDL", in = ParameterIn.QUERY, description = "Add ESDL content to the result", schema = @Schema(defaultValue = "false", implementation = boolean.class)),
		@Parameter(name = "maxResults", in = ParameterIn.QUERY, description = "Amount of results to return, default 25, a value of -1 removes this limit", schema = @Schema(defaultValue = "25", minimum = "-1", implementation = int.class)),
		@Parameter(name = "offset", in = ParameterIn.QUERY, description = "If more than 25 results, use offset to start at the 26th result for the next 25 results", schema = @Schema(minimum = "0", implementation = int.class)),
		@Parameter(name = "order", in = ParameterIn.QUERY, description = "Sort order, defaults to the order the descriptions are saved, ASC means ascending order on Title of the description", schema = @Schema(defaultValue = "DEFAULT", implementation = SortOrder.class)),
		@Parameter(name = "attributeName", in = ParameterIn.QUERY, description = "Filter based on a specific attribute name and value of the esdlType object in the Energy Data Description. Requires esdlType and attributeValue to be set too."),
		@Parameter(name = "attributeValue", in = ParameterIn.QUERY, description = "Filter based on a specific attributeName's value of the esdlType object in the Energy Data Description. Requires esdlType and attributeName to be set too. E.g. esdlType = Pipe, attributeName=diameter and attributeValue=esdl::PipeDiameterEnum::DN100"),
	})
	@APIResponses({
		@APIResponse(description = "Returns zero or more EnergyDataDescriptions in an array", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EnergyDataDescriptionJSON[].class)))
	})
	@Produces(MediaType.APPLICATION_JSON)
	public Response query(
			@QueryParam("path") String path, 
			@QueryParam("title") String title, 
			@QueryParam("tag") List<String> tag,  
			@QueryParam("esdlType") String esdlType,
			@QueryParam("addImageData") @DefaultValue("false") final Boolean addImageData,
			@QueryParam("addESDL") @DefaultValue("false") final Boolean addESDL,
			@QueryParam("addHistory") @DefaultValue("true") final Boolean addHistory,
			@QueryParam("maxResults") @DefaultValue("25") int maxResults, 
			@QueryParam("offset") @DefaultValue("0") int offset,
			@QueryParam("order") @DefaultValue("DEFAULT") SortOrder order,
			@QueryParam("attributeName") String attributeName,
			@QueryParam("attributeValue") String attributeValue) {
		PublicUserInfo userInfo = new PublicUserInfo();
		HashMap<String, String> filter = new HashMap<>();
		
		if (path != null && !path.isEmpty()) {
			path = URI.decode(path);
			if (!path.startsWith("/")) path = "/" + path;
			try {
				// TODO add ability to filter on path (add path to filter Map)
				CDOResourceNode resourceNode = cdoManager.getResourceNode(userInfo, path);
				if (resourceNode instanceof CDOResourceFolder) {
					// filter on folder
					boolean canAccess = AccessController.canAccess(path, userInfo);
					if (canAccess) {
						filter.put("path", path);
					} else {
						throw new PermissionDeniedException("Cannot access path " + path);
					}
				} else if (resourceNode instanceof CDOResource) {
	 				CDOResource cdoResource = cdoManager.getPublicResource(userInfo, path);
					EObject eObject = cdoResource.getContents().get(0);
					if (eObject instanceof EnergyDataDescription) {
						EnergyDataDescription energyDataDescription = (EnergyDataDescription) eObject;
						EnergyDataDescriptionJSON dataDescriptionJSON = EnergyDataDescriptionJSON.fromEDD(energyDataDescription, addImageData, addESDL, addHistory);
						return Response.ok(Collections.singletonList(dataDescriptionJSON)).build();
					} else {
						return Response.serverError().entity(JSONUtil.getErrorAsJson("Not an EnergyDataDescription")).build();
					}
				}
			} catch (PermissionDeniedException e) {
				log.info(userInfo.getUserName() + ": " + e.getMessage());
				//e.printStackTrace();
				throw e;
			} catch (CDOResourceNodeNotFoundException e) {
				return Response.status(Status.BAD_REQUEST).entity(JSONUtil.getErrorAsJson("Cannot find path")).build();
			}
			//return Response.status(Status.BAD_REQUEST).entity(JSONUtil.getErrorAsJson("Cannot get EDD from path " + path)).build();
		}
			
			
		if (title != null && !title.isEmpty()) filter.put("title", title);
		if (tag != null && !tag.isEmpty()) filter.put("tags", String.join(",", tag));
		if (attributeName != null && !attributeName.isEmpty()) filter.put("attributeName", attributeName);
		if (attributeValue != null && !attributeValue.isEmpty()) filter.put("attributeValue", attributeValue);
		if (maxResults >= 0) filter.put("limit", String.valueOf(maxResults));
		if (offset > 0) filter.put("offset", String.valueOf(offset));
		if (order != null && order != SortOrder.DEFAULT) filter.put("order", order.name());
		if (esdlType != null && !esdlType.isEmpty()) filter.put("esdlType", esdlType);
		//System.out.println("filter: " + filter);
		//List<EnergyDataDescription> queryEDD = cdoManager.queryEDD(userInfo, filter);
		try {
			List<EnergyDataDescription> queryEDD = cdoManager.oclQueryEDD(userInfo, filter);
			//System.out.println(queryEDD);
			List<EnergyDataDescriptionJSON> result = queryEDD.stream().map(edd -> EnergyDataDescriptionJSON.fromEDD(edd, addImageData, addESDL, addHistory)).collect(Collectors.toList());
			return Response.ok(result).build();
		} catch (Exception e) {
			return Response.serverError().entity("Error querying database: " + e.getMessage()).build();
		}
	}
	
	@GET
	@Path("/query/tags")
	@Operation(summary="Query all the tags from EnergyDataDescriptions from the database")
	@Tag(ref = "EDR")
	@Parameters({
		@Parameter(name = "path", in = ParameterIn.QUERY, description = "Retrieve a tags from EDDs below a specific path"),
	})
	@APIResponses({
		@APIResponse(description = "Returns a list of all EnergyDataDescription tags", 
				content = @Content(mediaType = MediaType.APPLICATION_JSON, 
				schema = @Schema(implementation = String[].class)))
	})
	@Produces(MediaType.APPLICATION_JSON)
	public Response queryTags(@QueryParam("path") String path) {
		PublicUserInfo userInfo = new PublicUserInfo();
		List<String> tags;
		if (path != null && !path.isEmpty()) {
			path = URI.decode(path);
			if (!path.startsWith("/")) path = "/" + path;
			// This OCL query filters first based on folder path and then casts the contents of the resource to EDDs, and then collect the tags of those EDDs
			String oclQuery = "eresource::CDOResource.allInstances()"
					+ "->select(r | r.path.startsWith('" + path + "'))"
					+ "->collect(r | r.contents)"
					+ "->select(e | e.oclIsTypeOf(edd::EnergyDataDescription))->collect(e| e.oclAsType(edd::EnergyDataDescription))" 
					+ "->collect(e | e.tags)->asSet()->sortedBy(t|t)";
			tags = cdoManager.runQueryT(userInfo, oclQuery, String.class, EresourcePackage.Literals.CDO_RESOURCE);

		} else {
			// no path: just select all EDDs and get their tags as a set. (
			//TODO: filter on /edr/Public, as this also reviels other EDDs elsewhere (e.g. in protected folders)
			String oclQuery = "edd::EnergyDataDescription.allInstances()->collect(e:EnergyDataDescription | e.tags)->asSet()->sortedBy(t|t)";
			tags = cdoManager.runQueryT(userInfo, oclQuery, String.class, EddPackage.Literals.ENERGY_DATA_DESCRIPTION);
		}
		return Response.ok(tags).build();
	}
	
	@GET
	@Path("/query/tags/{esdlType}")
	@Operation(summary="Query all the tags for a specific ESDL type from EnergyDataDescriptions from the database")
	@Tag(ref = "EDR")
	@Parameters({
		@Parameter(name = "esdlType", in = ParameterIn.PATH, description = "esdlType to query tags from, e.g. Producer or GeothermalSource"),
	})
	@APIResponses({
		@APIResponse(description = "Returns a list of EnergyDataDescription tags for an ESDL type", 
				content = @Content(mediaType = MediaType.APPLICATION_JSON, 
				schema = @Schema(implementation = String[].class)))
	})
	@Produces(MediaType.APPLICATION_JSON)
	public Response queryTagsByESDLType(/*@QueryParam("path") String path*/ @PathParam("esdlType") String esdlType) {
		PublicUserInfo userInfo = new PublicUserInfo();
		EClassifier eClassifier = EsdlPackage.eINSTANCE.getEClassifier(esdlType);
		if (eClassifier != null) {				
			// check if this is a valid ESDL type
			String nsPrefix = eClassifier.getEPackage().getNsPrefix();
			String oclESDLType = nsPrefix + "::" + eClassifier.getName();
			//esdlTypeWithPrefix = nsPrefix+"::"+eClassifier.getName();
			String oclQuery = "edd::EnergyDataDescription.allInstances()->select(e:EnergyDataDescription | e.esdl.oclIsKindOf("+oclESDLType+"))->collect(e:EnergyDataDescription | e.tags)->asSet()->sortedBy(t|t)";
			List<String> tags = cdoManager.runQueryT(userInfo, oclQuery, String.class, EddPackage.Literals.ENERGY_DATA_DESCRIPTION);
			return Response.ok(tags).build();
		} else {
			return Response.status(Status.BAD_REQUEST).entity(JSONUtil.getErrorAsJson("Unknown ESDL type: " + esdlType)).build();
		}
		
		
	}
	
	@GET
	@Path("/query/esdltypes")
	@Operation(summary="Query all the ESDL types defined in EnergyDataDescriptions in the EDR")
	@Tag(ref = "EDR")
//	@Parameters({
//		@Parameter(name = "path", in = ParameterIn.QUERY, description = "Retrieve a specific EDD based on the path of the description (id field), all filters are ignored"),
//	})
	@APIResponses({
		@APIResponse(description = "Returns a list of ESDL types defined in EDDs", 
				content = @Content(mediaType = MediaType.APPLICATION_JSON, 
				schema = @Schema(implementation = String[].class)))
	})
	@Produces(MediaType.APPLICATION_JSON)
	public Response queryESDLtypes(/*@QueryParam("path") String path*/) {
		PublicUserInfo userInfo = new PublicUserInfo();
		String oclQuery = "edd::EnergyDataDescription.allInstances()->collect(e:EnergyDataDescription | e.esdlType)->asSet()->sortedBy(t|t)";
		List<String> tags = cdoManager.runQueryT(userInfo, oclQuery, String.class, EddPackage.Literals.ENERGY_DATA_DESCRIPTION);
		return Response.ok(tags).build();
	}
	
	
	
	@RolesAllowed("cdo_read")
	@POST
	@Path("/upload/{resourceURL:.+}")
	@Tag(ref = "EDR")
	@Operation(summary="Upload a new Energy Data Description to the EDR")
	@Parameters({
		@Parameter(allowEmptyValue = false, name = "resourceURL", in = ParameterIn.PATH, description = "The path and filename of the resource (URL-encoded if using spaces) for upload. Use .edd as file extension.", example = "/edr/Public/ETM/ETM WindTurbine.edd"),
		@Parameter(name = "commitMessage", in = ParameterIn.QUERY, description = "The commit message used when uploading the new or updated file")
	})
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@APIResponses( {
		@APIResponse(responseCode = "201", description = "Upload successful, created resource at resourceURL"),
		@APIResponse(responseCode = "403", description = "Access denied, provide proper credentials"),
		@APIResponse(responseCode = "500", description = "Internal server error.")
	})
	public Response upload(@Valid EnergyDataDescriptionJSON eddJson, @PathParam("resourceURL") String resourceURL, @QueryParam("commitMessage") String commitMessage) throws URISyntaxException {
		String path = URI.decode(resourceURL);
		if (path.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity(JSONUtil.getErrorAsJson("Missing path and filename of this EDD in EDR. Put it after the url /upload/<path to store>")).build();
		}
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		
		if (commitMessage == null || commitMessage.isBlank()) {
			commitMessage = "No commit message";
		}
		commitMessage = URI.decode(commitMessage);
		UserInfo userInfo;
		try {
			userInfo = AccessController.userInfoFromToken(PropagationHelper.getIdToken());
		} catch (InvalidLoginInformationException e) {
			return Response.status(Status.FORBIDDEN).entity(e.getMessage()).build();
		}
		log.info("EDR upload: " + resourceURL + " by " + userInfo.getUserName());
		
		try {
			ResourceMetaData resourceMetaData = EddFactory.eINSTANCE.createResourceMetaData();
			resourceMetaData.setResourcePath(path);
			resourceMetaData.setByteSize(request.getContentLengthLong());
			EnergyDataDescription edd = eddJson.toEDD(resourceMetaData);
//			if (edd.getId() == null || edd.getId().isBlank()) {
//				edd.setId(path); // needed for EMF Compare
//			}
			Resource resource;
			resource = new XMLResourceImpl();
			resource.getContents().add(edd);
			CDOCommitInfo commitInfo = cdoManager.storeResource(userInfo, path, resource, commitMessage, false, resourceMetaData);
			System.out.println(commitInfo);
		} catch (IOException e) {
			log.severe(e.getMessage());
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).entity(JSONUtil.getErrorAsJson("Error parsing input:" + e.getMessage())).build();
		} catch (ConcurrentAccessException | TimeoutException |InterruptedException e) {
			log.severe(e.getMessage());
			e.printStackTrace();
			return Response.serverError().entity(JSONUtil.getErrorAsJson("Internal server error" + e.getMessage())).build();
		} catch (CommitException e) {
			log.severe(e.getMessage());
			e.printStackTrace();
			return Response.serverError().entity(JSONUtil.getErrorAsJson("Error storing to EDR:"+e.getMessage())).build();
		} catch (PermissionDeniedException e) {
			return Response.status(Status.FORBIDDEN).entity(JSONUtil.getErrorAsJson("Access denied: " + e.getMessage())).build();
		}
		
		//return Response.ok().build();
		// convert spaces to %20 if needed in URI fragments
		String fileUri = URI.encodeFragment(resourceURL, true);
		return Response.created(java.net.URI.create(fileUri)).build();
	}
	
	
	

}
