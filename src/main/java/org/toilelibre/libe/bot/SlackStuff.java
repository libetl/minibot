package org.toilelibre.libe.bot;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

class SlackStuff {

	static final String COMMON_CURL_PREFIX = "curl -X'POST' -H\"Content-Type:application/x-www-form-urlencoded\" -d \"token=";
	static final String SLACK_API_PREFIX = "https://slack.com/api/";
	static final String ATTACHMENTS = "&attachments=";
	static final Pattern BLOCKQUOTE_PATTERN = Pattern.compile("\n> ");

	private static final Map<String, Map<String, Object>> CHANNELS = new HashMap<>();
	private static final Map<String, Map<String, Object>> GROUPS = new HashMap<>();
	private static final Map<String, Map<String, Object>> INSTANT_MESSAGES = new HashMap<>();
	private static final Map<String, Map<String, Object>> USERS = new HashMap<>();

	static boolean botIsMentioned(String text, SlackChannelConfiguration configuration) {
		return text != null && text.contains("<@" + configuration.getUserId() + ">");
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> fetchedObject(Map<String, Map<String, Object>> collection, String id,
			String curlRequest, String objectType) {
		Map<String, Object> result = null;
		try {
			result = CurlStuff.curlForJson(curlRequest);
		} catch (final IOException e) {
			return result;
		}
		final Map<String, Object> object = objectType == null ? result : (Map<String, Object>) result.get(objectType);
		collection.put(id, object);
		return object;
	}

	static Map<String, Object> getAnyKindOfChannel(String channelId, String token) {
		Map<String, Object> channel = SlackStuff.getChannel(channelId, token);
		channel = channel != null ? channel : SlackStuff.getGroup(channelId, token);
		channel = channel != null ? channel : SlackStuff.getInstantMessage(channelId, token);
		return channel;
	}

	static Map<String, Object> getChannel(String channelId, String token) {
		if (SlackStuff.CHANNELS.get(channelId) != null) {
			return SlackStuff.CHANNELS.get(channelId);
		}
		return SlackStuff.fetchedObject(SlackStuff.CHANNELS, channelId, SlackStuff.getChannelRequest(channelId, token),
				"channel");

	}

	static String getChannelRequest(String channelId, String token) {
		return SlackStuff.COMMON_CURL_PREFIX + token + "&channel=" + channelId + "\" " + SlackStuff.SLACK_API_PREFIX
				+ "channels.info";
	}

	static Map<String, Object> getGroup(String channelId, String token) {
		if (SlackStuff.GROUPS.get(channelId) != null) {
			return SlackStuff.GROUPS.get(channelId);
		}
		return SlackStuff.fetchedObject(SlackStuff.GROUPS, channelId, SlackStuff.getGroupRequest(channelId, token),
				"channel");
	}

	static String getGroupRequest(String channelId, String token) {
		return SlackStuff.COMMON_CURL_PREFIX + token + "&channel=" + channelId + "\" " + SlackStuff.SLACK_API_PREFIX
				+ "groups.info";
	}

	@SuppressWarnings("unchecked")
	static Map<String, Object> getInstantMessage(String channelId, String token) {
		if (SlackStuff.INSTANT_MESSAGES.get(channelId) != null) {
			return SlackStuff.INSTANT_MESSAGES.get(channelId);
		}
		Map<String, Object> object = SlackStuff.fetchedObject(SlackStuff.INSTANT_MESSAGES, channelId,
				SlackStuff.getInstantMessageRequest(token), null);
		final List<Object> im1 = JsonStuff.read(object, "$.ims[?(@.id == '" + channelId + "')]");
		object = (Map<String, Object>) im1.get(0);
		SlackStuff.INSTANT_MESSAGES.put(channelId, object);
		object.put("name", "Chat with " + SlackStuff.getUser(object.get("user").toString(), token).get("name"));
		return object;
	}

	static String getInstantMessageRequest(String token) {
		return SlackStuff.COMMON_CURL_PREFIX + token + "\" " + SlackStuff.SLACK_API_PREFIX + "im.list";
	}

	static Map<String, Object> getUser(String userId, String token) {
		if (SlackStuff.USERS.get(userId) != null) {
			return SlackStuff.USERS.get(userId);
		}
		return SlackStuff.fetchedObject(SlackStuff.USERS, userId, SlackStuff.getUserRequest(userId, token), "user");
	}

	static String getUserRequest(String userId, String token) {
		return SlackStuff.COMMON_CURL_PREFIX + token + "&user=" + userId + "\" " + SlackStuff.SLACK_API_PREFIX + "users.info";
	}

	static boolean isAnIntantMessageChat(Map<String, Object> channel) {
		return channel.get("name") != null && channel.get("name").toString().startsWith("Chat with ");
	}
	
	static boolean messageIsAskingToReconnect(Map<String, Object> jsonMessage) {
		final String type = JsonStuff.read(jsonMessage, "$.type");
		return "team_migration_started".equals(type);
	}
	
	static boolean messageIsAPostInAChannel(Map<String, Object> jsonMessage) {
		final String type = JsonStuff.read(jsonMessage, "$.type");
		return "message".equals(type) && !"message_deleted".equals(jsonMessage.get("subtype"));
	}

	static String postMessage(String text, String channelId, SlackChannelConfiguration configuration) {
		String encodedText = text;
		String encodedAttachments = "";
		try {
			encodedText = configuration.getNiceChar() + URLEncoder.encode(text, "UTF-8");
		} catch (final UnsupportedEncodingException e) {
			encodedText = "Problem%20while%encrypting%20message";
		}
		try {
			encodedAttachments = !SlackStuff.textContainsBlockQuote(text) ? SlackStuff.ATTACHMENTS
					+ URLEncoder.encode(String.format(configuration.getAttachments(), text), "UTF-8")
							.replace("NICE_CHAR", configuration.getNiceChar())
					: "";
		} catch (final UnsupportedEncodingException e) {
			encodedAttachments = "";
		}
		return SlackStuff.COMMON_CURL_PREFIX + configuration.getToken() + "&channel=" + channelId + "&username=" + configuration.getUserName()
				+ "&icon_url=" + configuration.getIcon()
				+ "&text=" + (configuration.getAttachments().contains("%s") && !SlackStuff.textContainsBlockQuote(text)
						? "" : encodedText)
				+ encodedAttachments + "\" " + SlackStuff.SLACK_API_PREFIX + "chat.postMessage";
	}

	static String startRtmRequest(String token) {
		return SlackStuff.COMMON_CURL_PREFIX + token + "&simple_latest=1&no_unreads=1\" " + SlackStuff.SLACK_API_PREFIX
				+ "rtm.start";
	}

	static boolean textContainsBlockQuote(String text) {
		return SlackStuff.BLOCKQUOTE_PATTERN.matcher(text).find();
	}

	public static String[] injectSlackVars(SlackChannelConfiguration configuration, String[] env) {
		String [] resultEnv = new String [env.length + 3];
		System.arraycopy(env, 0, resultEnv, 0, env.length);
		resultEnv [resultEnv.length - 3] = UnixStuff.toEnvVar ("SLACK_CHANNEL", configuration.getPreferedChannel());
		resultEnv [resultEnv.length - 2] = UnixStuff.toEnvVar ("SLACK_TOKEN", configuration.getToken());
		resultEnv [resultEnv.length - 1] = UnixStuff.toEnvVar ("SLACK_BOT_ID", configuration.getUserId());
		return resultEnv;
	}

}
