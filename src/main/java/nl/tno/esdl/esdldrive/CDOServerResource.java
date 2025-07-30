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

package nl.tno.esdl.esdldrive;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;

import org.eclipse.emf.cdo.common.commit.CDOCommitInfo;
import org.eclipse.emf.cdo.common.util.CDOResourceNodeNotFoundException;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.util.CDOUtil;
import org.eclipse.emf.cdo.util.InvalidURIException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.xmi.PackageNotFoundException;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameters;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.ibm.websphere.security.openidconnect.PropagationHelper;
import com.ibm.websphere.security.openidconnect.token.IdToken;

import esdl.EsdlPackage;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
import nl.tno.esdl.esdldrive.cdo.AccessController;
import nl.tno.esdl.esdldrive.cdo.CDOManager;
import nl.tno.esdl.esdldrive.cdo.DrivePermission;
import nl.tno.esdl.esdldrive.cdo.ResourceMetadata;
import nl.tno.esdl.esdldrive.cdo.UserInfo;
import nl.tno.esdl.esdldrive.cdo.UserInfo.InvalidLoginInformationException;
import nl.tno.esdl.esdldrive.cdo.exceptions.PermissionDeniedException;
import nl.tno.esdl.esdldrive.edr.PublicUserInfo;

/**
 * @author werkmane
 * 
 * 
 * This JAX-RS resource manages the up and download of ESDL-files
 */

@Path("/")
@RequestScoped
public class CDOServerResource {

	Logger log = Logger.getLogger(this.getClass().getName());

	@Inject
	CDOManager cdoManager;
	
	@Context 
	private HttpServletRequest request;
	

	@GET
	@Path("/resource/{resourceURL:.+}")
	@Produces(MediaType.APPLICATION_XML)
	@RolesAllowed("cdo_read")
	@Operation(summary = "Retrieves a ESDL-file from the server", description = "It loads the ESDL-file from the cache, or if not available or 'nocache=true', loads it from the database. If a revision=[EpochMilliseconds] is added to the request URI it will load the version of this Energy system at that time, if available.")
	@Tag(ref = "ESDL Drive")
	@Parameters({
		@Parameter(allowEmptyValue = false, name = "resourceURL", in = ParameterIn.PATH, description = "The path and filename of the resource (URL-encoded if using spaces)", example = "/Projects/MyProject/MyEnergySystem.esdl"),
		@Parameter(allowEmptyValue = true, name = "revision", in = ParameterIn.QUERY, description = "The revision to get from this resource, defined as the creation time of the revision in EPOCH milliseconds"),
		@Parameter(allowEmptyValue = true, name = "nocache", in = ParameterIn.QUERY, description = "If true, don't retrieve a cached version, but the version from the database", schema = @Schema(defaultValue = "false", type = SchemaType.BOOLEAN))
	})
	public Response getResource(
			@PathParam("resourceURL") String resourceURL, 
			@QueryParam("revision") Long revision, 
			@QueryParam("nocache") @DefaultValue("false") Boolean nocache) {
		// String resourceURL = "ewoud/test1.esdl";
		if (!resourceURL.startsWith("/")) {
			resourceURL = "/" + resourceURL;
		}
		UserInfo userInfo;
		try {
			userInfo = AccessController.userInfoFromToken(PropagationHelper.getIdToken());
		} catch (InvalidLoginInformationException e) {
			return Response.serverError().status(Status.FORBIDDEN).entity(e.getMessage()).build();
		}
		
		DrivePermission permission = AccessController.getPermission(resourceURL, userInfo);
		//System.out.println("Admin: " + userInfo.isAdmin());
		//System.out.println("Acessible: " + permission.isAccessible());
		if (!(userInfo.isAdmin() || permission.isAccessible())) {
			return Response.serverError().status(Status.FORBIDDEN).entity("Access denied").build();
		}
				
		log.info("getResource() ResourceURL: " + resourceURL + " [username=" + userInfo.getUserName() + ", ip=" + request.getRemoteAddr() + "]");
		// try to find it in the cache
		String cachedModel = cdoManager.getCachedModelXML(resourceURL);
		if (cachedModel != null && revision == null && !nocache) {
			log.fine("getResource(): Retrieving model from cache");
			System.out.println("getResource(): Retrieving model from cache: " + resourceURL);
			return Response.ok(cachedModel).build();
		}
		try {
			CDOResource cdoResource = cdoManager.getResource(userInfo, resourceURL, revision);
			CDOUtil.setIntrinsicIDToEObjectMap(cdoResource, new HashMap<String, EObject>());
			String xmlString = cdoManager.convertToXMLString(cdoResource);
			//log.info("Retrieved: " + xmlString);
			if (xmlString == null) {
				return Response.status(Status.NOT_FOUND).entity("Failed to find the resource").type(MediaType.TEXT_PLAIN).build();
			}
			if (revision == null) {
				// currently only cache the latest version
				cdoManager.setCache(resourceURL,xmlString, cdoResource);
			}
			Date lastModified = new Date(cdoResource.cdoRevision(true).getTimeStamp());
			cdoResource.cdoView().close();
			return Response.ok(xmlString).lastModified(lastModified).build();
		} catch (Exception e) {
			if (!(e instanceof CDOResourceNodeNotFoundException) && !(e instanceof InvalidURIException)) { // ignore these errors
				log.severe("Error: " + e);
				e.printStackTrace();
			}
			return Response.status(Status.NOT_FOUND).entity("Failed to find the resource at " + resourceURL).type(MediaType.TEXT_PLAIN).build();
		}
	}
	
