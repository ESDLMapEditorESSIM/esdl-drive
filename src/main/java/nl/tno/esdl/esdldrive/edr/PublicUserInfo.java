package nl.tno.esdl.esdldrive.edr;


import nl.tno.esdl.esdldrive.cdo.AccessController;
import nl.tno.esdl.esdldrive.cdo.UserInfo;

/**
 * Represents a public user accessing the public part(s) of the system
 * specifically adds the edr/Public folder
 * @author werkmane
 *
 */
public class PublicUserInfo extends UserInfo {

	public PublicUserInfo() {
		super(); // initializes the public user
		this.getGroups().add(AccessController.EDR_PUBLIC_FOLDER_PATH);
	}
	
	@Override
	public boolean isPublic() {
		return true;
	}
	

}
