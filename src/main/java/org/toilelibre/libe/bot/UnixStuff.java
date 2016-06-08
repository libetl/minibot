package org.toilelibre.libe.bot;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

class UnixStuff {

	private static final String[] STATIC_ENV_VARS;

	static {
		try {
			STATIC_ENV_VARS = IOUtils
					.toString(Runtime.getRuntime().exec(Params.BASH_BINARY + " " + Params.SCRIPTS_WD + "/printenv")
							.getInputStream(), Charset.defaultCharset())
					.split("\n");
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	static Map<String, String> envStringToMap(String[] envp) {
		return Arrays.asList(envp).stream().collect(Collectors.toMap(envI -> {
			return envI.toString().split("=")[0];
		}, envI -> {
			return envI.toString().split("=")[1];
		}));
	}

	private static synchronized Process execInsideEnvironment(String realCommand, String[] envp) {
		final String[] enrichedEnvp = new String[envp.length + UnixStuff.STATIC_ENV_VARS.length];
		System.arraycopy(envp, 0, enrichedEnvp, 0, envp.length);
		System.arraycopy(UnixStuff.STATIC_ENV_VARS, 0, enrichedEnvp, envp.length, UnixStuff.STATIC_ENV_VARS.length);
		try {
			return Runtime.getRuntime().exec(realCommand, enrichedEnvp);
		} catch (final IOException e) {
			new RuntimeException(e);
		}
		throw new RuntimeException("Nothing happened");
	}

	/**
	 * @param command
	 * @param envp
	 * @return
	 */
	static String execute(String command, String[] envp) {
		final String realCommand = Params.BASH_BINARY + " " + Params.SCRIPTS_WD + "/" + command;
		final Process process = UnixStuff.execInsideEnvironment(realCommand, envp);
		String result = "";
		try {
			result += IOUtils.toString(process.getInputStream(), Charset.defaultCharset());
		} catch (final IOException e) {
		}
		try {
			result += IOUtils.toString(process.getErrorStream(), Charset.defaultCharset());
		} catch (final IOException e) {
		}

		return result;
	}

	public static boolean programExists(String verb) {
		final File f = new File(Params.SCRIPTS_WD_WINDOWS + File.separator + verb);
		return f.exists() && !f.isDirectory();
	}

	public static String toEnvVar(String var, String val) {
		return var + "=" + val;
	}

}
