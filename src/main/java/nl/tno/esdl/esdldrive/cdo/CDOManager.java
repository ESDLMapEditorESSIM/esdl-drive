/**
 *  This work is based on original code developed and copyrighted by TNO 2020. 
 *  Subsequent contributions are licensed to you by the developers of such code and are
 *  made available to the Project under one or several contributor license agreements.
 *
 *  This work is licensed to you under the Apache License, Version 2.0.
 *  You may obtain a copy of the license at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Contributors:
 *      TNO         - Initial implementation
 *  Manager:
 *      TNO
 */


package nl.tno.esdl.esdldrive.cdo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.CDOObjectHistory;
import org.eclipse.emf.cdo.CDOState;
import org.eclipse.emf.cdo.common.commit.CDOCommitInfo;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.util.CDOResourceNodeNotFoundException;
import org.eclipse.emf.cdo.eresource.CDOBinaryResource;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.eresource.CDOResourceFolder;
import org.eclipse.emf.cdo.eresource.CDOResourceLeaf;
import org.eclipse.emf.cdo.eresource.CDOResourceNode;
import org.eclipse.emf.cdo.eresource.CDOTextResource;
import org.eclipse.emf.cdo.net4j.CDONet4jSession;
import org.eclipse.emf.cdo.net4j.CDONet4jSessionConfiguration;
import org.eclipse.emf.cdo.net4j.CDONet4jUtil;
import org.eclipse.emf.cdo.net4j.ReconnectingCDOSessionConfiguration;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.session.CDOSessionConfiguration.SessionOpenedEvent;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.CDOUtil;
import org.eclipse.emf.cdo.util.CommitException;
import org.eclipse.emf.cdo.util.ConcurrentAccessException;
import org.eclipse.emf.cdo.util.InvalidURIException;
import org.eclipse.emf.cdo.view.CDOQuery;
import org.eclipse.emf.cdo.view.CDORevisionPrefetchingPolicy;
import org.eclipse.emf.cdo.view.CDOView;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.merge.BatchMerger;
import org.eclipse.emf.compare.merge.IMerger;
import org.eclipse.emf.compare.merge.IMerger.Registry;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.net4j.Net4jUtil;
import org.eclipse.net4j.connector.IConnector;
import org.eclipse.net4j.tcp.TCPUtil;
import org.eclipse.net4j.util.WrappedException;
import org.eclipse.net4j.util.container.ContainerUtil;
import org.eclipse.net4j.util.container.IManagedContainer;
import org.eclipse.net4j.util.event.IEvent;
import org.eclipse.net4j.util.event.IListener;
import org.eclipse.net4j.util.lifecycle.ILifecycle;
import org.eclipse.net4j.util.lifecycle.LifecycleEventAdapter;
import org.eclipse.net4j.util.lifecycle.LifecycleUtil;
import org.eclipse.net4j.util.om.OMPlatform;
import org.eclipse.net4j.util.om.log.PrintLogHandler;
import org.eclipse.net4j.util.om.trace.PrintTraceHandler;
import org.emfjson.jackson.module.EMFModule;
import org.emfjson.jackson.resource.JsonResource;

import com.fasterxml.jackson.databind.ObjectMapper;

import edd.EddFactory;
import edd.EddPackage;
import edd.EnergyDataDescription;
import esdl.EnergySystem;
import esdl.EsdlFactory;
import esdl.EsdlPackage;
import esdl.util.EsdlResourceFactoryImpl;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nl.tno.esdl.esdldrive.browser.BrowserNode;
import nl.tno.esdl.esdldrive.browser.BrowserNode.BrowserNodeType;
import nl.tno.esdl.esdldrive.cdo.cache.CachedModel;
import nl.tno.esdl.esdldrive.cdo.cache.LRUCache;
import nl.tno.esdl.esdldrive.cdo.exceptions.MergeConfictException;
import nl.tno.esdl.esdldrive.cdo.exceptions.PermissionDeniedException;
import nl.tno.esdl.esdldrive.edr.SearchESDLType;
import nl.tno.esdl.esdldrive.edr.SortOrder;
import nl.tno.esdl.esdldrive.edr.query.EDRQueryResult;
import nl.tno.esdl.esdldrive.util.ESDLUtil;

/**
 * @author werkmane
 *
 */
//@Singleton
@ApplicationScoped //--> gives timeout errors on the connection....
public class CDOManager {
	
	//private final static long CONCURRENTWRITE_TIMEOUT_MINUTES = 10;
	
// Injecting Does not work somehow?
	@Inject
	@ConfigProperty(name = "CDO_REPOSITORY_NAME" /* , defaultValue = "esdldrive" */)
	private String cdoRepositoryName;

	@Inject
	@ConfigProperty(name = "CDO_SERVER_HOST" /* , defaultValue = "localhost:2036" */)
	private String cdoServerHostName;

	private IConnector connector;
	private IManagedContainer container;
	
	private ConcurrentHashMap<String, SessionDetails> sessionMap = new ConcurrentHashMap<String, SessionDetails>();
	private Map<String, CachedModel> cache = LRUCache.newSynchronizedInstance(200);
	private final EMFCompare emfCompare;
	private final Registry emfCompareMergerRegistry;
	private BasicMonitor emfCompareMonitor;

	private final static CDORevisionPrefetchingPolicy BIG_PREFETCHER = CDOUtil.createRevisionPrefetchingPolicy(1000);
	private static final Logger log = Logger.getLogger(CDOManager.class.getName());
	private static final ObjectMapper jsonMapper = EMFModule.setupDefaultMapper();
	
	/**
	 * TODO: have a look at https://github.com/epsilonlabs/emc-cdo/blob/master/org.eclipse.epsilon.emc.cdo/src/org/eclipse/epsilon/emc/cdo/CDOModel.java
	 * to improve read speed.
	 */
	
	

	public CDOManager() {
		log.info("Injected config: CDO@:" + cdoServerHostName + "/" + cdoRepositoryName);
		cdoServerHostName = ConfigProvider.getConfig().getValue("CDO_SERVER_HOST", String.class);
		cdoRepositoryName = ConfigProvider.getConfig().getValue("CDO_REPOSITORY_NAME", String.class);
		log.info("Config from ConfigProvider: CDO@:" + cdoServerHostName + "/" + cdoRepositoryName);
		log.info("From environment: " + System.getenv("CDO_SERVER_HOST"));
		//setUpCDO();
		
		
		emfCompare = EMFCompare.builder().build();
		emfCompareMergerRegistry = IMerger.RegistryImpl.createStandaloneInstance();
		emfCompareMonitor = new BasicMonitor(); //new BasicMonitor.Printing(System.out);
		
		initModels();
	}
	
	public CDONet4jSession getSession(UserInfo userInfo) {
		SessionDetails sessionDetails = sessionMap.get(userInfo.getUserName());
		if (sessionDetails == null) {
			CDONet4jSessionConfiguration userSessionConfiguration = createUserSessionConfiguration(userInfo);
			sessionDetails = new SessionDetails(userSessionConfiguration, userInfo);
			CDONet4jSession cdoSession = sessionDetails.getCDOSession();
			sessionMap.put(userInfo.getUserName(), sessionDetails);
			log.info("Creating new CDO session configuration for " + userInfo.getUserName());
			ensureUserFoldersPresent(userInfo);
			return cdoSession;
		} else {
			CDONet4jSession cdoSession = sessionDetails.getCDOSession();
			return cdoSession;
		}
	}
	
	public CDONet4jSession getUnManagedSession(UserInfo userInfo) {
		SessionDetails sessionDetails = sessionMap.get(userInfo.getUserName());
		if (sessionDetails != null) {
			return sessionDetails.createUnManagedSession();
		} else {
			return getSession(userInfo);
		}
		
	}
	
	public void logoutSession(UserInfo userInfo) {
		SessionDetails sessionDetails = sessionMap.get(userInfo.getUserName());
		if (sessionDetails != null) 
			sessionDetails.logout();
			sessionMap.remove(userInfo.getUserName());
	}
	
	public Map<String,CachedModel> getCache() {
		return cache; 
	}
	
	public String getCachedModelXML(String path) {
		CachedModel cachedModel = cache.get(path);
		if (cachedModel != null) {
			return cachedModel.getXmlContent();
		}
		return null;
	}
	
