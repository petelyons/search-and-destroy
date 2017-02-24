package com.developingstorm.games.hexboard;

/**
 * The directions a unit may move
 */
public class Direction {

  

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_id == null) ? 0 : _id.hashCode());
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
    Direction other = (Direction) obj;
    if (_id == null) {
      if (other._id != null)
        return false;
    } else if (!_id.equals(other._id))
      return false;
    return true;
  }

  public static final Direction NORTH_WEST = new Direction("NORTH_WEST");
  public static final Direction NORTH_EAST = new Direction("NORTH_EAST");
  public static final Direction EAST = new Direction("EAST");
  public static final Direction SOUTH_EAST = new Direction("SOUTH_EAST");
  public static final Direction SOUTH_WEST = new Direction("SOUTH_WEST");
  public static final Direction WEST = new Direction("WEST");

  private String _id;

  private Direction(String id) {
    _id = id;
  }
  
  public String toString() {
    return _id;
  }
}
