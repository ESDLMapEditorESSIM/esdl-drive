package nl.tno.esdl.esdldrive.geometry.messages;

import javax.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name="GeometryRequest", description="Request to get geometries from an ESDL file stored in the ESDL drive as GeoJson")
public class GeometryRequest {
	@NotNull
	@Schema(description="Path of the ESDL file in the ESDL Drive", required=true)
	private String path;
	
	public GeometryRequest() {}
	
	public GeometryRequest(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}

}
