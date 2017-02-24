package com.developingstorm.games.sad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.util.Log;

public class Path {

  private ArrayList<Location> _path;
  private Location _start;
  private Location _last;

  public Path(Location start) {
    _path = new ArrayList<Location>();
    _start = start;
    _last = start;
    if (_last == null) {
      throw new Error("A path start cannot be null");
    }
  }

  public void addLocation(Location loc) {
    if (!loc.isNear(_last, 1)) {
      throw new SaDException("Path points must be contiguous: " + loc + " : " + _last);
    }
    _path.add(loc);
    _last = loc;
  }

  public void reverse() {
    Collections.reverse(_path);
    Location temp = _last;
    _last = _start;
    _start = temp;
  }

  public boolean isOnPath(Location loc) {
    for (Location l : _path) {
      if (loc.equals(l))
        return true;
    }
    return false;
  }

  private Location next0(Location loc) {
    boolean returnNext = false;
    for (Location l : _path) {
      if (returnNext) {
        return l;
      }
      if (loc.equals(l)) {
        returnNext = true;
      }
    }
    return null;
  }

  public Location next(Location loc) {
    Location l = next0(loc);
    return l;
  }

  public int length() {
    return _path.size();
  }

  public Iterator<Location> iterator() {
    return _path.iterator();
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    Iterator<Location> itr = _path.iterator();
    sb.append("Path:{");
    while (itr.hasNext()) {
      Location loc = itr.next();
      sb.append(loc);
      if (itr.hasNext()) {
        sb.append(",");
      }
    }
    sb.append("}");
    return sb.toString();
  }

  public boolean isEmpty() {
    return _path.size() == 0;
  }

  public void verify(Location from, Location to) {
    Log.debug("Verify path:" + this);
    if (!_path.contains(from)) {
      Log.error("Path missing start:" + from);
    }
    if (!_path.contains(to)) {
      Log.error("Path missing dest:" + to);
    }
    
  }

  public boolean contains(Location to) {
    return _path.contains(to);
  }

}
