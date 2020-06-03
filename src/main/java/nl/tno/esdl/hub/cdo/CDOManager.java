/**
 * 
 */
package nl.tno.esdl.hub.cdo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.common.commit.CDOCommitInfo;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.util.CDOResourceNodeNotFoundException;
import org.eclipse.emf.cdo.eresource.CDOBinaryResource;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.eresource.CDOResourceFolder;
import org.eclipse.emf.cdo.eresource.CDOResourceNode;
import org.eclipse.emf.cdo.eresource.CDOTextResource;
import org.eclipse.emf.cdo.net4j.CDONet4jSessionConfiguration;
import org.eclipse.emf.cdo.net4j.CDONet4jUtil;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.session.CDOSessionConfiguration.SessionOpenedEvent;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.CommitException;
import org.eclipse.emf.cdo.util.ConcurrentAccessException;
import org.eclipse.emf.cdo.view.CDOView;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
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
import esdl.EsdlPackage;
import nl.tno.esdl.hub.browser.BrowserNode;
import nl.tno.esdl.hub.browser.BrowserNode.BrowserNodeType;

/**
 * @author werkmane
 *
 */
//@ApplicationScoped
@Singleton
public class CDOManager {

	@Inject
	@ConfigProperty(name = "cdo.repository.name" /* , defaultValue = "mondaine" */)
	private String cdoRepositoryName;

	@Inject
	@ConfigProperty(name = "cdo.server.host" /* , defaultValue = "localhost:2036" */)
	private String cdoServerHostName;

	private CDONet4jSessionConfiguration configuration;
	private IConnector connector;
	private IManagedContainer container;

	private static final Logger log = Logger.getLogger(CDOManager.class.getName());

	public CDOManager() {
		cdoServerHostName = ConfigProvider.getConfig().getValue("cdo.server.host", String.class);
		cdoRepositoryName = ConfigProvider.getConfig().getValue("cdo.repository.name", String.class);
		log.info("CDO@:" + cdoServerHostName + "/" + cdoRepositoryName);
		setUpCDO();
	}


	public CDOCommitInfo storeResource(String resourceName, EObject rootObject)
			throws ConcurrentAccessException, CommitException {
		log.info("Starting storeResource transaction");

		// Open session
		CDOSession session = configuration.openNet4jSession();

		// Open transaction
		CDOTransaction transaction = session.openTransaction();

		// Get or create resource
		CDOResource resource = transaction.getOrCreateResource(resourceName); // $NON-NLS-1$

		// Work with the resource and commit the transaction
		if (resource.getContents().size() == 1) {
			// overwrite
			resource.getContents().set(0, rootObject);
		} else {
			resource.getContents().add(rootObject);
		}
		CDOCommitInfo commitInfo = transaction.commit();
		resource.setTimeStamp(commitInfo.getTimeStamp());

		// Cleanup
		log.info("CommitInfo: " + commitInfo);
		log.info("Ending storeResource transaction");
		transaction.close();
		session.close();

		return commitInfo;
	}