	@GET
	@Path("/resource/public/{resourceURL:.+}")
	@Tag(ref = "ESDL Drive")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@APIResponses({
		@APIResponse(description = "Retrieves the file as JSON or XML", responseCode = "200", content = {
				@Content(mediaType = "application/json"), 
				@Content(mediaType = "application/xml")
			})
	})
	public Response getPublicResource(@PathParam("resourceURL") String resourceURL, @Context HttpHeaders headers) {
		if (!resourceURL.startsWith("/")) {
			resourceURL = "/" + resourceURL;
		}
		PublicUserInfo userInfo = new PublicUserInfo();
		resourceURL = AccessController.PUBLIC_FOLDER_PATH + resourceURL; // make sure to look in public folders only
		System.out.println("PUBLIC Retrieving " + resourceURL);
		log.info("PUBLIC getResource() ResourceURL: " + resourceURL + " [username=" + userInfo.getUserName() + ", ip=" + request.getRemoteAddr() + "]");
		boolean jsonContentType = headers.getAcceptableMediaTypes().contains(MediaType.APPLICATION_JSON_TYPE);

		// try to find it in the cache
		String cachedModel = cdoManager.getCachedModelXML(resourceURL);
		if (!jsonContentType && cachedModel != null /*&& revision == null && !nocache*/) {
			log.fine("PUBLIC getResource(): Retrieving model from cache");
			System.out.println("PUBLIC getResource(): Retrieving model from cache: " + resourceURL);
			return Response.ok(cachedModel).build();
		}
		try {
			CDOResource cdoResource = cdoManager.getPublicResource(userInfo, resourceURL);
			String result = null;
			MediaType contentType = MediaType.APPLICATION_XML_TYPE;
			if (jsonContentType) {
				result = cdoManager.convertToJSONString(cdoResource);
				contentType = MediaType.APPLICATION_JSON_TYPE;
			} else {
				result = cdoManager.convertToXMLString(cdoResource);
				// currently only cache the latest XML version - not revisions
				if (result != null && !result.isEmpty())
					cdoManager.setCache(resourceURL, result, cdoResource);
			}
			Date lastModified = new Date(cdoResource.cdoRevision(true).getTimeStamp());
			cdoResource.cdoView().close();
			return Response.ok(result).type(contentType).lastModified(lastModified).build();
		} catch (CDOResourceNodeNotFoundException | InvalidURIException e ) {
			return Response.status(Status.NOT_FOUND).entity("Failed to find the resource at " + resourceURL).type(MediaType.TEXT_PLAIN).build();
		} catch (Exception e) {
			log.severe("Error: " + e);
			e.printStackTrace();
			return Response.status(Status.NOT_FOUND).entity("Failed to find the resource at " + resourceURL).type(MediaType.TEXT_PLAIN).build();
		}
	}

