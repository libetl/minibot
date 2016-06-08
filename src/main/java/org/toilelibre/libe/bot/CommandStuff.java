package org.toilelibre.libe.bot;

class CommandStuff {

	static String filter(String command, String userId) {
		return command.replaceAll("<mailto:([^|]+)\\|[^>]+>", "$1").replaceAll("\\s*<@" + userId + ">(\\s*:\\s*)?", "")
				.trim();
	}

}
