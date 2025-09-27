package me.elian.ezauctions;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.UnimplementedOperationException;
import net.milkbowl.vault.Vault;
import net.milkbowl.vault.economy.Economy;
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
			if (!(exception instanceof UnimplementedOperationException)
					&& !(exception.getCause() instanceof UnimplementedOperationException)) {
				throw exception;
			}
		}
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