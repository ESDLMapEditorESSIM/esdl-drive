package nl.tno.esdl.esdldrive.geometry.dao;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import nl.tno.esdl.esdldrive.geometry.entities.ESDLGeoObjects;

@RequestScoped
public class ESDLGeoObjectsDao {

	private static final int BATCH_SIZE = 100; // see also persistence.xml
	
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
		for (int i = 1; i<=geoObjects.size(); i++) {
			if (i % BATCH_SIZE == 0) {
				// clear entity manager cache
				em.flush();
				em.clear();
			}
			ESDLGeoObjects entity = geoObjects.get(i-1);
			System.out.println("Inserting: " + entity);
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
}
