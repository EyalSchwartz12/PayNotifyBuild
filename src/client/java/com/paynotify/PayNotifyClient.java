package com.paynotify;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PayNotify - adds [2X] [R] [X] buttons to the end of DonutSMP "X paid you $Y" chat lines.
 *
 * <p>Client-side only. Nothing in this mod sends money or runs /pay.
 */
public final class PayNotifyClient implements ClientModInitializer {
	public static final String MOD_ID = "paynotify";
	static final Logger LOGGER = LoggerFactory.getLogger("PayNotify");

	private static final Queue<Runnable> TASKS = new ConcurrentLinkedQueue<>();

	private static PayNotifyConfig config;
	private static KeyMapping toggleKey;

	@Override
	public void onInitializeClient() {
		config = PayNotifyConfig.load();

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		toggleKey = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.paynotify.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, category));

		// Add the buttons to matching server messages.
		ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
			try {
				return onServerMessage(message, overlay);
			} catch (Throwable t) {
				LOGGER.error("PayNotify failed to process a chat message; showing it unchanged.", t);
				return message;
			}
		});

		// Keybind + deferred UI work (runs on the main thread, one tick after the click).
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.consumeClick()) {
				toggleEnabled();
			}
			Runnable task;
			while ((task = TASKS.poll()) != null) {
				try {
					task.run();
				} catch (Throwable t) {
					LOGGER.error("PayNotify task failed", t);
				}
			}
		});

		registerCommands();
		LOGGER.info("PayNotify loaded. Config: {}", PayNotifyConfig.file());
	}

	// ------------------------------------------------------------------ chat

	private static Component onServerMessage(Component message, boolean overlay) {
		PayNotifyConfig cfg = config;
		if (overlay || !cfg.enabled || !ServerCheck.isActive(cfg)) {
			return message;
		}

		Matcher matcher = cfg.pattern().matcher(message.getString());
		if (!matcher.find()) {
			return message;
		}

		String player = matcher.group("player");
		String rawAmount = matcher.group("amount");
		if (player == null || rawAmount == null) {
			return message;
		}

		int id = PaymentStore.allocateId();
		double amount = AmountUtil.parse(rawAmount);
		Component withButtons = ChatButtons.append(message, id, player, rawAmount, amount, cfg);
		PaymentStore.put(new PaymentRecord(id, player, rawAmount, amount, withButtons), cfg.maxTrackedPayments);
		return withButtons;
	}

	// -------------------------------------------------------------- commands

	private static void registerCommands() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
			ClientCommandManager.literal("paynotify")
				.then(ClientCommandManager.literal("toggle").executes(ctx -> {
					toggleEnabled();
					return 1;
				}))
				.then(ClientCommandManager.literal("reload").executes(ctx -> {
					config = PayNotifyConfig.load();
					ctx.getSource().sendFeedback(prefixed("Config reloaded from " + PayNotifyConfig.file().getFileName(), 0x55FF55));
					return 1;
				}))
				.then(ClientCommandManager.literal("status").executes(ctx -> {
					PayNotifyConfig cfg = config;
					ctx.getSource().sendFeedback(prefixed("Buttons " + (cfg.enabled ? "ENABLED" : "DISABLED")
						+ " | active on this server: " + ServerCheck.isActive(cfg)
						+ " | tracked payments: " + PaymentStore.size(), 0xFFFFFF));
					return 1;
				}))
				.then(ClientCommandManager.literal("action")
					.then(ClientCommandManager.argument("kind", StringArgumentType.word())
						.then(ClientCommandManager.argument("id", IntegerArgumentType.integer(0))
							.executes(ctx -> {
								runAction(StringArgumentType.getString(ctx, "kind"),
									IntegerArgumentType.getInteger(ctx, "id"));
								return 1;
							}))))));
	}

	/** Called when a button in chat is clicked. Never sends anything to the server. */
	private static void runAction(String kind, int id) {
		PaymentRecord record = PaymentStore.get(id);
		if (record == null) {
			TASKS.add(() -> sendLocal(prefixed("That payment is no longer tracked.", 0xFF5555)));
			return;
		}

		switch (kind) {
			case "2x" -> TASKS.add(() -> openConfirmation(record, true));
			case "r" -> TASKS.add(() -> openConfirmation(record, false));
			case "x" -> TASKS.add(() -> {
				boolean hidden = ChatHider.hide(record.message());
				PaymentStore.remove(record.id());
				if (!hidden) {
					sendLocal(prefixed("Could not hide that message.", 0xFF5555));
				}
			});
			default -> LOGGER.warn("Unknown PayNotify action '{}'", kind);
		}
	}

	// ---------------------------------------------------------- confirmation

	private static void openConfirmation(PaymentRecord record, boolean doublePayment) {
		Minecraft mc = Minecraft.getInstance();

		String received = "$" + record.rawAmount();
		StringBuilder body = new StringBuilder();
		if (doublePayment) {
			body.append("Are you sure you want to pay double this amount to this player?");
		} else {
			body.append("Do you want to refund this amount to this player?");
		}
		body.append("\n\nPlayer: ").append(record.player());
		body.append("\nAmount received: ").append(received);
		if (doublePayment) {
			body.append("\nDouble amount: ")
				.append(Double.isNaN(record.amount()) ? "unknown" : "$" + AmountUtil.compact(record.amount() * 2.0));
		} else {
			body.append("\nAmount to refund: ").append(received);
		}
		body.append("\n\nPayments are disabled in this version. Nothing will be sent.");

		Component title = Component.literal(doublePayment ? "PayNotify - Pay double?" : "PayNotify - Refund?");
		Component message = Component.literal(body.toString());

		mc.setScreen(new ConfirmScreen(confirmed -> {
			mc.setScreen(null);
			if (confirmed) {
				sendLocal(prefixed("The payment action is currently disabled. No money was sent.", 0xFFAA00));
			}
		}, title, message, Component.literal("YES"), Component.literal("CANCEL")));
	}

	// ---------------------------------------------------------------- helpers

	private static void toggleEnabled() {
		config.enabled = !config.enabled;
		config.save();
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			mc.player.displayClientMessage(
				prefixed("Chat buttons " + (config.enabled ? "enabled" : "disabled"),
					config.enabled ? 0x55FF55 : 0xFF5555), true);
		}
	}

	private static MutableComponent prefixed(String text, int color) {
		MutableComponent out = Component.empty();
		out.append(Component.literal("[PayNotify] ").withStyle(Style.EMPTY.withColor(0xFFAA00).withBold(Boolean.TRUE)));
		out.append(Component.literal(text).withStyle(Style.EMPTY.withColor(color)));
		return out;
	}

	private static void sendLocal(Component message) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			mc.player.displayClientMessage(message, false);
		}
	}
}
