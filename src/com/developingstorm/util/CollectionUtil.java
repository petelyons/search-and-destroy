package com.developingstorm.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectionUtil {
  public static <T> Set<T> subtract(Set<T> main, Set<T> other) {
    Set<T> newSet = new HashSet<T>(main);
    newSet.removeAll(other);
    return newSet;
  }
  
  public static <T> Set<T> intersect(Set<T> main, Set<T> other) {
    Set<T> newSet = new HashSet<T>(main);
    newSet.retainAll(other);
    return newSet;
  }
  
  public static <T> Set<T> create(T[] vals) {
    return new HashSet<T>(Arrays.asList(vals));
    
  }
  
  
  public static <T> List<T> shuffle(Collection<T> vals) {
    List<T> list = new ArrayList<T>(vals);
    Collections.shuffle(list);
    return list;
  }
  
}
