package nl.tno.esdl.esdldrive.geometry;

import java.security.Principal;
import java.util.Set;

import org.eclipse.microprofile.jwt.JsonWebToken;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

@Path("/test")
public class TestResource {
	
	
	@Context
	SecurityContext sec;

	@GET
	@Path("/user")
	public Set<String> getGroups() {
	       Set<String> groups = null;
	       Principal user = sec.getUserPrincipal();
	       System.out.println(user);
	       if (user instanceof JsonWebToken) {
	                JsonWebToken jwt = (JsonWebToken) user;
	                groups = jwt.getGroups();
	       }
	       return groups;
	}
}
