package com.developingstorm.games.sad.util;

import com.developingstorm.util.Tracer;

public class Log {
  public enum Severity {
    DEBUG,
    INFO,
    WARN,
    ERROR
    
  };

  private Log() {
  }
  
  private static void println(Severity sev, Object context, String desc) {
    StringBuilder sb = new StringBuilder();
    sb.append(sev.name());
    sb.append(' ');
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
    Tracer.INSTANCE.println(sb.toString());
  }

  public static void info(Object context, String desc) {
    println(Severity.INFO, context, desc);
  }

  public static void debug(Object context, String desc) {
    println(Severity.DEBUG, context, desc);
  }

  public static void error(Object context, String desc) {
    println(Severity.ERROR, context, desc);
  }

  public static void warn(Object context, String desc) {
    println(Severity.WARN, context, desc);
  }

  public static void info(String desc) {
    println(Severity.INFO, null, desc);
  }

  public static void debug(String desc) {
    println(Severity.DEBUG, null, desc);
  }

  public static void error(String desc) {
    println(Severity.ERROR, null, desc);
  }

  public static void warn(String desc) {
    println(Severity.WARN, null, desc);
  }

  public static void stack(String string) {
    Tracer.INSTANCE.println(string);
    Exception e = new Exception(string);
    e.printStackTrace();
  }

}
