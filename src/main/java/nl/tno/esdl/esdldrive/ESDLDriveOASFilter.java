package nl.tno.esdl.esdldrive;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;

/**
 * 
 * @author werkmane
 * Filter for OpenAPI Specification, to update the version number in the info section to match the one in the POM.xml
 * 
 * This Filter is activated through microprofile-config.properties specification.
 */

public class ESDLDriveOASFilter implements OASFilter {

	@Override
	public void filterOpenAPI(OpenAPI openAPI) {
		// override the version specified
		openAPI.getInfo().setVersion(ESDLDriveApplication.version);

//		openAPI.setInfo(OASFactory.createInfo()
//			.title("ESDL Drive")
//			.description("This API provides ways to store and retrieve ESDL files from the ESDL Drive, a storage solution for ESDL files")
//			.license(OASFactory.createLicense()
//						.name("Apache License 2.0")
//						.url("https://www.apache.org/licenses/LICENSE-2.0.html")
//		)
//			.version(ESDLDriveApplication.version));
	}
	
}
