package nl.tno.esdl.esdldrive.validation.data;

import java.util.List;

import javax.json.bind.annotation.JsonbPropertyOrder;

@JsonbPropertyOrder({"validationInput", "status", "message", "line", "column", "causes"})
public class ValidationStatus {
	
	private String validationInput;
	private DiagnosticStatus status;
	private String message;
	private Integer column;
	private Integer line;
	private List<ValidationStatus> causes;;
	
	
	public ValidationStatus(DiagnosticStatus status, String message) {
		this.status = status;
		this.message = message;
	}
		
	public ValidationStatus(DiagnosticStatus status, String message, int line, int column) {
		this.status = status;
		this.message = message;
		this.line = line;
		this.column = column;
	}
	
	public DiagnosticStatus getStatus() {
		return status;
	}
	public String getMessage() {
		return message;
	}
	public List<ValidationStatus> getCauses() {
		return causes;
	}
	public void setCauses(List<ValidationStatus> causes) {
		this.causes = causes;
	}

	public String getValidationInput() {
		return validationInput;
	}

	public void setValidationInput(String validationInput) {
		this.validationInput = validationInput;
	}

	public Integer getColumn() {
		return column;
	}

	public Integer getLine() {
		return line;
	}
}