package nl.tno.esdl.esdldrive.geometry;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.junit.Test;

import esdl.EnergySystem;
import esdl.EsdlPackage;
import esdl.util.EsdlResourceImpl;
import nl.tno.esdl.esdldrive.geometry.entities.ESDLGeoObjects;
import nl.tno.esdl.esdldrive.geometry.processing.ConvertEnergySystemToGeometryAssets;

public class GeometryParserTest {

	@Test
	public void testGeometryParsing() throws IOException {
		EsdlPackage.eINSTANCE.eClass(); // load ESDL package
		System.out.print("Loading file...");
		//EsdlResourceImpl ecwResource = new EsdlResourceImpl(URI.createFileURI("c:/Users/werkmane/OneDrive - TNO/Documents/ESDL/EnergySystems/ECW.esdl"));
		//EsdlResourceImpl resource = new EsdlResourceImpl(URI.createFileURI("C:/Users/werkmane/OneDrive - TNO/Documents/ESDL/EnergySystems/RvE/Noord-Brabant ETM QaU_refs.esdl"));
		EsdlResourceImpl resource = new EsdlResourceImpl(URI.createFileURI("C:\\Users\\werkmane\\OneDrive - TNO\\Documents\\ESDL\\EnergySystems\\RvE\\Drenthe ETM refs.esdl"));
		resource.getDefaultLoadOptions().put(XMLResource.OPTION_DEFER_IDREF_RESOLUTION, Boolean.TRUE);
		resource.setIntrinsicIDToEObjectMap(new HashMap<String, EObject>());
		resource.load(null);
		System.out.println("done.");
		EnergySystem ecw = (EnergySystem)resource.getContents().get(0);
		List<ESDLGeoObjects> geoObjects = ConvertEnergySystemToGeometryAssets.convert(ecw, "http://geis-01.hesi.energy:4003/");
		System.out.println("Length of geoObjects list: " + geoObjects.size());
		for (ESDLGeoObjects o : geoObjects) {
			System.out.println(o.getAssetType() + "." + o.getName() + " " + o.getGeometry());
			
		}
		
	}
	
	@Test
	public void testPolygonCreation() throws IOException {
		EsdlPackage.eINSTANCE.eClass(); // load ESDL package
		System.out.print("Loading file...");
		//EsdlResourceImpl ecwResource = new EsdlResourceImpl(URI.createFileURI("c:/Users/werkmane/OneDrive - TNO/Documents/ESDL/EnergySystems/ECW.esdl"));
		//EsdlResourceImpl resource = new EsdlResourceImpl(URI.createFileURI("C:/Users/werkmane/OneDrive - TNO/Documents/ESDL/EnergySystems/RvE/Noord-Brabant ETM QaU_refs.esdl"));
		// /esdl-drive/src/test/resources/esdl/Example North Sea Energy System.esdl
		EsdlResourceImpl resource = new EsdlResourceImpl(URI.createURI("src/test/resources/esdl/Example North Sea Energy System.esdl"));
		resource.getDefaultLoadOptions().put(XMLResource.OPTION_DEFER_IDREF_RESOLUTION, Boolean.TRUE);
		resource.setIntrinsicIDToEObjectMap(new HashMap<String, EObject>());
		resource.load(null);
		System.out.println("done.");
		EnergySystem es = (EnergySystem)resource.getContents().get(0);
		List<ESDLGeoObjects> geoObjects = ConvertEnergySystemToGeometryAssets.convert(es, "http://geis-01.hesi.energy:4003/");
		System.out.println("Length of geoObjects list: " + geoObjects.size());
		for (ESDLGeoObjects o : geoObjects) {
			System.out.println(o.getName() + " " + o.getGeometry());
			
		}
	}

}