	// update or create a new resource with resourceURL
	@PUT
	@Path("/resource/{resourceURL:.+}")
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_OCTET_STREAM})
	@RolesAllowed("cdo_read")
	@Operation(summary = "Stores a ESDL-file to the ESDL Drive", description = "When a file is being saved and already exists in the database, it calculates the differences and only stores the diffences. When this gives issues, use 'overwrite=true' in the query to always overwrite the old version.")
	@Tag(ref = "ESDL Drive")
	@Parameters({
			@Parameter(allowEmptyValue = false, name = "resourceURL", in = ParameterIn.PATH, description = "The path and filename of the resource, e.g. /folder1/myEnergySystem.esdl. If it has spaces, it needs to be url encoded", example = "/Projects/MyProject/MyEnergySystem.esdl"),
			@Parameter(allowEmptyValue = true, name = "commitMessage", in = ParameterIn.QUERY, description = "The commit message that is added to the history of this save action", example = "Cool commit message"),
			@Parameter(allowEmptyValue = true, name = "overwrite", in = ParameterIn.QUERY, description = "Overwrite the contents in the database if the resource exists, instead of only saving the differences, this options makes storing a resource slower", example = "false", schema = @Schema(type = SchemaType.BOOLEAN))
	})
	@APIResponses({
			@APIResponse(responseCode = "200", description = "Stores a resource in the repository using the supplied URI. Only response is a HTTP status code"),
			@APIResponse(responseCode = "400", description = "Insufficient access to store the resource"),
			@APIResponse(responseCode = "500", description = "Saving failed.")
	})
	public Response putResource(
			@PathParam("resourceURL") String resourceURL,
			@QueryParam("commitMessage") String commitMessage,
			@QueryParam("overwrite") @DefaultValue("false") Boolean overwrite,
			@RequestBody(description = "ESDL content in XML format", name = "stream", 
				content = {
					@Content(example = "<?xml version=\"1.0\" encoding=\"UTF-8\">\n<esdl:EnergySystem ...></esdl:EnergySystem", 
							schema = @Schema(required = true, format="XML String", description = "ESDL content"))
				}
			)
			InputStream stream) {
		if (!resourceURL.startsWith("/")) {
			resourceURL = "/" + resourceURL;
		}
		UserInfo userInfo;
		try {
			userInfo = AccessController.userInfoFromToken(PropagationHelper.getIdToken());
		} catch (InvalidLoginInformationException e) {
			return Response.serverError().status(Status.FORBIDDEN).entity(e.getMessage()).build();
		}
		DrivePermission permission = AccessController.getPermission(resourceURL, userInfo);
		System.out.println("putResource: " + resourceURL + " [username=" + userInfo.getUserName() + ", ip=" + request.getRemoteAddr() + "]");
		if (permission.isWritable()) {
			
			// put in cache: and convert to EMF model
			String oldXML = cdoManager.getCachedModelXML(resourceURL);
			try {
				System.out.println("Reading contents of " + resourceURL);
				ByteArrayOutputStream result = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				int length;
				while ((length = stream.read(buffer)) != -1) {
				    result.write(buffer, 0, length);
				}
				String xmlString = result.toString(StandardCharsets.UTF_8.name());
				System.out.println("Storing in cache");
				
				cdoManager.setCache(resourceURL, xmlString, null);
				ByteArrayInputStream resourceInputStream = new ByteArrayInputStream(result.toByteArray()); 
				
				
				System.out.println("Commit message:" + commitMessage);
				System.out.println("Overwrite:" + overwrite);
				
				// set OVERWRITE to true to disable merging feature 
				//overwrite = true;
				
				
				// create hashmap to speed up reading
				HashMap<String, EObject> INTRINSIC_ID_TO_E_OBJECT_MAP = new HashMap<String, EObject>();
				XMLResourceImpl resource = new XMLResourceImpl();
				resource.getDefaultLoadOptions().put(XMLResource.OPTION_DEFER_IDREF_RESOLUTION, Boolean.TRUE);
				resource.getDefaultLoadOptions().put(XMLResource.OPTION_RECORD_UNKNOWN_FEATURE, Boolean.TRUE);
				resource.setIntrinsicIDToEObjectMap(INTRINSIC_ID_TO_E_OBJECT_MAP);
				resource.load(resourceInputStream, null);
				System.out.println("Done reading contents of " + resourceURL);
				CDOCommitInfo commitInfo = cdoManager.storeResource(userInfo, resourceURL, resource, commitMessage, overwrite);
				System.out.println("Saved " + resourceURL + ", commitInfo: " + commitInfo);
				long timeStamp = commitInfo.getTimeStamp();

				return Response.ok().lastModified(new Date(timeStamp)).build();
			} catch (Exception e) {
				log.severe("Error: " + e);
				e.printStackTrace();
				System.out.println("Reverting Cache to old version!");
				cdoManager.setCache(resourceURL, oldXML, null);
				if (e instanceof PackageNotFoundException) {
					return Response.serverError().entity(
							"Input is not of type ESDL: XML namespaceURI is not defined or (xmlns:esdl) is different from "
									+ EsdlPackage.eNS_URI)
							.type(MediaType.TEXT_PLAIN).build();
				}
				System.out.println("Error saving resource " + resourceURL + ": " + e.getMessage());
				return Response.serverError().entity("Failed to store the resource " + resourceURL + ": " + e.getMessage())
						.type(MediaType.TEXT_PLAIN).build();
			}
		} else {
			System.out.println("No write access to " + permission.getPath());
			return Response.serverError().status(Status.FORBIDDEN).entity("Permission denied for " + resourceURL).build();
		}
	}

	/**
	 * HEAD -> ok if file exists
	 * 
	 * in GET, HEAD, HeaderField("last-modified") also content-length
	 * 
	 * in OPTIONS String allow = httpURLConnection.getHeaderField("Allow"); if PUT
	 * is in there, URI is marked read-write, otherwise read-only
	 * 
	 */
	@Operation(description = "Returns the status of the resource, e.g. if it exists and the last-modified state", summary = "Checks if a file exists in the database")
	@Tag(ref = "ESDL Drive")
	@Parameter(allowEmptyValue = false, name = "resourceURL", description = "The path and filename of the resource, e.g. folder1/myEnergySystem.esdl, without spaces", example = "folder/filename.esdl")
	@APIResponses({
		@APIResponse(responseCode = "200", description = "Returns OK if the resource exists and is accessible by the user"),
		@APIResponse(responseCode = "403", description = "Returns Forbidden if the user is not allowed to access the resource"),
		@APIResponse(responseCode = "404", description = "Returns Not Found if resource is not found")
	})
	@HEAD
	@Path("/resource/{resourceURL:.+}")
	@RolesAllowed("cdo_read")
	public Response head(@PathParam("resourceURL") String resourceURL) {
		if (!resourceURL.startsWith("/")) {
			resourceURL = "/" + resourceURL;
		}
		boolean resourceExists = false;
		try {
			UserInfo userInfo = AccessController.userInfoFromToken(PropagationHelper.getIdToken());
			log.info("HEAD ResourceURL: " + resourceURL + " [username=" + userInfo.getUserName() + ", ip=" + request.getRemoteAddr() + "]");
			
			DrivePermission permission = AccessController.getPermission(resourceURL, userInfo);
			if (!permission.isAccessible()) {
				return Response.serverError().status(Status.FORBIDDEN).entity("Access denied").build();
			}
			resourceExists = cdoManager.existResource(userInfo, resourceURL);
			if (resourceExists) {
				ResourceMetadata resourceMetaData = cdoManager.getResourceMetaData(userInfo, resourceURL);
				return Response.ok().lastModified(new Date(resourceMetaData.getTimeStamp())).build();
			} else {
				return Response.status(Status.NOT_FOUND).build();
			}
		} catch (PermissionDeniedException | InvalidLoginInformationException e) {
			return Response.serverError().status(Status.FORBIDDEN).build();
		}
	}

	
	/*
	@GET
	@Path("/store")
	@Produces(MediaType.TEXT_PLAIN)
	public Response storeEDSL() {
		UserInfo userInfo = AccessController.userInfoFromToken(PropagationHelper.getIdToken());
		EnergySystem es = EsdlFactory.eINSTANCE.createEnergySystem();
		es.setName("New ES");
		es.setDescription("now = " + System.currentTimeMillis());

		try {
			cdoManager.storeResource(userInfo, "/ewoud/test1.esdl", es);
		} catch (Exception e) {
			log.severe("Error storing file in CDO repository " + e.getMessage());
			e.printStackTrace();
			return Response.serverError().entity("Error storing resource " + e.getMessage()).build();
		}

		return Response.accepted("Success").build();
	}
	*/
	
	@GET
	@Path("/logout")
	@RolesAllowed("cdo_read")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Invalidates the user session", description="Removes the current session with the server and the backend")
	@Tag(ref = "ESDL Drive")
	public Response logout(@Context SecurityContext securityContext) {
		UserInfo userInfo;
		IdToken idToken = PropagationHelper.getIdToken();
		try {
			userInfo = AccessController.userInfoFromToken(idToken);
		} catch (InvalidLoginInformationException e) {
			return Response.serverError().status(Status.FORBIDDEN).entity(e.getMessage()).build();
		}
		if (userInfo != null) {
			cdoManager.logoutSession(userInfo);
			if (request != null) {
				try {
					request.logout();
				} catch (ServletException e) {
					e.printStackTrace();
					return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
				}
			}
			log.info("Logout request, principal=" + securityContext.getUserPrincipal() + " [username=" + userInfo.getUserName() + ", ip=" + request.getRemoteAddr() + "]");
			String keycloak_base_url = ConfigProvider.getConfig().getValue("keycloak_realm_external", String.class);
			String old_keycloak = ConfigProvider.getConfig().getOptionalValue("OLD_KEYCLOAK", String.class).orElse("");
			//return redirect(oidc.client_secrets.get('issuer') + '/protocol/openid-connect/logout?redirect_uri=' + request.host_url)
			String serverURL = request.getScheme() + "://" + request.getServerName()+":"+request.getServerPort() + "/";
			//post_logout_redirect_uri=http://localhost&client_id=cdo
			String redirect_url = keycloak_base_url + "/protocol/openid-connect/logout";
			
			// TODO: backchannel logout works since 24.0.0.2: use  /oidcclient/backchannel_logout/{oidcClientId} where 
			// oidcClientId = cdoMondaine (as defined in server.xml)
			
			// get ID token here!
			// String idTokenString = request.getParameter(OIDCConstants.OIDC_LOGOUT_ID_TOKEN_HINT);
			//redirect_url = redirect_url + "?post_logout_redirect_uri=" + serverURL + "&id_token_hint=" + idToken.getAccessToken();
			// TODO: WARNING: hard-coded client id!
			redirect_url = redirect_url + "?post_logout_redirect_uri=" + serverURL + "&client_id=" + "cdo-mondaine";
			if (old_keycloak != null && !old_keycloak.isBlank()) {
				redirect_url = redirect_url + "?redirect_uri=" + serverURL;
			}
			log.info("redirect uri: " + redirect_url);
			return Response.seeOther(URI.create(redirect_url)).build();
			//return Response.ok("Logout complete").build();
			//keycloak_realm
			//<variable name="keycloak_realm" value="https://idm.hesi.energy/auth/realms/esdl-mapeditor" />
		} else {
			return Response.serverError().status(Status.FORBIDDEN).build();
		}
	}
	
	
	// TODO: make this work: file issue at OpenLibery why getRunAsSubject() returns null
