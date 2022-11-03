package nl.tno.esdl.esdldrive.edr;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name="History", description="Json that describes the history/changelog of this EDD")
public class HistoryJSON {
		@Schema(description="The author's name of the change", implementation = String.class)
		String user;
		@Schema(description="Commit message describing the change", implementation = String.class)
		String commitMessage;
		@Schema(description="Time at which the change was commited, UNIX Epoch timestamp", implementation = long.class)
		long timestamp;
//		@Schema(description="Number of changes in this commit", implementation = Integer.class)
//		Integer numberOfChanges;
		//@Schema(description="The branch of the commit", implementation = String.class)
		//String branch;
		
		
		
		public HistoryJSON() {}




		/**
		 * @param user
		 * @param commitMessage
		 * @param timestamp
		 * @param numberOfChanges
		 */
		public HistoryJSON(String user, String commitMessage, long timestamp) {
			super();
			this.user = user;
			this.commitMessage = commitMessage;
			this.timestamp = timestamp;
			//this.numberOfChanges = numberOfChanges;
		}
		
		

		public String getUser() {
			return user;
		}



		public void setUser(String user) {
			this.user = user;
		}



		public String getCommitMessage() {
			return commitMessage;
		}



		public void setCommitMessage(String commitMessage) {
			this.commitMessage = commitMessage;
		}



		public long getTimestamp() {
			return timestamp;
		}



		public void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}



//		public Integer getNumberOfChanges() {
//			return numberOfChanges;
//		}
//
//		public void setNumberOfChanges(int numberOfChanges) {
//			this.numberOfChanges = numberOfChanges;
//		}


		
		
	}