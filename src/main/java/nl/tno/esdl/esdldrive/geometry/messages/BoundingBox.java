package nl.tno.esdl.esdldrive.geometry.messages;

import java.util.Objects;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

public class BoundingBox {
	@Schema(description="First Longitute coordinate", required=true)
	@NotNull private double lon1;
	@Schema(description="First Lattitude coordinate", required=true)
	@NotNull private double lat1;
	
	@Schema(description="Second Longitute coordinate", required=true)
	@NotNull private double lon2;
	@Schema(description="Second Lattitude coordinate", required=true)
	@NotNull private double lat2;
	
	public BoundingBox() {
	}

	public BoundingBox(@NotNull double lon1, @NotNull double lat1, @NotNull double lon2, @NotNull double lat2) {
		super();
		this.lon1 = lon1;
		this.lat1 = lat1;
		this.lon2 = lon2;
		this.lat2 = lat2;
	}

	

	public double getLon1() {
		return lon1;
	}

	public void setLon1(double lon1) {
		this.lon1 = lon1;
	}

	public double getLat1() {
		return lat1;
	}

	public void setLat1(double lat1) {
		this.lat1 = lat1;
	}

	public double getLon2() {
		return lon2;
	}

	public void setLon2(double lon2) {
		this.lon2 = lon2;
	}

	public double getLat2() {
		return lat2;
	}

	public void setLat2(double lat2) {
		this.lat2 = lat2;
	}

	@Override
	public int hashCode() {
		return Objects.hash(lon1, lon2, lat1, lat2);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BoundingBox other = (BoundingBox) obj;
		return Double.doubleToLongBits(lon1) == Double.doubleToLongBits(other.lon1)
				&& Double.doubleToLongBits(lon2) == Double.doubleToLongBits(other.lon2)
				&& Double.doubleToLongBits(lat1) == Double.doubleToLongBits(other.lat1)
				&& Double.doubleToLongBits(lat2) == Double.doubleToLongBits(other.lat2);
	}

	@Override
	public String toString() {
		return "BoundingBox [lon1=" + lon1 + ", lat1=" + lat1 + ", lon2=" + lon2 + ", lat2=" + lat2 + "]";
	}
	

}
