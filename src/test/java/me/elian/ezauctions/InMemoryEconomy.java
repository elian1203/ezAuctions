package me.elian.ezauctions;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InMemoryEconomy implements Economy {
	private final Map<UUID, Double> balances = new HashMap<>();

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public String getName() {
		return "TestEconomy";
	}

	@Override
	public boolean hasBankSupport() {
		return false;
	}

	@Override
	public int fractionalDigits() {
		return 9;
	}

	@Override
	public String format(double v) {
		return Double.toString(v);
	}

	@Override
	public String currencyNamePlural() {
		return null;
	}

	@Override
	public String currencyNameSingular() {
		return null;
	}

	@Override
	public boolean hasAccount(String s) {
		return false;
	}

	@Override
	public boolean hasAccount(OfflinePlayer offlinePlayer) {
		return false;
	}

	@Override
	public boolean hasAccount(String s, String s1) {
		return false;
	}

	@Override
	public boolean hasAccount(OfflinePlayer offlinePlayer, String s) {
		return false;
	}

	@Override
	public double getBalance(String s) {
		return 0;
	}

	@Override
	public double getBalance(OfflinePlayer offlinePlayer) {
		return balances.get(offlinePlayer.getUniqueId());
	}

	@Override
	public double getBalance(String s, String s1) {
		return 0;
	}

	@Override
	public double getBalance(OfflinePlayer offlinePlayer, String s) {
		return 0;
	}

	@Override
	public boolean has(String s, double v) {
		return false;
	}

	@Override
	public boolean has(OfflinePlayer offlinePlayer, double v) {
		return false;
	}

	@Override
	public boolean has(String s, String s1, double v) {
		return false;
	}

	@Override
	public boolean has(OfflinePlayer offlinePlayer, String s, double v) {
		return false;
	}

	@Override
	public EconomyResponse withdrawPlayer(String s, double v) {
		return null;
	}

	@Override
	public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, double v) {
		double balance = balances.getOrDefault(offlinePlayer.getUniqueId(), 0D);
		balances.put(offlinePlayer.getUniqueId(), balance - v);
		return null;
	}

	@Override
	public EconomyResponse withdrawPlayer(String s, String s1, double v) {
		return null;
	}

	@Override
	public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, String s, double v) {
		return null;
	}

	@Override
	public EconomyResponse depositPlayer(String s, double v) {
		return null;
	}

	@Override
	public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, double v) {
		double balance = balances.getOrDefault(offlinePlayer.getUniqueId(), 0D);
		balances.put(offlinePlayer.getUniqueId(), balance + v);
		return null;
	}

	@Override
	public EconomyResponse depositPlayer(String s, String s1, double v) {
		return null;
	}

	@Override
	public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, String s, double v) {
		return null;
	}

	@Override
	public EconomyResponse createBank(String s, String s1) {
		return null;
	}

	@Override
	public EconomyResponse createBank(String s, OfflinePlayer offlinePlayer) {
		return null;
	}

	@Override
	public EconomyResponse deleteBank(String s) {
		return null;
	}

	@Override
	public EconomyResponse bankBalance(String s) {
		return null;
	}

	@Override
	public EconomyResponse bankHas(String s, double v) {
		return null;
	}

	@Override
	public EconomyResponse bankWithdraw(String s, double v) {
		return null;
	}

	@Override
	public EconomyResponse bankDeposit(String s, double v) {
		return null;
	}

	@Override
	public EconomyResponse isBankOwner(String s, String s1) {
		return null;
	}

	@Override
	public EconomyResponse isBankOwner(String s, OfflinePlayer offlinePlayer) {
		return null;
	}

	@Override
	public EconomyResponse isBankMember(String s, String s1) {
		return null;
	}

	@Override
	public EconomyResponse isBankMember(String s, OfflinePlayer offlinePlayer) {
		return null;
	}

	@Override
	public List<String> getBanks() {
		return null;
	}

	@Override
	public boolean createPlayerAccount(String s) {
		return false;
	}

	@Override
	public boolean createPlayerAccount(OfflinePlayer offlinePlayer) {
		return false;
	}

	@Override
	public boolean createPlayerAccount(String s, String s1) {
		return false;
	}

	@Override
	public boolean createPlayerAccount(OfflinePlayer offlinePlayer, String s) {
		return false;
	}
}
