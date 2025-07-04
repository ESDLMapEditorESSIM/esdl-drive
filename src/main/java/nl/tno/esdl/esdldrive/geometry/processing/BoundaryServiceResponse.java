package nl.tno.esdl.esdldrive.geometry.processing;

import org.geolatte.geom.Geometry;

public class BoundaryServiceResponse {

	private String code; //: "GM1680", 
	private String name; //: "Aa en Hunze",

	// as geolatte geometries deserializer are jackson based, use ObjectMapper to parse Json, 
	// instead of the Jakarta Jsonb implementation
	//@JsonDeserialize(using = FeatureDeserializer.class)
	private Geometry<?> geom; // "geom":
	
	public BoundaryServiceResponse() {
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Geometry<?> getGeom() {
		return geom;
	}

	public void setGeom(Geometry<?> geom) {
		this.geom = geom;
	}
	
	
}
