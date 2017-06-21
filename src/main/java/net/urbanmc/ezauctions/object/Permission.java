package net.urbanmc.ezauctions.object;

public enum Permission {

	COMMAND_BASE("auction"),
	COMMAND_START("auction.start"),
	COMMAND_START_SEALED("auction.start.sealed"),
	COMMAND_CANCEL("auction.cancel"),
	COMMAND_CANCEL_OTHERS("auction.cancel.others"),
	COMMAND_INFO("auction.info"),
	COMMAND_REMOVE("auction.remove"),
	COMMAND_SPAM("auction.spam"),
	COMMAND_IGNORE("auction.ignore"),
	COMMAND_IMPOUND("auction.impound"),
	COMMAND_ENABLE("auction.enable"),
	COMMAND_DISABLE("auction.disable"),
	COMMAND_RELOAD("auction.reload"),
	COMMAND_BID("bid"),
	NOTIFY_UPDATE("updatemessage");


	private String permission;

	Permission(String permission) {
		this.permission = "ezauctions." + permission;
	}

	@Override
	public String toString() {
		return permission;
	}
}
