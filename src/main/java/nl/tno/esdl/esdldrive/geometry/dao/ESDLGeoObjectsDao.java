package nl.tno.esdl.esdldrive.geometry.dao;

import java.util.List;
import java.util.logging.Logger;

import org.geolatte.geom.G2D;
import org.geolatte.geom.LineString;

import jakarta.enterprise.context.RequestScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import nl.tno.esdl.esdldrive.geometry.entities.ESDLGeoObjects;
import nl.tno.esdl.esdldrive.geometry.messages.BoundingBox;
import nl.tno.esdl.esdldrive.geometry.processing.ConvertEnergySystemToGeometryAssets;

@RequestScoped
public class ESDLGeoObjectsDao {

	private static final int BATCH_SIZE = 1000; // see also persistence.xml
	
	private Logger log = Logger.getLogger(this.getClass().getName());
	
	@PersistenceContext(name = "jpa-unit")
	private EntityManager em;

	public void createGeoObject(ESDLGeoObjects geoObject) {
		em.persist(geoObject);
	}

	public ESDLGeoObjects readGeoObject(long primaryKey) {
		return em.find(ESDLGeoObjects.class, primaryKey);
	}

	public void updateGeoObject(ESDLGeoObjects geoObject) {
		em.merge(geoObject);
	}

	public void deleteGeoObject(ESDLGeoObjects geoObject) {
		em.remove(geoObject);
	}
	
	public void createAllGeoObjects(List<ESDLGeoObjects> geoObjects) {
		log.info("Persisting " + geoObjects.size() + " geo objects");
		for (int i = 1; i<=geoObjects.size(); i++) {
			if (i % BATCH_SIZE == 0) {
				// clear entity manager cache
				em.flush();
				em.clear();
				log.info(" ... persisting at " + ( Math.round(i/(double)geoObjects.size()*100.0)) + " %");
			}
			ESDLGeoObjects entity = geoObjects.get(i-1);
			//System.out.println("Inserting: " + entity);
			//em.setProperty("jakarta.persistence.query.timeout", 480000); // msec
			em.persist(entity);
		}
	}

	public List<ESDLGeoObjects> readAllAssets() {
		return em.createNamedQuery("ESDLGeoObjects.findAll", ESDLGeoObjects.class).getResultList();
	}

	public List<ESDLGeoObjects> findAssets(String energySystemId) {
		return em.createNamedQuery("ESDLGeoObjects.findAssetsForES", ESDLGeoObjects.class)
				.setParameter("energySystemId", energySystemId)
				.getResultList();
	}

	public List<ESDLGeoObjects> findAssetsFilter(String energySystemId, String assetType) {
		return em.createNamedQuery("ESDLGeoObjects.findAssetsForESFilter", ESDLGeoObjects.class)
				.setParameter("energySystemId", energySystemId)
				.setParameter("assetType", assetType)
				.getResultList();
	}

	public List<ESDLGeoObjects> findAssetsWithin(String energySystemId, BoundingBox bbox) {
		LineString<G2D> lineString = ConvertEnergySystemToGeometryAssets.createGeoLatteLineString(bbox);
		return em.createNamedQuery("ESDLGeoObjects.findAssetsForESWithin", ESDLGeoObjects.class)
				.setParameter("energySystemId", energySystemId)
				.setParameter("bbox", lineString)
				.getResultList();
	}
	public List<ESDLGeoObjects> findAssetsWithinFilter(String energySystemId, BoundingBox bbox, String assetType) {
		LineString<G2D> lineString = ConvertEnergySystemToGeometryAssets.createGeoLatteLineString(bbox);
		return em.createNamedQuery("ESDLGeoObjects.findAssetsForESWithinFilter", ESDLGeoObjects.class)
				.setParameter("energySystemId", energySystemId)
				.setParameter("bbox", lineString)
				.setParameter("assetType", assetType)
				.getResultList();
	}

	public List<ESDLGeoObjects> getAssetById(String energySystemId, String assetId) {
		return em.createNamedQuery("ESDLGeoObjects.getAsset", ESDLGeoObjects.class)
				.setParameter("energySystemId", energySystemId)
				.setParameter("assetId", assetId)
				.getResultList();
	}

	public boolean hasEnergySystem(String energySystemId) {
		try {
			ESDLGeoObjects result = em.createNamedQuery("ESDLGeoObjects.hasEnergySystem", ESDLGeoObjects.class)	
				.setParameter("energySystemId", energySystemId)
				.setMaxResults(1)
				.getSingleResult();
			if (result != null) {
				return true;
			} else {
				return false;
			}
			
		} catch (NoResultException nre) {
			return false;
		}
	}
	
	public int deleteEnergySystem(String energySystemId) {
		return em.createNamedQuery("ESDLGeoObjects.deleteEnergySystem")
				.setParameter("energySystemId", energySystemId)
				.executeUpdate();
	}
}
