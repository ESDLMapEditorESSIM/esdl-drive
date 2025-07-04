package nl.tno.esdl.esdldrive.scripts;

import java.io.IOException;

import org.eclipse.emf.cdo.net4j.CDONet4jSessionConfiguration;
import org.eclipse.emf.cdo.net4j.CDONet4jUtil;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.net4j.Net4jUtil;
import org.eclipse.net4j.tcp.ITCPConnector;
import org.eclipse.net4j.tcp.TCPUtil;
import org.eclipse.net4j.util.container.ContainerUtil;
import org.eclipse.net4j.util.container.IManagedContainer;
import org.eclipse.net4j.util.om.OMPlatform;
import org.eclipse.net4j.util.om.log.PrintLogHandler;
import org.eclipse.net4j.util.om.trace.PrintTraceHandler;

import edd.EddPackage;
import esdl.EsdlPackage;

public class CDOConnection {
	
	private IManagedContainer container;
	private ITCPConnector connector;
	private CDONet4jSessionConfiguration configuration;
	private static final String REPOSITORY_NAME = "esdldrive";

	public CDOSession setupCDOSession() throws IOException {
		// Delete of http://www.tno.nl/esdl#//BuildingTypePercentage in old cdo_objects
		// db: {cdo_class=-697, cdo_id=2495388, cdo_created=1614697201663}
		OMPlatform.INSTANCE.setDebugging(true);
		OMPlatform.INSTANCE.addLogHandler(PrintLogHandler.CONSOLE);
		OMPlatform.INSTANCE.addTraceHandler(PrintTraceHandler.CONSOLE);

		container = ContainerUtil.createContainer();
		Net4jUtil.prepareContainer(container); // Register Net4j factories
		TCPUtil.prepareContainer(container); // Register TCP factories
		CDONet4jUtil.prepareContainer(container); // Register CDO factories
		container.activate();

		connector = TCPUtil.getConnector(container, "localhost:2036");

		configuration = CDONet4jUtil.createNet4jSessionConfiguration();
		configuration.setConnector(connector);
		configuration.setRepositoryName(REPOSITORY_NAME); // $NON-NLS-1$
		configuration.setUserID("ewoud");

		System.out.println("Connected, opening session...");
		CDOSession session = configuration.openNet4jSession();
		session.getPackageRegistry().putEPackage(EsdlPackage.eINSTANCE);
		session.getPackageRegistry().putEPackage(EddPackage.eINSTANCE);
		
		return session;
	}
	public void shutdownSession(CDOSession session) {
		session.close();
		container.deactivate();
	}
}



