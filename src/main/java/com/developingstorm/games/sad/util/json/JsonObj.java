package com.developingstorm.games.sad.util.json;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

public class JsonObj {
  private HashMap<String, Object> _map = new HashMap<String, Object>();
  

  public String getString(String key) {
    return (String) get(key);
  }
  
  private Object get(String key) {
    return _map.get(key);
  }
  
  public JsonObj getObj(String key) {
    return (JsonObj) get(key);
  }
  
  public Integer getInteger(String key) {
    return (Integer) get(key);
  }
  
  public Long getLong(String key) {
    return (Long) get(key);
  }

  public Double getDouble(String key) {
    return (Double) get(key);
  }
  
  public Boolean getBoolean(String key) {
    return (Boolean) get(key);
  }
  
  public Object[] getArray(String key) {
    return (Object[]) get(key);
  }
//
  
  public void put(String key, Object val) {
    _map.put(key, val);
  }
  
  
  public Set<String> keySet() {
    return _map.keySet();
  }
  public Set<Entry<String, Object>> entrySet() {
    return _map.entrySet();
  }

}