	/**
	 * 
	 * @param resourceName in the form of /folder1/folder2/filename.ext
	 * @return
	 * @throws IOException
	 */
	public ResourceResult getResource(String resourceName) throws Exception {
		log.info("Starting getResource transaction");

		// Open session
		CDOSession session = configuration.openNet4jSession();

		// create view
		CDOView view = session.openView();

		try {
			// Get or create resource
			CDOResource resource = view.getResource(resourceName); // $NON-NLS-1$
			CDOResourceNode resourceNode = view.getResourceNode(resourceName);
			log.info("ResourceNode: " + resourceNode);
			CDOObject cdoObject = view.getObject(resourceNode.cdoID());
			log.info("CDOObject: " + cdoObject);
			log.info("Timestamp: " + resource.getTimeStamp());

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

	public boolean existResource(String resourceURL) {
		log.info("Starting existResource transaction");

		// Open session
		CDOSession session = configuration.openNet4jSession();

		// create view
		CDOView view = session.openView();

		boolean hasResource = view.hasResource(resourceURL);

		// clean up
		view.close();
		session.close();

		return hasResource;
	}

	public ResourceMetadata getResourceMetaData(String resourceURL) throws CDOResourceNodeNotFoundException {
		log.info("Get metadata for " + resourceURL);
		// Open session
		CDOSession session = configuration.openNet4jSession();

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

		CDOTransaction transaction = session.openTransaction();
		CDOResource resource2 = transaction.getResource(resourceURL);
		log.info("Resource.getTimeStamp() = " + resource2.getTimeStamp());
		long lastUpdateTime = transaction.getLastUpdateTime();
		log.info("Transaction.getLastUpdateTime() = " + lastUpdateTime);
		transaction.close();
		ResourceMetadata resourceMetadata = new ResourceMetadata(lastUpdateTime);
		session.close();
		return resourceMetadata;

	}

	public ArrayList<String> listPath(String path) {
		CDOSession session = configuration.openNet4jSession();
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
		transaction.close();
		session.close();
		return list;
	}

	public ArrayList<String> listResources() {
		CDOSession session = configuration.openNet4jSession();
		CDOTransaction transaction = session.openTransaction();
		CDOResourceNode[] elements = transaction.getElements();
		ArrayList<String> list = new ArrayList<String>();
		EList<CDOResourceNode> resourceNodes = ECollections.asEList(elements);
		createResourceList(resourceNodes, list);
		transaction.close();
		session.close();
		return list;
	}

	private void createResourceList(EList<CDOResourceNode> nodes, ArrayList<String> list) {
		for (CDOResourceNode cdoResourceNode : nodes) {
			log.info(cdoResourceNode.eClass().getName() + ": " + cdoResourceNode.getPath());
			list.add(cdoResourceNode.getPath());
			if (cdoResourceNode instanceof CDOResourceFolder) {
				CDOResourceFolder cdoResourceFolder = (CDOResourceFolder) cdoResourceNode;
				EList<CDOResourceNode> subNodes = cdoResourceFolder.getNodes();
				createResourceList(subNodes, list);
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
			configuration.addListener(new IListener()
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
		    });

			// Register ESDL
			CDOSession session = configuration.openNet4jSession();
			session.getPackageRegistry().putEPackage(EsdlPackage.eINSTANCE);
			session.close();
		} catch (Exception e) {
			log.severe("Error connecting to the CDO Server: " + e);
		}

	}

	@Destroyed(ApplicationScoped.class)
	private void shutdownCDO() {
		log.info("Shutting down CDO connection");
		connector.close();
		container.deactivate();
	}


	public List<BrowserNode> getFolderContents(String path) {
		CDOSession session = configuration.openNet4jSession();
		CDOTransaction transaction = session.openTransaction();
		EList<CDOResourceNode> nodes;
		if (path.equals("/") || path.equals("")) {
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
			BrowserNode browserNode = new BrowserNode(node.getName(), node.getPath());
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
		transaction.close();
		session.close();
		return list;
	}


	public Map<String, Object> getContentSummary(String fileName) {
		HashMap<String, Object> map = new HashMap<>();
		if (!fileName.endsWith(".esdl")) return map;
		
		map.put("type", "esdl");
		map.put("path", fileName);
		CDOSession session = configuration.openNet4jSession();
		CDOTransaction transaction = session.openTransaction();
		CDOResource resource = transaction.getResource(fileName);
		if (resource == null) {
			
			map.put("fileName", fileName);
			map.put("content", fileName + " not found");
			return map;
		}
		System.out.println("Retreiving " + fileName);
		System.out.print("getting revisions");
		resource.cdoPrefetch( CDORevision.DEPTH_INFINITE);
		System.out.println("done");
//		resource.cdoHistory().triggerLoad();
//		
//		while (resource.cdoHistory().isLoading()) {
//			try {
//				Thread.sleep(10);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
		EnergySystem es = (EnergySystem) resource.getContents().get(0);
		//System.out.println("ES name: " + es.getName());
		map.put("fileName", resource.getName());
		map.put("Revision version", resource.cdoRevision().getVersion());
		map.put("Last saved", LocalDateTime.ofInstant(Instant.ofEpochMilli(resource.cdoRevision().getTimeStamp()), ZoneId.systemDefault()).toString());
		
		EList<EStructuralFeature> eAllStructuralFeatures = es.eClass().getEAllStructuralFeatures();
		for(EStructuralFeature feature: eAllStructuralFeatures) {
			if (feature instanceof EAttribute) {
				map.put(feature.getName(), es.eGet(feature));
			}
		}				
		transaction.close();
		session.close();
		return map;
	}
	
	
	public String createFolder(String path) throws ConcurrentAccessException, CommitException {
		CDOSession session = configuration.openNet4jSession();
		CDOTransaction transaction = session.openTransaction();
		CDOResourceFolder resourceFolder = transaction.getOrCreateResourceFolder(path);
		String folderName = resourceFolder.getPath();
		transaction.commit();
		session.close();
		return folderName;
	}
	
	public String rename(String path, String newName) throws ConcurrentAccessException, CommitException, CDOResourceNodeNotFoundException {
		CDOSession session = configuration.openNet4jSession();
		CDOTransaction transaction = session.openTransaction();
		try {
			CDOResourceNode resourceNode = transaction.getResourceNode(path);
			resourceNode.setName(newName);
			String pathName = resourceNode.getPath();
			transaction.commit();
			session.close();
			return pathName;
		} catch (CDOResourceNodeNotFoundException e) {
			session.close();
			throw e;
		}
	}
	
	public void deleteResource(String path) throws IOException, CDOResourceNodeNotFoundException, ConcurrentAccessException, CommitException {
		CDOSession session = configuration.openNet4jSession();
		CDOTransaction transaction = session.openTransaction();
		try {
			CDOResourceNode resourceNode = transaction.getResourceNode(path);
			resourceNode.delete(null);
			transaction.commit();
			session.close();
		} catch (CDOResourceNodeNotFoundException e) {
			session.close();
			throw e;
		} catch (IOException e) {
			session.close();
			throw e;
		}
		
	}


	public String moveNode(String path, String newParentFolder) throws IOException, ConcurrentAccessException, CommitException {
		CDOSession session = configuration.openNet4jSession();
		CDOTransaction transaction = session.openTransaction();
		try {
			CDOResourceNode resourceNode = transaction.getResourceNode(path);
			CDOResourceFolder newFolder = transaction.getResourceFolder(newParentFolder);
			resourceNode.setFolder(newFolder);
			resourceNode.setPath(newParentFolder);
			transaction.commit();
			String newPath =  resourceNode.getPath();
			session.close();
			return newPath;
		} catch (CDOResourceNodeNotFoundException e) {
			session.close();
			throw e;
		}
				
	}

}
