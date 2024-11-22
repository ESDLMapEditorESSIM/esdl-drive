package nl.tno.esdl.esdldrive.geometry;

import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.ibm.websphere.security.openidconnect.PropagationHelper;
import com.ibm.websphere.security.openidconnect.token.IdToken;

import nl.tno.esdl.esdldrive.cdo.AccessController;
import nl.tno.esdl.esdldrive.cdo.CDOManager;
import nl.tno.esdl.esdldrive.cdo.UserInfo;
import nl.tno.esdl.esdldrive.cdo.UserInfo.InvalidLoginInformationException;
import nl.tno.esdl.esdldrive.geometry.messages.GeometryRequest;
import nl.tno.esdl.esdldrive.util.JSONUtil;

@RequestScoped
@Path("/geometry")
public class GeometryResource {

	@Inject
	CDOManager cdoManager;

	private Logger log = Logger.getLogger(this.getClass().getName());

	public GeometryResource() {

	}

	@POST
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "Query the geometries of an ESDL")
	@Tag(ref = "Geometry")
	public Response compare(GeometryRequest request) {
		log.info("Request geometries for ESDL file: " + request.getPath());
		
		IdToken token = PropagationHelper.getIdToken();
        UserInfo userInfo;
		try {
			userInfo = AccessController.userInfoFromToken(token);
		} catch (InvalidLoginInformationException e) {
			return Response.serverError().status(Status.FORBIDDEN).entity(e.getMessage()).build();
		} 
		
		CDOResource resource = cdoManager.getResource(userInfo, request.getPath());
		EObject contents = resource.getContents().get(0);
		
		if (contents instanceof esdl.EnergySystem) {
			esdl.EnergySystem energySystem = (esdl.EnergySystem) contents;
			String id = energySystem.getId();
			// query geom database
			// if not found, generate geometries
		} else {
			return Response.status(Status.BAD_REQUEST).entity(JSONUtil.getErrorAsJson("Specified path is not an EnergySystem.")).build();
		}

		return Response.ok().build();
	}
}
