package nl.tno.esdl.hub.cdo;

import org.eclipse.emf.cdo.net4j.CDONet4jSession;

public class SessionDetails {


	private final CDONet4jSession cdoSession;
	private final UserInfo userInfo;
	private long lastUsed = System.currentTimeMillis();

	public SessionDetails(CDONet4jSession cdoSession, UserInfo userInfo) {
		this.cdoSession = cdoSession;
		this.userInfo = userInfo;
	}

	public CDONet4jSession getCDOSession() {
		this.lastUsed = System.currentTimeMillis();
		return cdoSession;
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
}
