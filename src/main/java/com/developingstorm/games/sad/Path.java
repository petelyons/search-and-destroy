package com.developingstorm.games.sad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.util.Log;

public class Path implements Iterable<Location> {

  private ArrayList<Location> path;
  private Location start;
  private Location last;

  public Path(Location start) {
    path = new ArrayList<Location>();
    this.start = start;
    last = start;
    if (last == null) {
      throw new Error("A path start cannot be null");
    }
  }

  public void addLocation(Location loc) {
    if (!loc.isNear(this.last, 1)) {
      throw new SaDException("Path points must be contiguous: " + loc + " : " + this.last);
    }
    this.path.add(loc);
    last = loc;
  }

  public void reverse() {
    Collections.reverse(this.path);
    Location temp = last;
    last = start;
    start = temp;
  }

  public boolean isOnPath(Location loc) {
    for (Location l : this.path) {
      if (loc.equals(l))
        return true;
    }
    return false;
  }

  private Location next0(Location loc) {
    boolean returnNext = false;
    for (Location l : this.path) {
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
    return this.path.size();
  }

  public Iterator<Location> iterator() {
    return this.path.iterator();
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    Iterator<Location> itr = this.path.iterator();
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
    return this.path.size() == 0;
  }

  public void verify(Location from, Location to) {
    Log.debug("Verify path:" + this);
    if (!this.path.contains(from)) {
      Log.error("Path missing start:" + from);
    }
    if (!this.path.contains(to)) {
      Log.error("Path missing dest:" + to);
    }
    
  }

  public boolean contains(Location to) {
    return this.path.contains(to);
  }

}
