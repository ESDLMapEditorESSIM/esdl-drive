/**
 *  This work is based on original code developed and copyrighted by TNO 2020. 
 *  Subsequent contributions are licensed to you by the developers of such code and are
 *  made available to the Project under one or several contributor license agreements.
 *
 *  This work is licensed to you under the Apache License, Version 2.0.
 *  You may obtain a copy of the license at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Contributors:
 *      TNO         - Initial implementation
 *  Manager:
 *      TNO
 */

package nl.tno.esdl.esdldrive.validation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EPackage.Registry;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameters;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

import esdl.EsdlPackage;
import nl.tno.esdl.esdldrive.util.ESDLUtil;
import nl.tno.esdl.esdldrive.validation.data.DiagnosticStatus;
import nl.tno.esdl.esdldrive.validation.data.ValidationStatus;

@RequestScoped
@Path("/validate")
public class ESDLValidator {

	Logger log = Logger.getLogger(this.getClass().getName());
	private Diagnostician DIAGNOSTICIAN;

	public ESDLValidator() {
		Registry ePackageRegistry = EPackage.Registry.INSTANCE;
		ePackageRegistry.put(EsdlPackage.eNS_URI, EsdlPackage.eINSTANCE);
	}


	@Path("/")
	// All roles are allowed: @RolesAllowed("cdo_read")
	@POST
	@Operation(summary = "Validates the ESDL file and checks for errors in the XML")
	@Consumes({MediaType.APPLICATION_XML + "; charset=utf-8", MediaType.APPLICATION_OCTET_STREAM + "; charset=utf-8"})
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	@Parameters({
		@Parameter(name = "nonstict", description = "Don't use strict validation (e.g. accept unknown attributes)", 
				in = ParameterIn.QUERY, schema = @Schema(type = SchemaType.BOOLEAN))
	})
	public Response validateESDL(@RequestBody(description = "ESDL content in XML format", name = "esdlStream", 
				content = {
					@Content(example = "<?xml version=\"1.0\" encoding=\"UTF-8\">\n<esdl:EnergySystem ...></esdl:EnergySystem", 
							schema = @Schema(required = true, format="XML String", description = "ESDL content"))
				}
			)
			InputStream esdlStream, @QueryParam("nonstict") @DefaultValue("false") Boolean nonstict) throws IOException {
		
//		ByteArrayOutputStream result = new ByteArrayOutputStream();
//		byte[] buffer = new byte[1024];
//		int length;
//		while ((length = esdlStream.read(buffer)) != -1) {
//		    result.write(buffer, 0, length);
//		}
//		String xmlString = result.toString(StandardCharsets.UTF_8.name());
//		ByteArrayInputStream resourceInputStream = new ByteArrayInputStream(result.toByteArray());
//		
		// create hashmap to speed up reading
		HashMap<String, EObject> INTRINSIC_ID_TO_E_OBJECT_MAP = new HashMap<String, EObject>();
		XMLResourceImpl resource = new XMLResourceImpl();		
		resource.getDefaultLoadOptions().put(XMLResource.OPTION_DEFER_IDREF_RESOLUTION, Boolean.TRUE);
		if (nonstict) {
			resource.getDefaultLoadOptions().put(XMLResource.OPTION_RECORD_UNKNOWN_FEATURE, Boolean.TRUE);
		}
		resource.setIntrinsicIDToEObjectMap(INTRINSIC_ID_TO_E_OBJECT_MAP);
		try {
			resource.load(esdlStream, null);
			//resource.setURI(URI.createURI("https://esdldrive/input.esdl"));
			System.out.println("Validating :" + ESDLUtil.eObjectToString(resource.getContents().get(0), true));
			ValidationStatus validationStatus = validateConformance(resource);
			validationStatus.setValidationInput("Validated: " + ESDLUtil.eObjectToString(resource.getContents().get(0), false));
			return Response.ok(validationStatus).build();
		} catch (IOException e) {
			//e.printStackTrace();
			ValidationStatus validateParsing = validateParsing(resource);
			return Response.accepted(validateParsing).status(Status.NOT_ACCEPTABLE).build();
			//throw new WebApplicationException("Error parsing the XML file", e, Status.NOT_ACCEPTABLE);
		}
	}



