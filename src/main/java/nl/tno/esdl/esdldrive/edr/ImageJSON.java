package nl.tno.esdl.esdldrive.edr;
public class ImageJSON {
		String contentType;
		String imageData;
		
		
		
		public ImageJSON() {}
		
		/**
		 * @param contentType
		 * @param imageData
		 */
		public ImageJSON(String contentType, String imageData) {
			this.contentType = contentType;
			this.imageData = imageData;
		}
		
		public String getContentType() {
			return contentType;
		}
		public void setContentType(String contentType) {
			this.contentType = contentType;
		}
		
		public String getImageData() {
			return imageData;
		}
		
		public void setImageData(String imageData) {
			this.imageData = imageData;
		}
		
		
	}