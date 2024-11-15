package nl.tno.esdl.esdldrive.compare;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name="CompareResult", description="Describes a comparision result of two input ESDLs")
public class CompareResult {
	
	public enum FeatureType {
		Attribute,
		Reference
	}
	
	
	FeatureType featureType;
	@Schema(name="featureName", description="Name of the attribute or reference", readOnly = true)
	String featureName;
	String leftValue;
	String rightValue;

	
	public CompareResult(FeatureType featureType, String featureName, String leftValue, String rightValue) {
		super();
		this.featureType = featureType;
		this.featureName = featureName;
		this.leftValue = leftValue;
		this.rightValue = rightValue;
	}
	
}
