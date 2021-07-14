package nl.tno.esdl.esdldrive;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;
import org.emfjson.jackson.module.EMFModule;
import org.emfjson.jackson.resource.JsonResource;
import org.emfjson.jackson.resource.JsonResourceFactory;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import edd.EddFactory;
import edd.EddPackage;
import edd.EnergyDataDescription;
import edd.Image;
import esdl.EsdlFactory;
import esdl.EsdlPackage;
import esdl.WindTurbine;
import esdl.WindTurbineTypeEnum;

public class EDRTest {
	
	private static final EddFactory EDR = EddFactory.eINSTANCE;
	ObjectMapper mapper = EMFModule.setupDefaultMapper();

	@Test
	public void testClasses() throws IOException {
		
		EnergyDataDescription energyDataDescription = EddFactory.eINSTANCE.createEnergyDataDescription();
		WindTurbine windTurbine = EsdlFactory.eINSTANCE.createWindTurbine();
		windTurbine.setName("ETM WindTurbine 3MW");
		windTurbine.setType(WindTurbineTypeEnum.WIND_ON_COAST);
		energyDataDescription.setTitle("ETM WindTurbine");
		energyDataDescription.setEsdl(windTurbine);
		Image image = EDR.createImage();
		image.setContentType("image/png");
		energyDataDescription.setImage(image);
		
		System.out.println(eObjectToString(energyDataDescription));
		//System.out.println(eObjectToString(windTurbine));
		
		
		
		System.out.println(eObjectToJSON(energyDataDescription));
		//System.out.println(saveEObjectTpJSON(windTurbine));
		
		
	}
	
	public String eObjectToString(EObject eObject) {
		XMLResourceImpl resource = new XMLResourceImpl();
		resource.getContents().add(eObject);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		HashMap<String, Object> options = new HashMap<String, Object>();
		options.put(XMLResource.OPTION_ENCODING, "UTF-8");
		try {
			resource.save(outputStream, options);
		} catch (IOException e) {
			e.printStackTrace();
		}
		String resourceAsString = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
		return resourceAsString;		
		
	}
	public String eObjectToJSON(EObject eObject) {
		JsonResource resource = new JsonResource(URI.createURI("test.json"), mapper);
		resource.getContents().add(eObject);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		HashMap<String, Object> options = new HashMap<String, Object>();
		try {
			resource.save(outputStream, options);
		} catch (IOException e) {
			e.printStackTrace();
		}
		String resourceAsJson = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
		return resourceAsJson;
	}
	
	@SuppressWarnings("unused")
	private String saveEObjectToJSON(EObject model) throws IOException { 
	    ResourceSet resourceSet = new ResourceSetImpl();
	    resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new JsonResourceFactory());

	    resourceSet.getPackageRegistry().put(EsdlPackage.eINSTANCE.getNsURI(), EsdlPackage.eINSTANCE);
	    resourceSet.getPackageRegistry().put(EddPackage.eINSTANCE.getNsURI(), EddPackage.eINSTANCE);
	    Resource resource = resourceSet.createResource(URI.createURI("*.json"));
	    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	    //InputStream stream = new ByteArrayInputStream(model.getBytes(StandardCharsets.UTF_8));
	    //resource.load(stream, null);
	    resource.getContents().add(model);
	    resource.save(outputStream, Collections.EMPTY_MAP);

	    return outputStream.toString();
	}

}
