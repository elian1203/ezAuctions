package net.urbanmc.ezauctions.object;

public enum Permission {

	COMMAND_BASE("auction"),
	COMMAND_START("auction.start"),
	COMMAND_START_SEALED("auction.start.sealed"),
	COMMAND_CANCEL("auction.cancel"),
	COMMAND_CANCEL_OTHERS("auction.cancel.others"),
	COMMAND_INFO("auction.info"),
	COMMAND_REMOVE("auction.remove"),
	COMMAND_IMPOUND("auction.impound"),
	COMMAND_RELOAD("auction.reload"),
	COMMAND_BID("bid");

	private String permission;

	Permission(String permission) {
		this.permission = "ezauctions." + permission;
	}

	@Override
	public String toString() {
		return permission;
	}
}
