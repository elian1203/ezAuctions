package me.elian.ezauctions.controller;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import me.elian.ezauctions.EzAuctions;
import me.elian.ezauctions.InMemoryEconomy;
import net.milkbowl.vault.Vault;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.ServicePriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TaxControllerTest {
	private EzAuctions plugin;

	@BeforeEach
	public void setup() {
		ServerMock server = MockBukkit.mock();
		Vault vaultPlugin = MockBukkit.load(Vault.class);
		server.getServicesManager().register(Economy.class, new InMemoryEconomy(), vaultPlugin,
				ServicePriority.Normal);
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
	void testFlatRateDefaultConfig() {
		TaxController taxController = plugin.getInjector().getInstance(TaxController.class);
		assertEquals(675, taxController.calculateTaxAmount(27000), 0.000001);
		assertEquals(2500, taxController.calculateTaxAmount(100000), 0.000001);
		assertEquals(1419.73025, taxController.calculateTaxAmount(56789.21), 0.000001);
	}

	@Test
	void testFlatRateDifferentRate() {
		ConfigController config = plugin.getInjector().getInstance(ConfigController.class);
		config.getConfig().set("auctions.fees.tax-percent", 3.7);
		TaxController taxController = plugin.getInjector().getInstance(TaxController.class);
		assertEquals(999, taxController.calculateTaxAmount(27000), 0.000001);
		assertEquals(3700, taxController.calculateTaxAmount(100000), 0.000001);
		assertEquals(2101.20077, taxController.calculateTaxAmount(56789.21), 0.000001);
	}


	@Test
	void testFlatRateDifferentRateFromConfigChange() {
		TaxController taxController = plugin.getInjector().getInstance(TaxController.class);
		assertEquals(675, taxController.calculateTaxAmount(27000), 0.000001);
		assertEquals(2500, taxController.calculateTaxAmount(100000), 0.000001);
		assertEquals(1419.73025, taxController.calculateTaxAmount(56789.21), 0.000001);

		ConfigController config = plugin.getInjector().getInstance(ConfigController.class);
		config.getConfig().set("auctions.fees.tax-percent", 3.7);

		// need to get a new taxcontroller instance to reflect the changes to the config
		// each auction is injected with a new taxcontroller instance
		taxController = plugin.getInjector().getInstance(TaxController.class);
		assertEquals(999, taxController.calculateTaxAmount(27000), 0.000001);
		assertEquals(3700, taxController.calculateTaxAmount(100000), 0.000001);
		assertEquals(2101.20077, taxController.calculateTaxAmount(56789.21), 0.000001);
	}

	@Test
	void testMarginalRateDefaultConfig() {
		ConfigController config = plugin.getInjector().getInstance(ConfigController.class);
		config.getConfig().set("auctions.fees.tax-mode", "marginal");
		TaxController taxController = plugin.getInjector().getInstance(TaxController.class);
		assertEquals(250, taxController.calculateTaxAmount(27000), 0.000001);
		assertEquals(1537.5, taxController.calculateTaxAmount(100000), 0.000001);
		assertEquals(673.2842, taxController.calculateTaxAmount(56789.21), 0.000001);
	}

	@Test
	void testMarginalRateCustomConfig() {
		ConfigController config = plugin.getInjector().getInstance(ConfigController.class);
		config.getConfig().set("auctions.fees.tax-mode", "marginal");

		// build out custom brackets
		config.getConfig().set("auctions.fees.tax-brackets", null);

		config.getConfig().set("auctions.fees.tax-brackets.2500", 1);
		config.getConfig().set("auctions.fees.tax-brackets.25000", 3.75);
		config.getConfig().set("auctions.fees.tax-brackets.55000", 7);
		config.getConfig().set("auctions.fees.tax-brackets.100000", 10);
		config.getConfig().set("auctions.fees.tax-brackets.250000", 15);

		TaxController taxController = plugin.getInjector().getInstance(TaxController.class);
		assertEquals(0, taxController.calculateTaxAmount(1500), 0.000001);
		assertEquals(25, taxController.calculateTaxAmount(5000), 0.000001);
		assertEquals(1475.2447, taxController.calculateTaxAmount(56789.21), 0.000001);
		assertEquals(4500, taxController.calculateTaxAmount(100000), 0.000001);
		assertEquals(21750, taxController.calculateTaxAmount(265000), 0.000001);
	}

}
