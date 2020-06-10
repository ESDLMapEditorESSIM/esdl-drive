package nl.tno.esdl.hub.cdo;

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.security.openidconnect.token.IdToken;

public class UserInfo {
	
	private static final String USER_NAME_CLAIM = "email";
	private static final String FULL_NAME_CLAIM = "name";
	private static final String USER_GROUP_PATH_CLAIM = "user_group_path";
	private static final String PERSONAL_FOLDER_PREFIX = "/Users/";
	
	private final String userId;
	private final String userName;
	private final String fullName;
	private final List<String> groups = new ArrayList<String>();
	
	@SuppressWarnings("unchecked")
	public UserInfo(IdToken jwtToken) throws InvalidLoginInformationException {
		try {
			this.userId = jwtToken.getSubject();
			this.userName = (String)jwtToken.getClaim(USER_NAME_CLAIM);
			this.fullName = (String)jwtToken.getClaim(FULL_NAME_CLAIM); 
			if (jwtToken.getClaim(USER_GROUP_PATH_CLAIM) != null && jwtToken.getClaim(USER_GROUP_PATH_CLAIM) instanceof List) {
				groups.addAll((List<String>)jwtToken.getClaim(USER_GROUP_PATH_CLAIM));
			} else {
				throw new InvalidLoginInformationException(USER_GROUP_PATH_CLAIM + " is not present in the JWT token");
			}
			
			// add personal folder to groups
			groups.add(PERSONAL_FOLDER_PREFIX + this.userName);
		} catch (Exception e) {
			e.printStackTrace();
			throw new InvalidLoginInformationException(e);
		}

	}
	
	@Override
	public String toString() {
		return "UserInfo [userName=" + userName + ", fullName=" + fullName + ", groups=" + groups + ", userId=" + userId + "]";
	}

	public String getUserName() {
		return userName;
	}

	public String getFullName() {
		return fullName;
	}

	public List<String> getGroups() {
		return groups;
	}

	public String getUserId() {
		return userId;
	}

	public class InvalidLoginInformationException extends Exception {
		private static final long serialVersionUID = 1L;
		public InvalidLoginInformationException(Exception ex) {
			super(ex);
		}
		
		public InvalidLoginInformationException(String reason) {
			super(reason);
		}
	}
}
