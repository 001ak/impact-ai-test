package com.impactai.impactai.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized logger utility for consistent logging across the application
 */
public class LoggerUtil {

    private static final Logger logger = LoggerFactory.getLogger(LoggerUtil.class);

    /**
     * Get logger for a specific class
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    /**
     * Log info with context
     */
    public static void info(Class<?> clazz, String message) {
        getLogger(clazz).info(message);
    }

    /**
     * Log error with context
     */
    public static void error(Class<?> clazz, String message, Throwable throwable) {
        getLogger(clazz).error(message, throwable);
    }

    /**
     * Log debug with context
     */
    public static void debug(Class<?> clazz, String message) {
        getLogger(clazz).debug(message);
    }

    /**
     * Log warning with context
     */
    public static void warn(Class<?> clazz, String message) {
        getLogger(clazz).warn(message);
    }
}
