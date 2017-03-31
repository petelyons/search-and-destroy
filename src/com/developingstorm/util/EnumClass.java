package com.developingstorm.util;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * EnumClass represents a set of related enumeration objects. This class should
 * be used in conjunction with com.ibm.nagano.util.Enum to form sets of
 * persistable enumerations.
 * 
 * <p>
 * A typical usage where you don't care what integer ID is assigned to an
 * enumeration would look like:
 * <p>
 * 
 * <code><pre>
 * public class Foo extends Enum {
 *     private static final EnumClass _class = new EnumClass("Foo");
 * 
 *     public static final Foo EXAMPLE1 = new Foo("Example1");
 *     public static final Foo EXAMPLE2 = new Foo("Example2"); 
 * 
 *     private Foo(String name) {
 *         super(_class, name);
 *     }
 * }
 * </pre></code>
 * <p>
 * 
 * In this use case the order of the construction implies the Id; EXAMPLE1 would
 * be assigned the id zero while EXAMPLE2 would be assigned the id one. If you
 * want to force the id assignment you can use the second form of the
 * constructor that takes an id, however, this requires that id's are specified
 * in ascending order - the same order that they would naturually be assigned.
 * 
 * <code><pre>
 * public class Foo extends Enum {
 *     private static final EnumClass _class = new EnumClass("Foo");
 *  
 *     public static final int EXAMPLE1_ID = 0;
 *     public static final int EXAMPLE2_ID = 1; 
 * 
 *     public static final Foo EXAMPLE1 = new Foo(EXAMPLE1_ID, "Example1");
 *     public static final Foo EXAMPLE2 = new Foo(EXAMPLE2_ID, "Example2"); 
 * 
 *     private Foo(int id, String name) {
 *         super(_class, id, name);
 *     }
 * 
 *     public Foo get(int id) {
 *         return (Foo) _class.get(id);
 *     }
 * }
 * </pre></code>
 * <p>
 */
public final class EnumClass {
  private final String _name;

  // TODO: If we implemented a sparse array mechanism here instead of the
  // ArrayList,
  // we could allow the enums to have ordinals that were out of sequence...
  private final ArrayList<Enum> _list;
  private final HashMap<String, Enum> _map;

  /**
   * Construct an enumeration class
   * 
   * @param name
   *          the debug displayable name of the enumeration class
   */
  public EnumClass(String name) {
    _name = name;
    _list = new ArrayList<Enum>();
    _map = new HashMap<String, Enum>();
  }

  /**
   * Adds a enumeration object to this class of enumerations
   * 
   * @param e
   *          the enumeration to add
   * @return int the integer Id to be assigned to the enumeration
   */
  final synchronized int add(Enum e) {
    int id = _list.size();
    _list.add(e);

    if (_map.containsKey(e.getName())) {
      throw new Error("An enumeration with this name already exists");
    }

    _map.put(e.getName(), e);

    return id;
  }

  /**
   * Adds a enumeration object to this class of enumerations at a specific
   * offset. The offset MUST be equal to the size of the current enumeration.
   * This means that callers must add enumerations in order from id 0 to id n
   * 
   * @param id
   *          the offset to add the enumeration at
   * @param e
   *          the enumeration to add
   */
  final synchronized void add(int id, Enum e) {
    int offset = add(e);
    if (offset != id) {
      throw new Error("Enumerations must be added in order");
    }
  }

  /**
   * Get the debug displayable name of the enumeration
   */
  public final String toString() {
    return _name;
  }

  /**
   * Get the enumeration object that represents the integer ID.
   * 
   * @param id
   *          the interger ID of the enumeraion
   * @return Enum the enumeration object
   */
  public final Enum get(int id) {
    if (id >= 0 && id < _list.size())
      return _list.get(id);
    else
      return null;
  }

  /**
   * Get the enumeration object with a specific name.
   * 
   * @param name
   *          the case sensitive name of the enumeraion
   * @return Enum the enumeration object
   */
  public final Enum get(String name) {
    return _map.get(name);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_name == null) ? 0 : _name.hashCode());
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
    EnumClass other = (EnumClass) obj;
    if (_name == null) {
      if (other._name != null)
        return false;
    } else if (!_name.equals(other._name))
      return false;
    return true;
  }

  public int items() {
    return _list.size();
  }

}
