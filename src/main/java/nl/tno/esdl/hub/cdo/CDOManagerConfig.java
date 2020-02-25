package nl.tno.esdl.hub.cdo;

import org.eclipse.microprofile.config.inject.ConfigProperty;

public interface CDOManagerConfig {

    @ConfigProperty(name = "cdo.repository.name" /*, defaultValue = "mondaine"*/)
	public String repoName();
	
    @ConfigProperty(name = "cdo.server.host" /*, defaultValue = "localhost:2036"*/)
	public String host();
		
}
