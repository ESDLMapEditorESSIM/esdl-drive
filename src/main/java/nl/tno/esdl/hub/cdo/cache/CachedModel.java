package nl.tno.esdl.hub.cdo.cache;

import org.eclipse.emf.cdo.eresource.CDOResource;

/**
 * Represents a cached model<br/>
 * This makes retrieval and storing of large XML content faster
 * @author werkmane
 *
 */
public class CachedModel {
	private String path;
	private String xmlContent;
	private CDOResource resource;
	
	
	/**
	 * @param path
	 * @param xmlContent
	 * @param resource
	 */
	public CachedModel(String path, String xmlContent, CDOResource resource) {
		super();
		this.path = path;
		this.setXmlContent(xmlContent);
		this.setResource(resource);
	}
	
	public void setPath(String path) {
		this.path = path;
	}


	public String getPath() {
		return path;
	}


	public void setXmlContent(String xmlContent) {
		this.xmlContent = xmlContent;
	}

	public String getXmlContent() {
		return xmlContent;
	}


	public void setResource(CDOResource resource) {
		this.resource = resource;
	}

	public CDOResource getResource() {
		return resource;
	}

	

}
