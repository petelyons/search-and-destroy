package com.developingstorm.games.sad.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Log {

    private static final Logger logger = LoggerFactory.getLogger(Log.class);

    public enum Severity {
        DEBUG,
        INFO,
        WARN,
        ERROR,
    }

    private Log() {}

    private static void log(Severity sev, Object context, String desc) {
        String message = formatMessage(context, desc);

        switch (sev) {
            case DEBUG:
                logger.debug(message);
                break;
            case INFO:
                logger.info(message);
                break;
            case WARN:
                logger.warn(message);
                break;
            case ERROR:
                logger.error(message);
                break;
        }
    }

    private static String formatMessage(Object context, String desc) {
        StringBuilder sb = new StringBuilder();
        if (context != null) {
            sb.append('<');
            sb.append(context.toString());
            sb.append('>');
            sb.append(':');
        }
        sb.append('[');
        sb.append(Thread.currentThread().getId());
        sb.append(']');
        sb.append(':');
        sb.append(desc);
        return sb.toString();
    }

    public static void info(Object context, String desc) {
        log(Severity.INFO, context, desc);
    }

    public static void debug(Object context, String desc) {
        log(Severity.DEBUG, context, desc);
    }

    public static void error(Object context, String desc) {
        log(Severity.ERROR, context, desc);
    }

    public static void warn(Object context, String desc) {
        log(Severity.WARN, context, desc);
    }

    public static void info(String desc) {
        log(Severity.INFO, null, desc);
    }

    public static void debug(String desc) {
        log(Severity.DEBUG, null, desc);
    }

    public static void error(String desc) {
        log(Severity.ERROR, null, desc);
    }

    public static void warn(String desc) {
        log(Severity.WARN, null, desc);
    }

    public static void stack(String string) {
        logger.error(string, new Exception(string));
    }
}
