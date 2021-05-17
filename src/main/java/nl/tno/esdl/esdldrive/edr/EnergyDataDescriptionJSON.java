package nl.tno.esdl.esdldrive.edr;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.json.bind.annotation.JsonbProperty;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;

import com.google.common.collect.Lists;

import nl.tno.edr.EDRFactory;
import nl.tno.edr.EnergyDataDescription;
import nl.tno.edr.Image;

public class EnergyDataDescriptionJSON {
	
	private String id;
	@JsonbProperty(nillable = false)
	private String title;
	@JsonbProperty(nillable = false)
	private String description;
	private List<String> tags;
	private String version;
	private String graphURL;
	@JsonbProperty(nillable = false)
	private String esdl;
	private ImageJSON image;
	
	public EnergyDataDescriptionJSON() {
	}
	
	@Override
	public String toString() {
		return "EnergyDataDescriptionJSON [id=" + id + ", title=" + title + ", description=" + description + ", tags="
				+ tags + ", version=" + version + ", graphURL=" + graphURL + ", esdl=" + esdl + ", image=" + image + "]";
	}
	
	public EnergyDataDescription toEDD() throws IOException {
		EnergyDataDescription edd = EDRFactory.eINSTANCE.createEnergyDataDescription();
		edd.setTitle(title);
		edd.setId(id);
		edd.setDescription(description);
		edd.setGraphURL(graphURL);
		edd.setVersion(version);
		edd.getTags().addAll(tags);
		
		// handle ESDL string
		String esdl = getEsdl();
		byte[] esdlBytes = esdl.getBytes("UTF-8");
		ByteArrayInputStream stream = new ByteArrayInputStream(esdlBytes);
		HashMap<String, EObject> INTRINSIC_ID_TO_E_OBJECT_MAP = new HashMap<String, EObject>();
		XMLResourceImpl resource = new XMLResourceImpl();
		resource.getDefaultLoadOptions().put(XMLResource.OPTION_DEFER_IDREF_RESOLUTION, Boolean.TRUE);
		resource.getDefaultLoadOptions().put(XMLResource.OPTION_RECORD_UNKNOWN_FEATURE, Boolean.TRUE);
		resource.setIntrinsicIDToEObjectMap(INTRINSIC_ID_TO_E_OBJECT_MAP);
		resource.load(stream, null);
		EObject esdlObject = resource.getContents().get(0);
		edd.setEsdl(esdlObject);
		
		// handle image bytes
		
		if (image != null && image.getImageData() != null) {
			// no data, no image
			Image edrImage = EDRFactory.eINSTANCE.createImage();
			edrImage.setContentType(image.getContentType());
			byte[] imageBytes = image.getImageData().getBytes("UTF-8");
			edrImage.setImageData(imageBytes);
			edd.setImage(edrImage);
		}
				
		return edd;
	}
	
	public static EnergyDataDescriptionJSON fromEDD(EnergyDataDescription edd) {
		EnergyDataDescriptionJSON json = new EnergyDataDescriptionJSON();
		json.setDescription(edd.getDescription());
		json.setTitle(edd.getTitle());
		json.setGraphURL(edd.getGraphURL());
		json.setVersion(edd.getVersion());
		json.setId(edd.getId());
		json.setTags(new ArrayList<String>(edd.getTags()));
		EObject esdlObject = edd.getEsdl();
		if (esdlObject != null) {
			ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
			XMLResource resource = new XMLResourceImpl();
			// need to copy here as esdlObject is CDOObject with references to the database
			resource.getContents().add(EcoreUtil.copy(esdlObject));
			HashMap<String, Object> options = new HashMap<String, Object>();
			options.put(XMLResource.OPTION_ENCODING, "UTF-8");
			try {
				resource.save(byteArray, options);
				json.setEsdl(byteArray.toString("UTF-8"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (edd.getImage() != null && edd.getImage().getImageData() != null) {
			ImageJSON imageJSON = new ImageJSON(edd.getImage().getContentType(), new String(edd.getImage().getImageData()));
			json.setImage(imageJSON);
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

	
	
}
