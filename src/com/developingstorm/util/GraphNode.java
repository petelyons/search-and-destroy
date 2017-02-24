package com.developingstorm.util;

import java.util.HashSet;

import java.util.Set;

import com.developingstorm.games.sad.City;

public class GraphNode<T, S> {
  
  GraphNode(Graph<T, S> g, T start) {
    _graph = g;
    _obj = start;
    _relatives = new HashSet<GraphNode<T, S>>();
  }
  
  private Graph<T, S> _graph;
  private T _obj;
  private Set<GraphNode<T, S>> _relatives;
  private S _state;
  
  public void add(GraphNode<T, S> node) {
    _relatives.add(node);
  }
  
  public T getContent() {
    return _obj;
  }
 
  public S getState() {
    return _state;
  }
  
  public void setState(S state) {
    _state = state;
  }
  
  public boolean isRelative(GraphNode<T, S> node) {
    return _relatives.contains(node);
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_obj == null) ? 0 : _obj.hashCode());
    result = prime * result
        + ((_relatives == null) ? 0 : _relatives.hashCode());
    result = prime * result + ((_state == null) ? 0 : _state.hashCode());
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
    if (_obj == null) {
      if (other._obj != null)
        return false;
    } else if (!_obj.equals(other._obj))
      return false;
    if (_relatives == null) {
      if (other._relatives != null)
        return false;
    } else if (!_relatives.equals(other._relatives))
      return false;
    if (_state == null) {
      if (other._state != null)
        return false;
    } else if (!_state.equals(other._state))
      return false;
    return true;
  }

  public Set<GraphNode<T, S>> relatives() {
    return _relatives;
  }
}
