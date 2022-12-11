package nl.tno.esdl.esdldrive.edr;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import edd.AnnotationTypeEnum;

@Schema(name="Annotation", description="Annotations for the ESDL described in this EDD")
@JsonInclude(value = Include.NON_NULL)
public class AnnotationJSON {
		@Schema(description="Free text field to describe the explanation", implementation = String.class)
		private String explanation;
		@Schema(description="Reference to the objectID contained in the ESDL [for ObjectAnnotation]", implementation = String.class)
		private String objectId;
		@Schema(description="Name of the attribute this is referred to [for ObjectAnnotation]", implementation = String.class)
		private String attributeName;
		@Schema(description="Type of annotation [for CategoryAnnotation]", implementation = AnnotationTypeEnum.class)
		private AnnotationTypeEnum type;
		
		public AnnotationJSON() {}

		public String getExplanation() {
			return explanation;
		}

		public void setExplanation(String explanation) {
			this.explanation = explanation;
		}

		public String getObjectId() {
			return objectId;
		}

		public void setObjectId(String objectId) {
			this.objectId = objectId;
		}

		public String getAttributeName() {
			return attributeName;
		}

		public void setAttributeName(String attributeName) {
			this.attributeName = attributeName;
		}

		public AnnotationTypeEnum getType() {
			return type;
		}

		public void setType(AnnotationTypeEnum type) {
			this.type = type;
		}

		

		
	}