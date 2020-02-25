/**
 * 
 */
package nl.tno.esdl.hub.cdo;

/**
 * @author werkmane
 *
 */
public class ResourceMetadata {
	private long timeStamp;

	public ResourceMetadata() {
	}

	public ResourceMetadata(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	public long getTimeStamp() {
		return timeStamp;
	}
	
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	
}
