package nl.tno.esdl.esdldrive.geometry;

import java.util.List;
import java.util.logging.Logger;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

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
import nl.tno.esdl.esdldrive.geometry.dao.ESDLGeoObjectsDao;
import nl.tno.esdl.esdldrive.geometry.entities.ESDLGeoObjects;
import nl.tno.esdl.esdldrive.geometry.messages.GeometryRequest;
import nl.tno.esdl.esdldrive.geometry.processing.ConvertEnergySystemToGeometryAssets;
import nl.tno.esdl.esdldrive.util.JSONUtil;

@RequestScoped
@Path("/geometry")
public class GeometryResource {

	@Inject
	CDOManager cdoManager;

	private Logger log = Logger.getLogger(this.getClass().getName());

	@Inject
    private ESDLGeoObjectsDao assetDao;

	@POST
	@Path("/assets")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "Query the geometries of an ESDL")
	@Tag(ref = "Geometry")
	@Transactional
	public Response getGeometries(GeometryRequest request) {
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
		
		if (contents != null && contents instanceof esdl.EnergySystem) {
			esdl.EnergySystem energySystem = (esdl.EnergySystem) contents;
			String id = energySystem.getId();
			// query geom database
			List<ESDLGeoObjects> assets = assetDao.findAssets(id);
			return Response.ok(assets).build();
			// if not found, generate geometries
		} else {
			return Response.status(Status.BAD_REQUEST).entity(JSONUtil.getErrorAsJson("Specified path is not an EnergySystem.")).build();
		}

		//return Response.ok().build();
	}
	
	@POST
	@Path("/init")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "Initialize an Energy system's geometries")
	@Tag(ref = "Geometry")
	@Transactional
	public Response initializeEnergySystem(GeometryRequest request) {
		log.info("Initialize geometries for ESDL file: " + request.getPath());
		
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
			if (assetDao.hasEnergySystem(energySystem.getId())) {
				return Response.status(Status.FOUND).entity(JSONUtil.getErrorAsJson("Energy system " + energySystem.getName() + " already present")).build();
			} else {
				// if not found, generate geometries
				List<ESDLGeoObjects> objects = ConvertEnergySystemToGeometryAssets.convert(energySystem);
				// initalize database with assets with geometries
				assetDao.createAllGeoObjects(objects);
				return Response.ok().build();
			}
		} else {
			return Response.status(Status.BAD_REQUEST).entity(JSONUtil.getErrorAsJson("Specified path is not an EnergySystem.")).build();
		}
		
		//return Response.ok().build();
	}
}
