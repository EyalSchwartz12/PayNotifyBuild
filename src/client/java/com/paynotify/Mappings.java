package com.paynotify;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Helpers that translate stable intermediary names into whatever names the running game uses.
 * Used only for a few reflective lookups, so they keep working in both development and production.
 */
final class Mappings {
	private Mappings() {
	}

	static String field(String ownerIntermediary, String intermediaryName, String descriptor) {
		try {
			return FabricLoader.getInstance().getMappingResolver()
				.mapFieldName("intermediary", ownerIntermediary, intermediaryName, descriptor);
		} catch (Throwable t) {
			return intermediaryName;
		}
	}

	static String method(String ownerIntermediary, String intermediaryName, String descriptor) {
		try {
			return FabricLoader.getInstance().getMappingResolver()
				.mapMethodName("intermediary", ownerIntermediary, intermediaryName, descriptor);
		} catch (Throwable t) {
			return intermediaryName;
		}
	}
}
