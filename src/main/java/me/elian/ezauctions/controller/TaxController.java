package me.elian.ezauctions.controller;

import com.google.inject.Inject;
import me.elian.ezauctions.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TaxController {
	private final Logger logger;
	private final boolean marginalTax;
	private final double taxPercentage;
	private final List<TaxBracket> brackets;

	@Inject
	public TaxController(ConfigController config, Logger logger) {
		this.logger = logger;

		String taxMode = config.getConfig().getString("auctions.fees.tax-mode", "flat");
		marginalTax = taxMode.equalsIgnoreCase("marginal");

		taxPercentage = config.getConfig().getDouble("auctions.fees.tax-percent") / 100D;
		brackets = marginalTax ? loadTaxBrackets(config) : null;
	}

	public double calculateTaxAmount(double payout) {
		if (marginalTax) {
			return calculateMarginalTaxAmount(payout);
		} else {
			return calculateFlatTaxAmount(payout);
		}
	}

	private double calculateFlatTaxAmount(double payout) {
		return payout * taxPercentage;
	}

	private double calculateMarginalTaxAmount(double payout) {
		double remainingTaxableAmount = payout;
		double totalAmountTaxed = 0;

		if (brackets.isEmpty()) {
			return totalAmountTaxed;
		}

		int bracketIndex = 0;

		// if first bracket does not start at $0, ensure $0 to the first bracket starting amount is untaxed
		TaxBracket firstBracket = brackets.get(0);
		if (firstBracket.startingAmount() > 0) {
			remainingTaxableAmount -= firstBracket.startingAmount();
		}

		while (remainingTaxableAmount > 0) {
			TaxBracket currentBracket = brackets.get(bracketIndex);

			double amountToBeTaxed = remainingTaxableAmount;
			if (brackets.size() > bracketIndex + 1) {
				TaxBracket nextBracket = brackets.get(bracketIndex + 1);
				double delta = nextBracket.startingAmount() - currentBracket.startingAmount();
				amountToBeTaxed = Math.min(amountToBeTaxed, delta);
			}

			double taxFromBracket = amountToBeTaxed * currentBracket.taxPercent();
			totalAmountTaxed += taxFromBracket;

			remainingTaxableAmount -= amountToBeTaxed;
			bracketIndex += 1;
		}

		return totalAmountTaxed;
	}

	private List<TaxBracket> loadTaxBrackets(ConfigController config) {
		List<TaxBracket> brackets = new ArrayList<>();
		ConfigurationSection configSection = config.getConfig().getConfigurationSection("auctions.fees.tax-brackets");
		if (configSection == null) {
			logger.warning("Tax mode is set to marginal, but no tax brackets were specified." +
					" This will result in no taxation");
			return brackets;
		}

		Set<String> keys = configSection.getKeys(false);
		for (String key : keys) {
			try {
				double startingAmount = Double.parseDouble(key);
				String taxPercentString = config.getConfig().getString("auctions.fees.tax-brackets." + key, "0");
				double taxPercent = Double.parseDouble(taxPercentString) / 100D;
				TaxBracket bracket = new TaxBracket(startingAmount, taxPercent);
				brackets.add(bracket);
			} catch (NumberFormatException e) {
				logger.severe("Invalid tax bracket starting amount or tax percentage. Value could not be parsed to a double! " +
						"(" + key + ") Tax brackets will not be correct. Check your config file.", e);
			}
		}

		// must be sorted in order of starting amount to calculate properly
		Collections.sort(brackets);
		return brackets;
	}

	private record TaxBracket(double startingAmount, double taxPercent) implements Comparable<TaxBracket> {

		@Override
		public int compareTo(@NotNull TaxController.TaxBracket taxBracket) {
			return Double.compare(this.startingAmount, taxBracket.startingAmount);
		}
	}
}
