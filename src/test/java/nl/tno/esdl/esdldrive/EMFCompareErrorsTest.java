package nl.tno.esdl.esdldrive;

import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.merge.BatchMerger;
import org.eclipse.emf.compare.merge.IMerger;
import org.eclipse.emf.compare.merge.IMerger.Registry;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.ecore.EObject;
import org.junit.Before;
import org.junit.Test;

import esdl.EnergySystem;
import esdl.EsdlFactory;
import esdl.EsdlPackage;
import esdl.util.EsdlResourceImpl;
import nl.tno.esdl.esdldrive.cdo.exceptions.MergeConfictException;

public class EMFCompareErrorsTest {
	
	
	private EMFCompare emfCompare;
	private Registry emfCompareMergerRegistry;
	private BasicMonitor emfCompareMonitor;


	private static final Logger log = Logger.getLogger(EMFCompareErrorsTest.class.getName());
	private static final EsdlFactory esdl = EsdlFactory.eINSTANCE;
	
	
	
	

	@Before
	public void setupTest() {
		emfCompare = EMFCompare.builder().build();
		emfCompareMergerRegistry = IMerger.RegistryImpl.createStandaloneInstance();
		emfCompareMonitor = /*new BasicMonitor();*/ new BasicMonitor.Printing(System.out);
	}
	
	
	@Test
	public void testSimple() throws IOException {
		EsdlPackage.eINSTANCE.eClass(); // load ESDL package
		EsdlResourceImpl ecwResource = new EsdlResourceImpl(URI.createFileURI("c:/Users/werkmane/OneDrive - TNO/Documents/ESDL/EnergySystems/ECW.esdl"));
		ecwResource.load(null);
		EnergySystem ecw = (EnergySystem)ecwResource.getContents().get(0);
		
		EsdlResourceImpl ecwRS2 = new EsdlResourceImpl(URI.createFileURI("C:/Users/werkmane/OneDrive - TNO/Documents/ESDL/EnergySystems/ECW_with_carriers.esdl"));
		ecwRS2.load(null);
		EnergySystem ecw2 = (EnergySystem)ecwRS2.getContents().get(0);
		
		compare(ecw, ecw2);
		simpleCompare(ecw, ecw2);
		
		EnergySystem es = esdl.createEnergySystem();
		simpleCompare(ecw, es);
		
		System.out.println(ecw);
		
		
	}


	public void simpleCompare(EObject left, EObject right) {
		
		DefaultComparisonScope scope = new DefaultComparisonScope(left, right, null);
		log.info("Creating comparision...");
		Comparison comparison = emfCompare.compare(scope); //, emfCompareMonitor);
		final BatchMerger batchMerger = new BatchMerger(emfCompareMergerRegistry);
		batchMerger.copyAllRightToLeft(comparison.getDifferences(), emfCompareMonitor);
	}
	
	
	public void compare(EObject left, EObject right) {
		
		
		DefaultComparisonScope scope = new DefaultComparisonScope(left, right, null);
		log.info("Creating comparision...");
		Comparison comparison = emfCompare.compare(scope); //, emfCompareMonitor);
		if (comparison.getConflicts().size() > 0) {
			// conflicts only happen in three-way compare, see org.eclipse.emf.compare.Conflict 
			throw new MergeConfictException("Conflict(s) detected during saving.");
		}
		log.info("Merging differences: " + comparison.getDifferences().size() );
		
		LinkedList<Diff> diffs = new LinkedList<Diff>();
		for (Diff diff: comparison.getDifferences()) {
			createMergeDiffList(diffs, diff, 0);
		}
		log.info("Diff list size: " + diffs.size() );
		while (!diffs.isEmpty()) {
			Diff diff = diffs.removeLast(); // iterate from tail to head
			IMerger merger = emfCompareMergerRegistry.getHighestRankingMerger(diff);
			merger.copyRightToLeft(diff, emfCompareMonitor);
		}
	}
	
	private void createMergeDiffList(LinkedList<Diff> diffs, Diff diff, int depth) {
		diffs.addLast(diff);
		for (Diff req: diff.getRequires()) {
			if (!diffs.contains(req)) { 
				diffs.addLast(req);
				createMergeDiffList(diffs, req, depth+1);
				//System.out.println("RecursiveMerge depth:"+depth+1);
			}
		}
	}

}
