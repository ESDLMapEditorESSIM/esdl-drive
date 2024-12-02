package nl.tno.esdl.esdldrive;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status.Family;

import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import esdl.EsdlPackage;
import esdl.QuantityAndUnits;
import nl.tno.esdl.esdldrive.edr.EnergyDataDescriptionJSON;



/***
 * Does some testing with the public parts of the ESDL Drive
 * @author werkmane
 *
 */
public class ESDLDriveTest {

	private final static String BASE_URL = "http://localhost:9080/store/";
	//private final static String RESOURCE = "resource";
	private final static String PUBLIC_RESOURCE = "resource/public/";
	private final static String EDR_QUERY = "edr/query";
	private final static String EDR_BROWSE = "edr/browse";
	private Client client;

	@Before
	public void setUp() throws Exception {
		EsdlPackage.eINSTANCE.eClass();
		client = ClientBuilder.newClient();
	}
	
	@After
	public void tearDown() {
		client.close();
	}
	

	@Test
	public void testPublicGet() throws IOException {
		Response response = client.target(BASE_URL + PUBLIC_RESOURCE + "QuantityAndUnits.esdl").request()
				.accept(MediaType.APPLICATION_XML).get();
		assertEquals(Family.SUCCESSFUL,response.getStatusInfo().getFamily());
		String response_esdl_string = response.readEntity(String.class);
		System.out.println(response_esdl_string);
		XMLResourceImpl xmlResource = new XMLResourceImpl();
		xmlResource.load(new ByteArrayInputStream(response_esdl_string.getBytes(StandardCharsets.UTF_8)), null);
		QuantityAndUnits qua = (QuantityAndUnits) xmlResource.getContents().get(0);
		assertEquals("14a75c03-5d64-4405-8df6-7e42f952ea72", qua.getId());

		response.close();
	}
	
	@Test
	public void testPublicEDRQuery() throws IOException {
		Response response = client.target(BASE_URL + EDR_QUERY).queryParam("esdlType", "EnergyAsset").request()
				.accept(MediaType.APPLICATION_JSON).get();
		assertEquals(Family.SUCCESSFUL, response.getStatusInfo().getFamily());
		Jsonb jsonb = JsonbBuilder.create();
		//EnergyDataDescriptionJSON eddJson = response.readEntity(EnergyDataDescriptionJSON.class);
		String resp = response.readEntity(String.class);
		System.out.println(resp);
		List<EnergyDataDescriptionJSON> edd = Arrays.asList(jsonb.fromJson(resp, EnergyDataDescriptionJSON[].class));
		System.out.println(edd);
		
		

		response.close();
	}
	
	@Test
	public void testPublicEDRQueryWithOCL() throws IOException {
		Response response = client.target(BASE_URL + EDR_QUERY)
				.queryParam("esdlType", "Pipe")
				.queryParam("attributeName", "diameter")
				.queryParam("attributeValue", "esdl::PipeDiameterEnum::DN100")
				.request().accept(MediaType.APPLICATION_JSON).get();
		assertEquals(Family.SUCCESSFUL, response.getStatusInfo().getFamily());
		Jsonb jsonb = JsonbBuilder.create();
		//EnergyDataDescriptionJSON eddJson = response.readEntity(EnergyDataDescriptionJSON.class);
		String resp = response.readEntity(String.class);
		System.out.println(resp);
		List<EnergyDataDescriptionJSON> edd = Arrays.asList(jsonb.fromJson(resp, EnergyDataDescriptionJSON[].class));
		System.out.println(edd);
		
		
		
		response.close();
	}

	@Test
	public void testPublicEDRBrowse() throws IOException {
		
		Response response = client.target(BASE_URL + EDR_BROWSE)
				.queryParam("operation", "get_node")
				.queryParam("id", "/edr/Public")
				.request().accept(MediaType.APPLICATION_JSON).get();
		assertEquals(Family.SUCCESSFUL, response.getStatusInfo().getFamily());
		Jsonb jsonb = JsonbBuilder.create();
		//EnergyDataDescriptionJSON eddJson = response.readEntity(EnergyDataDescriptionJSON.class);
		//jsonb.
		String resp = response.readEntity(String.class);
		System.out.println(resp);
		List<GetNodeJSON> fromJson = Arrays.asList(jsonb.fromJson(resp, GetNodeJSON[].class));
		System.out.println(fromJson);
		
		
		response.close();
	}

	public static class GetNodeJSON {
		String text;
		String id;
		String type;
		boolean writable;
		boolean deletable;
		Object children;
		
		public GetNodeJSON() {}
	}
	

}
