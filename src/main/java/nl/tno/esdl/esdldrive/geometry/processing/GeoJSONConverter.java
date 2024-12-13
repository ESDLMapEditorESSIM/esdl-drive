package nl.tno.esdl.esdldrive.geometry.processing;

import static org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.geolatte.geom.Feature;
import org.geolatte.geom.G2D;
import org.geolatte.geom.crs.CoordinateReferenceSystem;
import org.geolatte.geom.json.GeoJsonFeature;
import org.geolatte.geom.json.GeoJsonFeatureCollection;
import org.geolatte.geom.json.GeolatteGeomModule;
import org.geolatte.geom.json.Setting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.tno.esdl.esdldrive.geometry.entities.ESDLGeoObjects;

public class GeoJSONConverter {
	
	public static String convert(List<ESDLGeoObjects> list) {
		CoordinateReferenceSystem<G2D> crs = WGS84;
	    ObjectMapper mapper=new ObjectMapper();
	    GeolatteGeomModule module = new GeolatteGeomModule(crs);
	    module.set(Setting.SUPPRESS_CRS_SERIALIZATION, true);
		mapper.registerModule(module);
	    
		ArrayList<Feature<G2D,Long>> geoJsonFeatures = new ArrayList<Feature<G2D,Long>>();
		for (ESDLGeoObjects obj : list) {
			Long id = -1l;
			HashMap<String, Object> attributes = new HashMap<>();
			Field[] fields = obj.getClass().getDeclaredFields();
			for (Field field : fields) {
				if (!field.getName().equals("serialVersionUID") && !field.getName().equals("geometry")) {
					field.setAccessible(true);
					Object value = null;
					try {
						value = field.get(obj);
					} catch (IllegalArgumentException | IllegalAccessException e) {
						e.printStackTrace();
						break;
					}
					if (field.getName() != "id") {
						attributes.put(field.getName(), value);
					} else {
						id = (Long) value;
					}
				}
			}
			
			GeoJsonFeature<G2D, Long> geoJsonFeature = new GeoJsonFeature<>(obj.getGeometry(), id, attributes);
			geoJsonFeatures.add(geoJsonFeature);
		}
		GeoJsonFeatureCollection<G2D, Long> collection = new GeoJsonFeatureCollection<G2D, Long>(geoJsonFeatures);
		try {
			String result = mapper.writeValueAsString(collection);
			return result;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return null;
		}
	}
}
