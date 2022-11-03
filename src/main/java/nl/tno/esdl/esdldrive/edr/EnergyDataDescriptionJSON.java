package nl.tno.esdl.esdldrive.edr;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;

import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.common.commit.CDOCommitInfo;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import edd.EddFactory;
import edd.EnergyDataDescription;
import edd.Image;


@JsonbPropertyOrder({"id","title","description","version","tags","image", "esdlType", "esdl"})
@Schema(name="EnergyDataDescription", description="Json that describes an Energy Data Description (EDD) in the EDR, i.e. metadata for an EDR entry")
public class EnergyDataDescriptionJSON {
	
	@Schema(name="id", description="Not used when storing and EDD, describes the unique path of this EDD when retrieved from the EDR", required = false, readOnly = true)
	private String id;
	@JsonbProperty(nillable = false)
	@Schema(description="Title of the description", required=true)
	private String title;
	@JsonbProperty(nillable = false)
	@Schema(description="Longer description about the data that is described")
	private String description;
	@Schema(description="Define tags for this data", required=false)
	private List<String> tags;
	@Schema(description="The version of this description, will probably be generated in the future", required=false)
	private String version;
	@Schema(description="The URL to a grafana dashboard when timeseries or profiles are described", required=false)
	private String graphURL;
	@JsonbProperty(nillable = false)
	@Schema(description="The ESDL as XML string and Base64 encoded, that is described by this EDD", required=true)
	private String esdl;
	@Schema(description="Image and content type for this description. Image data should be Base64 encoded", required=false, implementation = ImageJSON.class)
	private ImageJSON image;
	@Schema(description="This is automatically generated based on the ESDL that is set.", required=false, readOnly=true)
	private String esdlType;
	@Schema(description="Timestamp of when the file was last changed (generated)", required=false, readOnly=true)
	private long lastChanged;
	@Schema(description="Timestamp of when the file was first published (generated)", required=false, readOnly=true)
	private long publicationDate;
	@Schema(description="Describes the commit history of this EDD", required=false, readOnly=true)
	private List<HistoryJSON> history;
	
	
	public EnergyDataDescriptionJSON() {
	}
	
	@Override
	public String toString() {
		return "EnergyDataDescriptionJSON [id=" + id + ", title=" + title + ", description=" + description + ", tags="
				+ tags + ", version=" + version + ", graphURL=" + graphURL + ", esdl=" + esdl + ", image=" + image + "]";
	}
	
	public EnergyDataDescription toEDD() throws IOException {
		EnergyDataDescription edd = EddFactory.eINSTANCE.createEnergyDataDescription();
		edd.setTitle(title);
		//edd.setId(id);
		edd.setDescription(description);
		edd.setGraphURL(graphURL);
		edd.setVersion(version);
		
		if (tags != null && !tags.isEmpty()) {
			edd.getTags().addAll(tags);
		}
		
		// handle ESDL string
		String esdl = getEsdl();
		if (esdl != null && !esdl.isEmpty()) {
			byte[] esdlBytes = esdl.getBytes("UTF-8");
			byte[] decoded = Base64.getDecoder().decode(esdlBytes);
			ByteArrayInputStream stream = new ByteArrayInputStream(decoded);
			HashMap<String, EObject> INTRINSIC_ID_TO_E_OBJECT_MAP = new HashMap<String, EObject>();
			XMLResourceImpl resource = new XMLResourceImpl();
			resource.getDefaultLoadOptions().put(XMLResource.OPTION_DEFER_IDREF_RESOLUTION, Boolean.TRUE);
			//resource.getDefaultLoadOptions().put(XMLResource.OPTION_RECORD_UNKNOWN_FEATURE, Boolean.TRUE);
			resource.setIntrinsicIDToEObjectMap(INTRINSIC_ID_TO_E_OBJECT_MAP);
			resource.load(stream, null);
			EObject esdlObject = resource.getContents().get(0);
			edd.setEsdl(esdlObject);
			edd.setEsdlType(esdlObject.eClass().getName());
		}
		
		// handle image bytes
		
		if (image != null && image.getImageData() != null) {
			// no data, no image
			Image edrImage = EddFactory.eINSTANCE.createImage();
			edrImage.setContentType(image.getContentType());
			byte[] imageBytes = image.getImageData().getBytes("UTF-8");
			edrImage.setImageData(imageBytes);
			edd.setImage(edrImage);
		}
				
		return edd;
	}
	
	
	public static  EnergyDataDescriptionJSON fromEDD(EnergyDataDescription edd) {
		return fromEDD(edd, true, true, true);
	}
	