	public void setCache(String path, String xmlContent, CDOResource cdoResource) {
		CachedModel cachedModel = cache.get(path);
		if (cachedModel == null) {
			cachedModel = new CachedModel(path, xmlContent, cdoResource);
			cache.put(path, cachedModel);
		} else {
			cachedModel.setXmlContent(xmlContent);
			cachedModel.setResource(cdoResource);
		}
	}


	public synchronized CDOCommitInfo storeResource(UserInfo userInfo, String resourceName, Resource uploadedResource, String commitMessage, boolean overwrite)
			throws ConcurrentAccessException, CommitException, PermissionDeniedException, IllegalStateException, TimeoutException, InterruptedException {
		log.info("Starting storeResource transaction " + resourceName);
		DrivePermission permission = AccessController.getPermission(resourceName, userInfo);
		if (!permission.isWritable()) {
			throw new PermissionDeniedException(permission.getPath() + " is not writable by " + userInfo.getUserName());
		}
		CDONet4jSession session = null;
			// Open session
		session = getSession(userInfo);
		// Open transaction
		CDOTransaction transaction = session.openTransaction();
		//transaction.options().addChangeSubscriptionPolicy(CDOAdapterPolicy.CDO);
		CDOCommitInfo commitInfo = null;
		CDOResource fromDatabase = null;
		try {
			if (overwrite) {
				// overwrite the file, not merge the differences
				commitInfo = storeFullResource(transaction, resourceName, uploadedResource, commitMessage);
			} else {
				// try merging differences
				fromDatabase = transaction.getResource(resourceName);
				// file exists in database. Store only the differences, which is significantly faster. 
				EObject esFromDatabase = fromDatabase.getContents().get(0);
				
				// Create a URI for the uploaded resource, this is needed in the compare
				uploadedResource.setURI(URI.createURI(resourceName));
				EObject uploadedEObject = uploadedResource.getContents().get(0);
				if (esFromDatabase instanceof EnergySystem && uploadedEObject instanceof EnergySystem) {
					EnergySystem energySystem = (EnergySystem) esFromDatabase;
					EnergySystem uploadedES = (EnergySystem) uploadedEObject;
					CDORevision esRevision = energySystem.cdoRevision(true);
					int version = esRevision.getVersion();
					uploadedES.setVersion(Integer.toString(version + 1));
					// update ES version manually: this triggers an update of the EnergySystem CDO object
					// which makes it easier to load revisions of the whole ES as any change also
					// triggers an update to the ES.
					log.info("Setting ES version to " + uploadedES.getVersion());
				}
				if (esFromDatabase instanceof EnergyDataDescription && uploadedEObject instanceof EnergyDataDescription) {
					// for EMFCompare the id needs to be set in order to find equal objects
					EnergyDataDescription edd = (EnergyDataDescription) esFromDatabase;
					//log.info("EMF Compare (pre):  EDD Input path " + resourceName + ", database path: " + fromDatabase.getPath());
					edd.setId(fromDatabase.getPath());
					//log.info("EMF Compare (post): EDD Input path " + resourceName + ", database path: " + fromDatabase.getPath());
				}
				
				log.info("Database: " + esFromDatabase + ", input: " + uploadedEObject);
				// Calculate differences and merge difference. Left = database, right is updated model
				DefaultComparisonScope scope = new DefaultComparisonScope(fromDatabase, uploadedResource, null);
				log.info("Creating comparision...");
				Comparison comparison = emfCompare.compare(scope); //, emfCompareMonitor);
				if (comparison.getConflicts().size() > 0) {
					// conflicts only happen in three-way compare, see org.eclipse.emf.compare.Conflict 
					throw new MergeConfictException("Conflict(s) detected during saving.");
				}
//				var printstream = new PrintStream(System.out);
//				EMFComparePrettyPrinter.printComparison(comparison, printstream);
				log.info("Merging differences: " + comparison.getDifferences().size() );
				// Now testing if this works:
				final BatchMerger batchMerger = new BatchMerger(emfCompareMergerRegistry);
				batchMerger.copyAllRightToLeft(comparison.getDifferences(), emfCompareMonitor);
			
				if (commitMessage == null || commitMessage.isEmpty()) {
					commitMessage = "Update";
				}
				commitMessage += " (" + comparison.getDifferences().size() + " changes)";
				//System.out.println("Resource modified?: " + fromDatabase.isModified());
				transaction.setCommitComment(commitMessage);
				commitInfo = transaction.commit();
			}
		} catch (Exception e) {
			// EMFCompare throws NullPointerException when there are errors in the XML
//			try {
//				if (fromDatabase != null) {
//					String databaseESDL = convertToXMLString(fromDatabase);
//					EObject copy = EcoreUtil.copy(uploadedResource.getContents().get(0));
//					String uploaded = convertEObjectToString(copy);
//					FileUtil.saveStringToFile(databaseESDL, "database.esdl");
//					FileUtil.saveStringToFile(uploaded, "uploaded.esdl");
//				}
//			} catch (IOException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
			if (e instanceof CDOResourceNodeNotFoundException || e instanceof InvalidURIException) {
				log.log(Level.INFO, "Creating new file using full save: " + resourceName);
				commitInfo = storeFullResource(transaction, resourceName, uploadedResource, commitMessage);
			} else if (e instanceof NullPointerException) {
				log.log(Level.SEVERE, "Error saving by merging, trying full save: ", e);
				commitInfo = storeFullResource(transaction, resourceName, uploadedResource, commitMessage);
			} else if (e instanceof IllegalStateException) {
				log.log(Level.SEVERE, "Error saving by merging, trying full save: ", e);
				commitInfo = storeFullResource(transaction, resourceName, uploadedResource, commitMessage);
			} else {
				throw e;
			}
		}
		return commitInfo;
	}

	/**
	 * Writes a uploadedResource to the database, overwriting any earlier data that is contained in this resource.
	 * Only supports one root object in the uploadedResource
	 * @param transaction
	 * @param resourceName
	 * @param uploadedResource
	 * @param commitMessage
	 * @return
	 * @throws ConcurrentAccessException
	 * @throws CommitException
	 */
	private CDOCommitInfo storeFullResource(CDOTransaction transaction, String resourceName, Resource uploadedResource,
			String commitMessage) throws ConcurrentAccessException, CommitException {
		// New file or forced overwrite
		if (commitMessage == null) {
			commitMessage = "Initial commit";
		}
		transaction.setCommitComment(commitMessage);
		CDOResource resource = transaction.getOrCreateResource(resourceName); // $NON-NLS-1$
		// Work with the resource and commit the transaction
		if (resource.getContents().size() == 1) {
			// overwrite
			resource.getContents().set(0, uploadedResource.getContents().get(0));
		} else {
			resource.getContents().add(uploadedResource.getContents().get(0));
		}
		log.info("Committing transaction");
		CDOCommitInfo commitInfo = transaction.commit();
		// Cleanup
		log.info("CommitInfo: " + commitInfo);
		log.info("Ending storeResource transaction");
		return commitInfo;
	}
	
	
//	private void createMergeDiffList(LinkedList<Diff> diffs, Diff diff, int depth) {
//		diffs.addLast(diff);
//		for (Diff req: diff.getRequires()) {
//			if (!diffs.contains(req)) { 
//				diffs.addLast(req);
//				createMergeDiffList(diffs, req, depth+1);
//				//System.out.println("RecursiveMerge depth:"+depth+1);
//			}
//		}
//	}

	/**
	 * @see #getResource(UserInfo, String, Long)
	 * @param resourceName in the form of /folder1/folder2/filename.ext
	 * @return
	 * @throws IOException
	 */
	public CDOResource getResource(UserInfo userInfo, String resourceName) throws CDOResourceNodeNotFoundException, PermissionDeniedException {
		return getResource(userInfo, resourceName, null);
	}
	
