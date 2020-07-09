package nl.tno.esdl.hub.cdo;

import java.util.logging.Logger;

import org.eclipse.emf.cdo.net4j.CDONet4jSession;
import org.eclipse.emf.cdo.net4j.CDONet4jSessionConfiguration;

public class SessionDetails {

	private static final Logger log = Logger.getLogger(SessionDetails.class.getName());

	private CDONet4jSession cdoSession;
	private final CDONet4jSessionConfiguration sessionConfiguration;
	private final UserInfo userInfo;
	private long lastUsed = System.currentTimeMillis();

	public SessionDetails(CDONet4jSessionConfiguration sessionConfiguration, UserInfo userInfo) {
		this.sessionConfiguration = sessionConfiguration;
		this.userInfo = userInfo;
	}

	public CDONet4jSession getCDOSession() {
		if (cdoSession == null || cdoSession.isClosed()) {
			log.info("Creating new session for " + userInfo.getUserName());
			cdoSession = sessionConfiguration.openNet4jSession();
		} else {
			log.info("Retrieving cached session for " + userInfo.getUserName());
		}
		this.lastUsed = System.currentTimeMillis();
		return cdoSession;
	}
	
	public CDONet4jSession createUnManagedSession() {
		return sessionConfiguration.openNet4jSession();
	}

	public UserInfo getUserInfo() {
		return userInfo;
	}

	public long getLastUsed() {
		return lastUsed;
	}

	public void setLastUsed(long lastUsed) {
		this.lastUsed = lastUsed;
	}

	@Override
	public String toString() {
		return "SessionDetails [cdoSession=" + cdoSession + ", userInfo=" + userInfo + ", lastUsed=" + lastUsed + "]";
	}

	public void logout() {
		cdoSession.close();
	}
}
