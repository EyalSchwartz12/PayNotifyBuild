package com.paynotify;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings stored in {@code config/paynotify.json}.
 *
 * <p>Edit the file while the game is closed (or run {@code /paynotify reload} in game).
 * Remember that backslashes inside JSON strings must be doubled ({@code \\$} for a literal dollar sign).
 */
public final class PayNotifyConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger("PayNotify");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	/**
	 * Default detection pattern. It must contain the two named groups "player" and "amount".
	 * Matches DonutSMP messages such as: {@code Steve paid you $2.5M}
	 */
	public static final String DEFAULT_PATTERN =
		"^[^A-Za-z0-9_.*]*(?<player>[.*]?[A-Za-z0-9_]{1,16}) paid you \\$(?<amount>[0-9][0-9,]*(?:\\.[0-9]+)?[KkMmBbTt]?)";

	public String _readme =
		"paymentPattern is a Java regular expression tested against the plain text of every server message. "
			+ "It MUST contain the named groups (?<player>...) and (?<amount>...). Backslashes must be doubled in JSON. "
			+ "serverAddressContains: buttons are only added when the server address contains one of these "
			+ "(case-insensitive). Use \"*\" to allow every server. Colors are hex like #FFAA00. "
			+ "Run /paynotify reload in game after editing.";

	/** Master switch (also toggled by the keybind or /paynotify toggle). */
	public boolean enabled = true;

	/** The feature is only active on servers whose address contains one of these fragments. */
	public List<String> serverAddressContains = new ArrayList<>(List.of("donutsmp"));

	/** Regex used to detect "player paid you amount" messages. Needs groups "player" and "amount". */
	public String paymentPattern = DEFAULT_PATTERN;

	/** Text placed between the payment message and the first button. */
	public String spaceBeforeButtons = "  ";
	/** Text placed between two buttons. */
	public String spaceBetweenButtons = " ";

	public String leftBracket = "[";
	public String rightBracket = "]";

	public String doubleLabel = "2X";
	public String refundLabel = "R";
	public String closeLabel = "X";

	public String doubleColor = "#FFAA00";
	public String refundColor = "#55FFFF";
	public String closeColor = "#FF5555";
	public String bracketColor = "#AAAAAA";

	public boolean boldLabels = true;
	public boolean showHoverText = true;

	/** How many payment messages are remembered so their buttons keep working. */
	public int maxTrackedPayments = 200;

	private transient Pattern compiled;

	public Pattern pattern() {
		return compiled;
	}

	public static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("paynotify.json");
	}

	public static PayNotifyConfig load() {
		Path file = file();
		PayNotifyConfig cfg = null;
		boolean parseFailed = false;

		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				cfg = GSON.fromJson(reader, PayNotifyConfig.class);
			} catch (Exception e) {
				parseFailed = true;
				LOGGER.error("Could not read {} - using defaults (the file was not overwritten).", file, e);
			}
		}

		if (cfg == null) {
			cfg = new PayNotifyConfig();
		}

		cfg.sanitize();

		if (!Files.exists(file) && !parseFailed) {
			cfg.save();
		}

		return cfg;
	}

	public void save() {
		Path file = file();
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(this), StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOGGER.error("Could not write {}", file, e);
		}
	}

	private void sanitize() {
		if (serverAddressContains == null) {
			serverAddressContains = new ArrayList<>(List.of("donutsmp"));
		}
		if (spaceBeforeButtons == null) spaceBeforeButtons = "  ";
		if (spaceBetweenButtons == null) spaceBetweenButtons = " ";
		if (leftBracket == null) leftBracket = "[";
		if (rightBracket == null) rightBracket = "]";
		if (doubleLabel == null || doubleLabel.isEmpty()) doubleLabel = "2X";
		if (refundLabel == null || refundLabel.isEmpty()) refundLabel = "R";
		if (closeLabel == null || closeLabel.isEmpty()) closeLabel = "X";
		if (doubleColor == null) doubleColor = "#FFAA00";
		if (refundColor == null) refundColor = "#55FFFF";
		if (closeColor == null) closeColor = "#FF5555";
		if (bracketColor == null) bracketColor = "#AAAAAA";
		if (maxTrackedPayments < 10) maxTrackedPayments = 10;
		if (maxTrackedPayments > 5000) maxTrackedPayments = 5000;

		compiled = compileOrDefault(paymentPattern);
	}

	private static Pattern compileOrDefault(String source) {
		if (source != null && !source.isBlank()) {
			try {
				Pattern p = Pattern.compile(source);
				if (p.namedGroups().containsKey("player") && p.namedGroups().containsKey("amount")) {
					return p;
				}
				LOGGER.error("paymentPattern must contain the named groups (?<player>...) and (?<amount>...). Using the default pattern.");
			} catch (PatternSyntaxException e) {
				LOGGER.error("paymentPattern is not a valid regular expression ({}). Using the default pattern.", e.getDescription());
			}
		}
		return Pattern.compile(DEFAULT_PATTERN);
	}

	/** Parses "#RRGGBB" / "RRGGBB"; returns the fallback on any problem. */
	public static int parseColor(String hex, int fallback) {
		if (hex == null) return fallback;
		String s = hex.trim();
		if (s.startsWith("#")) s = s.substring(1);
		if (s.length() != 6) return fallback;
		try {
			return Integer.parseInt(s, 16) & 0xFFFFFF;
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}
