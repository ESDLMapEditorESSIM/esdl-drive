package nl.tno.esdl.esdldrive.validation.data;

import java.util.Arrays;

public enum DiagnosticStatus {
	OK(0), 
	INFO(1), 
	WARNING(2), 
	ERROR(4), 
	CANCEL(8);

	private int value;

	private DiagnosticStatus(int value) {
		this.value = value;
	}

	public static DiagnosticStatus fromValue(int value) {
		return Arrays.stream(DiagnosticStatus.values()).filter(t -> t.value == value).findFirst().get();
	}
}