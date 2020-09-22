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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.inject.Singleton;
import javax.jms.IllegalStateException;

import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.CDOObjectHistory;
import org.eclipse.emf.cdo.common.commit.CDOCommitInfo;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.util.CDOResourceNodeNotFoundException;
import org.eclipse.emf.cdo.eresource.CDOBinaryResource;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.eresource.CDOResourceFolder;
import org.eclipse.emf.cdo.eresource.CDOResourceNode;
import org.eclipse.emf.cdo.eresource.CDOTextResource;
import org.eclipse.emf.cdo.net4j.CDONet4jSession;
import org.eclipse.emf.cdo.net4j.CDONet4jSessionConfiguration;
import org.eclipse.emf.cdo.net4j.CDONet4jUtil;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.session.CDOSessionConfiguration.SessionOpenedEvent;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.CommitException;
import org.eclipse.emf.cdo.util.ConcurrentAccessException;
import org.eclipse.emf.cdo.util.InvalidURIException;
import org.eclipse.emf.cdo.view.CDOAdapterPolicy;
import org.eclipse.emf.cdo.view.CDOView;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.BasicMonitor.Printing;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Conflict;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.internal.merge.MergeMode;
import org.eclipse.emf.compare.internal.merge.MergeOperation;
import org.eclipse.emf.compare.merge.IMerger;
import org.eclipse.emf.compare.merge.IMerger.Registry;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.net4j.Net4jUtil;
import org.eclipse.net4j.connector.IConnector;
import org.eclipse.net4j.tcp.TCPUtil;
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

import esdl.EnergySystem;
import esdl.EsdlFactory;
import esdl.EsdlPackage;
import nl.tno.esdl.esdldrive.browser.BrowserNode;
import nl.tno.esdl.esdldrive.browser.BrowserNode.BrowserNodeType;
import nl.tno.esdl.esdldrive.cdo.cache.CachedModel;
import nl.tno.esdl.esdldrive.cdo.cache.LRUCache;
import nl.tno.esdl.esdldrive.cdo.exceptions.MergeConfictException;
import nl.tno.esdl.esdldrive.cdo.exceptions.PermissionDeniedException;

/**
 * @author werkmane
 *
 */
//@ApplicationScoped
@Singleton
public class CDOManager {
	
	//private final static long CONCURRENTWRITE_TIMEOUT_MINUTES = 10;
	
// Injecting Does not work somehow?
//	@Inject
//	@ConfigProperty(name = "CDO_REPOSITORY_NAME" /* , defaultValue = "mondaine" */)
	private String cdoRepositoryName;

//	@Inject
//	@ConfigProperty(name = "CDO_SERVER_HOST" /* , defaultValue = "localhost:2036" */)
	private String cdoServerHostName;

	private CDONet4jSessionConfiguration configuration;
	private IConnector connector;
	private IManagedContainer container;
	
	private ConcurrentHashMap<String, SessionDetails> sessionMap = new ConcurrentHashMap<String, SessionDetails>();
	private Map<String, CachedModel> cache = LRUCache.newSynchronizedInstance(200);

	private static final Logger log = Logger.getLogger(CDOManager.class.getName());

