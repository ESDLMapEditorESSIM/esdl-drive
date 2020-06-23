/**
 * 
 */
package nl.tno.esdl.hub.cdo;

import java.io.Closeable;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.view.CDOView;

/**
 * @author werkmane
 *
 */
public class ResourceResult implements Closeable {
	private CDOResource cdoResource;
	private CDOView view;

	private final static Logger log = Logger.getLogger(ResourceResult.class.getName());
	
	public ResourceResult(CDOResource cdoResource, CDOView view) {
		this.setCDOResource(cdoResource);
		this.view = view;
	}
	
	

	@Override
	public void close() throws IOException {
		//log.info("Closing view of " + cdoResource.getPath());
		//view.close(); // not necessary in with new cached sessions
	}


	public Date getLastModified() {
		return new Date(cdoResource.getTimeStamp());
	}

	public CDOResource getCDOResource() {
		return cdoResource;
	}



	public void setCDOResource(CDOResource cdoResource) {
		this.cdoResource = cdoResource;
	}
}