	public static EnergyDataDescriptionJSON fromEDD(EnergyDataDescription edd, boolean addImageData, boolean addESDL, boolean addHistory) {
		EnergyDataDescriptionJSON json = new EnergyDataDescriptionJSON();
		json.setDescription(edd.getDescription());
		json.setTitle(edd.getTitle());
		json.setGraphURL(edd.getGraphURL());
		json.setVersion(edd.getVersion());
		json.setId(edd.cdoDirectResource().getPath());
		json.setTags(new ArrayList<String>(edd.getTags()));
		EObject esdlObject = edd.getEsdl();
		if (esdlObject != null) {
			json.setEsdlType(esdlObject.eClass().getName());
			if (esdlObject instanceof CDOObject) {
				// TODO check if this works
				CDOObject cdoObject = (CDOObject) esdlObject;
				CDORevision cdoRevision = cdoObject.cdoRevision(true);
				if (cdoRevision != null) {					
					json.setLastChanged(cdoRevision.getTimeStamp());
					if (json.getVersion() != null) {
						json.setVersion(json.getVersion() + "-R" + cdoRevision.getVersion());
					} else {
						json.setVersion(String.valueOf(cdoRevision.getVersion()));
					}
				} else {
					System.out.println("No last change date for " + edd.cdoDirectResource().getPath());
				}
			}
			if (addESDL) {
				ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
				XMLResource resource = new XMLResourceImpl();
				// need to copy here as esdlObject is CDOObject with references to the database
				resource.getContents().add(EcoreUtil.copy(esdlObject));
				HashMap<String, Object> options = new HashMap<String, Object>();
				options.put(XMLResource.OPTION_ENCODING, "UTF-8");
				try {
					resource.save(byteArrayStream, options);
					byte[] encoded = Base64.getEncoder().encode(byteArrayStream.toByteArray());
					json.setEsdl(new String(encoded, "UTF-8"));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (addImageData && edd.getImage() != null && edd.getImage().getImageData() != null) {
			try {
				ImageJSON imageJSON = new ImageJSON(edd.getImage().getContentType(), new String(edd.getImage().getImageData(), "UTF-8"));
				json.setImage(imageJSON);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		
		if (edd.cdoDirectResource() != null) {
			CDOResource resource = edd.cdoDirectResource();
			// load history to find creation time of resource and history
			TreeSet<CDOCommitInfo> allCommits = new TreeSet<CDOCommitInfo>((o1, o2) -> -Long.compare(o1.getTimeStamp(), o2.getTimeStamp()));
			resource.cdoHistory().triggerLoad();
			edd.cdoHistory().triggerLoad();
			while (resource.cdoHistory().isLoading()) {
				resource.cdoHistory().waitWhileLoading(10);
			}
			while (edd.cdoHistory().isLoading()) {
				edd.cdoHistory().waitWhileLoading(10);
			}
			
			allCommits.addAll(List.of(resource.cdoHistory().getElements()));
			allCommits.addAll(List.of(edd.cdoHistory().getElements()));
			json.setPublicationDate(resource.cdoHistory().getLastElement().getTimeStamp());
			if (allCommits.size() > 0) {
				json.setHistory(new ArrayList<HistoryJSON>());
				for(CDOCommitInfo t: allCommits) {
					HistoryJSON historyJson = new HistoryJSON();
					historyJson.setCommitMessage(t.getComment());
					historyJson.setUser(t.getUserID());
					historyJson.setTimestamp(t.getTimeStamp());
					// below: this is an expensive operation, and disabled for now
//					if (!t.isInitialCommit()) {
//						historyJson.setNumberOfChanges(t.getAffectedIDs().size());
//					}
					json.getHistory().add(historyJson);
				}
			}
		}
		
		
		return json;
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

	public ImageJSON getImage() {
		return image;
	}

	public void setImage(ImageJSON image) {
		this.image = image;
	}

	public String getEsdlType() {
		return esdlType;
	}

	public void setEsdlType(String edrType) {
		this.esdlType = edrType;
	}

	public long getLastChanged() {
		return lastChanged;
	}

	public void setLastChanged(long lastChanged) {
		this.lastChanged = lastChanged;
	}

	public long getPublicationDate() {
		return publicationDate;
	}

	public void setPublicationDate(long publicationDate) {
		this.publicationDate = publicationDate;
	}

	public List<HistoryJSON> getHistory() {
		return history;
	}

	public void setHistory(List<HistoryJSON> history) {
		this.history = history;
	}

	
	
	
}
