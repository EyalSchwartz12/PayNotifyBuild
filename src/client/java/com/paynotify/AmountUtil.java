package com.paynotify;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Parses and formats DonutSMP style amounts such as 1,500 / 2.5K / 3m / 1.2B. */
final class AmountUtil {
	private AmountUtil() {
	}

	/** Returns NaN when the text cannot be understood. */
	static double parse(String raw) {
		if (raw == null) return Double.NaN;

		String s = raw.trim().replace(",", "").replace("$", "");
		if (s.isEmpty()) return Double.NaN;

		double multiplier = 1.0;
		char last = Character.toLowerCase(s.charAt(s.length() - 1));
		switch (last) {
			case 'k' -> multiplier = 1e3;
			case 'm' -> multiplier = 1e6;
			case 'b' -> multiplier = 1e9;
			case 't' -> multiplier = 1e12;
			default -> {
			}
		}
		if (multiplier != 1.0) {
			s = s.substring(0, s.length() - 1);
		}

		try {
			return Double.parseDouble(s) * multiplier;
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
	}

	/** Formats 5000000 as "5M", 1250 as "1.25K", 40 as "40". */
	static String compact(double value) {
		if (Double.isNaN(value) || Double.isInfinite(value)) return "?";

		double abs = Math.abs(value);
		String[] suffixes = {"", "K", "M", "B", "T"};
		double[] divisors = {1.0, 1e3, 1e6, 1e9, 1e12};

		int index = 0;
		for (int i = suffixes.length - 1; i >= 1; i--) {
			if (abs >= divisors[i]) {
				index = i;
				break;
			}
		}

		BigDecimal scaled = BigDecimal.valueOf(value / divisors[index]).setScale(2, RoundingMode.HALF_UP);
		if (scaled.signum() == 0) return "0";
		return scaled.stripTrailingZeros().toPlainString() + suffixes[index];
	}
}
