package nl.tno.esdl.esdldrive;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.BasicMonitor.Printing;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Conflict;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.merge.IMerger;
import org.eclipse.emf.compare.merge.IMerger.Registry;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import esdl.Area;
import esdl.EnergySystem;
import esdl.EsdlFactory;
import esdl.EsdlPackage;
import esdl.Instance;
import esdl.util.EsdlResourceImpl;
import nl.tno.esdl.esdldrive.cdo.exceptions.MergeConfictException;

@Ignore
public class EMFCompareTest {

	private EsdlResourceImpl rs1 = new EsdlResourceImpl(URI.createURI("test1.esdl"));
	private EsdlResourceImpl rs2 = new EsdlResourceImpl(URI.createURI("test2.esdl"));
	private EsdlResourceImpl rs3 = new EsdlResourceImpl(URI.createURI("test2.esdl"));
	private EnergySystem es1;
	private EnergySystem es2;
	private EnergySystem es3;
	@SuppressWarnings("unused")
	private EnergySystem ecw;
	private EsdlResourceImpl ecwResource;

	@Before
	public void createModels() throws IOException {
		EsdlPackage.eINSTANCE.eClass(); // init esdl
		EsdlFactory esdl = EsdlFactory.eINSTANCE;
		
		ecwResource = new EsdlResourceImpl(URI.createFileURI("c:/Users/werkmane/OneDrive - TNO/Documents/ESDL/EnergySystems/ECW.esdl"));
		ecwResource.load(null);
		ecw = (EnergySystem)ecwResource.getContents().get(0);
		
		
		es1 = esdl.createEnergySystem();
		es1.setName("Test ES 1");
		Instance instance = esdl.createInstance();
		instance.setName("Instance1");
		es1.getInstance().add(instance);
		Area area = esdl.createArea();
		area.setName("Area1");
		instance.setArea(area);
		area.getAsset().add(esdl.createAirco());
		area.getAsset().add(esdl.createAirco());
		area.getAsset().add(esdl.createAirco());
		area.getAsset().add(esdl.createAirco());
		area.getAsset().add(esdl.createAirco());
		area.getAsset().add(esdl.createAirco());
		area.getAsset().add(esdl.createAirco());
		area.getAsset().add(esdl.createAirco());
		area.getAsset().add(esdl.createAirco());
		rs1.getContents().add(es1);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		rs1.doSave(baos, null);
		System.out.println(baos.toString());
		
		es2 = EcoreUtil.copy(es1);
		rs2.getContents().add(es2);
		es2.getInstance().get(0).getArea().setName("Area2");
		
		es3 = esdl.createEnergySystem();
		es3.setName("Test ES 3");
		Instance instance3 = esdl.createInstance();
		instance3.setName("Instance3");
		es3.getInstance().add(instance3);
		Area area3 = esdl.createArea();
		area3.setName("Area3");
		instance3.setArea(area);
		rs3.getContents().add(es3);
		
		
		
	}
	
	@Test
	public void test() throws IOException {
		EMFCompare compare = EMFCompare.builder().build();
		Registry mergerRegistry = IMerger.RegistryImpl.createStandaloneInstance();
		Printing monitor = new BasicMonitor.Printing(System.out);
		
		DefaultComparisonScope scope = new DefaultComparisonScope(ecwResource, rs1, null);
		System.out.println("Creating comparision...");
		Comparison comparison = compare.compare(scope, monitor);
		// conflicts only happen in three-way compare, see org.eclipse.emf.compare.Conflict 
		String conflicts = "";
		for (Conflict conflict : comparison.getConflicts()) {
			System.out.println("Conflict:" + conflict);
			Diff diff = conflict.getDifferences().get(0);
			conflicts += conflict.getKind() + " " + diff.toString() + ", ";
		}
		System.out.println("Conflicts: " + conflicts);
		//comparison.getEquivalences().get(0).get
		if (comparison.getConflicts().size() > 0) {
			throw new MergeConfictException("Conflict(s) detected during saving.");
		}
		System.out.println("Merging differences...");
		for (Diff diff: comparison.getDifferences()) {
			recursiveMerge(diff, monitor, mergerRegistry,0);
		}
		//printES(ecwResource.getContents().get(0), 0);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ecwResource.doSave(baos, null);
		System.out.println(baos.toString());
		
		baos = new ByteArrayOutputStream();
		rs1.doSave(baos, null);
		System.out.println(baos.toString());
		
		System.out.println("Done");
		
	}
	
	private void recursiveMerge(Diff diff, BasicMonitor monitor, Registry mergerRegistry, int depth) {
		System.out.println("RecursiveMerge depth: "+depth);
		for (Diff req: diff.getRequiredBy()) {
			recursiveMerge(req, monitor, mergerRegistry, depth+1);
		}
		System.out.println("Merging " + diff);
		IMerger merger = mergerRegistry.getHighestRankingMerger(diff);
		merger.copyRightToLeft(diff, monitor);
		
	}
	
	@SuppressWarnings("unused")
	private void printES(EObject es, int depth) {
		EList<EAttribute> eAllAttributes = es.eClass().getEAttributes();
		String indent = "";
		for (int i=0;i<depth;i++) {
			indent+=" ";
		}
		System.out.println(indent + es);
		for (EAttribute a: eAllAttributes) {
		//Strings.padStart(eAtt, depth, 0)
			System.out.println(indent + a.getName() + "=" + es.eGet(a));
		}
		EList<EReference> eAllReferences = es.eClass().getEReferences();
		for (EReference r: eAllReferences) {
			System.out.println(r);
			if (r.isContainment()) {
				if (r.isMany()) {
					@SuppressWarnings("unchecked")
					EList<EObject> l = (EList<EObject>)es.eGet(r);
					if (l!= null) {
						for (EObject li: l) {
							System.out.println(indent + r.getName() + ":");
							printES(li, depth+1);
						}
					}
				} else {
					EObject e = (EObject)es.eGet(r);
					if (e != null) {
						System.out.println(indent + r.getName() + ":");
						printES(e, depth+1);
					}
				}
			}
		}
		
		
	}

}
