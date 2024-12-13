package nl.tno.esdl.esdldrive.geometry.entities;

import java.io.Serializable;
import java.util.Objects;

import org.geolatte.geom.G2D;
import org.geolatte.geom.Geometry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
	name = "esdl_geo_objects", 
	uniqueConstraints = { @UniqueConstraint(columnNames = { "energy_system_id", "asset_id" }) }, 
	indexes = @Index(name = "es_asset_id_index", columnList = "energy_system_id, asset_id")
)
@NamedQuery(name = "ESDLGeoObjects.findAll", query = "SELECT a FROM ESDLGeoObjects a")
@NamedQuery(name = "ESDLGeoObjects.findAssetsForES", query = "SELECT a FROM ESDLGeoObjects a WHERE a.energySystemId = :energySystemId")
@NamedQuery(name = "ESDLGeoObjects.getAsset", query = "SELECT a FROM ESDLGeoObjects a WHERE a.energySystemId = :energySystemId and a.assetId = :assetId")
@NamedQuery(name = "ESDLGeoObjects.hasEnergySystem", query = "SELECT a FROM ESDLGeoObjects a WHERE a.energySystemId = :energySystemId")
public class ESDLGeoObjects implements Serializable {


	private static final long serialVersionUID = 1L;

	@Id // SEQUENCE because of batch inserts in Hibernate
	@GeneratedValue(strategy = GenerationType.SEQUENCE)  
	private Long id;

	@Column(name = "energy_system_id", nullable = false)
	private String energySystemId;

	@Column(name = "asset_id", nullable = false)
	private String assetId;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "asset_type", nullable = false)
	private String assetType;

	@Column(name = "capability_type", nullable = true)
	private String capabilityType;

	@Column(name = "geometry", nullable = false)
//	@Type(type = "jts_geometry")
	private Geometry<G2D> geometry;

	// private portList // {'name': port.name, 'id': port.id, 'type':
	// type(port).__name__, 'conn_to': connTo_ids, 'carrier': carrier_id})

	@Column(name = "state", nullable = true)
	@Enumerated(EnumType.STRING)
	private esdl.AssetStateEnum state;

	// JSONObject extra_attributes

	public ESDLGeoObjects() {
	}

	public ESDLGeoObjects(String energySystemId, String assetId, String name, String assetType, String capabilityType,
			Geometry<G2D> geometry) {
		this.energySystemId = energySystemId;
		this.assetId = assetId;
		this.name = name;
		this.assetType = assetType;
		this.capabilityType = capabilityType;
		this.geometry = geometry;
	}

	public String getEnergySystemId() {
		return energySystemId;
	}

	public void setEnergySystemId(String energySystemId) {
		this.energySystemId = energySystemId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAsset_id() {
		return assetId;
	}

	public void setAsset_id(String assetId) {
		this.assetId = assetId;
	}

	public String getAssetType() {
		return assetType;
	}

	public void setAssetType(String assetType) {
		this.assetType = assetType;
	}

	public String getCapabilityType() {
		return capabilityType;
	}

	public void setCapabilityType(String capabilityType) {
		this.capabilityType = capabilityType;
	}


	public Geometry<G2D> getGeometry() {
		return geometry;
	}

	public void setGeometry(Geometry<G2D> geometry) {
		this.geometry = geometry;
	}

	public esdl.AssetStateEnum getState() {
		return state;
	}

	public void setState(esdl.AssetStateEnum state) {
		this.state = state;
	}

	public Long getId() {
		return id;
	}
	
	

	@Override
	public int hashCode() {
		return Objects.hash(assetId, assetType, capabilityType, energySystemId, geometry, name, state);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ESDLGeoObjects other = (ESDLGeoObjects) obj;
		return Objects.equals(assetId, other.assetId) && Objects.equals(assetType, other.assetType)
				&& Objects.equals(capabilityType, other.capabilityType)
				&& Objects.equals(energySystemId, other.energySystemId) && Objects.equals(geometry, other.geometry)
				&& Objects.equals(name, other.name) && state == other.state;
	}

	@Override
	public String toString() {
		return "ESDLGeoObjects [id=" + id + ", energySystemId=" + energySystemId + ", assetId=" + assetId + ", name="
				+ name + ", assetType=" + assetType + ", capabilityType=" + capabilityType + ", geometry=" + geometry
				+ ", state=" + state + "]";
	}

}
