package nl.tno.esdl.esdldrive.edr;

import java.io.IOException;
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
import org.eclipse.emf.cdo.eresource.CDOResourceFactory;
import org.eclipse.emf.cdo.util.CommitException;
import org.eclipse.emf.cdo.util.ConcurrentAccessException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;
import org.eclipse.microprofile.openapi.annotations.Operation;

import nl.tno.edr.EnergyDataDescription;
import nl.tno.esdl.esdldrive.browser.BrowserController;
import nl.tno.esdl.esdldrive.browser.BrowserController.BrowseOperation;
import nl.tno.esdl.esdldrive.browser.BrowserNode;
import nl.tno.esdl.esdldrive.cdo.CDOManager;
import nl.tno.esdl.esdldrive.cdo.ResourceResult;
import nl.tno.esdl.esdldrive.cdo.UserInfo;
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
	@Operation(summary = "browse the public EDR")
	public Response browsePublicEDR( 
    		@Context SecurityContext securityContext, 
    		@QueryParam("operation") @DefaultValue("none") BrowseOperation operation,
    		@QueryParam("id") String id) {
		
		
		
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
	@Operation(summary="Retrieve a ESDL file from the public Energy Data Repository")
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
	@Operation(summary="Search for a certain ESDL type in the public Energy Data Repository")
	@Produces(MediaType.APPLICATION_JSON)
	public Response search(@QueryParam("type") SearchESDLType type) {
		
		PublicUserInfo userInfo = new PublicUserInfo();
		
		List<EDRQueryResult> result = cdoManager.query(type, userInfo);
				
		return Response.ok(result).build();
	}
	
	
	@GET
	@Path("/query")
	@Operation(summary="Query EnergyData Descriptions in the database")
	@Produces(MediaType.APPLICATION_JSON)
	public Response query(@QueryParam("title") String title, @QueryParam("tag") String tag) {
		PublicUserInfo userInfo = new PublicUserInfo();
		HashMap<String, String> filter = new HashMap<>();
		if (title != null && !title.isEmpty()) filter.put("title", title);
		if (tag != null && !tag.isEmpty()) filter.put("tags", tag);
		
		List<EnergyDataDescription> queryEDD = cdoManager.queryEDD(userInfo, filter);
		System.out.println(queryEDD);
		List<EnergyDataDescriptionJSON> result = queryEDD.stream().map(edd -> EnergyDataDescriptionJSON.fromEDD(edd)).collect(Collectors.toList());
		
		return Response.ok(result).build();
	}
	
	
	
	//@RolesAllowed("cdo_read")
	@POST
	@Path("/upload/{resourceURL:.+}")
	@Operation(summary="Upload a new Energy Data Description to the EDR")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response upload(EnergyDataDescriptionJSON eddJson, @PathParam("resourceURL") String resourceURL) {
		String path = new String(resourceURL);
		if (path.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity(getErrorAsJson("Missing path and filename of this EDD in EDR. Put it after the url /upload/<path to store>")).build();
		}
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		// handle security here
		PublicUserInfo userInfo = new PublicUserInfo();
		String commitMessage = null; // TODO: fixme
//		UserInfo userInfo;
//		try {
//			userInfo = AccessController.userInfoFromToken(PropagationHelper.getIdToken());
//		} catch (InvalidLoginInformationException e) {
//			return Response.serverError().status(Status.FORBIDDEN).entity(e.getMessage()).build();
//		}
		System.out.println("EDR upload: " + resourceURL);
		
		System.out.println(eddJson);
		try {
			EnergyDataDescription edd = eddJson.toEDD();
			System.out.println(edd);
			System.out.println(edd.getEsdl());
			System.out.println(edd.getImage());
			Resource resource;
//			boolean existResource = cdoManager.existResource(userInfo, path);
//			if (existResource) {
//				ResourceResult resourceResult = cdoManager.getResource(userInfo, path);
//				resource = resourceResult.getCDOResource();
//				resource.getContents().set(0, edd); // overwrite
//			} else {
				resource = new XMLResourceImpl();
				resource.getContents().add(edd);
//			}
			CDOCommitInfo commitInfo = cdoManager.storeResource(userInfo, path, resource, commitMessage, false);
			System.out.println(commitInfo);
			
		} catch (IOException e) {
			e.printStackTrace();
			return Response.serverError().entity(getErrorAsJson("Error parsing input")).build();
		} catch (ConcurrentAccessException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (CommitException e) {
			e.printStackTrace();
		} catch (PermissionDeniedException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		
		return Response.ok().build();
	}
	
	
	
	private JsonObject getErrorAsJson(String reason) {
		return Json.createObjectBuilder()
				.add("error", reason)
				.build();
	}
}
