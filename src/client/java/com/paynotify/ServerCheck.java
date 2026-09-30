package com.paynotify;

import java.lang.reflect.Field;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

/** Decides whether the current connection is DonutSMP (or another server allowed in the config). */
final class ServerCheck {
	private static boolean warned = false;

	private ServerCheck() {
	}

	static boolean isActive(PayNotifyConfig cfg) {
		if (cfg.serverAddressContains == null || cfg.serverAddressContains.isEmpty()) {
			return false;
		}
		for (String fragment : cfg.serverAddressContains) {
			if ("*".equals(fragment)) {
				return true;
			}
		}

		ServerData server = Minecraft.getInstance().getCurrentServer();
		if (server == null) {
			return false; // singleplayer / not connected
		}

		String address = addressOf(server);
		if (address == null) {
			if (!warned) {
				warned = true;
				PayNotifyClient.LOGGER.warn("Could not read the server address, so PayNotify stays inactive. "
					+ "Set serverAddressContains to [\"*\"] in config/paynotify.json to disable the server check.");
			}
			return false;
		}

		String lower = address.toLowerCase(Locale.ROOT);
		for (String fragment : cfg.serverAddressContains) {
			if (fragment != null && !fragment.isBlank() && lower.contains(fragment.toLowerCase(Locale.ROOT).trim())) {
				return true;
			}
		}
		return false;
	}

	private static String addressOf(ServerData server) {
		String[] candidates = {
			Mappings.field("net.minecraft.class_642", "field_3761", "Ljava/lang/String;"),
			"ip",
			"address"
		};
		for (String name : candidates) {
			try {
				Field field = ServerData.class.getDeclaredField(name);
				field.setAccessible(true);
				Object value = field.get(server);
				if (value instanceof String s) {
					return s;
				}
			} catch (Throwable ignored) {
				// try the next candidate
			}
		}
		return null;
	}
}
