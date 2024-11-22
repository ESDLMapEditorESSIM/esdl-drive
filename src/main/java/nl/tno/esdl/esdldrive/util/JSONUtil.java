package nl.tno.esdl.esdldrive.util;

import jakarta.json.Json;
import jakarta.json.JsonObject;

public class JSONUtil {
	public static JsonObject getErrorAsJson(String reason) {
		return Json.createObjectBuilder()
				.add("error", reason)
				.build();
	}
}
