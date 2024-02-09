package nl.tno.esdl.esdldrive;
import java.io.File;
import java.io.IOException;

import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.eresource.CDOResourceFolder;
import org.eclipse.emf.cdo.net4j.CDONet4jSessionConfiguration;
import org.eclipse.emf.cdo.net4j.CDONet4jUtil;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.CommitException;
import org.eclipse.emf.cdo.util.ConcurrentAccessException;
import org.eclipse.emf.cdo.view.CDOView;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
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
import esdl.util.EsdlResourceImpl;
import nl.tno.esdl.esdldrive.util.CDOQueryUtil;

public class CalculationExample {

	private IManagedContainer container;
	private CDONet4jSessionConfiguration configuration;
	private static final String REPOSITORY_NAME = "esdldrive";

	public CalculationExample() throws IOException, ConcurrentAccessException, CommitException {

		OMPlatform.INSTANCE.setDebugging(true);
		OMPlatform.INSTANCE.addLogHandler(PrintLogHandler.CONSOLE);
		OMPlatform.INSTANCE.addTraceHandler(PrintTraceHandler.CONSOLE);

		container = ContainerUtil.createContainer();
		Net4jUtil.prepareContainer(container); // Register Net4j factories
		TCPUtil.prepareContainer(container); // Register TCP factories
		CDONet4jUtil.prepareContainer(container); // Register CDO factories
		container.activate();

		ITCPConnector connector = TCPUtil.getConnector(container, "localhost:2036");

		configuration = CDONet4jUtil.createNet4jSessionConfiguration();
		configuration.setConnector(connector);
		configuration.setRepositoryName(REPOSITORY_NAME); // $NON-NLS-1$
		configuration.setUserID("ewoud");

		CDOSession session = configuration.openNet4jSession();
		session.getPackageRegistry().putEPackage(EsdlPackage.eINSTANCE);
		session.getPackageRegistry().putEPackage(EddPackage.eINSTANCE);

		// EnergyDataDescription edd =
		// EddFactory.eINSTANCE.createEnergyDataDescription();

		CDOView view = session.openView();
		//QueryUtil.getTableName(EsdlPackage.eINSTANCE.getKPI());
		//view.queryInstances(null);
		//view.createQuery("sql", "SELECT * FROM")
		
		//"C:\data\git\RvE\scripts\ETM sankey to ESDL\data\ETM_Bergen op Zoom_Roosendaal_Sankey.esdl"
		
		CDOQueryUtil queryUtil = new CDOQueryUtil();
		String tableName = queryUtil.getTableName(EsdlPackage.eINSTANCE.getKPIs(), EsdlPackage.eINSTANCE.getKPI_Kpi());
		System.out.println("Tablename for KPI:" + tableName);
		//view.createQuery("sql", "SELECT * FROM " + queryUtil.getTableName(EsdlPackage.eINSTANCE.getKPI()) + " WHERE " );
		view.close();
	}
	
	
	@SuppressWarnings("unused")
	private void storeResource(CDOSession session) throws IOException, ConcurrentAccessException, CommitException {
		CDOTransaction transaction = session.openTransaction();
		CDOResourceFolder folder = transaction.getOrCreateResourceFolder("/Users/ewoud.werkman@tno.nl/Ruimte voor Energie/");
		CDOResource resource = transaction.getOrCreateResource(folder.getPath() + File.separator + "ETM_Bergen op Zoom_Roosendaal_Sankey.esdl");
		
		EsdlResourceImpl fileResource = new EsdlResourceImpl(URI.createFileURI("C:\\data\\git\\RvE\\scripts\\ETM sankey to ESDL\\data\\ETM_Bergen op Zoom_Roosendaal_Sankey.esdl"));
		fileResource.load(null);
		EObject copy = EcoreUtil.copy(fileResource.getContents().get(0));
		resource.getContents().add(copy);
		transaction.commit();
		transaction.close();
		System.out.println("File saved");
	}
	
	public static void main(String[] args) throws IOException, ConcurrentAccessException, CommitException {
		new CalculationExample();
	}
}
