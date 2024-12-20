package nl.tno.esdl.esdldrive.geometry;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
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
import nl.tno.esdl.esdldrive.geometry.messages.GeometryPathRequest;
import nl.tno.esdl.esdldrive.geometry.messages.GeometryRequest;
import nl.tno.esdl.esdldrive.geometry.processing.ConvertEnergySystemToGeometryAssets;
import nl.tno.esdl.esdldrive.geometry.processing.GeoJSONConverter;
import nl.tno.esdl.esdldrive.util.JSONUtil;

@RequestScoped
@Path("/geometry")
public class GeometryResource {

	@Inject
	CDOManager cdoManager;
	
    @Inject
    @ConfigProperty(name = "boundary_service_base_uri", defaultValue = "http://geis-01.hesi.energy:4003")
    private String boundary_service_base_uri;

	private Logger log = Logger.getLogger(GeometryResource.class.getClass().getName());

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
			List<ESDLGeoObjects> assets = new ArrayList<ESDLGeoObjects>();
			// query geom database
			if (request.getBbox() != null) {
				if (request.getAssetType() != null) {
					log.info("Retrieving geometries for " + energySystem.getName() + "(id=" + energySystem.getId() + ") within " + request.getBbox());
					assets = assetDao.findAssetsWithinFilter(id, request.getBbox(), request.getAssetType());
				} else {
					log.info("Retrieving geometries for " + energySystem.getName() + "(id=" + energySystem.getId() + ") within " + request.getBbox());
					assets = assetDao.findAssetsWithin(id, request.getBbox());
				}		
			} else {
				if (request.getAssetType() != null) { 
					log.info("Retrieving geometries for " + energySystem.getName() + "(id=" + energySystem.getId() + ")");
					assets = assetDao.findAssetsFilter(id, request.getAssetType());
				} else {
					log.info("Retrieving geometries for " + energySystem.getName() + "(id=" + energySystem.getId() + ")");
					assets = assetDao.findAssets(id);
				}
			}
			log.info("Converting these geometries to GeoJSon.");
			String geojson = GeoJSONConverter.convert(assets);
			log.info("Sending GeoJson to client.");
			return Response.ok(geojson).build();
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
	public Response initializeEnergySystem(GeometryPathRequest request) {
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
				List<ESDLGeoObjects> objects = ConvertEnergySystemToGeometryAssets.convert(energySystem, boundary_service_base_uri);
				// initalize database with assets with geometries
				assetDao.createAllGeoObjects(objects);
				return Response.ok().build();
			}
		} else {
			return Response.status(Status.BAD_REQUEST).entity(JSONUtil.getErrorAsJson("Specified path is not an EnergySystem.")).build();
		}
		
		//return Response.ok().build();
	}
	
	@DELETE
	@Path("/init")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "Delete an Energy system's geometries from the database")
	@Tag(ref = "Geometry")
	@Transactional
	public Response deleteEnergySystem(GeometryPathRequest request) {
		log.info("Delete geometries for ESDL file: " + request.getPath());
		
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
				assetDao.deleteEnergySystem(energySystem.getId());
				return Response.status(Status.OK).build();
			} else {
				return Response.status(Status.NOT_FOUND).entity(JSONUtil.getErrorAsJson("Energy system " + energySystem.getName() + " is not found and cannot be deleted")).build();
			}
		} else {
			return Response.status(Status.BAD_REQUEST).entity(JSONUtil.getErrorAsJson("Specified path is not an EnergySystem.")).build();
		}
		
		//return Response.ok().build();
	}
}
