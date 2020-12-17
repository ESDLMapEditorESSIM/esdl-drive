package nl.tno.esdl.esdldrive.edr;


import nl.tno.esdl.esdldrive.cdo.UserInfo;

/**
 * Represents a public user accessing the public part(s) of the system
 * @author werkmane
 *
 */
public class PublicUserInfo extends UserInfo {

	public PublicUserInfo() {
		super(); // initializes the public user
		this.getGroups().add("/" + EDRResource.EDR_FOLDER_NAME + PUBLIC_FOLDER);
	}
	
	@Override
	public boolean isPublic() {
		return true;
	}
	

}
