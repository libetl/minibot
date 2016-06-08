package org.toilelibre.libe.bot;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;


public class MiniBot {

	private static class BotOnMessageListener implements WebSocketStuff.OnMessageListener {

		private final SlackChannelConfiguration configuration;

		public BotOnMessageListener(SlackChannelConfiguration configuration) {
			this.configuration = configuration;
		}

		private String getReactionToMessage(String text, String channelId, Map<String, Object> user,
				Map<String, Object> channel) {
			final String realText = CommandStuff.filter(text, this.configuration.getUserId());
			final String[] tokens = realText.split("\\s+");
			final String verb = tokens[0];
			if (UnixStuff.programExists(verb)) {
				return AuthStuff.storeAuthIfPossible(
						UnixStuff.execute(realText, SlackStuff.injectSlackVars (configuration, AuthStuff.injectEnv(user.get("name").toString()))),
						user.get("name").toString());
			} else if ("set".equals(verb) && realText.split("\\s+").length == 3) {
				AuthStuff.saveEnvVar(tokens[1], tokens[2], user.get("name").toString());
				return "@" + user.get("name") + ": new envVar for you : " + tokens[1] + "=" + tokens[2];
			} else if ("unset".equals(verb) && realText.split("\\s+").length == 2) {
				final String oldValue = AuthStuff.removeEnvVar(tokens[1], user.get("name").toString());
				return "@" + user.get("name") + ": removed envVar for you : " + tokens[1] + " (was '" + oldValue + "')";
			}
			return AutoResponsesStuff.autoAnswer(realText.toLowerCase(), "@" + user.get("name") + ": echo : " + text);
		}

		@Override
		public void onMessage(Map<String, Object> jsonMessage) {
			if (SlackStuff.messageIsAskingToReconnect(jsonMessage)) {
				new Thread (() -> {MiniBot.launchChannelConfiguration(configuration);}).start();
				throw new SlackChannelReconnectException ();
			}else if (SlackStuff.messageIsAPostInAChannel(jsonMessage)) {
				final String text = JsonStuff.$(jsonMessage, "$.text");
				final String channelId = JsonStuff.$(jsonMessage, "$.channel");
				final Map<String, Object> user = SlackStuff.getUser(JsonStuff.$(jsonMessage, "$.user"),
						this.configuration.getToken());
				final Map<String, Object> channel = SlackStuff.getAnyKindOfChannel(channelId,
						this.configuration.getToken());
				System.out.println(user.get("name") + " says : " + text + " (channel : " + channel.get("name") + ")");
				this.reactIfMessageAdressedToMe(text, channelId, user, channel);
			} else {
				System.out.println(jsonMessage);
			}
		}

		private void reactIfMessageAdressedToMe(String text, String channelId, Map<String, Object> user,
				Map<String, Object> channel) {
			if (SlackStuff.isAnIntantMessageChat(channel) || SlackStuff.botIsMentioned(text, this.configuration)) {
				try {
					final String reaction = this.getReactionToMessage(text, channelId, user, channel);
					System.out.println("Answer curl, got this : "
							+ CurlStuff.curlForJson(SlackStuff.postMessage(reaction, channelId, this.configuration)));
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	public static void main(String[] args) throws Exception {
		CronStuff.startScheduler();
		for (final SlackChannelConfiguration configuration : Params.CONFIGURATIONS) {
			MiniBot.launchChannelConfiguration (configuration);
		}
	}

	private static void launchChannelConfiguration(SlackChannelConfiguration configuration) {
		try {
			WebSocketStuff.boilerPlateWebSocketForUrl(
					configuration.getToken(), new URI(JsonStuff
							.$(CurlStuff.curlForJson(SlackStuff.startRtmRequest(configuration.getToken())), "$.url")),
					new BotOnMessageListener(configuration));
		} catch (URISyntaxException | IOException e) {
			throw new RuntimeException (e);
		}
		
	}
}