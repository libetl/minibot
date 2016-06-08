package org.toilelibre.libe.bot;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Params {

	static final List<SlackChannelConfiguration> CONFIGURATIONS = Arrays.asList(
			
			
			new SlackChannelConfiguration("username",
					"userid", "https%3A%2F%2Favatar.url/file.jpg",
					"xoxb-token", "[{\"color\": \"#000000\",\"text\": \"NICE_CHAR %s\"}]",
					"channelId", "%E2%9E%A4"));

	static final List<String> AUTO_ANSWERS = Arrays.asList("salut", "Bonjour", "merci", "De rien");

	static final String SCRIPTS_WD = "/home/user/bash";
	static final String BASH_BINARY = "/usr/bin/bash";
	static final String SCRIPTS_WD_WINDOWS = "home/user/bash";

	static Map<String, Runnable> TASKS = new HashMap<String, Runnable>() {
		/**
		*
		*/
		private static final long serialVersionUID = -3405285846657435665L;

		{
			this.put("59 9 * * 1-5", () -> Params.sendTextInEveryConfiguration("DSM time (Daily Standup Meeting)"));
			this.put("0 16 * * 5",
					() -> Params.sendTextInEveryConfiguration("Don't forget to fill in your Clarity weekly form"));
			this.put("* * * * *", () -> {
				if (new java.util.Random().nextInt(1000) == 1) {
						Params.sendTextInEveryConfiguration(
								CurlStuff.quietCurlForJson("curl -s http://quotes.stormconsultancy.co.uk/random.json")
										.get("quote").toString());

				}
			});
		}
	};

	private static void sendTextInEveryConfiguration(String text) {
		for (final SlackChannelConfiguration configuration : Params.CONFIGURATIONS) {

			try {
				CurlStuff.curlForJson(SlackStuff.postMessage(text,
						configuration.getPreferedChannel(), configuration));
			} catch (final IOException e) {
			}
		}
	}

}