//	public String getRawIdToken() throws WSSecurityException {
//	    Subject subj = WSSubject.getRunAsSubject();
//	    Set<Object> privCredentials = subj.getPrivateCredentials();
//	    for (Object credentialObj : privCredentials) {
//	        if (credentialObj instanceof Map) {
//	            Object value = ((Map<?, ?>) credentialObj).get("id_token");
//	            if (value != null)
//	                return (String) value;
//	        }
//	    }
//	    
//	    return null;
//	}

	@GET
	@Path("/list")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed("cdo_read")
	@Operation(summary = "Lists all the files the database that the logged-in user can access", deprecated = true, 
			description= "Deprecated, as this operation becomes quite expensive then the amount of files in the database grows. Use the /store/browse endpoint instead. Returns a json array with file-path and names.")
	@Tag(ref = "ESDL Drive")
	public Response listResources() {
//		log.info("Token: " + callerPrincipal);
//		log.info("Username: " + callerPrincipal.getName());
//		log.info("Groups: " + callerPrincipal.getGroups());
//		log.info("Subject: " + callerPrincipal.getSubject());
//		log.info("Claim names: " + callerPrincipal.getClaimNames());
//		log.info("Roles: " + callerPrincipal.getClaim("roles"));
		UserInfo userInfo;
		try {
			userInfo = AccessController.userInfoFromToken(PropagationHelper.getIdToken());
		} catch (InvalidLoginInformationException e) {
			return Response.serverError().status(Status.FORBIDDEN).entity(e.getMessage()).build();
		}
		if (userInfo != null) {
			log.info("List resources request by: " + userInfo);
			ArrayList<String> resources = cdoManager.listResources(userInfo);
			return Response.ok(resources).build();
		} else {
			return Response.serverError().status(Status.FORBIDDEN).build();
		}
	}
	
	
	
	
	/*
	@GET
	@Path("/listpath/{path}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listResourcesByPath(@PathParam("path") String path) {
		log.info("Path=" + path);
		IdToken token = PropagationHelper.getIdToken();
		UserInfo userInfo = AccessController.userInfoFromToken(token);
		if (userInfo != null) {
			ArrayList<String> resources = cdoManager.listPath(userInfo, path);
			return Response.ok(resources).build();
		} else {
			return Response.serverError().status(Status.FORBIDDEN).build();
		}
	}
	 */
}
