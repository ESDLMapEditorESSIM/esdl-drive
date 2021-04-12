package nl.tno.esdl.esdldrive.edr;

import java.util.List;

public class EnergyDataDescriptionJSON {
	String id;
	String title;
	String description;
	List<String> tags;
	String version;
	String graphURL;
	String esdl;
	Image image;
	
	@Override
	public String toString() {
		return "EnergyDataDescriptionJSON [id=" + id + ", title=" + title + ", description=" + description + ", tags="
				+ tags + ", version=" + version + ", graphURL=" + graphURL + ", esdl=" + esdl + ", image=" + image + "]";
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getGraphURL() {
		return graphURL;
	}

	public void setGraphURL(String graphURL) {
		this.graphURL = graphURL;
	}

	public String getEsdl() {
		return esdl;
	}

	public void setEsdl(String esdl) {
		this.esdl = esdl;
	}

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
	}

	public class Image {
		String contentType;
		byte[] imageData;
		
		
		public String getContentType() {
			return contentType;
		}
		public void setContentType(String contentType) {
			this.contentType = contentType;
		}
		
		public byte[] getImageData() {
			return imageData;
		}
		public void setImageData(byte[] imageData) {
			this.imageData = imageData;
		}
		
		
	}
	
}
