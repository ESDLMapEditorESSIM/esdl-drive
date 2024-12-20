package nl.tno.esdl.esdldrive.geometry.processing;

import static org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84;

import java.util.Map;
import java.util.logging.Logger;

import org.geolatte.geom.G2D;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.crs.CoordinateReferenceSystem;
import org.geolatte.geom.json.GeolatteGeomModule;
import org.geolatte.geom.json.Setting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class BoundaryService {

	private Logger log = Logger.getLogger(this.getClass().getName());
	/**
	 * 
	 * pattern:
	 * 'PV[0-9]{2,2}|ES[0-9]{2,2}|GM[0-9]{4,4}|WK[0-9]{6,6}|BU[0-9]{8,8}|[0-9]{2,2}'
	 * 
	 * DEFAULT_BOUNDARIES_YEAR = 2019
	 * 
	 * scope_type: from ESDL-scope to boundaryservice scope boundary_service_mapping
	 * = { 'ZIPCODE': 'zipcodes', 'NEIGHBOURHOOD': 'neighbourhoods', 'DISTRICT':
	 * 'districts', 'MUNICIPALITY': 'municipalities', 'REGION': 'regions',
	 * 'PROVINCE': 'provinces', 'COUNTRY': 'countries' }
	 * 
	 * 
	 * 
	 * boundary = self.get_boundary_from_service(boundaries_year,
	 * esdl.AreaScopeEnum.from_string(str.upper(scope)), str.upper(identifier))
	 * 
	 */

	private final static String BOUNDARY_SERVICE_URL_PATH = "/boundaries/YEAR/{year}/{scope}/{identifier}";
	
	public final static int DEFAULT_BOUNDARIES_YEAR = 2019;

	private final static Map<String, String> areaScopeBoundaryServiceMapping = Map.of(
			"ZIPCODE", "zipcodes",
			"NEIGHBOURHOOD", "neighbourhoods", 
			"DISTRICT", "districts", 
			"MUNICIPALITY", "municipalities", 
			"REGION", "regions", 
			"PROVINCE", "provinces", 
			"COUNTRY", "countries");

	private ObjectMapper mapper;
	private WebTarget boundaryService;
	

	public BoundaryService(String boundary_service_base_uri) {
		CoordinateReferenceSystem<G2D> crs = WGS84;
		mapper = new ObjectMapper();
		GeolatteGeomModule module = new GeolatteGeomModule(crs);
		module.set(Setting.SUPPRESS_CRS_SERIALIZATION, true);
		mapper.registerModule(module);
		
		Client client =  ClientBuilder.newClient();;
		boundaryService = client.target(boundary_service_base_uri).path(BOUNDARY_SERVICE_URL_PATH);
		
	}

	
	/**
	 * 
	 * @param year
	 * @param scope
	 * @param identifier
	 * @return null when no match is found or an error occured when contacting the boundary service
	 */
	public Geometry<G2D> getBoundaryFromBoundaryService(int year, @NotNull esdl.AreaScopeEnum scope, @NotNull String identifier) {
		String boundaryServiceScope = areaScopeBoundaryServiceMapping.get(scope.getName());
		if (boundaryServiceScope == null) {
			return null;
		}
		//URI uri = UriBuilder.fromUri(BOUNDARY_SERVICE_URL).build(year, boundaryServiceScope, identifier);
		
		Response response = boundaryService
				.resolveTemplate("year", year)
				.resolveTemplate("scope", boundaryServiceScope)
				.resolveTemplate("identifier", identifier)
				//.request(MediaType.APPLICATION_JSON)
				.request(MediaType.TEXT_HTML)
				.get();
		
		if (response.getStatusInfo().getFamily().equals(Status.Family.SUCCESSFUL)) {
			String stringResponse = response.readEntity(String.class);
			// manual parsing, because boundary service returns text/html instead of application/json!!
			try {
				BoundaryServiceResponse boundaryServiceResponse = mapper.readValue(stringResponse, BoundaryServiceResponse.class);
				@SuppressWarnings("unchecked")
				Geometry<G2D> geometry = (Geometry<G2D>) boundaryServiceResponse.getGeom();
				return geometry;
				
			} catch (JsonProcessingException e) {
				log.severe("Error processing response from Boundary Service: " + e.getMessage());
				return null;
			}
		} else {
			return null;
		}
		
	}

}