	/**
	 * 
	 * @param resourceName in the form of /folder1/folder2/filename.ext
	 * @param revision the revision in milliseconds or null if the latest version is requested.
	 * @return
	 * @throws IOException
	 */
	public CDOResource getResource(UserInfo userInfo, String resourceName, Long revision) throws CDOResourceNodeNotFoundException, PermissionDeniedException {
		//log.info("Starting getResource transaction");
		DrivePermission permission = AccessController.getPermission(resourceName, userInfo);
		if (!(userInfo.isAdmin() || permission.isAccessible())) {
			throw new PermissionDeniedException(resourceName + " is not accessible by " + userInfo.getUserName());
		}
		
		CDONet4jSession session = getSession(userInfo);
		// create view
		
		CDOResource resource = null;
		
		try {
			CDOView view = session.openView();
			view.options().setRevisionPrefetchingPolicy(BIG_PREFETCHER);
			if (revision!=null) {
				// we need some special attention, as the filename might not be the same in earlier revisions
				log.info("Retrieving revision " + revision);
				CDOView revisionView = session.openView(revision);
				if (revisionView.hasResource(resourceName)) {
					// file and path still exist in previous version
					resource = revisionView.getResource(resourceName);
				} else {
					// file has been renamed or moved, get object by ID
					CDOResource currentResource = view.getResource(resourceName);
					CDOObject object = revisionView.getObject(currentResource.cdoID());
					if (object instanceof CDOResource) {
						resource = (CDOResource) object;
					} else if (object instanceof CDOObject) {
						CDOObject energySystem = (CDOObject) object;
						resource = energySystem.cdoResource();
					}
				}
			} else {
				log.info("Retrieving from database: " + resourceName);
				// no revision just the latest version
				resource = view.getResource(resourceName);
			}
			
			// as we gonna serialize the full object to XML, prefetch as much as possible
		
			// Get or create resource
			//CDOResource resource = view.getResource(resourceName); // $NON-NLS-1$
			//resource.cdoPrefetch(2); // Fetch full in steps of 2
			//CDOResourceNode resourceNode = view.getResourceNode(resourceName);
			//log.info("ResourceNode: " + resourceNode);
			Integer version = resource.cdoRevision(true) != null ? resource.cdoRevision(true).getVersion() : null;
			log.info("getResource found: " + resource.getName() + ", version:" + version);

			return resource;
		} catch (Exception e) {
			//e.printStackTrace();
			throw e;
		}

	}

	public String convertToXMLString(CDOResource cdoResource) throws IOException {
		LocalDateTime start = LocalDateTime.now();
		EObject eObject = cdoResource.getContents().get(0);
//		if (eObject instanceof CDOObject) {
//			// prefetch of cdoResource gives timeout, but this seems to work
//			CDOObject cdoObject = (CDOObject) eObject;
//			cdoObject.cdoPrefetch(CDORevision.DEPTH_INFINITE);
//		}
		EObject copy = EcoreUtil.copy(eObject);
		LocalDateTime stop = LocalDateTime.now();
		log.info("Copy " + cdoResource.getPath() + " took " + Duration.between(start, stop));
		String resourceAsString = convertEObjectToString(copy);
		
		stop = LocalDateTime.now();
		log.info("Serializing " + cdoResource.getPath() + " took " + Duration.between(start, stop));
		
		return resourceAsString;
	}

