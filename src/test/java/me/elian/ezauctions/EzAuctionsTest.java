package me.elian.ezauctions;

import net.milkbowl.vault.Vault;
import net.milkbowl.vault.economy.Economy;
import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.UnimplementedOperationException;
import org.bukkit.plugin.ServicePriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EzAuctionsTest {
	private ServerMock server;
	private EzAuctions plugin;

	@BeforeEach
	public void setup() {
		server = MockBukkit.mock();
		Vault vaultPlugin = MockBukkit.load(Vault.class);
		server.getServicesManager().register(Economy.class, new InMemoryEconomy(), vaultPlugin, ServicePriority.Normal);
		plugin = MockBukkit.load(EzAuctions.class);
	}

	@AfterEach
	public void cleanup() {
		try {
			MockBukkit.unmock();
		} catch (Exception exception) {
			if (!isUnimplementedOperationException(exception)
					&& !isUnimplementedOperationException(exception.getCause())) {
				throw exception;
			}
		}
	}

	private boolean isUnimplementedOperationException(Throwable exception) {
		return exception != null && "UnimplementedOperationException".equals(exception.getClass().getSimpleName());
	}

	@Test
	void getInjector() {
		// ensure injector was successfully created
		assert plugin.getInjector() != null;
	}

	@Test
	void onEnable() {
		// ensure plugin enabled successfully
		assert plugin.isEnabled();
	}
}
