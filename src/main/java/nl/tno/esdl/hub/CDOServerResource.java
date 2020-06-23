package nl.tno.esdl.hub;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.emf.cdo.common.commit.CDOCommitInfo;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.xmi.PackageNotFoundException;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import com.ibm.websphere.security.openidconnect.PropagationHelper;
import com.ibm.websphere.security.openidconnect.token.IdToken;

import esdl.EnergySystem;
import esdl.EsdlFactory;
import esdl.EsdlPackage;
import nl.tno.esdl.hub.cdo.AccessController;
import nl.tno.esdl.hub.cdo.CDOManager;
import nl.tno.esdl.hub.cdo.PermissionDeniedException;
import nl.tno.esdl.hub.cdo.ResourceMetadata;
import nl.tno.esdl.hub.cdo.ResourceResult;
import nl.tno.esdl.hub.cdo.UserInfo;

/**
 * @author werkmane
 */

@Path("/")
@RequestScoped
public class CDOServerResource {

	Logger log = Logger.getLogger(this.getClass().getName());

	@Inject
	CDOManager cdoManager;
	

	@Parameter(allowEmptyValue = false, in = ParameterIn.PATH, name = "resourceURL", description = "The path and filename of the resource, e.g. folder1/myEnergySystem.esdl, without spaces")
	@GET
	@Path("/resource/{resourceURL:.+}")
	@Produces(MediaType.APPLICATION_XML)
	public Response getResource(@PathParam("resourceURL") String resourceURL) {
		// String resourceURL = "ewoud/test1.esdl";
		UserInfo userInfo = AccessController.userInfoFromToken(PropagationHelper.getIdToken());
		log.info("getResource() ResourceURL: " + resourceURL);
		try (ResourceResult resourceResult = cdoManager.getResource(userInfo, "/" + resourceURL)) {
			String xmlString = cdoManager.convertToXMLString(resourceResult.getCDOResource());
			//log.info("Retrieved: " + xmlString);
			if (xmlString == null) {
				resourceResult.close();
				return Response.status(Status.NOT_FOUND).entity("Failed to find the resource").type(MediaType.TEXT_PLAIN).build();
			}
			Date lastModified = resourceResult.getLastModified();
			resourceResult.close();
			return Response.ok(xmlString).lastModified(lastModified).build();
		} catch (Exception e) {
			log.severe("Error: " + e);
			e.printStackTrace();
			return Response.status(Status.NOT_FOUND).entity("Failed to find the resource").type(MediaType.TEXT_PLAIN).build();
		}
	}

	// update or create a new resource with resourceURL
	@Parameter(allowEmptyValue = false, name = "resourceURL", in = ParameterIn.PATH, description = "The path and filename of the resource, e.g. folder1/myEnergySystem.esdl, without spaces", example = "folder/filename.esdl")
/*	@Parameter(
			description = "The Energy System to store as a string in the POST data",
			name = "stream",
			required = true,
			in = ParameterIn.DEFAULT,
			example = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><esdl:EnergySystem name=\"...\" ...",
			schema = @Schema(type = SchemaType.STRING))*/
	@APIResponse(responseCode = "200", description = "Stores a resource in the repository using the supplied URI. Only response is a HTTP status code")
	@PUT
	@Path("/resource/{resourceURL:.+}")
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_OCTET_STREAM})
	public Response putResource(@PathParam("resourceURL") String resourceURL, InputStream stream) {
		// String resourceURL = "ewoud/test1.esdl";
		UserInfo userInfo = AccessController.userInfoFromToken(PropagationHelper.getIdToken());
		XMLResourceImpl resource = new XMLResourceImpl();
		try {
			log.info("Reading contents of /" + resourceURL);
			resource.load(stream, null);
			log.info("Done reading contents of /" + resourceURL);
			EObject rootEObject = resource.getContents().get(0);
			CDOCommitInfo commitInfo = cdoManager.storeResource(userInfo, "/" + resourceURL, rootEObject);
			long timeStamp = commitInfo.getTimeStamp();
			return Response.ok().lastModified(new Date(timeStamp)).build();
		} catch (Exception e) {
			log.severe("Error: " + e);
			e.printStackTrace();
			if (e instanceof PackageNotFoundException) {
				return Response.serverError().entity(
						"Input is not of type ESDL: XML namespaceURI is not defined or is (xmlns:esdl) is different from "
								+ EsdlPackage.eNS_URI)
						.type(MediaType.TEXT_PLAIN).build();
			}
			return Response.serverError().entity("Failed to store the resource " + resourceURL)
					.type(MediaType.TEXT_PLAIN).build();
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
	@Parameter(allowEmptyValue = false, name = "resourceURL", description = "The path and filename of the resource, e.g. folder1/myEnergySystem.esdl, without spaces", example = "folder/filename.esdl")
	@APIResponse(responseCode = "200", description = "Returns OK if the resource exists and is accessible by the user")
	@APIResponse(responseCode = "403", description = "Returns not found if resource is not found")
	@HEAD
	@Path("/resource/{resourceURL:.+}")
	public Response head(@PathParam("resourceURL") String resourceURL) {
		log.info("getResource() ResourceURL: " + resourceURL);
		UserInfo userInfo = AccessController.userInfoFromToken(PropagationHelper.getIdToken());
		boolean resourceExists = false;
		try {
			resourceExists = cdoManager.existResource(userInfo, "/" + resourceURL);
		} catch (PermissionDeniedException e) {
			return Response.serverError().status(Status.FORBIDDEN).build();
		}
		if (resourceExists) {
			ResourceMetadata resourceMetaData = cdoManager.getResourceMetaData(userInfo, resourceURL);
			return Response.ok().lastModified(new Date(resourceMetaData.getTimeStamp())).build();
		} else {
			return Response.status(Status.NOT_FOUND).build();
		}
	}

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

	@GET
	@Path("/list")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed("cdo_read")
	public Response listResources() {
//		log.info("Token: " + callerPrincipal);
//		log.info("Username: " + callerPrincipal.getName());
//		log.info("Groups: " + callerPrincipal.getGroups());
//		log.info("Subject: " + callerPrincipal.getSubject());
//		log.info("Claim names: " + callerPrincipal.getClaimNames());
//		log.info("Roles: " + callerPrincipal.getClaim("roles"));
		IdToken token = PropagationHelper.getIdToken();
		UserInfo userInfo = AccessController.userInfoFromToken(token);
		if (userInfo != null) {
			log.info("List resources request by: " + userInfo);
			ArrayList<String> resources = cdoManager.listResources(userInfo);
			return Response.ok(resources).build();
		} else {
			return Response.serverError().status(Status.FORBIDDEN).build();
		}
	}
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

}
