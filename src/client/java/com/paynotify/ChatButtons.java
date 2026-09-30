package com.paynotify;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/** Builds the payment message with the [2X] [R] [X] buttons appended on the same line. */
final class ChatButtons {
	/** Root of the client-side command the buttons run. It is handled locally and never sent to the server. */
	static final String COMMAND_ROOT = "paynotify action ";

	private ChatButtons() {
	}

	static Component append(Component original, int id, String player, String rawAmount, double amount,
							PayNotifyConfig cfg) {
		int gray = PayNotifyConfig.parseColor(cfg.bracketColor, 0xAAAAAA);

		String amountText = "$" + rawAmount;
		Component doubleTip = tooltip(0xFFAA00, "Pay double", player + " - " + amountText
			+ (Double.isNaN(amount) ? "" : " -> $" + AmountUtil.compact(amount * 2.0)),
			"Opens a confirmation. Payments are disabled in this version.");
		Component refundTip = tooltip(0x55FFFF, "Refund", player + " - " + amountText,
			"Opens a confirmation. Payments are disabled in this version.");
		Component closeTip = tooltip(0xFF5555, "Hide this message", player + " - " + amountText,
			"Only hides it locally. Other chat is not affected.");

		MutableComponent result = Component.empty();
		result.append(original);
		result.append(Component.literal(cfg.spaceBeforeButtons));
		result.append(button(cfg.doubleLabel, PayNotifyConfig.parseColor(cfg.doubleColor, 0xFFAA00), gray,
			COMMAND_ROOT + "2x " + id, doubleTip, cfg));
		result.append(Component.literal(cfg.spaceBetweenButtons));
		result.append(button(cfg.refundLabel, PayNotifyConfig.parseColor(cfg.refundColor, 0x55FFFF), gray,
			COMMAND_ROOT + "r " + id, refundTip, cfg));
		result.append(Component.literal(cfg.spaceBetweenButtons));
		result.append(button(cfg.closeLabel, PayNotifyConfig.parseColor(cfg.closeColor, 0xFF5555), gray,
			COMMAND_ROOT + "x " + id, closeTip, cfg));
		return result;
	}

	private static MutableComponent button(String label, int labelColor, int bracketColor, String command,
										   Component tooltip, PayNotifyConfig cfg) {
		Style whole = Style.EMPTY.withClickEvent(new ClickEvent.RunCommand(command));
		if (cfg.showHoverText) {
			whole = whole.withHoverEvent(new HoverEvent.ShowText(tooltip));
		}

		Style bracketStyle = Style.EMPTY.withColor(bracketColor);
		Style labelStyle = Style.EMPTY.withColor(labelColor).withBold(Boolean.valueOf(cfg.boldLabels));

		MutableComponent button = Component.empty().withStyle(whole);
		button.append(Component.literal(cfg.leftBracket).withStyle(bracketStyle));
		button.append(Component.literal(label).withStyle(labelStyle));
		button.append(Component.literal(cfg.rightBracket).withStyle(bracketStyle));
		return button;
	}

	private static Component tooltip(int color, String title, String detail, String note) {
		MutableComponent tip = Component.empty();
		tip.append(Component.literal(title).withStyle(Style.EMPTY.withColor(color).withBold(Boolean.TRUE)));
		tip.append(Component.literal("\n" + detail).withStyle(Style.EMPTY.withColor(0xFFFFFF)));
		tip.append(Component.literal("\n" + note).withStyle(Style.EMPTY.withColor(0xAAAAAA)));
		return tip;
	}
}
