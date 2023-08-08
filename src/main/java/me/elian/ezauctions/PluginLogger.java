package me.elian.ezauctions;

import com.google.inject.ImplementedBy;

@ImplementedBy(PluginLoggerImpl.class)
public interface PluginLogger {
	void warning(String message, Exception exception);

	void severe(String message);

	void severe(String message, Exception exception);
}
