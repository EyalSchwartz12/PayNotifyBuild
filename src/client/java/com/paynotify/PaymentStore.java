package com.paynotify;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Remembers recent payments so every button set keeps its own payer and amount. */
final class PaymentStore {
	private static final Map<Integer, PaymentRecord> RECORDS = new LinkedHashMap<>();
	private static int nextId = 1;

	private PaymentStore() {
	}

	static synchronized int allocateId() {
		return nextId++;
	}

	static synchronized void put(PaymentRecord record, int max) {
		RECORDS.put(record.id(), record);
		while (RECORDS.size() > max) {
			Iterator<Integer> it = RECORDS.keySet().iterator();
			it.next();
			it.remove();
		}
	}

	static synchronized PaymentRecord get(int id) {
		return RECORDS.get(id);
	}

	static synchronized void remove(int id) {
		RECORDS.remove(id);
	}

	static synchronized int size() {
		return RECORDS.size();
	}
}
