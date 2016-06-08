package org.toilelibre.libe.bot;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli2.Argument;
import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.OptionException;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.commons.cli2.util.HelpFormatter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.message.BasicHeader;

class CurlStuff {

	@SuppressWarnings("unchecked")
	private static Response curl(String requestCommand) {

		final DefaultOptionBuilder oBuilder = new DefaultOptionBuilder();
		final ArgumentBuilder aBuilder = new ArgumentBuilder();
		final GroupBuilder gBuilder = new GroupBuilder();

		final Option httpMethod = oBuilder.withShortName("X").withDescription("Http Method").withRequired(false)
				.withArgument(aBuilder.withName("method").withMaximum(1).withDefault("GET").withMaximum(1).create())
				.create();

		final Option header = oBuilder.withShortName("H").withDescription("Header").withRequired(false)
				.withArgument(aBuilder.withName("headerValue").withMinimum(1).create()).create();

		final Option data = oBuilder.withShortName("d").withDescription("Data").withRequired(false)
				.withArgument(aBuilder.withName("dataValue").withMaximum(1).create()).create();

		final Option silent = oBuilder.withShortName("s").withDescription("silent curl").withRequired(false).create();

		final Option trustInsecure = oBuilder.withShortName("k").withDescription("trust insecure").withRequired(false)
				.create();

		final Option noBuffering = oBuilder.withShortName("N").withDescription("no buffering").withRequired(false)
				.create();

		final Argument url = aBuilder.withName("url").withMaximum(1).withMinimum(0).create();

		final Group curlGroup = gBuilder.withOption(url).withOption(httpMethod).withOption(header).withOption(data)
				.withOption(silent).withOption(trustInsecure).withOption(noBuffering).create();

		final HelpFormatter hf = new HelpFormatter();

		// configure a parser
		final Parser parser = new Parser();
		parser.setGroup(curlGroup);
		parser.setHelpFormatter(hf);
		parser.setHelpTrigger("--help");
		final String requestCommandWithoutBasename = requestCommand.replaceAll("^[ ]*curl[ ]*", "");
		final String[] args = CurlStuff.getArgsFromCommand(requestCommandWithoutBasename);
		CommandLine commandLine;
		try {
			commandLine = parser.parse(args);
		} catch (final OptionException e) {
			parser.parseAndHelp(new String[] { "--help" });
			throw new RuntimeException(e);
		}

		final Request request = CurlStuff.getBuilder(commandLine, httpMethod, url);

		((List<String>) commandLine.getValues(header)).stream().map(optionAsString -> optionAsString.split(":"))
				.map(optionAsArray -> new BasicHeader(optionAsArray[0].trim().replaceAll("^\"", "")
						.replaceAll("\\\"$", "").replaceAll("^\\'", "").replaceAll("\\'$", ""),
						optionAsArray[1].trim()))
				.forEach(basicHeader -> request.addHeader(basicHeader));

		if (commandLine.getValue(data) != null) {
			request.bodyByteArray(commandLine.getValue(data).toString().getBytes());
		}

		try {
			return request.execute();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	static Map<String, Object> curlForJson(String requestAsString) throws ClientProtocolException, IOException {
		return JsonStuff.parse(CurlStuff.curl(requestAsString).returnContent().asString());
	}

	private static String[] getArgsFromCommand(String requestCommandWithoutBasename) {
		return requestCommandWithoutBasename.replaceAll("(^-[a-zA-Z0-9])", " $1 ")
				.replaceAll("(?:(?<!\\\\) )(-[a-zA-Z0-9])", " $1 ").trim().split("((?<!\\\\)\\s+)");
	}

	private static Request getBuilder(CommandLine cl, Option httpMethod, Argument url) {
		try {
			return (Request) Request.class.getDeclaredMethod(
					StringUtils.capitalize(cl.getValue(httpMethod).toString().toLowerCase().replaceAll("[^a-z]", "")),
					String.class).invoke(null, cl.getValue(url));
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException | IllegalStateException e) {
			throw new RuntimeException(e);
		}
	}

	public static Map<String, Object> quietCurlForJson(String request) {
		try {
			return CurlStuff.curlForJson(request);
		} catch (IOException e) {
			
		}
		return Collections.emptyMap();
	}
}
