package org.toilelibre.libe.bot;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EventListener;
import java.util.Map;

import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

class WebSocketStuff {
	@ClientEndpoint
	static class Broker extends Endpoint {

		private final MessageHandler.Whole<String> messageHandler;

		Broker(MessageHandler.Whole<String> messageHandler1) {
			this.messageHandler = messageHandler1;
		}

		@Override
		public void onClose(Session session, CloseReason closeReason) {
			super.onClose(session, closeReason);
		}

		@Override
		public void onError(Session session, Throwable thr) {
			if (thr instanceof SlackChannelReconnectException) {
				throw (SlackChannelReconnectException) thr;
			}
			super.onError(session, thr);
		}

		@Override
		public void onOpen(Session session, EndpointConfig endpointConfig) {
			session.addMessageHandler(String.class, this.messageHandler);
		}

	}

	static interface OnMessageListener extends EventListener {
		void onMessage(Map<String, Object> jsonMessage);
	};

	private static class WebSocketStuffMessageHandler implements MessageHandler.Whole<String> {

		private final OnMessageListener onMessageListener;

		WebSocketStuffMessageHandler(OnMessageListener onMessageListener1) {
			this.onMessageListener = onMessageListener1;
		}

		@Override
		public void onMessage(String partialMessage) {
			final Map<String, Object> messageAsJson = JsonStuff.parse(partialMessage);
			this.onMessageListener.onMessage(messageAsJson);

		}

	}

	static void boilerPlateWebSocketForUrl(String token, URI uri, OnMessageListener onMessageListener) {
		try {
			//final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(5);
			WebSocketStuff.start(
					new java.net.URI(JsonStuff.read(CurlStuff.curlForJson(SlackStuff.startRtmRequest(token)), "$.url")),
					onMessageListener);

			//latch.countDown();
			//latch.await();
			//WebSocketStuff.stop(session);
		} catch (DeploymentException | IOException | URISyntaxException e) {
			throw new RuntimeException(e);
		}

	}

	static Object start(URI uri, OnMessageListener onMessageListener) throws DeploymentException, IOException {

		final Endpoint endpoint = new Broker(new WebSocketStuffMessageHandler(onMessageListener));
		final WebSocketContainer container = ContainerProvider.getWebSocketContainer();
		return container.connectToServer(endpoint, ClientEndpointConfig.Builder.create().build(), uri);
	}

	static void stop(Object session) throws IOException {
		((Session) session).close();

	}
}
