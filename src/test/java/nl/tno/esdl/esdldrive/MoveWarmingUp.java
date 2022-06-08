package nl.tno.esdl.esdldrive;

import java.io.IOException;

import org.eclipse.emf.cdo.eresource.CDOResourceFolder;
import org.eclipse.emf.cdo.net4j.CDONet4jSession;
import org.eclipse.emf.cdo.net4j.CDONet4jSessionConfiguration;
import org.eclipse.emf.cdo.net4j.CDONet4jUtil;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.CommitException;
import org.eclipse.emf.cdo.util.ConcurrentAccessException;
import org.eclipse.net4j.Net4jUtil;
import org.eclipse.net4j.connector.IConnector;
import org.eclipse.net4j.tcp.TCPUtil;
import org.eclipse.net4j.util.container.ContainerUtil;
import org.eclipse.net4j.util.container.IManagedContainer;
import org.eclipse.net4j.util.om.OMPlatform;
import org.eclipse.net4j.util.om.log.PrintLogHandler;
import org.eclipse.net4j.util.om.trace.PrintTraceHandler;

public class MoveWarmingUp {

	private static final String REPOSITORY_NAME = "esdldrive";
	private CDONet4jSessionConfiguration configuration;
	private IConnector connector;
	private IManagedContainer container;
	
	public MoveWarmingUp() throws Exception {
		this.setUp();
		this.moveFolder();
		this.shutdown();
	}

	private void shutdown() {
		connector.close();
	    container.deactivate();
	}

	public void setUp() throws Exception {
		// Enable logging and tracing
		OMPlatform.INSTANCE.setDebugging(true);
		OMPlatform.INSTANCE.addLogHandler(PrintLogHandler.CONSOLE);
		OMPlatform.INSTANCE.addTraceHandler(PrintTraceHandler.CONSOLE);

		container = ContainerUtil.createContainer();
		Net4jUtil.prepareContainer(container); // Register Net4j factories
		TCPUtil.prepareContainer(container); // Register TCP factories
		CDONet4jUtil.prepareContainer(container); // Register CDO factories
		container.activate();

		connector = TCPUtil.getConnector(container, "localhost:3036");

		configuration = CDONet4jUtil.createNet4jSessionConfiguration();
		configuration.setConnector(connector);
		configuration.setRepositoryName(REPOSITORY_NAME); // $NON-NLS-1$
		configuration.setUserID("ewoud.werkman@tno.nl");
		
		System.out.println("Connected!");
		

	}
	
	
	public void moveFolder() throws IOException, ConcurrentAccessException, CommitException {
		CDONet4jSession session = configuration.openNet4jSession();
		CDOTransaction transaction = session.openTransaction();
		CDOResourceFolder emptyFolder = transaction.getResourceFolder("/Projects/WarmingUp");
		System.out.println("Delete " + emptyFolder.getPath());
		emptyFolder.delete(null);
		System.in.read();
		
		CDOResourceFolder wuFolder = transaction.getResourceFolder("/Projects/Wind cost model/WarmingUp");
		System.out.println("Move " + wuFolder.getPath());
		wuFolder.setPath("/Projects/WarmingUp");
		transaction.setCommitComment("Relocate the WarmingUp folder to the correct one in /Projects");
		transaction.commit();
		session.close();
		System.in.read();
		
		
	}

	public static void main(String[] args) throws Exception {
		new MoveWarmingUp();

	}

}
