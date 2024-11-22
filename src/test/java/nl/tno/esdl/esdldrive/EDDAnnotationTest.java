package nl.tno.esdl.esdldrive;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status.Family;

import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.eresource.CDOResourceFolder;
import org.eclipse.emf.cdo.net4j.CDONet4jSession;
import org.eclipse.emf.cdo.net4j.CDONet4jSessionConfiguration;
import org.eclipse.emf.cdo.net4j.CDONet4jUtil;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.CommitException;
import org.eclipse.emf.cdo.util.ConcurrentAccessException;
import org.eclipse.net4j.Net4jUtil;
import org.eclipse.net4j.tcp.ITCPConnector;
import org.eclipse.net4j.tcp.TCPUtil;
import org.eclipse.net4j.util.container.ContainerUtil;
import org.eclipse.net4j.util.container.IManagedContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import edd.AnnotationTypeEnum;
import edd.CategoryAnnotation;
import edd.EddFactory;
import edd.EnergyDataDescription;
import edd.ObjectAnnotation;
import esdl.EsdlFactory;
import esdl.PowerPlant;
import nl.tno.esdl.esdldrive.edr.EnergyDataDescriptionJSON;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EDDAnnotationTest {

	private static final String REPOSITORY_NAME = "esdldrive";
	private final static String BASE_URL = "http://localhost:9080/store/";
	//private final static String RESOURCE = "resource";
	@SuppressWarnings("unused")
	private final static String PUBLIC_RESOURCE = "resource/public/";
	private final static String EDR_QUERY = "edr/query";
	@SuppressWarnings("unused")
	private final static String EDR_BROWSE = "edr/browse";
	private Client client;
	
	private IManagedContainer container;
	private ITCPConnector connector;
	private CDONet4jSessionConfiguration configuration;
	private CDOResourceFolder testFolder;
	private String eddFileName;


	@Before
	public void setUp() throws Exception {
		container = ContainerUtil.createContainer();
		Net4jUtil.prepareContainer(container); // Register Net4j factories
		TCPUtil.prepareContainer(container); // Register TCP factories
		CDONet4jUtil.prepareContainer(container); // Register CDO factories
		container.activate();

		connector = TCPUtil.getConnector(container, "localhost:2036");
		
		// init and register packages
		EsdlFactory.eINSTANCE.eClass();
		EddFactory.eINSTANCE.eClass();

		configuration = CDONet4jUtil.createNet4jSessionConfiguration();
		configuration.setConnector(connector);
		configuration.setRepositoryName(REPOSITORY_NAME); // $NON-NLS-1$
		configuration.setUserID("ewoud");
		
		
		// create test folder in CDO
		CDONet4jSession session = configuration.openNet4jSession();
		CDOTransaction transaction = session.openTransaction();		
		transaction.getOrCreateResourceFolder("/edr/Public");
		testFolder = transaction.getOrCreateResourceFolder("/edr/Public" + "/Test");
		eddFileName = testFolder.getPath() + "/EDDWithAnnotation.edd";
		transaction.commit();
		transaction.close();
		session.close();
		
		client = ClientBuilder.newClient();
	}
	
	@After
	public void tearDown() {
		client.close();
		
	}
	
	

	@Test
	public void testA_CreateEDDWithAnnotation() throws ConcurrentAccessException, CommitException {
		CDONet4jSession session = configuration.openNet4jSession();
		// first create EDD with annotation
		EnergyDataDescription edd = EddFactory.eINSTANCE.createEnergyDataDescription();
		edd.setTitle("Test EDD with Annotation");
		edd.setAuthor("Ewoud Werkman <ewoud.werkman@tno.nl>");
		edd.getTags().add("TEST");
		PowerPlant pp = EsdlFactory.eINSTANCE.createPowerPlant();
		pp.setPower(12_500_000.0);
		pp.setId("powerPlantId");
		ObjectAnnotation objectAnnotation = EddFactory.eINSTANCE.createObjectAnnotation();
		objectAnnotation.setExplanation("The power value is based on several sources and is typically between 10-15 MW. An average between these values is used here");
		objectAnnotation.setAbout(pp);
		objectAnnotation.setAboutAttribute("power");
		edd.getAnnotation().add(objectAnnotation);
		CategoryAnnotation categoryAnnotation = EddFactory.eINSTANCE.createCategoryAnnotation();
		categoryAnnotation.setExplanation("Generic description about this powerplant. It is for year 2030.");
		categoryAnnotation.setType(AnnotationTypeEnum.GENERAL);
		edd.getAnnotation().add(categoryAnnotation);
		edd.setEsdl(pp);
		edd.setEsdlType("PowerPlant");
		edd.setValidityYear(2030);
		
		CDOTransaction transaction = session.openTransaction();
		
		CDOResource cdoResource = transaction.getOrCreateResource(eddFileName);
		cdoResource.getContents().clear();
		cdoResource.getContents().add(edd);
		transaction.commit();
		transaction.close();
		session.close();
		
	}
	
	@Test
	public void testB_ReadEDDWithAnnotation() {
		Response response = client.target(BASE_URL + EDR_QUERY).queryParam("path", eddFileName).request()
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

}
