package nl.tno.esdl.hub;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 *
 */
@Path("/hello")
//@Singleton
@RequestScoped
public class HelloController {

    @GET
    @RolesAllowed(value = "cdo_read")
    public String sayHello() {
        return "Hello World";
    }
}