	public static String convertEObjectToString(EObject eObject) throws IOException {
		XMLResourceImpl xmlResourceImpl = new XMLResourceImpl();
		xmlResourceImpl.getContents().add(eObject);
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		HashMap<String, Object> options = new HashMap<String, Object>();
		options.put(XMLResource.OPTION_ENCODING, "UTF-8");
		xmlResourceImpl.save(outputStream, options);

		String resourceAsString = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
		return resourceAsString;
	}
	
	
	public String convertToJSONString(CDOResource cdoResource) throws IOException {
		LocalDateTime start = LocalDateTime.now();
		
		JsonResource resource = new JsonResource(URI.createURI(cdoResource.getName() + ".json"), jsonMapper);
		EObject eObject = cdoResource.getContents().get(0);
		EObject copy = EcoreUtil.copy(eObject);
		resource.getContents().add(copy);
		LocalDateTime stop = LocalDateTime.now();
		log.info("Copy " + cdoResource.getPath() + " took " + Duration.between(start, stop));
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		HashMap<String, Object> options = new HashMap<String, Object>();
		options.put(XMLResource.OPTION_ENCODING, "UTF-8");
		try {
			resource.save(outputStream, options);
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		String resourceAsJson = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
		
		stop = LocalDateTime.now();
		log.info("Serializing to JSON" + cdoResource.getPath() + " took " + Duration.between(start, stop));
		
		return resourceAsJson;
	}

	public boolean existResource(UserInfo userInfo, String resourceURL) throws PermissionDeniedException {
		log.info("Starting existResource transaction");

		CDONet4jSession session = getSession(userInfo);
		// create view
		CDOView view = session.openView();

		DrivePermission permission = AccessController.getPermission(resourceURL, userInfo);
		if (!permission.isWritable()) {
			throw new PermissionDeniedException(permission.getUserGroup() + " is not accessible by " + userInfo.getUserName());
		}
		
		boolean hasResource = view.hasResource(resourceURL);

		// clean up
		view.close();

		return hasResource;
	}
	
	public CDOResourceNode getResourceNode(UserInfo userInfo, String resourceURL) throws PermissionDeniedException {
		if (AccessController.canAccess(resourceURL, userInfo)) {
			CDONet4jSession session = getSession(userInfo);
			CDOView view = session.openView();
			CDOResourceNode resourceNode = view.getResourceNode(resourceURL);
			// check if it is a file or a folder or something else
			return resourceNode;
		} else {
			throw new PermissionDeniedException("Cannot access " + resourceURL);
		}
		
	}
	
	public CDOResource getPublicResource(UserInfo userInfo, String resourceURL) throws PermissionDeniedException, CDOResourceNodeNotFoundException {
		if (resourceURL.startsWith(AccessController.PUBLIC_FOLDER_PATH) || resourceURL.startsWith(AccessController.EDR_PUBLIC_FOLDER_PATH )) {
			try {
				CDONet4jSession session = getSession(userInfo);
				CDOView view = session.openView();
				view.options().setRevisionPrefetchingPolicy(BIG_PREFETCHER);
				CDOResource resource = view.getResource(resourceURL);
				log.info("getPulicResource Retrieved: " + resource.getName());
				return resource;
			} catch (Exception e) {				
				throw e;
			}	
			
		} else {
			throw new PermissionDeniedException("Cannot access " + resourceURL);
		}
	}

	public ResourceMetadata getResourceMetaData(UserInfo userInfo, String resourceURL) throws CDOResourceNodeNotFoundException, PermissionDeniedException {
		log.info("Get metadata for " + resourceURL);
		// Open session 
		// PRE: access is allowed
		CDONet4jSession session = getSession(userInfo);
		CDOView transaction = session.openView();
		CDOResource resource2 = transaction.getResource(resourceURL);
		long timeStamp = resource2.cdoRevision(true).getTimeStamp();
		log.info("HEAD getLastUpdateTime() = " + timeStamp);
		ResourceMetadata resourceMetadata = new ResourceMetadata(timeStamp);
		return resourceMetadata;

	}

	public ArrayList<String> listPath(UserInfo userInfo, String path) {
		CDONet4jSession session = getSession(userInfo);
		CDOTransaction transaction = session.openTransaction();
		CDOResourceFolder resourceFolder = transaction.getResourceFolder(path);
		EList<CDOResourceNode> nodes = resourceFolder.getNodes();
		ArrayList<String> list = new ArrayList<String>();
		for (CDOResourceNode cdoResourceNode : nodes) {
			if (cdoResourceNode instanceof CDOResourceFolder) {
				CDOResourceFolder cdoResourceFolder = (CDOResourceFolder) cdoResourceNode;
				list.add(cdoResourceFolder.getName() + "/");
			} else {
				list.add(cdoResourceNode.getName() + "." + cdoResourceNode.getExtension());
			}
			
		}
		//EList<CDOResourceNode> resourceNodes = ECollections.asEList(elements);
		//resourceNodes.add(nodes);
		return list;
	}

	public ArrayList<String> listResources(UserInfo userInfo) {
		CDONet4jSession session = getSession(userInfo);
		CDOTransaction transaction = session.openTransaction();
		CDOResourceNode[] elements = transaction.getElements();
		ArrayList<String> list = new ArrayList<String>();
		EList<CDOResourceNode> resourceNodes = ECollections.asEList(elements);
		createResourceList(resourceNodes, list, userInfo);
		return list;
	}

	private void createResourceList(EList<CDOResourceNode> nodes, ArrayList<String> list, UserInfo userInfo) {
		for (CDOResourceNode cdoResourceNode : nodes) {
			log.info(cdoResourceNode.eClass().getName() + ": " + cdoResourceNode.getPath());
			if (userInfo.isAdmin() || AccessController.canAccess(cdoResourceNode.getPath(), userInfo)) {
				list.add(cdoResourceNode.getPath());
				if (cdoResourceNode instanceof CDOResourceFolder) {
					CDOResourceFolder cdoResourceFolder = (CDOResourceFolder) cdoResourceNode;
					EList<CDOResourceNode> subNodes = cdoResourceFolder.getNodes();
					createResourceList(subNodes, list, userInfo);
				}
			}
		}
	}
	
	
	private void initModels() {
		// register EDR model
		EddFactory.eINSTANCE.eClass();
		// Register ESDL
		EsdlFactory.eINSTANCE.eClass();
		EPackage.Registry ePackageRegistry = EPackage.Registry.INSTANCE;
		if (ePackageRegistry.get(EsdlPackage.eNS_URI) == null) {
			log.info("Registering ESDL and EDR models.");
			ePackageRegistry.put(EsdlPackage.eNS_URI, EsdlPackage.eINSTANCE);
			ePackageRegistry.put(EddPackage.eNS_URI, EddPackage.eINSTANCE);

			// register esdl resources with extension .esdl
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("esdl", new EsdlResourceFactoryImpl()); 
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("edd", new XMLResourceFactoryImpl()); 
			//Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new EsdlResourceFactoryImpl()); 
		}
		
	}

	//@Initialized(ApplicationScoped.class)
	@PostConstruct
	private void setUpCDO() {
		initModels();
		
		try {
			
			// Enable logging and tracing
			OMPlatform.INSTANCE.setDebugging(true);
			OMPlatform.INSTANCE.addLogHandler(PrintLogHandler.CONSOLE);
			OMPlatform.INSTANCE.addTraceHandler(PrintTraceHandler.CONSOLE);
			//OMPlatform.INSTANCE.isProperty("org.eclipse.emf.cdo.CDOResourceNode.singleExtensions");

			System.out.println("Setting up CDO connection to " + cdoServerHostName + "/" + cdoRepositoryName);
			log.info("Setting up CDO connection to " + cdoServerHostName + "/" + cdoRepositoryName);

			container = ContainerUtil.createContainer();
			Net4jUtil.prepareContainer(container); // Register Net4j factories
			TCPUtil.prepareContainer(container); // Register TCP factories
			CDONet4jUtil.prepareContainer(container); // Register CDO factories
			//OCLQueryHandler.prepareContainer(container); // register OCL query factories
			//CDONet4jServerUtil.prepareContainer(container);
			container.activate();
			//LifecycleUtil.activate(container);

			connector = TCPUtil.getConnector(container, cdoServerHostName);
			LifecycleUtil.activate(connector);

//			configuration = CDONet4jUtil.createNet4jSessionConfiguration();
//			configuration.setConnector(connector);
//			configuration.setRepositoryName(cdoRepositoryName); // $NON-NLS-1$
//			log.info("Signal timeout" + configuration.getSignalTimeout());
//			//configuration.setSignalTimeout(600000);
//			configuration.addListener(cdoEventListener);

			

			
			new SessionDetailsManager(sessionMap);
		} catch (Exception e) {
			log.severe("Error connecting to the CDO Server: " + e);
			e.printStackTrace();
		}

	}
	
	private CDONet4jSessionConfiguration createUserSessionConfiguration(UserInfo userInfo) {
		//CDONet4jSessionConfiguration sessionConfiguration = CDONet4jUtil.createNet4jSessionConfiguration();
		ReconnectingCDOSessionConfiguration sessionConfiguration = CDONet4jUtil.createReconnectingSessionConfiguration(cdoServerHostName, cdoRepositoryName, container);;
		sessionConfiguration.addListener(new IListener() {
			
			@Override
			public void notifyEvent(IEvent event) {
				log.fine("Sessionconfiguration event: " + event);
				
			}
		});
		sessionConfiguration.setReconnectInterval(5000); // wait 5 seconds before trying again
		//sessionConfiguration.setMaxReconnectAttempts(50); // max 50 reconnection attempts
		sessionConfiguration.setConnector(connector);
		sessionConfiguration.setRepositoryName(cdoRepositoryName); // $NON-NLS-1$
		sessionConfiguration.setSignalTimeout(60000);
		log.info("Session signal timeout " + sessionConfiguration.getSignalTimeout());
		sessionConfiguration.addListener(cdoEventListener);
		sessionConfiguration.setUserID(userInfo.getUserName());
		sessionConfiguration.setActivateOnOpen(true);
		// Somehow this is necessary when starting up clean
//		CDOSession session = configuration.openNet4jSession();
//		session.getPackageRegistry().putEPackage(EsdlPackage.eINSTANCE);
//		session.getPackageRegistry().putEPackage(EDRPackage.eINSTANCE); 
//		session.close();
		return sessionConfiguration;		
	}
	
	
	private IListener cdoEventListener = new IListener()
    {
	      @Override
	      public void notifyEvent(IEvent event)
	      {
	    	log.info("CDO Event: " + event);
	        if (event instanceof SessionOpenedEvent)
	        {
	          SessionOpenedEvent e = (SessionOpenedEvent)event;
	          CDOSession session = e.getOpenedSession();
	          log.info("Opened " + session);

	          session.addListener(new LifecycleEventAdapter()
	          {
	            @Override
	            protected void onAboutToDeactivate(ILifecycle lifecycle)
	            {
	            	log.info("Closing " + lifecycle);
	            }
	          });
	        }
	      }
	    };



	@PreDestroy
	private void shutdownCDO() {
		if (connector.isConnected()) {
			log.info("Shutting down CDO connection....");
			//container.deactivate();
			connector.close();
		}
		Iterator<Entry<String, SessionDetails>> iterator = sessionMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, SessionDetails> entry = iterator.next();
			System.out.println("Closing session for: " + entry.getKey());
			entry.getValue().getCDOSession().close(); // close session
			iterator.remove();
		}
	}

	
	public List<BrowserNode> getFolderContents(UserInfo userInfo, String path) {
		return getFolderContents(userInfo, path, 1);		
	}

	public List<BrowserNode> getFolderContents(UserInfo userInfo, String path, int depth) {
		depth = Math.max(0, Math.min(depth, 5)); // max 5 levels deep
		CDONet4jSession session = getSession(userInfo);

		CDOView view = session.openView();
		EList<CDOResourceNode> nodes;
		// WARNING: filters out the EDR folder from the root, to place it below the drive
		if (path.equals("/") || path.equals("") || path.equals("j1_1")) {
			nodes = view.getRootResource().getContents()
					.stream().filter(CDOResourceNode.class::isInstance)
					.map(CDOResourceNode.class::cast)
					.filter(rn -> !rn.getName().equals(AccessController.EDR_FOLDER_NAME))
					.collect(Collectors.toCollection(BasicEList::new));
		} else {		
			CDOResourceNode resourceNode = view.getResourceNode(path);
			CDOResourceFolder folder = null;
			if (resourceNode instanceof CDOResourceFolder) {
				folder = view.getResourceFolder(path);
			} else {
				folder = resourceNode.getFolder();
			}
			if (folder == null) throw new CDOResourceNodeNotFoundException("Can't find requested resource");
			nodes = folder.getNodes();
		}
		ArrayList<BrowserNode> list = new ArrayList<>();
		for (CDOResourceNode node : nodes) {
			DrivePermission permission = AccessController.getPermission(node.getPath(), userInfo);
			if (permission.isAccessible()) {
				BrowserNode browserNode = new BrowserNode(node.getName(), node.getPath());
				browserNode.setWritable(permission.isWritable());
				if (node instanceof CDOResourceFolder) {
					CDOResourceFolder cdoResourceFolder = (CDOResourceFolder) node;
					browserNode.setType(BrowserNodeType.FOLDER);
					browserNode.setDeletable(permission.isDeletable());
					if (cdoResourceFolder.getNodes().size() > 0) {
						browserNode.setHasSubNodes(true);
						// recurse if necessary
						if (depth > 1) {
							List<BrowserNode> subFolderContents = getFolderContents(userInfo, node.getPath(), depth - 1);
							browserNode.setChildren(subFolderContents);
						}
					}
				} else if (node instanceof CDOBinaryResource) {
					browserNode.setType(BrowserNodeType.BINARY);
				} else if (node instanceof CDOTextResource) {
					browserNode.setType(BrowserNodeType.TEXT);
				} else if (node instanceof CDOResource) {
					String extension = getExtension(node.getName());
					browserNode.setType(BrowserNodeType.OTHER);
					//if (extension.equalsIgnoreCase("esdl")) {
					//	browserNode.setType(BrowserNodeType.ESDL_FILE);
					//} else 
					if (extension.equalsIgnoreCase("edd")) {
						browserNode.setType(BrowserNodeType.EDD_FILE);
					}
					if (extension.equalsIgnoreCase("esdl")) {
						browserNode.setType(BrowserNodeType.ESDL_FILE);
					}
				}
				list.add(browserNode);
			}
			
		}
		view.close();
		return list;
	}


