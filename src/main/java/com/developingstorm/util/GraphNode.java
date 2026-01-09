package com.developingstorm.util;

import java.util.HashSet;

import java.util.Set;

public class GraphNode<T, S> {
  
  GraphNode(Graph<T, S> g, T start) {
    graph = g;
    obj = start;
    relatives = new HashSet<GraphNode<T, S>>();
  }
  
  private Graph<T, S> graph;
  private T obj;
  private Set<GraphNode<T, S>> relatives;
  private S state;
  
  public void add(GraphNode<T, S> node) {
    this.relatives.add(node);
  }
  
  public T getContent() {
    return obj;
  }
 
  public S getState() {
    return state;
  }
  
  public void setState(S state) {
    this.state = state;
  }
  
  public boolean isRelative(GraphNode<T, S> node) {
    return this.relatives.contains(node);
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((obj == null) ? 0 : this.obj.hashCode());
    result = prime * result
        + ((relatives == null) ? 0 : this.relatives.hashCode());
    result = prime * result + ((state == null) ? 0 : this.state.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    GraphNode<?, ?> other = (GraphNode<?, ?>) obj;
    if (obj == null) {
      if (other.obj != null)
        return false;
    } else if (!this.obj.equals(other.obj))
      return false;
    if (relatives == null) {
      if (other.relatives != null)
        return false;
    } else if (!this.relatives.equals(other.relatives))
      return false;
    if (state == null) {
      if (other.state != null)
        return false;
    } else if (!this.state.equals(other.state))
      return false;
    return true;
  }

  public Set<GraphNode<T, S>> relatives() {
    return relatives;
  }
}
