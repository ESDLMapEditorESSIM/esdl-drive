package nl.tno.esdl.esdldrive.util;

import javax.json.Json;
import javax.json.JsonObject;

public class JSONUtil {
	public static JsonObject getErrorAsJson(String reason) {
		return Json.createObjectBuilder()
				.add("error", reason)
				.build();
	}
}