	public Map<String, Object> getContentSummary(UserInfo userInfo, String fileName) throws PermissionDeniedException, CDOResourceNodeNotFoundException {
		
		DrivePermission permission = AccessController.getPermission(fileName, userInfo);
		if (!permission.isAccessible()) {
			throw new PermissionDeniedException(permission.getUserGroup() + " is not accessible by " + userInfo.getUserName());
		}
		
		HashMap<String, Object> map = new LinkedHashMap<>(20);
		CDONet4jSession session = getSession(userInfo);
		CDOView view = session.openView();
		CDOResourceNode resourceNode = view.getResourceNode(fileName);
		//System.out.println("ResourceNode: " + resourceNode);
		
				
		if (resourceNode instanceof CDOResourceFolder || fileName.equals("/")) {
			map.put("type", "folder");
			map.put("writable", permission.isWritable());
			map.put("deletable", permission.isDeletable());
			map.put("path", fileName);
			return map;
		}
		
		CDOResource resource = view.getResource(fileName);
			
		log.info("Retrieving content summary for: " + fileName);
		map.put("fileName", resource.getName());
		map.put("path", fileName);
		//map.put("type", getExtension(resource.getName())); // default type based on extension
		EObject rootObject = resource.getContents().get(0); // load content
		map.put("type", getObjectType(rootObject));
		resource.cdoHistory().setLoadCount(1);
		resource.cdoHistory().triggerLoad();
		if (rootObject instanceof CDOObject) {
			CDOObject es = (CDOObject)rootObject; 
			
			es.cdoHistory().setLoadCount(2);
			es.cdoHistory().triggerLoad();
			while (es.cdoHistory().isLoading()) {
				es.cdoHistory().waitWhileLoading(10);
			}
			while (resource.cdoHistory().isLoading()) {
				resource.cdoHistory().waitWhileLoading(10);
			}
			//System.out.println("History size count: " + resource.cdoHistory().size() + "." + es.cdoHistory().size());
			CDOCommitInfo commitInfo = es.cdoHistory().getFirstElement();
			if (commitInfo != null) {
				String commit = "";
				commit += LocalDateTime.ofInstant(Instant.ofEpochMilli(commitInfo.getTimeStamp()), ZoneId.of("Europe/Amsterdam"));
				commit += " / " + commitInfo.getUserID();
				commit += commitInfo.getComment()==null? " / No commit message" : " / " + commitInfo.getComment();
				map.put("lastCommit", commit);
				map.put("revisionVersion", resource.cdoRevision().getVersion() + "." + es.cdoRevision().getVersion());
				map.put("lastChanged", es.cdoRevision().getTimeStamp());
			} else {
				log.info("No revisions present");
			}
		}
		
		
		// get properties of root object (EnergySystem)
		EList<EStructuralFeature> eAllStructuralFeatures = rootObject.eClass().getEAllStructuralFeatures();
		for(EStructuralFeature feature: eAllStructuralFeatures) {
			if (feature instanceof EAttribute && rootObject.eIsSet(feature)) {
				String name = feature.getName();
				name = name.substring(0,1).toUpperCase() + name.substring(1);
				Object value = rootObject.eGet(feature);
				if (value instanceof List) { // unpack string lists e.g. tags in edd 
					List<?> list = (List<?>) value;
					if (list.size() > 0 && list.get(0) instanceof String) {
						@SuppressWarnings("unchecked")
						List<String> stringList = (List<String>)list;
						value = stringList.stream().collect(Collectors.toList());
					} else {
						value = "[not supported]";
					}
					
				}
				map.put(rootObject.eClass().getName() + name, value);
			}
		}
		map.put("esdlType", rootObject.eClass().getName());
		
		
		map.put("writable", permission.isWritable());
		boolean cached = getCache().containsKey(fileName);
		map.put("cached", cached);
		if (cached) {
			map.put("size", ESDLUtil.bytesSizeToString(getCache().get(fileName).getXmlContent().length()));
		}
		view.close();
		return map;
	}
	
	private String getExtension(String resourceName) {
		int lastIndexOf = resourceName.lastIndexOf(".");
		if (lastIndexOf == -1) {
			return "esdl";
		} else {
			return resourceName.substring(lastIndexOf+1);
		}
	}
	
	private String getObjectType(EObject object) {
		return object.eClass().getEPackage().getName().toLowerCase();
//		switch(object.eClass().getEPackage().getNsURI()) {
//		case EsdlPackage.eNS_URI:
//			return 
//		}
	}
	
	
	public String createFolder(UserInfo userInfo, String path) throws ConcurrentAccessException, CommitException, PermissionDeniedException {
		DrivePermission permission = AccessController.getPermission(path, userInfo);
		if (!permission.isWritable()) {
			throw new PermissionDeniedException(permission.getUserGroup() + " is not accessible by " + userInfo.getUserName());
		}
		CDONet4jSession session = getSession(userInfo);
		CDOTransaction transaction = session.openTransaction();
		CDOResourceFolder resourceFolder = transaction.getOrCreateResourceFolder(path);
		String folderName = resourceFolder.getPath();
		transaction.setCommitComment("Created folder " + resourceFolder.getName());
		transaction.commit();
		return folderName;
	}
	
