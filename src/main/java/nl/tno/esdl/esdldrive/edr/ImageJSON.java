package nl.tno.esdl.esdldrive.edr;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name="Image", description="Json that describes an Image with content/type")
public class ImageJSON {
		@Schema(description="Content-type of the image, e.g. image/png", implementation = String.class)
		String contentType;
		@Schema(description="A byte array representation of the image that is base64 encoded", implementation = String.class)
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