package org.toilelibre.libe.bot;

import java.util.Map;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JsonProvider;

class JsonStuff {
	private static final JsonProvider JSON_PROVIDER = Configuration.defaultConfiguration().jsonProvider();

	static <T> T $(Object jsonObject, String jsonPath) {
		return JsonPath.read(jsonObject, jsonPath);
	}

	@SuppressWarnings("unchecked")
	static Map<String, Object> parse(String message) {
		return (Map<String, Object>) JsonStuff.JSON_PROVIDER.parse(message);
	}

	static <T> T read(Object jsonObject, String jsonPath) {
		return JsonPath.read(jsonObject, jsonPath);
	}
}
