package nl.tno.esdl.esdldrive.cdo;

import java.util.List;
import java.util.logging.Logger;

import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.CommitException;
import org.eclipse.emf.cdo.util.ConcurrentAccessException;
import org.eclipse.emf.cdo.view.CDOQuery;
import org.eclipse.emf.cdo.view.CDOView;

import edd.ESDLDriveMetaData;
import edd.EddFactory;
import edd.EddPackage;
import edd.ResourceMetaData;
import nl.tno.esdl.esdldrive.util.OCLUtil;



public class EObjectMetaData {
	
	public final static String METADATA_PREFIX = "_metadata";
	private final static String METADATA_RESOURCE = METADATA_PREFIX + "/metadata.edd";
	private boolean init = false;
	
	private static final Logger log = Logger.getLogger(EObjectMetaData.class.getName());
	
	public EObjectMetaData() {
		
	}
	
	private void ensureESDLDriveMetaData(CDOTransaction transaction) throws ConcurrentAccessException, CommitException {
		CDOResource resource = transaction.getOrCreateResource(METADATA_RESOURCE);
		if (resource.getContents().size() == 0) {
			ESDLDriveMetaData edmd = EddFactory.eINSTANCE.createESDLDriveMetaData();
			edmd.setName("ESDL Drive Meta Data");
			resource.getContents().add(edmd);
			transaction.commit();
			this.init = true;
		}
	}
	

	public ResourceMetaData getMetaData(CDOView view, String path) {
		//CDOQueryUtil
		EddPackage.eINSTANCE.eClass(); // load ED metadata model
		CDOQuery query = view.createQuery("ocl", "ResourceMetaData.allInstances()->select(rmd:ResourceMetaData | rmd.resourcePath='" + OCLUtil.escapeOCLPath(path) + "')", EddPackage.Literals.RESOURCE_META_DATA);
		//query.setParameter("path", path);
		List<ResourceMetaData> result = query.getResult(ResourceMetaData.class);
		log.info("Querying metadata: retrieved " + result.size() + " elements");
		if (result.size() > 0) {
			return result.get(0);
		} else {
			return null;
		}
		
	}
	
	public void deleteMetaData(CDOTransaction transaction, ResourceMetaData md) {
		log.info("Updating metadata: removing 1 element: " + md.getResourcePath());
		CDOResource resource = transaction.getOrCreateResource(METADATA_RESOURCE);
		ESDLDriveMetaData edmd = (ESDLDriveMetaData) resource.getContents().get(0);
		edmd.getMetadata().remove(md);
		
	}
	
	public void saveMetaData(CDOTransaction transaction, String path, int size, int objectCount) throws ConcurrentAccessException, CommitException {
		if (!init) ensureESDLDriveMetaData(transaction);
		if (path == null) {
			return; // if path==null, no meta data
		}
		ResourceMetaData md = getMetaData(transaction, path);
		if (md == null) {
			md = EddFactory.eINSTANCE.createResourceMetaData();
			CDOResource resource = transaction.getOrCreateResource(METADATA_RESOURCE);
			ESDLDriveMetaData edmd = (ESDLDriveMetaData) resource.getContents().get(0);
			edmd.getMetadata().add(md);
		}
		md.setObjectCount(objectCount);
		md.setResourcePath(path);
		md.setByteSize(size);
		//CDOQuery query = view.createQuery("SQL", "INSERT INTO edmetadata_metadata (path, ) VALUES (:path");
		//query.setParameter("path", path);
		transaction.commit();
		
	}
	
	public void saveResourceMetaData(CDOTransaction transaction, ResourceMetaData metadata) throws ConcurrentAccessException, CommitException {
		if (!init) ensureESDLDriveMetaData(transaction);
		if (metadata == null) {
			return; // if null, no meta data
		}
		ResourceMetaData md = getMetaData(transaction, metadata.getResourcePath());
		if (md == null) {
			CDOResource resource = transaction.getOrCreateResource(METADATA_RESOURCE);
			ESDLDriveMetaData edmd = (ESDLDriveMetaData) resource.getContents().get(0);
			edmd.getMetadata().add(metadata);
		} else {
			md.setObjectCount(metadata.getObjectCount());
			md.setResourcePath(metadata.getResourcePath());
			md.setByteSize(metadata.getByteSize());
		}
			
		transaction.commit();
	}

	public void deleteMetaDataPath(CDOTransaction transaction, String path) throws ConcurrentAccessException, CommitException {
		EddPackage.eINSTANCE.eClass(); // load ED metadata model
		CDOQuery query = transaction.createQuery("ocl", "ResourceMetaData.allInstances()->select(rmd:ResourceMetaData | rmd.resourcePath.startsWith='" + OCLUtil.escapeOCLPath(path) + "')", EddPackage.Literals.RESOURCE_META_DATA);
		//query.setParameter("path", path);
		List<ResourceMetaData> result = query.getResult(ResourceMetaData.class);
		log.info("Updating metadata: removing " + result.size() + " elements");
		CDOResource resource = transaction.getOrCreateResource(METADATA_RESOURCE);
		ESDLDriveMetaData edmd = (ESDLDriveMetaData) resource.getContents().get(0);
		result.stream().forEach(edmd.getMetadata()::remove);	
		transaction.commit();
	}

}
//final EcoreFactory efactory = EcoreFactory.eINSTANCE;
//final EcorePackage epackage = EcorePackage.eINSTANCE;
//
//EClass metadata = efactory.createEClass();
//metadata.setName("MetaData");
//
//EAttribute path = efactory.createEAttribute();
//path.setName("path");
//path.setEType(epackage.getEString());
//metadata.getEStructuralFeatures().add(path);
//
//EAttribute size = efactory.createEAttribute();
//size.setName("size");
//size.setEType(epackage.getEInt());
//metadata.getEStructuralFeatures().add(size);
//
//EAttribute objectCount = efactory.createEAttribute();
//objectCount.setName("objectCount");
//objectCount.setEType(epackage.getEInt());
//metadata.getEStructuralFeatures().add(objectCount);
//
//EPackage esdlMetadataPackage = efactory.createEPackage();
//esdlMetadataPackage.setName("esdlmetadata");
//esdlMetadataPackage.setNsPrefix("esdlmd");
//esdlMetadataPackage.setNsURI("http:///www.tno.nl/esdlmd");
//esdlMetadataPackage.getEClassifiers().add(metadata);
//EPackage.Registry.INSTANCE.put(esdlMetadataPackage.getNsURI(), esdlMetadataPackage);