package me.elian.ezauctions;

import com.google.inject.ImplementedBy;

@ImplementedBy(PluginLogger.class)
public interface Logger {
	void info(String message);

	void warning(String message);

	void warning(String message, Exception exception);

	void severe(String message);

	void severe(String message, Exception exception);
}