	public String rename(UserInfo userInfo, String path, String newName) throws ConcurrentAccessException, CommitException, CDOResourceNodeNotFoundException, PermissionDeniedException {
		DrivePermission permission = AccessController.getPermission(path, userInfo);
		if (!permission.isWritable() && !permission.isDeletable()) {
			throw new PermissionDeniedException("Action on " + permission.getPath() + " is not permitted by " + userInfo.getUserName());
		}
		CDONet4jSession session = getSession(userInfo);
		CDOTransaction transaction = session.openTransaction();
		try {
			CDOResourceNode resourceNode = transaction.getResourceNode(path);
			transaction.setCommitComment("Renaming " + resourceNode.getName() + " to " + newName);
			String oldPath = resourceNode.getPath();

			// update cache (somewhat complex unfortunately when renaming folders)
			if (resourceNode instanceof CDOResourceLeaf) {
				CachedModel cachedModel = getCache().get(oldPath);
				resourceNode.setName(newName);
				transaction.commit();
				String newPathName = resourceNode.getPath();
				if (cachedModel != null) {
					cachedModel.setPath(newPathName);
					getCache().remove(oldPath);
					getCache().put(newPathName, cachedModel);
				}
			} else {
				// for folders: invalidate cache for the files that this folder contains
				HashMap<CDOResource, CachedModel> renamedResources = new HashMap<>();
				Iterator<Entry<String, CachedModel>> iterator = getCache().entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<String, CachedModel> entry = iterator.next();
					if (entry.getKey().startsWith(oldPath + "/")) { // make sure it ends with a / otherwise also /folder_bla matches /folder
						CachedModel cachedModel = entry.getValue();
						CDOResource resourceNeedsUpdate = transaction.getResource(cachedModel.getPath());
						renamedResources.put(resourceNeedsUpdate, cachedModel);
						iterator.remove();
					}
				}
				log.info("Rename requires " + renamedResources.size() + " cache updates");
				resourceNode.setName(newName);
				transaction.commit();
				//String newPathName = resourceNode.getPath();
				for (Entry<CDOResource, CachedModel> renamedResource : renamedResources.entrySet()) {
					CDOResource cdoResource = renamedResource.getKey();
					CachedModel cachedModel = renamedResource.getValue();
					String newResourcePath = cdoResource.getPath(); // get new resource path 
					cachedModel.setPath(newResourcePath);
					getCache().put(newResourcePath, cachedModel);
					log.info("Rename: updating cache for " + newResourcePath);
				}
			}
			
			return resourceNode.getPath();
		} catch (CDOResourceNodeNotFoundException e) {
			throw e;
		}
	}
	
	public void deleteResource(UserInfo userInfo, String path) throws IOException, CDOResourceNodeNotFoundException, ConcurrentAccessException, CommitException, PermissionDeniedException {
		DrivePermission permission = AccessController.getPermission(path, userInfo);
		if (!permission.isDeletable()) {
			throw new PermissionDeniedException(permission.getPath() + " is not deletable by " + userInfo.getUserName());
		}
		CDONet4jSession session = getSession(userInfo);
		CDOTransaction transaction = session.openTransaction();
		try {
			CDOResourceNode resourceNode = transaction.getResourceNode(path);
			transaction.setCommitComment("Deleting: " + resourceNode.getName());
			log.info("Deleting: " + resourceNode.getPath());
			resourceNode.delete(null);
			if (resourceNode instanceof CDOResourceLeaf) {
				getCache().remove(path);
			} else {
				Iterator<Entry<String, CachedModel>> iterator = getCache().entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<String, CachedModel> entry = iterator.next();
					if (entry.getKey().startsWith(path + "/")) { // make sure it ends with a / otherwise also /folder_bla matches /folder
						iterator.remove();
						log.info("Delete: Invalidating cache for " + entry.getKey());
					}
				}
			}
			transaction.commit();
		} catch (CDOResourceNodeNotFoundException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		}
		
	}


	public String moveNode(UserInfo userInfo, String path, String newParentFolder) throws IOException, ConcurrentAccessException, CommitException, PermissionDeniedException {
		DrivePermission permission = AccessController.getPermission(path, userInfo);
		if (!permission.isWritable()) {
			throw new PermissionDeniedException(permission.getPath() + " is not accessible by " + userInfo.getUserName());
		}
		CDONet4jSession session = getSession(userInfo);
		CDOTransaction transaction = session.openTransaction();
		try {
			CDOResourceNode resourceNode = transaction.getResourceNode(path);
			String fileName = resourceNode.getName();
			//CDOResourceFolder newFolder = transaction.getResourceFolder(newParentFolder);
			//resourceNode.setFolder(newFolder);
			transaction.setCommitComment("Moved " + fileName + " -> " + newParentFolder);
			log.info(userInfo.getUserName() + "- Move node. Moved " + fileName + " -> " + newParentFolder); 
			resourceNode.setPath(newParentFolder + "/" + fileName);
			transaction.commit();
			String newPath =  resourceNode.getPath();
			// update cache
			if (resourceNode instanceof CDOResourceLeaf) {
				CachedModel cachedModel = getCache().get(path);
				if (cachedModel != null) {
					getCache().remove(path);
					cachedModel.setPath(newPath);
					getCache().put(path, cachedModel);
				}
			} else {
				Iterator<Entry<String, CachedModel>> iterator = getCache().entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<String, CachedModel> entry = iterator.next();
					if (entry.getKey().startsWith(path + "/")) { // make sure it ends with a / otherwise also /folder_bla matches /folder
						CachedModel cachedModel = entry.getValue();
						iterator.remove();
						log.info("Move node: Invalidate cache for: " + cachedModel.getPath());
					}
				}
				// TODO: update path of cache instead of just removing it
			}
			return newPath;
		} catch (CDOResourceNodeNotFoundException e) {
			e.printStackTrace();
			throw e;
		}
				
	}
	
	private String createUniqueName(CDOTransaction transaction, String name, String newFolder) {
		CDOResourceFolder resourceFolder = transaction.getResourceFolder(newFolder);
		EList<CDOResourceNode> nodes = resourceFolder.getNodes();
		boolean nameConflict = false;
		Set<String> names = new HashSet<>();
		// iterate through files in this folder and check if name exist, ifso change name, otherwise not
		for (CDOResourceNode node : nodes) {
			if (node.getName().equals(name)) {
				nameConflict = true;
			}
			names.add(node.getName());
		}
		if (nameConflict) {
			String renamed = name + "-copy";
	        for (int i = 0; i < Integer.MAX_VALUE; i++) {
	          String newName = renamed;
	          if (i != 0) {
	        	  newName += i;
	          }
	          if (!names.contains(newName)) {
	            return newName;
	          }
	        }
		}
		return name;
	}


	public String copyNode(UserInfo userInfo, String path, String newParentFolder) throws ConcurrentAccessException, CommitException, PermissionDeniedException {
		DrivePermission permission = AccessController.getPermission(path, userInfo);
		DrivePermission destinationPermission = AccessController.getPermission(newParentFolder, userInfo);
		if (!permission.isAccessible() && !destinationPermission.isWritable()) {
			throw new PermissionDeniedException(destinationPermission.getPath() + " is not accessible or writable by " + userInfo.getUserName());
		}
		CDONet4jSession session = getSession(userInfo);
		CDOTransaction transaction = session.openTransaction();
		try {
			String newPath = null;
			CDOResourceNode resourceNode = transaction.getResourceNode(path);
			if (resourceNode instanceof CDOResourceFolder) {
				// TODO
				log.warning("Copying Folders is not implemented yet");
			} else { // CDOResource or CDOFileResource
				String fileName = resourceNode.getName();
				//CDOResourceFolder newFolder = transaction.getResourceFolder(newParentFolder);
				//resourceNode.setFolder(newFolder);
				CDOResource resource = transaction.getResource(path);
				EObject copy = EcoreUtil.copy(resource.getContents().get(0));
				String uniqueName = createUniqueName(transaction, fileName, newParentFolder);
				CDOResource copyResource = transaction.createResource(newParentFolder + "/" + uniqueName);
				copyResource.getContents().add(copy);
				log.info(userInfo.getUserName() + "- Copy: " + path + " -> " + copyResource.getPath());
				transaction.setCommitComment("Copy " + path + " -> " + newParentFolder);
				transaction.commit();
				newPath = copyResource.getPath();
			}
			
			return newPath;
		} catch (CDOResourceNodeNotFoundException e) {
			e.printStackTrace();
			throw e;
		}
		
	}
	

	public TreeSet<CDOCommitInfo> getRevisions(UserInfo userInfo, String path) throws PermissionDeniedException, CDOResourceNodeNotFoundException {
		DrivePermission permission = AccessController.getPermission(path, userInfo);
		if (!permission.isAccessible()) {
			throw new PermissionDeniedException(permission.getPath() + " is not accessible or writable by " + userInfo.getUserName());
		}
		TreeSet<CDOCommitInfo> commits = new TreeSet<CDOCommitInfo>(
				(o1, o2) -> -Long.compare(o1.getTimeStamp(), o2.getTimeStamp()));
		try {
			CDONet4jSession session = getSession(userInfo);
			CDOView view = session.openView();
			CDOResource resource = view.getResource(path, true);
			EObject eObject = resource.getContents().get(0); // init Resource by loading the root
			CDOObjectHistory resourceHistory = resource.cdoHistory();
			resourceHistory.setLoadCount(20);
			resourceHistory.setAppendingTriggerLoadElement(true);
			resourceHistory.triggerLoad();
			if (eObject instanceof CDOObject) {
				CDOObject rootObject = (CDOObject) eObject;
				CDOObjectHistory esHistory = rootObject.cdoHistory();
				esHistory.setLoadCount(20);
				esHistory.setAppendingTriggerLoadElement(true);
				esHistory.triggerLoad();
				while (esHistory.isLoading()) {
					esHistory.waitWhileLoading(10);
				}				
				for (CDOCommitInfo info: esHistory.getElements()) {
					commits.add(info);
				}
			}
			while (resourceHistory.isLoading()) {
				resourceHistory.waitWhileLoading(10);
			}
			for (CDOCommitInfo info: resourceHistory.getElements()) {
				commits.add(info);
			}
			return commits;
			
		} catch (CDOResourceNodeNotFoundException e) {
			throw e;
		}
	}
	
	
	
	/**
	 * Checks if the folders of this user are available in the repository. If not they are created.
	 * @param userInfo
	 */
	public void ensureUserFoldersPresent(UserInfo userInfo) {
		//System.out.println(userInfo);
		SessionDetails sessionDetails = sessionMap.get(userInfo.getUserName());
		if (sessionDetails != null && !sessionDetails.isFoldersPresent()) {
			CDONet4jSession session = sessionDetails.getCDOSession();
			CDOTransaction transaction = session.openTransaction();
			log.info("Ensuring initial groups for user " + userInfo.getUserName());
			for (String group: userInfo.getReadWriteFolders()) {
				CDOResourceFolder newFolder = transaction.getOrCreateResourceFolder(group);
				if (newFolder.cdoState() == CDOState.NEW) {
					log.info("ensureUserFoldersPresent: Created new Keycloak folder: " + newFolder.getPath() + " [username="+userInfo.getUserName()+"]");
				}
			}
			
			try {
				transaction.setCommitComment("Create initial folders for " + userInfo.getUserName());
				transaction.commit();
				sessionDetails.setFoldersPresent(true);
			} catch (ConcurrentAccessException e) {
				e.printStackTrace();
			} catch (CommitException e) {
				e.printStackTrace();
			}
		}
	}

	
	 @Gauge(name = "model_cache_count", unit = MetricUnits.NONE)
	 private int getCacheCount() {
		 return getCache().size();
	 }
	 
	 @Gauge(name = "model_cache_size", unit = MetricUnits.BYTES)
	 private long getCacheSize() {
		 Map<String, CachedModel> cache = getCache();
		 long size = 0;
		 for (CachedModel cachedModel : cache.values()) {
			size += cachedModel.getXmlContent().length();
		 }
		 return size;
	 }

	public List<EDRQueryResult> query(SearchESDLType type, UserInfo userInfo) {
		CDONet4jSession session = getSession(userInfo);
		CDOView view = session.openView();
		CDOResourceFolder edrFolder = view.getResourceFolder("/"+AccessController.EDR_FOLDER_NAME);
		
		List<EDRQueryResult> queryResult = new ArrayList<EDRQueryResult>();
		
		String tableName = "esdl_" + type.name().toLowerCase();
		
		// EDR assets are direct resources (e.g. not part of an energy system ==> container column not 0), and have the cdo_resource column set)
		// cdo_revised = 0 means that there is no update of this object (e.g. the latest version)
		try {
			CDOQuery query = view.createQuery("sql", "SELECT cdo_id FROM " + tableName + " WHERE cdo_resource <> 0 AND cdo_revised = 0");
			List<Object> result = query.getResult();
			//System.out.println(result.size());
			if (result.size() > 0) System.out.println(result.get(0));
			
			for (Object e: result) {
				if (e instanceof CDOObject) {
					CDOResource directResource = ((CDOObject)e).cdoDirectResource();
					System.out.println("Direct resource: " + directResource);
					if (directResource instanceof CDOResourceNode) {
						CDOResourceNode cdoResourceNode = (CDOResourceNode) directResource;
						System.out.println("Path of search result: " + cdoResourceNode.getPath());
						if (cdoResourceNode.getPath().matches(edrFolder.getPath()+".+")) {
							EDRQueryResult edrQueryResult = new EDRQueryResult(cdoResourceNode.getBasename(), cdoResourceNode.getPath(), type.name());
							queryResult.add(edrQueryResult);
						}
					}
				} 
			}
		} catch (Exception e) {
			System.err.println("Error executing query: " + e.getMessage());
			log.severe(e.getMessage());
		}
		
		view.close();
		return queryResult;
		
		
	}
	
	public List<EnergyDataDescription> oclQueryEDD(UserInfo userInfo, final HashMap<String, String> filter) {
		CDONet4jSession session = getSession(userInfo);
		CDOView view = session.openView();
		
		ArrayList<String> oclFilter = new ArrayList<String>();
		String orderOCL = "";
		SortOrder sortOrder = SortOrder.DEFAULT;
		int limit = 0;
		int offset = 0;
		
		//runQuery(view, "EnergyDataDescription.allInstances()->select(edd:EnergyDataDescription|edd.esdl.oclIsTypeOf(esdl::Pipe))
		//                ->select(edd|edd.esdl.oclAsType(esdl::Pipe).diameter=esdl::PipeDiameterEnum::VALUE_SPECIFIED)");
		String esdlTypeWithPrefix = "";
		String rawType = "";
		String attributeName = null; //diameter";
		String attributeValue = null; //esdl::PipeDiameterEnum::VALUE_SPECIFIED";
		
		
		
		for (Map.Entry<String, String> entry: filter.entrySet()) {
			switch (entry.getKey()) {
			case "tags":
				List<String> tags = List.of(entry.getValue().split(","));
				// For checking for only one tag:
				//oclFilter.add("e.tags.toLowerCase()->includes('" + entry.getValue().toLowerCase() + "')");
				// toLowerCase -> makes search case insensitive
				String oclTagList = tags.stream().map(t -> "'" + t.toLowerCase() + "'").collect(Collectors.joining("," ));
				// AND operation:
				//oclFilter.add("e.tags.toLowerCase()->includesAll(Set{"+tagQuery+"})");
				// OR operation:
				oclFilter.add("e.tags.toLowerCase()->exists(t | Set{"+oclTagList+"}->includes(t))");			
				break;
			case "limit":
				limit = Integer.parseInt(entry.getValue());
				break;
			case "offset":
				offset = Integer.parseInt(entry.getValue());
				break;
			case "order":
				orderOCL = "->sortedBy(e|e.title)";
				sortOrder = SortOrder.valueOf(entry.getValue());
				break;
			case "esdlType":
				rawType = entry.getValue();
				EClassifier eClassifier = EsdlPackage.eINSTANCE.getEClassifier(rawType);
				if (eClassifier != null) {				
					// check if this is a valid ESDL type
					String nsPrefix = eClassifier.getEPackage().getNsPrefix();
					oclFilter.add("e.esdl.oclIsKindOf("+nsPrefix+"::"+eClassifier.getName()+")");
					esdlTypeWithPrefix = nsPrefix+"::"+eClassifier.getName();
				} else {
					throw new IllegalArgumentException("Not a known ESDL class: " + rawType);
				}
				break;
			case "attributeName":
				// query for a specific attribute in the esdl, combined with attributeValue
				attributeName = entry.getValue();
				break;
			case "attributeValue":
				attributeValue = entry.getValue().strip();
				if (attributeValue.contains(" and ")) {
					attributeValue = attributeValue.split(" and ")[0]; // get only the first part for security reasons  
				}
				if (attributeValue.contains(" or ")) {
					attributeValue = attributeValue.split(" or ")[0]; // get only the first part for security reasons  
				}
				break;
			case "path":
				// path is handled below
				break;
			default:
				//edd.title.matches('.*Ewoud.*')
				//eddFilter.add(entry.getKey() + " LIKE '%" + entry.getValue() + "%'");
				// (?i) means case insensitive
				oclFilter.add("e." + entry.getKey() + ".matches('(?i).*" + entry.getValue() + ".*')");
				break;
			}
		}

		if (!esdlTypeWithPrefix.isEmpty()) {
			if (attributeName == null ^ attributeValue == null) {
				throw new IllegalArgumentException("Missing arguments when querying attributeName or attributeValue; esdlType, attributeName and attributeValue should be specified.");
			} else {
		
				// query specific attribute of esdlType
				//->select(edd|edd.esdl.oclAsType(esdl::Pipe).diameter=esdl::PipeDiameterEnum::VALUE_SPECIFIED)
				EClassifier eClassifier = EsdlPackage.eINSTANCE.getEClassifier(rawType);
				if (eClassifier != null && eClassifier instanceof EClass) {	
					EClass eClass = (EClass)eClassifier;
					EStructuralFeature feature = eClass.getEStructuralFeature(attributeName);
					if (feature != null) {
						if ("EString".equals(feature.getEType().getName())) { // EStrings need to be quoted and matched
							System.out.println("type=" + feature.getEType());
							oclFilter.add("e.esdl.oclAsType("+esdlTypeWithPrefix+")."+attributeName+ ".matches('(?i).*" + attributeValue + ".*')");
						} else {
							// TODO: add case for multi valued attributes
							oclFilter.add("e.esdl.oclAsType("+esdlTypeWithPrefix+")."+attributeName+ "=" + attributeValue);
						}
					}
				}
			} 
		}
		
		String oclFilterString = "";
		if (oclFilter.size() > 0) {
			oclFilterString = "->select(e:EnergyDataDescription| " + oclFilter.stream().collect(Collectors.joining(" and ")) + ")";   
		}
		
		String oclQueryString = "edd::EnergyDataDescription.allInstances()" + oclFilterString + orderOCL;
		
		
		try {
			CDOQuery oclQuery = view.createQuery("ocl", oclQueryString, EddPackage.Literals.ENERGY_DATA_DESCRIPTION);
			log.info("EDR OCL query: " + oclQueryString + ", sort="+sortOrder.name());
			List<EnergyDataDescription> oclQueryResult = oclQuery.getResult(EnergyDataDescription.class);
			
			if (sortOrder == SortOrder.DESC) {
				// put elements in new list, as EList does not support the set(i,j,e) operation
				oclQueryResult = new ArrayList<EnergyDataDescription>(oclQueryResult);
				Collections.reverse(oclQueryResult);
			}
			
			// filter results based on path
			if (filter.get("path") != null) {
				oclQueryResult = oclQueryResult.stream().filter(edd -> edd.cdoDirectResource().getPath().startsWith(filter.get("path"))).collect(Collectors.toList());
			}
			// filter results based on accessibility
			List<EnergyDataDescription> accessibleEDD = oclQueryResult.stream().filter(edd -> AccessController.canAccess(edd.cdoDirectResource().getPath(), userInfo)).collect(Collectors.toList());
			
			limit = offset + limit;
			if (limit <= 0) limit = accessibleEDD.size(); // no limit
			limit = Math.min(accessibleEDD.size(), limit); // limit > result size
			log.info("EDR OCL Query total="+oclQueryResult.size()+", accessible="+accessibleEDD.size()+", limit=("+offset + ","+ limit+"), path="+filter.get("path"));
			List<EnergyDataDescription> subList = accessibleEDD.subList(offset, limit);
	
			return subList;
		} catch (WrappedException e) {
			log.severe("Error in OCL query: " + oclQueryString + ", e=" + e.getMessage());
			throw new IllegalArgumentException("Error in OCL query: " + WrappedException.unwrap(e).getMessage());
		}
	}
		
		
	public List<EnergyDataDescription> queryEDD(UserInfo userInfo, final HashMap<String, String> filter) {
		CDONet4jSession session = getSession(userInfo);
		CDOView view = session.openView();
		
		//CDOResourceFolder edrFolder = view.getResourceFolder("/"+EDRResource.EDR_FOLDER_NAME);
		String eddTableName = "edd_EnergyDataDescription";
		String tagsTableName = "edd_energydatadescription_tags_list";
		List<String> tagsFilter = new ArrayList<>(); 
		List<String> eddFilter = new ArrayList<>();
		String limit = "";
		String offset = "";
		String order = "";
		//String esdlType = "";
		for (Map.Entry<String, String> entry: filter.entrySet()) {
			switch (entry.getKey()) {
			case "tag": // tags feature requires joining of tags_list table, tags are in the cdo_value column
			case "tags":
				tagsFilter.add("t.cdo_value" + " LIKE '%" + entry.getValue() + "%'");
				break;
			case "limit":
				limit = " LIMIT " + entry.getValue();
				break;
			case "offset":
				offset = " OFFSET " + entry.getValue();
				break;
			case "order":
				order = " ORDER BY title " + entry.getValue(); // cdo_source is id of resource -> time of creation of edr resource
				break;
			case "esdlType":
				eddFilter.add(entry.getKey() + " = '" + entry.getValue()+ "'");
				//esdlType = entry.getValue();
				// maybe check if this is a valid ESDL type?
				break;
			default:
				eddFilter.add(entry.getKey() + " LIKE '%" + entry.getValue() + "%'");
				break;
			}
		}
		String eddFilterQuery = eddFilter.stream().collect(Collectors.joining(" AND "));
		String tagsFilterQuery = tagsFilter.stream().collect(Collectors.joining(" AND "));
		try {
			String sqlQuery = "SELECT cdo_id FROM " + eddTableName + " WHERE cdo_resource <> 0 AND cdo_revised = 0";
			if (!eddFilter.isEmpty()) sqlQuery += " AND " + eddFilterQuery;
			sqlQuery += order + limit + offset;
			
			// complex query with join when querying for tags
			/*
			  	SELECT o.cdo_id 
				FROM edr_EnergyDataDescription o LEFT JOIN edr_energydatadescription_tags_list t ON o.cdo_id=t.cdo_source
				WHERE o.cdo_resource <> 0 AND o.cdo_revised = 0 AND o.cdo_version=t.cdo_version AND t.cdo_value LIKE '%etm%'  
				WHERE o.cdo_resource <> 0 AND o.cdo_revised = 0 AND t.cdo_value LIKE '%etm%' GROUP BY o.cdo_id;
			*/
			if (!tagsFilterQuery.isEmpty()) {
				String totalFilter = tagsFilterQuery;
				if (!eddFilterQuery.isEmpty()) totalFilter += " AND " + eddFilterQuery;
				// we need to join with the tags table
				sqlQuery = "SELECT o.cdo_id "
						+ "FROM " + eddTableName + " o LEFT JOIN " + tagsTableName + " t ON o.cdo_id=t.cdo_source "
						+ "WHERE o.cdo_resource <> 0 AND o.cdo_revised = 0 AND o.cdo_version=t.cdo_version AND " + totalFilter + order + limit + offset;
			}
			log.info("EDD Query:" + sqlQuery);
			CDOQuery query = view.createQuery("sql", sqlQuery);
//			CDOQuery query = view.createQuery("ocl", "edr::EnergyDataDescription.allInstances()");
//			CDOQuery query = view.createQuery("ocl", "EnergyDataDescription.allInstances()->select(e:EnergyDataDescription|e.tags.includes(self))",
//					"cool");
			List<EnergyDataDescription> result = query.getResult(EnergyDataDescription.class);
//			List<EnergyDataDescription> typeFilteredResult = new ArrayList<>();
//			if (!esdlType.isEmpty()) {
//				ListIterator<EnergyDataDescription> iterator = result.listIterator();
//				while(iterator.hasNext()) {
//					EnergyDataDescription edd = iterator.next();
//					if (edd.getEsdl() == null || (edd.getEsdl() != null && !edd.getEsdl().eClass().getName().equals(esdlType))) {
//						System.out.println("Removing " + edd);
//						//iterator.remove(); // doesnt seem to work
//						
//					} else {
//						System.out.println("Keeping " + edd + " [type=" + edd.getEsdl() + "]");
//						typeFilteredResult.add(edd);
//					}
//				}
//				result = typeFilteredResult;
//			}
			return result;
		} catch (Exception e) {
			System.err.println("Error executing query: " + e.getMessage());
			e.printStackTrace();
			log.severe(e.getMessage());
		}
		return null;
	}
	
	/**
	 * Run a typed OCL query 
	 * @param <T> The type of result e.g. EnergyDataDescription.class
	 * @param userInfo - The UserInfo context of this query (for access control)
	 * @param oclQueryString - query string in OCL
	 * @param clazz The type of result e.g. EnergyDataDescription.class
	 * @param oclContext - Context of the OCL query to help the query succeed (e.g. the reference to self or where to start the query such as EsdlPackage.Literals.ENERGY_SYSTEM)
	 * @return List of type T object from the CDO database
	 */
	public <T> List<T> runQueryT(UserInfo userInfo, String oclQueryString, Class<T> clazz, Object oclContext) {
		CDONet4jSession session = getSession(userInfo);
		CDOView view = session.openView();		
		LocalDateTime start = LocalDateTime.now();
		CDOQuery oclQuery = view.createQuery("ocl", oclQueryString, oclContext);
		oclQuery.setParameter("cdoLazyExtents", true);
		List<T> result = oclQuery.getResult(clazz);		
		System.out.println(userInfo.getUserName() + " (runQueryT OCL): " + oclQueryString + 
				", size = " + result.size() + 
				", time=" + Duration.between(start, LocalDateTime.now()));
		return result;
	}
}
