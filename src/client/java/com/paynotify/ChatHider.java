package com.paynotify;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;

/**
 * Hides one message from the local chat window.
 *
 * <p>Only the client's own copy of the chat history is changed. The server is never told,
 * and no other message is touched. The chat component's internals are located by name via the
 * intermediary mappings and, as a fallback, by their type, so no mixin is needed.
 */
final class ChatHider {
	private static Field messagesField;
	private static Method contentMethod;
	private static Method refreshMethod;

	private ChatHider() {
	}

	/** Returns true if the message was found and removed from the visible chat. */
	static boolean hide(Component target) {
		try {
			Minecraft mc = Minecraft.getInstance();
			ChatComponent chat = mc.gui.getChat();

			List<?> allMessages = messageList(chat);
			if (allMessages == null) {
				return false;
			}

			boolean removed = allMessages.removeIf(line -> isSameMessage(line, target));
			if (removed) {
				refresh(chat);
			}
			return removed;
		} catch (Throwable t) {
			PayNotifyClient.LOGGER.error("Could not hide chat message", t);
			return false;
		}
	}

	private static boolean isSameMessage(Object line, Component target) {
		Component content = contentOf(line);
		return content != null && (content == target || content.equals(target));
	}

	private static Component contentOf(Object line) {
		try {
			if (contentMethod == null || !contentMethod.getDeclaringClass().isInstance(line)) {
				contentMethod = findContentMethod(line.getClass());
			}
			if (contentMethod == null) {
				return null;
			}
			Object value = contentMethod.invoke(line);
			return value instanceof Component c ? c : null;
		} catch (Throwable t) {
			return null;
		}
	}

	/** The chat history entry has exactly one no-argument method that returns a Component. */
	private static Method findContentMethod(Class<?> type) {
		for (Method m : type.getDeclaredMethods()) {
			if (m.getParameterCount() == 0 && !Modifier.isStatic(m.getModifiers())
				&& m.getReturnType() == Component.class) {
				m.setAccessible(true);
				return m;
			}
		}
		return null;
	}

	private static List<?> messageList(ChatComponent chat) throws IllegalAccessException {
		if (messagesField == null) {
			messagesField = findMessagesField();
		}
		if (messagesField == null) {
			return null;
		}
		Object value = messagesField.get(chat);
		return value instanceof List<?> list ? list : null;
	}

	private static Field findMessagesField() {
		// 1) By intermediary name (net.minecraft.client.gui.hud.ChatHud#messages).
		try {
			String name = Mappings.field("net.minecraft.class_338", "field_2061", "Ljava/util/List;");
			Field f = ChatComponent.class.getDeclaredField(name);
			f.setAccessible(true);
			return f;
		} catch (Throwable ignored) {
			// fall through to the type based search
		}

		// 2) By type: the list whose entries expose a Component (the other list holds rendered lines).
		for (Field f : ChatComponent.class.getDeclaredFields()) {
			if (Modifier.isStatic(f.getModifiers()) || !List.class.isAssignableFrom(f.getType())) {
				continue;
			}
			Type generic = f.getGenericType();
			if (generic instanceof ParameterizedType pt && pt.getActualTypeArguments().length == 1
				&& pt.getActualTypeArguments()[0] instanceof Class<?> element
				&& findContentMethod(element) != null) {
				f.setAccessible(true);
				return f;
			}
		}
		return null;
	}

	/** Rebuilds the on-screen lines from the (now shorter) history. */
	private static void refresh(ChatComponent chat) {
		try {
			if (refreshMethod == null) {
				refreshMethod = findRefreshMethod();
			}
			if (refreshMethod != null) {
				refreshMethod.invoke(chat);
			}
		} catch (Throwable t) {
			PayNotifyClient.LOGGER.error("Could not refresh the chat window", t);
		}
	}

	private static Method findRefreshMethod() {
		// net.minecraft.client.gui.hud.ChatHud#reset() - Mojang name: rescaleChat()
		String[] names = {
			Mappings.method("net.minecraft.class_338", "method_1817", "()V"),
			"rescaleChat"
		};
		for (String name : names) {
			try {
				Method m = ChatComponent.class.getDeclaredMethod(name);
				m.setAccessible(true);
				return m;
			} catch (Throwable ignored) {
				// try next
			}
		}
		return null;
	}
}