	private ValidationStatus validateConformance(Resource resource) {
		System.out.println("File loaded for validation.");
		ValidationStatus mainValidation = null;
		List<ValidationStatus> list = new ArrayList<ValidationStatus>();
		
		EObject eObject = resource.getContents().get(0);
		Diagnostic validation = getDiagnostician().validate(eObject); //Diagnostician.INSTANCE.validate(eObject);
		String message = "Valid ESDL";
		switch (validation.getSeverity()) {
		case Diagnostic.ERROR:
			message = "Invalid ESDL File, following errors were detected";
			break;
		case Diagnostic.WARNING:
			message = "Possible problems detected in the ESDL file";
			break;
		case Diagnostic.OK:
			message = "Valid ESDL";
			break;
		case Diagnostic.INFO:
			message = "Validation OK, see detailed information";
			break;
		case Diagnostic.CANCEL:
			message = "Validation was cancelled due to severe causes!";
			break;
		default:
			message = "Unknown validation result";
		}
		
		mainValidation = new ValidationStatus(DiagnosticStatus.fromValue(validation.getSeverity()), message);
		for (final Diagnostic child : validation.getChildren()) {
			ValidationStatus status = new ValidationStatus(DiagnosticStatus.fromValue(child.getSeverity()), child.getMessage());
			list.add(status);
		}
		List<ValidationStatus> parseStatus = validateParsing(resource).getCauses();
		list.addAll(parseStatus);
		if (!list.isEmpty()) { 
			mainValidation.setCauses(list);
		}
		return mainValidation;
	}


	private ValidationStatus validateParsing(Resource resource) {
		List<ValidationStatus> parseWarnings = resource.getWarnings().stream()
				.map(w -> new ValidationStatus(DiagnosticStatus.WARNING, w.getMessage(), w.getLine(), w.getColumn()))
				.collect(Collectors.toList());
		List<ValidationStatus> parseErrors = resource.getErrors().stream()
				.map(w -> new ValidationStatus(DiagnosticStatus.ERROR, w.getMessage(), w.getLine(), w.getColumn()))
				.collect(Collectors.toList());
		
		DiagnosticStatus parseStatus = parseErrors.size() > 0 ? DiagnosticStatus.ERROR : 
			(parseWarnings.size() > 0 ? DiagnosticStatus.WARNING : DiagnosticStatus.OK);
		ValidationStatus parseValidation = new ValidationStatus(parseStatus, "XML parse validation");
		List<ValidationStatus> fullList = new ArrayList<ValidationStatus>();
		fullList.addAll(parseErrors);
		fullList.addAll(parseWarnings);
		parseValidation.setCauses(fullList);
		return parseValidation;
	}
	
	
	/**
	 * Prettify the response of the diagnostic.
	 * @return
	 */
	private Diagnostician getDiagnostician() {
		if (DIAGNOSTICIAN == null) {
			DIAGNOSTICIAN = new Diagnostician() {
	            @Override
	            public String getObjectLabel(final EObject eObject) {
	                return ESDLUtil.eObjectToString(eObject, true) + " at //" + EcoreUtil.getRelativeURIFragmentPath(EcoreUtil.getRootContainer(eObject), eObject);
	            }
	        };
        }
		return DIAGNOSTICIAN;
	}
	
	
	@GET
	@Path("/long")
	@Produces(MediaType.APPLICATION_JSON)
	public void longResponse(@Context HttpServletResponse httpServletResponse) throws IOException, InterruptedException {
		for (int i = 0; i < 50; i+=10) {
			httpServletResponse.getWriter().append(getStatusJson("Uploading", i));
			httpServletResponse.getWriter().flush();
			
			Thread.sleep(1000);
		}
		
		
		httpServletResponse.getWriter().append(getStatusJson("Storing in databse", 75));
		httpServletResponse.getWriter().flush();
		Thread.sleep(1000);
		httpServletResponse.getWriter().append(getStatusJson("Done", 100));
		httpServletResponse.getWriter().flush();
	
	}
	
	private String getStatusJson(String action, int percentage) {
		return 
			"{" + System.lineSeparator() 
			+ "\"message\": \"" + action + "\"," + System.lineSeparator()
			+ "\"percentage\":" + percentage + System.lineSeparator()  
			+ "}" + System.lineSeparator() + System.lineSeparator();  
	}
		
	
	
	
	

	
}
