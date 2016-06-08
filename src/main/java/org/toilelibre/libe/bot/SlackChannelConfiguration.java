package org.toilelibre.libe.bot;

class SlackChannelConfiguration {

	private final String userName;
	private final String userId;
	private final String icon;
	private final String token;
	private final String attachments;
	private final String preferedChannel;

	private final String niceChar;

	public SlackChannelConfiguration(String userName, String userId, String icon, String token, String attachments,
			String preferedChannel, String niceChar) {
		super();
		this.userName = userName;
		this.userId = userId;
		this.icon = icon;
		this.token = token;
		this.attachments = attachments;
		this.preferedChannel = preferedChannel;
		this.niceChar = niceChar;
	}

	public String getAttachments() {
		return this.attachments;
	}

	public String getIcon() {
		return this.icon;
	}

	public String getNiceChar() {
		return this.niceChar;
	}

	public String getPreferedChannel() {
		return this.preferedChannel;
	}

	public String getToken() {
		return this.token;
	}

	public String getUserId() {
		return this.userId;
	}

	public String getUserName() {
		return this.userName;
	}

}
