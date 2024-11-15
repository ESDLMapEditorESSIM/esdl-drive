package nl.tno.esdl.esdldrive.compare;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.internal.spec.AttributeChangeSpec;
import org.eclipse.emf.compare.internal.spec.MatchSpec;
import org.eclipse.emf.compare.internal.spec.ReferenceChangeSpec;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.xmi.PackageNotFoundException;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import esdl.EnergySystem;
import esdl.EsdlPackage;
import nl.tno.esdl.esdldrive.cdo.CDOManager;
import nl.tno.esdl.esdldrive.cdo.exceptions.MergeConfictException;

@RequestScoped
@Path("/compare")
public class CompareResource {
	
	@Inject
	CDOManager cdoManager;

	private Logger log = Logger.getLogger(this.getClass().getName());
	private EMFCompare emfCompare;
	//private BasicMonitor emfCompareMonitor;

	public CompareResource() {
		emfCompare = EMFCompare.builder().build();
		//emfCompareMergerRegistry = IMerger.RegistryImpl.createStandaloneInstance();
		//emfCompareMonitor = new BasicMonitor(); //new BasicMonitor.Printing(System.out);
	}
	
	
	@POST
	@Path("/")
	//@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_OCTET_STREAM})
	//@Produces(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes("multipart/form-data")
	@Operation(summary = "Compare two ESDLs")
	@Tag(ref = "Compare")
	public Response compare(
//			@RequestBody(
//					description = "ESDL content in XML format", 
//					name = "stream", content = {@Content(example = "<?xml version=\"1.0\" encoding=\"UTF-8\">\n<esdl:EnergySystem ...></esdl:EnergySystem", 
//					schema = @Schema(required = true, 
//					format = "XML String", 
//					description = "ESDL content")) 
//			})
			@FormParam("left_esdl")
			String left_esdl,
			@FormParam("right_esdl")
			String right_esdl) 
	{
		try {
			System.out.println("Compare: reading input");
//			ByteArrayOutputStream result = new ByteArrayOutputStream();
//			byte[] buffer = new byte[1024];
//			int length;
//			while ((length = stream.read(buffer)) != -1) {
//			    result.write(buffer, 0, length);
//			}
			//String xmlString = result.toString(StandardCharsets.UTF_8.name());
			//ByteArrayInputStream resourceInputStream = new ByteArrayInputStream(result.toByteArray()); 
			ByteArrayInputStream resourceInputStream1 = new ByteArrayInputStream(left_esdl.getBytes(StandardCharsets.UTF_8)); 
			ByteArrayInputStream resourceInputStream2 = new ByteArrayInputStream(right_esdl.getBytes(StandardCharsets.UTF_8)); 
			
			// create hashmap to speed up reading
			HashMap<String, EObject> INTRINSIC_ID_TO_E_OBJECT_MAP = new HashMap<String, EObject>();
			XMLResourceImpl resource1 = new XMLResourceImpl();
			resource1.setURI(URI.createURI("LEFT.esdl"));
			resource1.getDefaultLoadOptions().put(XMLResource.OPTION_DEFER_IDREF_RESOLUTION, Boolean.TRUE);
			resource1.getDefaultLoadOptions().put(XMLResource.OPTION_RECORD_UNKNOWN_FEATURE, Boolean.TRUE);
			resource1.setIntrinsicIDToEObjectMap(INTRINSIC_ID_TO_E_OBJECT_MAP);
			resource1.load(resourceInputStream1, null);
			
			HashMap<String, EObject> INTRINSIC_ID_TO_E_OBJECT_MAP2 = new HashMap<String, EObject>();
			XMLResourceImpl resource2 = new XMLResourceImpl();
			resource2.setURI(URI.createURI("RIGHT.esdl"));
			resource2.getDefaultLoadOptions().put(XMLResource.OPTION_DEFER_IDREF_RESOLUTION, Boolean.TRUE);
			resource2.getDefaultLoadOptions().put(XMLResource.OPTION_RECORD_UNKNOWN_FEATURE, Boolean.TRUE);
			resource2.setIntrinsicIDToEObjectMap(INTRINSIC_ID_TO_E_OBJECT_MAP2);
			resource2.load(resourceInputStream2, null);
			
			
			//EObject firstESDL = resource1.getContents().get(0);
			//EObject secondESDL = resource2.getContents().get(0);
			
			System.out.println("Done reading input");
			System.out.println(left_esdl);
			System.out.println(right_esdl);
			
			DefaultComparisonScope scope = new DefaultComparisonScope(resource1, resource2, null);
			log.info("Creating comparision...");
			Comparison comparison = emfCompare.compare(scope); //, emfCompareMonitor);
			if (comparison.getConflicts().size() > 0) {
				// conflicts only happen in three-way compare, see org.eclipse.emf.compare.Conflict 
				throw new MergeConfictException("Conflict(s) detected during three-way compare...");
			}
			log.info("Comparing found differences: " + comparison.getDifferences().size() );
			// Now testing if this works:
			//final BatchMerger batchMerger = new BatchMerger(emfCompareMergerRegistry);
			//batchMerger.copyAllRightToLeft(comparison.getDifferences(), emfCompareMonitor);
			EnergySystem es  = (EnergySystem) resource2.getContents().get(0);
			System.out.println(es.getInstance().get(0).getArea().getAsset());
			
			String response = "";
			for (Diff diff : comparison.getDifferences()) {
				response = response + "\n" + diff.getKind() + ":" + diff;
				response = response + "\n - source:" + diff.getSource() + " match:" + diff.getMatch();
				if (diff instanceof AttributeChangeSpec) {
					AttributeChangeSpec attributeChangeSpec = (AttributeChangeSpec) diff;
					String name = attributeChangeSpec.getAttribute().getName();
					EObject left = attributeChangeSpec.getMatch().getLeft();
					String fragment = null;
					if (left != null) {
						fragment = resource1.getURIFragment(left);
					}
					response += "\n - leftSource="+attributeChangeSpec.getMatch().getLeft()+ ", fragment=" + fragment +" ,type=attribute, name=" + name + ", value="+attributeChangeSpec.getValue().toString();
					if (attributeChangeSpec.getMatch() instanceof MatchSpec) {
						MatchSpec matchSpec = (MatchSpec) attributeChangeSpec.getMatch();
						Object value = null;
						EObject right = matchSpec.basicGetRight();
						if (right != null) {
							value = right.eGet(attributeChangeSpec.getAttribute());
						}									
						response += "\n - rightSource="+matchSpec.getRight()+ " ,type=attribute, name=" + name + ", rightValue="+value;
					}
				} else if (diff instanceof ReferenceChangeSpec) {
					ReferenceChangeSpec referenceChangeSpec = (ReferenceChangeSpec) diff;
					String name = referenceChangeSpec.getReference().getName();
					EObject left = referenceChangeSpec.getMatch().getLeft();
					String fragment = null;
					if (left != null) {
						fragment = resource1.getURIFragment(left);
					}
					
					response += "\n - leftSource="+referenceChangeSpec.getMatch().getLeft()+ ", fragment=" + fragment +" ,type=reference, name=" + name + ", value="+referenceChangeSpec.getValue().toString();
					
					if (referenceChangeSpec.getMatch() instanceof MatchSpec) {
						MatchSpec matchSpec = (MatchSpec) referenceChangeSpec.getMatch();
						EObject right = matchSpec.basicGetRight();
						Object value = null;
						if (right != null) {
							value = right.eGet(referenceChangeSpec.getReference());
						}
						
						response += "\n - rightSource="+right+ " ,type=reference, name=" + name + ", rightValue="+ value;
					}
				} else {
					log.warning("Diff of type " + diff.getClass().getName() + " is not supported: " + diff);
				}
				
			}


			return Response.ok(response).build();
		} catch (Exception e) {
			log.severe("Error: " + e);
			e.printStackTrace();
			if (e instanceof PackageNotFoundException) {
				return Response.serverError().entity(
						"Input is not of type ESDL: XML namespaceURI is not defined or (xmlns:esdl) is different from "
								+ EsdlPackage.eNS_URI)
						.type(MediaType.TEXT_PLAIN).build();
			}
			System.out.println("Error comparing input" + ": " + e.getMessage());
			return Response.serverError().entity("Failed compare the input ESDLs: " + e.getMessage())
					.type(MediaType.TEXT_PLAIN).build();
		}
	}
}