	public CDOManager() {
		log.info("Injected config: CDO@:" + cdoServerHostName + "/" + cdoRepositoryName);
		cdoServerHostName = ConfigProvider.getConfig().getValue("CDO_SERVER_HOST", String.class);
		cdoRepositoryName = ConfigProvider.getConfig().getValue("CDO_REPOSITORY_NAME", String.class);
		log.info("Config from ConfigProvider: CDO@:" + cdoServerHostName + "/" + cdoRepositoryName);
		log.info("From environment: " + System.getenv("CDO_SERVER_HOST"));
		setUpCDO();
		new SessionDetailsManager(sessionMap);
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


	public synchronized CDOCommitInfo storeResource(UserInfo userInfo, String resourceName, EObject rootObject, String commitMessage, boolean overwrite)
			throws ConcurrentAccessException, CommitException, PermissionDeniedException, IllegalStateException, TimeoutException, InterruptedException {
		log.info("Starting storeResource transaction " + resourceName);
		HubPermission permission = AccessController.getPermission(resourceName, userInfo.getGroups());
		if (!permission.isWritable()) {
			throw new PermissionDeniedException(permission.getPath() + " is not writable by " + userInfo.getUserName());
		}
		CDONet4jSession session = null;
			// Open session
		session = getSession(userInfo);
		// Open transaction
		CDOTransaction transaction = session.openTransaction();
		transaction.options().addChangeSubscriptionPolicy(CDOAdapterPolicy.CDO);
		CDOCommitInfo commitInfo = null;
		try {
			// try merging differences
			CDOResource fromDatabase = transaction.getResource(resourceName);
			//fromDatabase.cdoPrefetch(1);
			//fromDatabase.setTrackingModification(true);
			// file exists in database. Store only the differences, which is significantly faster. 
			EObject esFromDatabase = fromDatabase.getContents().get(0);
			EMFCompare compare = EMFCompare.builder().build();
			Registry mergerRegistry = IMerger.RegistryImpl.createStandaloneInstance();
			Printing monitor = new BasicMonitor.Printing(System.out);
			// Create a URI for the uploaded resource, this is needed in the compare
			rootObject.eResource().setURI(URI.createURI(resourceName));
			if (esFromDatabase instanceof EnergySystem && rootObject instanceof EnergySystem) {
				EnergySystem energySystem = (EnergySystem) esFromDatabase;
				EnergySystem uploadedES = (EnergySystem) rootObject;
				CDORevision esRevision = energySystem.cdoRevision(true);
				int version = esRevision.getVersion();
				uploadedES.setVersion(Integer.toString(version + 1));
				System.out.println("Setting ES version to " + uploadedES.getVersion());
			}
			System.out.println("Database: " + esFromDatabase + ", input: " + rootObject);
			// Calculate differences and merge difference. Left = database, right is updated model
			DefaultComparisonScope scope = new DefaultComparisonScope(esFromDatabase, rootObject, null);
			System.out.println("Creating comparision...");
			Comparison comparison = compare.compare(scope, monitor);
			// conflicts only happen in three-way compare, see org.eclipse.emf.compare.Conflict 
//			String conflicts = "";
//			for(Conflict conflict: comparison.getConflicts()) {
//				System.out.println(conflict);
//				Diff diff = conflict.getDifferences().get(0);
//				conflicts += conflict.getKind() + " " + diff.toString() + ", " ;
//			}
			//comparison.getEquivalences().get(0).get
			if (comparison.getConflicts().size() > 0) {
				throw new MergeConfictException("Conflict(s) detected during saving.");
			}
			System.out.println("Merging differences");
			for (Diff diff: comparison.getDifferences()) {
				recursiveMerge(diff, monitor, mergerRegistry,0);
			}
			
			if (commitMessage == null) {
				commitMessage = "Update";
			}
			commitMessage += " (" + comparison.getDifferences().size() + " changes)";
			System.out.println("Resource modified?: " + fromDatabase.isModified());
			transaction.setCommitComment(commitMessage);
			commitInfo = transaction.commit();
		} catch (CDOResourceNodeNotFoundException | InvalidURIException e) {
			// New file or forced overwrite
			if (commitMessage == null) {
				commitMessage = "Initial commit";
			}
			transaction.setCommitComment(commitMessage);
			CDOResource resource = transaction.getOrCreateResource(resourceName); // $NON-NLS-1$
			// Work with the resource and commit the transaction
			if (resource.getContents().size() == 1) {
				// overwrite
				resource.getContents().set(0, rootObject);
			} else {
				resource.getContents().add(rootObject);
			}
			System.out.println("Committing transaction");
			commitInfo = transaction.commit();
			// Cleanup
			log.info("CommitInfo: " + commitInfo);
			log.info("Ending storeResource transaction");
		}
		return commitInfo;
			
	}
	
	private void recursiveMerge(Diff diff, BasicMonitor monitor, Registry mergerRegistry, int depth) {
		System.out.println("RecursiveMerge depth:"+depth);
		for (Diff req: diff.getRequiredBy()) {
			recursiveMerge(req, monitor, mergerRegistry, depth+1);
		}
		IMerger merger = mergerRegistry.getHighestRankingMerger(diff);
		merger.copyRightToLeft(diff, monitor);
		
	}

	/**
	 * @see #getResource(UserInfo, String, Long)
	 * @param resourceName in the form of /folder1/folder2/filename.ext
	 * @return
	 * @throws IOException
	 */
	public ResourceResult getResource(UserInfo userInfo, String resourceName) throws CDOResourceNodeNotFoundException, PermissionDeniedException {
		return getResource(userInfo, resourceName, null);
	}
	
	/**
	 * 
	 * @param resourceName in the form of /folder1/folder2/filename.ext
	 * @param revision the revision in milliseconds or null if the latest version is requested.
	 * @return
	 * @throws IOException
	 */
	public ResourceResult getResource(UserInfo userInfo, String resourceName, Long revision) throws CDOResourceNodeNotFoundException, PermissionDeniedException {
		log.info("Starting getResource transaction");
		HubPermission permission = AccessController.getPermission(resourceName, userInfo.getGroups());
		if (!permission.isWritable()) {
			throw new PermissionDeniedException(resourceName + " is not accessible by " + userInfo.getUserName());
		}
		
		CDONet4jSession session = getSession(userInfo);
		// create view
		CDOView view = null;
		if (revision!=null) {
			System.out.println("Retrieving revision " + revision);
			view = session.openView(revision);
		} else {
			view = session.openView();
		}

		try {
			// Get or create resource
			CDOResource resource = view.getResource(resourceName); // $NON-NLS-1$
			CDOResourceNode resourceNode = view.getResourceNode(resourceName);
			log.info("ResourceNode: " + resourceNode);
			ResourceResult resourceResult = new ResourceResult(resource, view);
			log.info("getResource Retrieved: " + resource.getName());

			return resourceResult;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

	}

	public String convertToXMLString(CDOResource cdoResource) throws IOException {
		LocalDateTime start = LocalDateTime.now();
		XMLResourceImpl xmlResourceImpl = new XMLResourceImpl();
		EObject copy = EcoreUtil.copy(cdoResource.getContents().get(0));
		LocalDateTime stop = LocalDateTime.now();
		log.info("Copy " + cdoResource.getPath() + " took " + Duration.between(start, stop));
		xmlResourceImpl.getContents().add(copy);
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		HashMap<String, Object> options = new HashMap<String, Object>();
		options.put(XMLResource.OPTION_ENCODING, "UTF-8");
		xmlResourceImpl.save(outputStream, options);

		String resourceAsString = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
		
		stop = LocalDateTime.now();
		log.info("Serializing " + cdoResource.getPath() + " took " + Duration.between(start, stop));
		
		return resourceAsString;
	}

	public boolean existResource(UserInfo userInfo, String resourceURL) throws PermissionDeniedException {
		log.info("Starting existResource transaction");

		CDONet4jSession session = getSession(userInfo);
		// create view
		CDOView view = session.openView();

		HubPermission permission = AccessController.getPermission(resourceURL, userInfo.getGroups());
		if (!permission.isWritable()) {
			throw new PermissionDeniedException(permission.getUserGroup() + " is not accessible by " + userInfo.getUserName());
		}
		
		boolean hasResource = view.hasResource(resourceURL);

		// clean up
		view.close();

		return hasResource;
	}

	public ResourceMetadata getResourceMetaData(UserInfo userInfo, String resourceURL) throws CDOResourceNodeNotFoundException {
		log.info("Get metadata for " + resourceURL);
		// Open session
		CDONet4jSession session = getSession(userInfo);

		/*
		 * // create view CDOView view = session.openView();
		 * 
		 * // Get or create resource CDOResource resource =
		 * view.getResource(resourceURL); // $NON-NLS-1$
		 * 
		 * long timeStamp = resource.getTimeStamp(); log.info("Resource timestamp 1: "+
		 * timeStamp);
		 * 
		 * ResourceMetadata resourceMetadata = new ResourceMetadata(timeStamp);
		 * 
		 * // clean up view.close();
		 */		

		
		// TODO add accesscontrol
		CDOTransaction transaction = session.openTransaction();
		CDOResource resource2 = transaction.getResource(resourceURL);
		log.info("Resource.getTimeStamp() = " + resource2.getTimeStamp());
		long lastUpdateTime = transaction.getLastUpdateTime();
		log.info("Transaction.getLastUpdateTime() = " + lastUpdateTime);
		ResourceMetadata resourceMetadata = new ResourceMetadata(lastUpdateTime);
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
		createResourceList(resourceNodes, list, userInfo.getGroups());
		return list;
	}

	private void createResourceList(EList<CDOResourceNode> nodes, ArrayList<String> list, List<String> accessGroups) {
		for (CDOResourceNode cdoResourceNode : nodes) {
			log.info(cdoResourceNode.eClass().getName() + ": " + cdoResourceNode.getPath());
			if (AccessController.canAccess(cdoResourceNode.getPath(), accessGroups)) {
				list.add(cdoResourceNode.getPath());
				if (cdoResourceNode instanceof CDOResourceFolder) {
					CDOResourceFolder cdoResourceFolder = (CDOResourceFolder) cdoResourceNode;
					EList<CDOResourceNode> subNodes = cdoResourceFolder.getNodes();
					createResourceList(subNodes, list, accessGroups);
				}
			}
		}
	}

	@Initialized(ApplicationScoped.class)
	private void setUpCDO() {

		log.info("Setting up CDO connection to " + cdoServerHostName + "/" + cdoRepositoryName);
		try {
			// Enable logging and tracing

			OMPlatform.INSTANCE.setDebugging(true);
			OMPlatform.INSTANCE.addLogHandler(PrintLogHandler.CONSOLE);
			OMPlatform.INSTANCE.addTraceHandler(PrintTraceHandler.CONSOLE);

			container = ContainerUtil.createContainer();
			Net4jUtil.prepareContainer(container); // Register Net4j factories
			TCPUtil.prepareContainer(container); // Register TCP factories
			CDONet4jUtil.prepareContainer(container); // Register CDO factories
			//CDONet4jServerUtil.prepareContainer(container);
			container.activate();
			//LifecycleUtil.activate(container);

			connector = TCPUtil.getConnector(container, cdoServerHostName);
			LifecycleUtil.activate(connector);

			configuration = CDONet4jUtil.createNet4jSessionConfiguration();
			configuration.setConnector(connector);
			configuration.setRepositoryName(cdoRepositoryName); // $NON-NLS-1$
			log.info("Signal timeout" + configuration.getSignalTimeout());
			configuration.setSignalTimeout(60000);
			configuration.addListener(cdoEventListener);

			// Register ESDL
			CDOSession session = configuration.openNet4jSession();
			// make sure ESDL is loaded here.
			EsdlFactory.eINSTANCE.eClass();
			session.getPackageRegistry().putEPackage(EsdlPackage.eINSTANCE);
			session.close();
		} catch (Exception e) {
			log.severe("Error connecting to the CDO Server: " + e);
		}

	}
	
	private CDONet4jSessionConfiguration createUserSessionConfiguration(UserInfo userInfo) {
		CDONet4jSessionConfiguration sessionConfiguration = CDONet4jUtil.createNet4jSessionConfiguration();
		sessionConfiguration.setConnector(connector);
		sessionConfiguration.setRepositoryName(cdoRepositoryName); // $NON-NLS-1$
		configuration.setSignalTimeout(60000);
		log.info("Signal timeout " + sessionConfiguration.getSignalTimeout());
		sessionConfiguration.addListener(cdoEventListener);
		sessionConfiguration.setUserID(userInfo.getUserName());
		sessionConfiguration.setActivateOnOpen(true);
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
	

	@Destroyed(ApplicationScoped.class)
	private void shutdownCDO() {
		log.info("Shutting down CDO connection");
		connector.close();
		container.deactivate();
	}


	public List<BrowserNode> getFolderContents(UserInfo userInfo, String path) {
		CDONet4jSession session = getSession(userInfo);

		CDOTransaction transaction = session.openTransaction();
		EList<CDOResourceNode> nodes;
		if (path.equals("/") || path.equals("") || path.equals("j1_1")) {
			nodes = transaction.getRootResource().getContents()
					.stream().filter(CDOResourceNode.class::isInstance)
					.map(CDOResourceNode.class::cast)
					.collect(Collectors.toCollection(BasicEList::new));
		} else {		
			CDOResourceFolder resourceNode = transaction.getResourceFolder(path);
			nodes = resourceNode.getNodes();
		}
		ArrayList<BrowserNode> list = new ArrayList<>();
		for (CDOResourceNode node : nodes) {
			HubPermission permission = AccessController.getPermission(node.getPath(), userInfo.getGroups());
			if (permission.isAccessible()) {
				BrowserNode browserNode = new BrowserNode(node.getName(), node.getPath());
				browserNode.setWritable(permission.isWritable());
				if (node instanceof CDOResourceFolder) {
					CDOResourceFolder cdoResourceFolder = (CDOResourceFolder) node;
					browserNode.setType(BrowserNodeType.FOLDER);
					if (cdoResourceFolder.getNodes().size() > 0) {
						browserNode.setHasSubNodes(true);
					}
				} else if (node instanceof CDOBinaryResource) {
					browserNode.setType(BrowserNodeType.BINARY);
				} else if (node instanceof CDOTextResource) {
					browserNode.setType(BrowserNodeType.TEXT);
				} else if (node instanceof CDOResource) {
					browserNode.setType(BrowserNodeType.ESDL_FILE);				
				}
				list.add(browserNode);
			}
			
		}
		return list;
	}


	public Map<String, Object> getContentSummary(UserInfo userInfo, String fileName) throws PermissionDeniedException, CDOResourceNodeNotFoundException {
		
		HubPermission permission = AccessController.getPermission(fileName, userInfo.getGroups());
		if (!permission.isAccessible()) {
			throw new PermissionDeniedException(permission.getUserGroup() + " is not accessible by " + userInfo.getUserName());
		}
		
		HashMap<String, Object> map = new LinkedHashMap<>(20);
		CDONet4jSession session = getSession(userInfo);
		CDOTransaction transaction = session.openTransaction();
		CDOResourceNode resourceNode = transaction.getResourceNode(fileName);
		System.out.println("ResourceNode: " + resourceNode);
				
		if (resourceNode instanceof CDOResourceFolder || fileName.equals("/")) {
			map.put("type", "folder");
			return map;
		}
		
		CDOResource resource = transaction.getResource(fileName);
			
		System.out.println("Retrieving:" + fileName);
		map.put("fileName", resource.getName());
		map.put("path", fileName);
		EObject rootObject = resource.getContents().get(0);
		if (rootObject instanceof CDOObject) {
			CDOObject es = (CDOObject)rootObject; 
			
			es.cdoHistory().setLoadCount(1);
			es.cdoHistory().triggerLoad();
			while (es.cdoHistory().isLoading()) {
				es.cdoHistory().waitWhileLoading(10);
			}
			System.out.println("History size count:" + resource.cdoHistory().size());
			CDOCommitInfo commitInfo = es.cdoHistory().getFirstElement();
			if (commitInfo != null) {
				String commit = "";
				commit += LocalDateTime.ofInstant(Instant.ofEpochMilli(commitInfo.getTimeStamp()), ZoneId.of("Europe/Amsterdam"));
				commit += " / " + commitInfo.getUserID();
				commit += commitInfo.getComment()==null? " / No commit message" : " / " + commitInfo.getComment();
				map.put("lastCommit", commit);
				map.put("revisionVersion", es.cdoRevision().getVersion());
				map.put("lastChanged", es.cdoRevision().getTimeStamp());
			} else {
				System.out.println("No revisions present");
			}
		}
		
		// get properties of root object (EnergySystem)
		EList<EStructuralFeature> eAllStructuralFeatures = rootObject.eClass().getEAllStructuralFeatures();
		for(EStructuralFeature feature: eAllStructuralFeatures) {
			if (feature instanceof EAttribute) {
				String name = feature.getName();
				name = name.substring(0,1).toUpperCase() + name.substring(1);
				map.put(rootObject.eClass().getName() + name, rootObject.eGet(feature));
			}
		}			
		
		map.put("type", resource.getExtension());
		map.put("writable", permission.isWritable());
		return map;
	}
	
	
	public String createFolder(UserInfo userInfo, String path) throws ConcurrentAccessException, CommitException, PermissionDeniedException {
		HubPermission permission = AccessController.getPermission(path, userInfo.getGroups());
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
		HubPermission permission = AccessController.getPermission(path, userInfo.getGroups());
		if (!permission.isWritable()) {
			throw new PermissionDeniedException(permission.getPath() + " is not accessible by " + userInfo.getUserName());
		}
		CDONet4jSession session = getSession(userInfo);
		CDOTransaction transaction = session.openTransaction();
		try {
			CDOResourceNode resourceNode = transaction.getResourceNode(path);
			transaction.setCommitComment("Renamed " + resourceNode.getName() + " to " + newName);
			resourceNode.setName(newName);
			String pathName = resourceNode.getPath();
			transaction.commit();
			return pathName;
		} catch (CDOResourceNodeNotFoundException e) {
			throw e;
		}
	}
	
	public void deleteResource(UserInfo userInfo, String path) throws IOException, CDOResourceNodeNotFoundException, ConcurrentAccessException, CommitException, PermissionDeniedException {
		HubPermission permission = AccessController.getPermission(path, userInfo.getGroups());
		if (!permission.isWritable()) {
			throw new PermissionDeniedException(permission.getPath() + " is not accessible by " + userInfo.getUserName());
		}
		CDONet4jSession session = getSession(userInfo);
		CDOTransaction transaction = session.openTransaction();
		try {
			CDOResourceNode resourceNode = transaction.getResourceNode(path);
			transaction.setCommitComment("Deleted " + resourceNode.getName());
			resourceNode.delete(null);
			getCache().remove(path);
			transaction.commit();
		} catch (CDOResourceNodeNotFoundException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		}
		
	}


	public String moveNode(UserInfo userInfo, String path, String newParentFolder) throws IOException, ConcurrentAccessException, CommitException, PermissionDeniedException {
		HubPermission permission = AccessController.getPermission(path, userInfo.getGroups());
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
			resourceNode.setPath(newParentFolder + "/" + fileName);
			System.out.println("New path: " + resourceNode.getPath()); 
			transaction.commit();
			String newPath =  resourceNode.getPath();
			// update cache
			CachedModel cachedModel = getCache().get(path);
			if (cachedModel != null) {
				getCache().remove(path);
				cachedModel.setPath(newPath);
				getCache().put(path, cachedModel);
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
		HubPermission permission = AccessController.getPermission(path, userInfo.getGroups());
		HubPermission destinationPermission = AccessController.getPermission(newParentFolder, userInfo.getGroups());
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
				System.out.println("Copying Folders is not implemented yet");
			} else { // CDOResource or CDOFileResource
				String fileName = resourceNode.getName();
				//CDOResourceFolder newFolder = transaction.getResourceFolder(newParentFolder);
				//resourceNode.setFolder(newFolder);
				CDOResource resource = transaction.getResource(path);
				EObject copy = EcoreUtil.copy(resource.getContents().get(0));
				String uniqueName = createUniqueName(transaction, fileName, newParentFolder);
				CDOResource copyResource = transaction.createResource(newParentFolder + "/" + uniqueName);
				copyResource.getContents().add(copy);
				System.out.println("Copy: New path: " + copyResource.getPath());
				transaction.setCommitComment("Copy " + fileName + " -> " + newParentFolder);
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
		HubPermission permission = AccessController.getPermission(path, userInfo.getGroups());
		if (!permission.isAccessible()) {
			throw new PermissionDeniedException(permission.getPath() + " is not accessible or writable by " + userInfo.getUserName());
		}
		TreeSet<CDOCommitInfo> commits = new TreeSet<CDOCommitInfo>(
				(o1, o2) -> -Long.compare(o1.getTimeStamp(), o2.getTimeStamp()));
		try {
			CDONet4jSession session = getSession(userInfo);
			CDOView view = session.openView();
			CDOResource resource = view.getResource(path,true);
			EObject eObject = resource.getContents().get(0); // init Resource by loading the root
			CDOObjectHistory resourceHistory = resource.cdoHistory();
			resourceHistory.setLoadCount(20);
			resourceHistory.setAppendingTriggerLoadElement(true);
			resourceHistory.triggerLoad();
			if (eObject instanceof EnergySystem) {
				EnergySystem energySystem = (EnergySystem) eObject;
				CDOObjectHistory esHistory = energySystem.cdoHistory();
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
		CDONet4jSession session = getSession(userInfo);
		CDOTransaction transaction = session.openTransaction();
		log.info("Ensuring initial groups for user " + userInfo.getUserName());
		for (String group: userInfo.getGroups()) {
			transaction.getOrCreateResourceFolder(group);
		}
		
		try {
			transaction.setCommitComment("Created folder");
			transaction.commit();
		} catch (ConcurrentAccessException e) {
			e.printStackTrace();
		} catch (CommitException e) {
			e.printStackTrace();
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

}
