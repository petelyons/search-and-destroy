package com.developingstorm.util;

import java.util.HashMap;
import java.util.List;

public abstract class Graph<T, S> {
  private HashMap<T, GraphNode<T, S>> processed = new HashMap<T, GraphNode<T, S>>();
  
  
  public GraphNode<T, S> buildGraphNodes(T start) {
    
    if (this.processed.containsKey(start)) {
      return this.processed.get(start);
    }
    
    GraphNode<T, S> node = new GraphNode<T, S>(this, start);
    this.processed.put(start, node);
    List<T> list = findRelatives(start);
    for (T c : list) {
      GraphNode<T, S> cn = buildGraphNodes(c);
      node.add(cn);
    }
    return node;
  }
  
  protected abstract List<T> findRelatives(T start);

  public boolean containsKey(T key) {
    return this.processed.containsKey(key);
  }
  
  public GraphNode<T, S> get(T key) {
    return this.processed.get(key);
  }
}
