package nl.tno.esdl.esdldrive.geometry.messages;

import jakarta.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name="GeometryRequest", description="Request to get geometries from an ESDL file stored in the ESDL drive as GeoJson within an optional boundingbox and/or zoom level")
public class GeometryRequest {
	
	
	@NotNull
	@Schema(description="Path of the ESDL file in the ESDL Drive", required=true)
	private String path;
	
	@Schema(description="Optional bounding box in which the geometries should return in (long,lat), (long, lat) format", required=false)
	private BoundingBox bbox;
	
	@Schema(description="Optional zoom level", required=false)
	private int zoom;
	
	@Schema(description="Filter result Geojson on esdl asset types: e.g. Area, or Transformer, default returns all assets in one GeoJson Feature Collection", required=false)
	private String assetType;
	
	
	public GeometryRequest() {}
	
	public GeometryRequest(String path) {
		this.path = path;
	}
	
	public GeometryRequest(@NotNull String path, BoundingBox bbox) {
		super();
		this.path = path;
		this.bbox = bbox;
	}
	

	public GeometryRequest(@NotNull String path, BoundingBox bbox, int zoom) {
		super();
		this.path = path;
		this.bbox = bbox;
		this.zoom = zoom;
	}

	public GeometryRequest(@NotNull String path, int zoom) {
		super();
		this.path = path;
		this.zoom = zoom;
	}

	public String getPath() {
		return path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}

	public BoundingBox getBbox() {
		return bbox;
	}

	public void setBbox(BoundingBox bbox) {
		this.bbox = bbox;
	}

	public int getZoom() {
		return zoom;
	}

	public void setZoom(int zoom) {
		this.zoom = zoom;
	}

	public String getAssetType() {
		return assetType;
	}

	public void setAssetType(String assetType) {
		this.assetType = assetType;
	}

}
