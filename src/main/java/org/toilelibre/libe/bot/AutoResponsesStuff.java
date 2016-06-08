package org.toilelibre.libe.bot;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class AutoResponsesStuff {

	private static Map<String, String> autoResponses = Params.AUTO_ANSWERS.stream()
			.collect(Collectors.toMap(Function.identity(), Params.AUTO_ANSWERS::indexOf)).entrySet().stream()
			.collect(Collectors.toMap(entry -> {
				return entry.getKey();
			}, entry -> {
				return entry.getValue() == Params.AUTO_ANSWERS.size() - 1 ? -1 : entry.getValue() + 1;
			})).entrySet().stream().filter(entry -> {
				return entry.getValue() % 2 == 1;
			}).collect(Collectors.toMap(entry -> {
				return entry.getKey();
			}, entry -> {
				return Params.AUTO_ANSWERS.get(entry.getValue());
			}));

	static String autoAnswer(String message, String defaultMessage) {
		return AutoResponsesStuff.autoResponses.get(message) == null ? defaultMessage
				: AutoResponsesStuff.autoResponses.get(message);
	}
}
