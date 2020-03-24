package nl.tno.esdl.hub;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.eclipse.microprofile.auth.LoginConfig;

/**
 *
 */
@LoginConfig(authMethod = "MP-JWT") 
@ApplicationPath("/store")
public class HubRestApplication extends Application {

}
