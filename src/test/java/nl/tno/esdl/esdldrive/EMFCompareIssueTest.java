package nl.tno.esdl.esdldrive;

import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Logger;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.merge.BatchMerger;
import org.eclipse.emf.compare.merge.IMerger;
import org.eclipse.emf.compare.merge.IMerger.Registry;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.utils.EMFComparePrettyPrinter;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.Before;
import org.junit.Test;

import edd.EddPackage;
import edd.EnergyDataDescription;
import esdl.EsdlPackage;
import esdl.util.EsdlResourceImpl;
import nl.tno.esdl.esdldrive.cdo.CDOManager;
import nl.tno.esdl.esdldrive.util.FileUtil;

public class EMFCompareIssueTest {

	private EMFCompare emfCompare;
	private Registry emfCompareMergerRegistry;
	private BasicMonitor emfCompareMonitor;


	private static final Logger log = Logger.getLogger(EMFCompareIssueTest.class.getName());

	@Before
	public void setupTest() {
		
		emfCompare = EMFCompare.builder().build();
		emfCompareMergerRegistry = IMerger.RegistryImpl.createStandaloneInstance();
		emfCompareMonitor = /*new BasicMonitor();*/ new BasicMonitor.Printing(System.out);
	}
	
	
	@Test
	public void testCompare() throws IOException {
		EsdlPackage.eINSTANCE.eClass(); // load ESDL package
		EddPackage.eINSTANCE.eClass();
		EsdlResourceImpl databaseVersion = new EsdlResourceImpl(URI.createFileURI("src/test/resources/database.edd"));
		databaseVersion.load(null);
		EnergyDataDescription db_edd = (EnergyDataDescription)databaseVersion.getContents().get(0);
		System.out.println(db_edd.getEsdl());
		
		EsdlResourceImpl uploadedVersion = new EsdlResourceImpl(URI.createFileURI("src/test/resources/uploaded.edd"));
		uploadedVersion.load(null);
		EnergyDataDescription uploaded_edd = (EnergyDataDescription)uploadedVersion.getContents().get(0);
		System.out.println(uploaded_edd.getEsdl());
		
		simpleCompare(db_edd, uploaded_edd);
		
		//simpleCompare(db_edd, uploaded_edd);
		
		String s = CDOManager.convertEObjectToString(EcoreUtil.copy(db_edd));
		FileUtil.saveStringToFile(s, "src/test/resources/merged.edd");
		
		
		
	}
	
	
	@Test
	public void testCompare2() throws IOException {
		/**
		 * Important for comparing EDDs: they should have an ID set, otherwise EMFCompare cannot identify if they are the same!! 
		 * 
		 */
		
		EsdlPackage.eINSTANCE.eClass(); // load ESDL package
		EddPackage.eINSTANCE.eClass();
		EsdlResourceImpl databaseVersion = new EsdlResourceImpl(URI.createFileURI("src/test/resources/Ondiepe_geothermie_200m_-_database.esdl"));
		databaseVersion.load(null);
		EnergyDataDescription db_edd = (EnergyDataDescription)databaseVersion.getContents().get(0);
		System.out.println(db_edd.getEsdl());
		
		EsdlResourceImpl uploadedVersion = new EsdlResourceImpl(URI.createFileURI("src/test/resources/Ondiepe_geothermie_200m_-_uploaded.esdl"));
		uploadedVersion.load(null);
		EnergyDataDescription uploaded_edd = (EnergyDataDescription)uploadedVersion.getContents().get(0);
		System.out.println(uploaded_edd.getEsdl());
		
		simpleCompare(db_edd, uploaded_edd);
		
		//simpleCompare(db_edd, uploaded_edd);
		
		String s = CDOManager.convertEObjectToString(EcoreUtil.copy(db_edd));
		FileUtil.saveStringToFile(s, "src/test/resources/merged.edd");
		
		
		
	}
	

	public void simpleCompare(Notifier left, Notifier right) {
		
		DefaultComparisonScope scope = new DefaultComparisonScope(left, right, null);
		log.info("Creating comparision...");
		Comparison comparison = emfCompare.compare(scope); //, emfCompareMonitor);
		log.info("Comparision contains " + comparison.getDifferences().size() + " differences ");
		for (Diff diff: comparison.getDifferences()) {
			System.out.println(diff);
		}
		//ComputeDiffsToMerge computer = new ComputeDiffsToMerge(true, new DiffRelationshipComputer(emfCompareMergerRegistry));
		//Set<Diff> allDiffsToMerge = computer.getAllDiffsToMerge(Iterables.filter(comparison.getDifferences(), alwaysTrue()));
		//for (Diff toMerge : allDiffsToMerge) {
		//	System.out.println(toMerge);
		//}
		
		var printstream = new PrintStream(System.out);
		EMFComparePrettyPrinter.printComparison(comparison, printstream);
		BatchMerger batchMerger = new BatchMerger(emfCompareMergerRegistry);
		batchMerger.copyAllRightToLeft(comparison.getDifferences(), emfCompareMonitor);
	}
	
//public void simpleCompare(Resource left, Resource right) {
//		
//		DefaultComparisonScope scope = new DefaultComparisonScope(left, right, null);
//		log.info("Creating comparision...");
//		Comparison comparison = emfCompare.compare(scope); //, emfCompareMonitor);
//		log.info("Comparision contains " + comparison.getDifferences().size() + " differences ");
//		for (Diff diff: comparison.getDifferences()) {
//			System.out.println(diff);
//		}
//		
//		var printstream = new PrintStream(System.out);
//		EMFComparePrettyPrinter.printComparison(comparison, printstream);
//		
//		final BatchMerger batchMerger = new BatchMerger(emfCompareMergerRegistry);
//		batchMerger.copyAllRightToLeft(comparison.getDifferences(), emfCompareMonitor);
//	}
//	

}
