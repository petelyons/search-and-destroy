package com.developingstorm.util;

/**
 * A base class for enumeration objects.
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
 * 
 */
public class Enum {

  private final EnumClass _class;
  private final String _name;
  private final int _id;

  /**
   * Construct a new enumeration object.
   */
  protected Enum(EnumClass ec, String name) {
    _class = ec;
    _name = name;
    _id = _class.add(this);
  }

  /**
   * Construct a new enumeration object with a specific Id. Objects MUST be
   * constructed in order of id from id 0 to id n.
   */
  protected Enum(EnumClass ec, int id, String name) {
    _class = ec;
    _name = name;
    _id = id;
    _class.add(id, this);
  }

  /**
   * Get the debug displayable name of this enumeration object
   * 
   * @return String the displayable name
   */
  public String toString() {
    return _name;
  }

  /**
   * Get the integer Id of this enumeration object:
   * 
   * @return int the integer id
   */
  public int getId() {
    return _id;
  }

  /**
   * Get the raw name of the enum
   */
  String getName() {
    return _name;
  }

}
