package nl.tno.esdl.esdldrive;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.BasicMonitor.Printing;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.EList;
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
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Strings;

import esdl.Area;
import esdl.EnergySystem;
import esdl.EsdlFactory;
import esdl.EsdlPackage;
import esdl.Instance;
import esdl.util.EsdlResourceFactoryImpl;
import esdl.util.EsdlResourceImpl;
import nl.tno.esdl.esdldrive.cdo.exceptions.MergeConfictException;
import nl.tno.esdl.esdldrive.util.ESDLUtil;

public class EMFCompareLargeFile {

	private EsdlResourceImpl rs1;
	//private EsdlResourceImpl rs2 = new EsdlResourceImpl(URI.createURI("test2.esdl"));
	//private EsdlResourceImpl rs3 = new EsdlResourceImpl(URI.createURI("test2.esdl"));
	private EsdlResourceImpl vestaResource;
	private Diagnostician DIAGNOSTICIAN;

	@Before
	public void createModels() throws IOException {
		EsdlPackage.eINSTANCE.eClass(); // init esdl
		EsdlFactory esdl = EsdlFactory.eINSTANCE;
		
		vestaResource = new EsdlResourceImpl(URI.createFileURI("c:/Users/werkmane/OneDrive - TNO/Documents/ESDL/EnergySystems/vesta_output_warmtekeuze_per_buurt.esdl"));
		vestaResource.load(null);
		//ecw = (EnergySystem)vestaResource.getContents().get(0);
		
		validateModel(vestaResource.getContents().get(0));
		
		rs1 = new EsdlResourceImpl(URI.createFileURI("c:/Users/werkmane/OneDrive - TNO/Documents/ESDL/EnergySystems/vesta_output_warmtekeuze_per_buurt.esdl"));
		rs1.load(Collections.EMPTY_MAP);
		
		
	
		
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		rs1.doSave(baos, null);
//		System.out.println(baos.toString());
//		
//		es2 = EcoreUtil.copy(es1);
//		rs2.getContents().add(es2);
//		es2.getInstance().get(0).getArea().setName("Area2");
//		
//		es3 = esdl.createEnergySystem();
//		es3.setName("Test ES 3");
//		Instance instance3 = esdl.createInstance();
//		instance3.setName("Instance3");
//		es3.getInstance().add(instance3);
//		Area area3 = esdl.createArea();
//		area3.setName("Area3");
//		instance3.setArea(area);
//		rs3.getContents().add(es3);
//		
		
		
	}
	
	@Test
	public void test() throws IOException {
		EMFCompare compare = EMFCompare.builder().build();
		Registry mergerRegistry = IMerger.RegistryImpl.createStandaloneInstance();
		Printing monitor = new BasicMonitor.Printing(System.out);
		
		DefaultComparisonScope scope = new DefaultComparisonScope(vestaResource, rs1, null);
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
		LinkedList<Diff> diffs = new LinkedList<Diff>();
		for (Diff diff: comparison.getDifferences()) {
			createMergeDiffList(diffs, diff, 0);
		}
		System.out.println("Diff list size: " + diffs.size() );
		while (!diffs.isEmpty()) {
			Diff diff = diffs.removeLast(); // iterate from tail to head
			IMerger merger = mergerRegistry.getHighestRankingMerger(diff);
			merger.copyRightToLeft(diff, monitor);
		}
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		vestaResource.doSave(baos, null);
		System.out.println(baos.toString());
		
//		baos = new ByteArrayOutputStream();
//		rs1.doSave(baos, null);
//		System.out.println(baos.toString());
//		
		System.out.println("Done");
		
	}
	
	
	private void createMergeDiffList(LinkedList<Diff> diffs, Diff diff, int depth) {
		diffs.addLast(diff);
		for (Diff req: diff.getRequires()) {
			if (!diffs.contains(req)) { 
				diffs.addLast(req);
				createMergeDiffList(diffs, req, depth+1);
				System.out.println("RecursiveMerge depth:"+depth+1);
			}
		}
	}
	
	private void validateModel(EObject eobject) {
		System.out.println("Starting validation...");
		Diagnostic diagnostic = getDiagnostician().validate(eobject);
		System.out.println("Validation:");
		if (diagnostic.getSeverity() == Diagnostic.ERROR) {
			final StringBuilder sb = new StringBuilder("Invalid ESDL file:");
			for (final Diagnostic child : diagnostic.getChildren()) {
				if (child.getSeverity() == Diagnostic.ERROR) {
					sb.append(System.lineSeparator());
					sb.append("ERROR: " + child.getMessage());
					
				} else if (child.getSeverity() == Diagnostic.WARNING) {
					sb.append(System.lineSeparator());
					sb.append("WARNING: " + child.getMessage());
				}
			}
			System.out.println(sb.toString());
		} else {
			System.out.println("Validation OK");
		}
	}
	
	
	private Diagnostician getDiagnostician() {
		if (DIAGNOSTICIAN == null) {
			DIAGNOSTICIAN = new Diagnostician() {
	            @Override
	            public String getObjectLabel(final EObject eObject) {
	                return ESDLUtil.eObjectToString(eObject, true);
	            }
	        };
        }
		return DIAGNOSTICIAN;
	}
	
	
	



}
