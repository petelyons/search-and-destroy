package com.developingstorm.games.sad.util.json;

import java.util.Map.Entry;

public class JsonFormatter {
  StringBuilder builder;
    
  private static String format(int v) {
    return "" + v;
  }
  
  private static String format(double v) {
    return "" + v;
  }
  
  private static String format(String v) {
    if (v == null) {
      return "null";
    }
    return "\"" + escape(v) + "\"";
  }

  private static String escape(String v) {
    return v.replace("\"", "\\\"");
  }
  
  private static String formatRaw(Object obj) {
    if (obj == null) {
      return "null";
    }
    if (obj instanceof String) {
      return format((String) obj);
    } else if (obj instanceof Object[]) {
      return format((Object[]) obj);
    } else if (obj instanceof JsonObj) {
      return format((JsonObj) obj);
    } else if (obj instanceof Boolean) {
      return format((Boolean) obj);
    } else if (obj instanceof Integer) {
      return format(((Integer) obj).intValue());
    }  else if (obj instanceof Double) {
      return format(((Double) obj).doubleValue());
    }
    throw new RuntimeException("Invalid data type." + obj.getClass().getSimpleName());
  }
  
  public static String format(JsonObj obj) {
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    int counter = 0;
    for (Entry<String, Object> entry : obj.entrySet()) {
      if (counter > 0) {
        sb.append(',');
      }
      counter++;
      sb.append('"');
      sb.append(entry.getKey());
      sb.append('"');
      sb.append(':');
      sb.append(formatRaw(entry.getValue()));
    
    }
    sb.append('}');
    return sb.toString();
  }

  public static String format(Boolean obj) {
    return "" + obj;
  }

  public static String format(Object[] v) {
    StringBuilder sb = new StringBuilder();
    sb.append('[');
    int counter = 0;
    for (Object v1 : v) {
      if (counter > 0) {
        sb.append(',');
      }
      counter++;
      sb.append(formatRaw(v1));
    }
    sb.append(']');
    return sb.toString();
  }
}
