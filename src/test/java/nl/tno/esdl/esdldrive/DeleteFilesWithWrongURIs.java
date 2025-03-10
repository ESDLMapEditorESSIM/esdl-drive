package nl.tno.esdl.esdldrive;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

import org.eclipse.emf.cdo.common.model.CDOPackageRegistry;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.eresource.CDOResourceFolder;
import org.eclipse.emf.cdo.eresource.CDOResourceNode;
import org.eclipse.emf.cdo.net4j.CDONet4jSession;
import org.eclipse.emf.cdo.net4j.CDONet4jSessionConfiguration;
import org.eclipse.emf.cdo.net4j.CDONet4jUtil;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.CommitException;
import org.eclipse.emf.cdo.util.ConcurrentAccessException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Factory.Registry;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;
import org.eclipse.net4j.Net4jUtil;
import org.eclipse.net4j.connector.IConnector;
import org.eclipse.net4j.tcp.TCPUtil;
import org.eclipse.net4j.util.container.ContainerUtil;
import org.eclipse.net4j.util.container.IManagedContainer;
import org.eclipse.net4j.util.om.OMPlatform;
import org.eclipse.net4j.util.om.log.PrintLogHandler;
import org.eclipse.net4j.util.om.trace.PrintTraceHandler;

import edd.EddPackage;
import esdl.EsdlPackage;

public class DeleteFilesWithWrongURIs {

	private static final String REPOSITORY_NAME = "esdldrive";
	private CDONet4jSessionConfiguration configuration;
	private IConnector connector;
	private IManagedContainer container;
	
	public DeleteFilesWithWrongURIs() throws Exception {
		this.setUp();
		this.deleteStubbornFiles();
		this.shutdown();
	}

	private void shutdown() {
		connector.close();
	    container.deactivate();
	}

	public void setUp() throws Exception {
		// Enable logging and tracing
		OMPlatform.INSTANCE.addLogHandler(PrintLogHandler.CONSOLE);
		OMPlatform.INSTANCE.addTraceHandler(PrintTraceHandler.CONSOLE);
		OMPlatform.INSTANCE.setDebugging(true);

		container = ContainerUtil.createContainer();
		Net4jUtil.prepareContainer(container); // Register Net4j factories
		TCPUtil.prepareContainer(container); // Register TCP factories
		CDONet4jUtil.prepareContainer(container); // Register CDO factories
		container.activate();

		//connector = TCPUtil.getConnector(container, "localhost:2036");
		// for GEIS-01 CDO-server
		connector = TCPUtil.getConnector(container, "localhost:2040");

		configuration = CDONet4jUtil.createNet4jSessionConfiguration();
		configuration.setConnector(connector);
		configuration.setRepositoryName(REPOSITORY_NAME); // $NON-NLS-1$
		configuration.setUserID("ewoud.werkman@tno.nl");
		
		System.out.println("Connected!");
		

	}
	
	
	
	public void deleteStubbornFiles() throws IOException, ConcurrentAccessException, CommitException {
		Scanner scanner = new Scanner(System.in);
		CDONet4jSession session = configuration.openNet4jSession();
		CDOPackageRegistry packageRegistry = session.getPackageRegistry();
		packageRegistry.put("edd", EddPackage.eINSTANCE);
		packageRegistry.put("esdl", EsdlPackage.eINSTANCE);
		CDOTransaction transaction = session.openTransaction(new NonExternalResourceSetImpl());
		Registry resourceFactoryRegistry = transaction.getResourceSet().getResourceFactoryRegistry();
		Map<String,Object> extensionToFactoryMap = resourceFactoryRegistry.getExtensionToFactoryMap();
		extensionToFactoryMap.put("esdl" , new XMLResourceFactoryImpl());
		
		
		String folder_name = "/edr/Public/Assets/Energie & Ruimte/Error - can be deleted/";
		CDOResourceFolder folder = transaction.getResourceFolder(folder_name);
		System.out.println("Files in folder: " + folder_name);
		for (CDOResourceNode node : new ArrayList<CDOResourceNode>(folder.getNodes())) {
			if (node instanceof CDOResource) {
				System.out.println("- " + node.getName());
				
				
				CDOResource resource = transaction.getResource(node.getPath(), false);
	//			URIConverter uriConverter = transaction.getResourceSet().getURIConverter();
	//			URIHandler uriHandler = uriConverter.getURIHandler(URI.createFileURI("to_string.esdl"));
	//			System.out.println(uriHandler);
				
	//			for (Entry<String, Object> entry : extensionToFactoryMap.entrySet()) {
	//				System.out.println(entry.getKey() + ":" + entry.getValue());
	//			}
				System.out.println("? Delete: " + resource);
				String nextLine = scanner.nextLine();
				if (nextLine.toLowerCase().startsWith("y")) {
					resource.delete(null);
					transaction.commit();
					System.out.println("# Deleted: " + resource);
				}
			
			}
		}
		
//		CDOResourceFolder emptyFolder = transaction.getResourceFolder("/Projects/WarmingUp");
//		System.out.println("Delete " + emptyFolder.getPath());
//		emptyFolder.delete(null);
//		System.in.read();
//		
//		CDOResourceFolder wuFolder = transaction.getResourceFolder("/Projects/Wind cost model/WarmingUp");
//		System.out.println("Move " + wuFolder.getPath());
//		wuFolder.setPath("/Projects/WarmingUp");
//		transaction.setCommitComment("Relocate the WarmingUp folder to the correct one in /Projects");
		transaction.commit();
		session.close();
		scanner.close();
		
		
	}

	public static void main(String[] args) throws Exception {
		new DeleteFilesWithWrongURIs();

	}
	
	
	public class NonExternalResourceSetImpl extends ResourceSetImpl {
		
		public NonExternalResourceSetImpl() {
			super();
		}
		
		@Override
		public Resource getResource(URI uri, boolean loadOnDemand) {
			// TODO Auto-generated method stub
			try {
				Resource resource = super.getResource(uri, loadOnDemand);
				return resource;
			} catch (Exception e) {
				System.out.println("getResource() for " + uri);
				Resource resource = super.getResource(uri, false);
				System.err.println("Cannot find " + uri + ", ignore loading this resource!");
				return resource;
			}
		}
	}

}
