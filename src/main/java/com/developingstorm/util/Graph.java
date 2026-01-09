package com.developingstorm.util;

import java.util.HashMap;
import java.util.List;

public abstract class Graph<T, S> {
  private HashMap<T, GraphNode<T, S>> _processed = new HashMap<T, GraphNode<T, S>>();
  
  
  public GraphNode<T, S> buildGraphNodes(T start) {
    
    if (_processed.containsKey(start)) {
      return _processed.get(start);
    }
    
    GraphNode<T, S> node = new GraphNode<T, S>(this, start);
    _processed.put(start, node);
    List<T> list = findRelatives(start);
    for (T c : list) {
      GraphNode<T, S> cn = buildGraphNodes(c);
      node.add(cn);
    }
    return node;
  }
  
  protected abstract List<T> findRelatives(T start);

  public boolean containsKey(T key) {
    return _processed.containsKey(key);
  }
  
  public GraphNode<T, S> get(T key) {
    return _processed.get(key);
  }
}
