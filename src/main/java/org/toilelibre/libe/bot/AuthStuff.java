package org.toilelibre.libe.bot;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class AuthStuff {

	private static Map<String, String> AUTH_SESSIONS = new HashMap<String, String>();
	private static Map<String, String> USERS = new HashMap<String, String>();
	private static Map<String, Map<String, String>> USERS_ENV_VARS = new HashMap<String, Map<String, String>>();

	private static void ensureUserEnvVars(String user) {
		AuthStuff.USERS_ENV_VARS.put(user, AuthStuff.USERS_ENV_VARS.get(user) == null ? new HashMap<String, String>()
				: AuthStuff.USERS_ENV_VARS.get(user));
	}

	private static String[] injectAuthSession(String user) {
		final String authSession = AuthStuff.AUTH_SESSIONS.get(user);
		if (authSession == null) {
			return new String[0];
		}
		return new String[] { "AUTHORIZATION=" + authSession + "", "USER_ID=" + AuthStuff.USERS.get(user) };
	}

	static String[] injectEnv(String user) {
		AuthStuff.ensureUserEnvVars(user);
		final String[] authSession = AuthStuff.injectAuthSession(user);
		final String[] envValues = AuthStuff.USERS_ENV_VARS.get(user).entrySet().stream().map(entry -> {
			return entry.getKey() + "=" + entry.getValue();
		}).collect(Collectors.toList()).toArray(new String[AuthStuff.USERS_ENV_VARS.get(user).size()]);
		final String[] allValues = new String[envValues.length + authSession.length];
		System.arraycopy(authSession, 0, allValues, 0, authSession.length);
		System.arraycopy(envValues, 0, allValues, authSession.length, envValues.length);
		return allValues;
	}

	static String removeEnvVar(String key, String user) {
		AuthStuff.ensureUserEnvVars(user);
		return AuthStuff.USERS_ENV_VARS.get(user).remove(key);
	}

	static void saveEnvVar(String key, String value, String user) {
		AuthStuff.ensureUserEnvVars(user);
		AuthStuff.USERS_ENV_VARS.get(user).put(key, value);
	}

	static String storeAuthIfPossible(String result, String user) {
		if (result.replaceAll("\n", " ").matches(".*Authorization: Bearer [^\\s]+.*")) {
			final Matcher matcher = Pattern.compile("Authorization: Bearer [^\\s]+").matcher(result);
			final boolean found = matcher.find();
			if (!found) {
				return result;
			}
			AuthStuff.AUTH_SESSIONS.put(user, matcher.group().replaceAll("\n|\r\t", ""));
		}
		if (result.replaceAll("\n", " ").matches(".*USER_ID=[0-9]+.*")) {
			final Matcher matcher = Pattern.compile("USER_ID=([0-9]+)").matcher(result);
			final boolean found = matcher.find();
			if (!found) {
				return result;
			}
			AuthStuff.USERS.put(user, matcher.group(1).replaceAll("\n|\r\t", ""));
		}
		return result;
	}
}
