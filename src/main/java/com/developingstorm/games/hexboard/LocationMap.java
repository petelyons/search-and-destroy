package com.developingstorm.games.hexboard;

public class LocationMap {
  public static LocationMap INSTANCE = null;
  int _width;
  int _height;
  Location[] _locations;
  
  
  public static void init(int width, int height) {
    INSTANCE = new LocationMap();
    INSTANCE.init2(width, height);
  }
  
  private int toOffset(int x, int y) {
    return (y * _width) + x;
  }
  
  
  private void init2(int width, int height) {
    _width = width;
    _height = height;
    _locations = new Location[width * height];
    for(int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        Location loc = Location.init(x, y);
        _locations[toOffset(x,y)] = loc;
      }
    }
    
    for (Location loc : _locations) {
      loc.initNeighbors();
    }
  }

  public Location get(int x, int y) {
    if (!valid(x, y)) {
      return null;
    }
    return _locations[toOffset(x,y)];
  }
  
  public boolean valid(int x, int y) {
    if (x < 0 || y < 0) {
      return false;
    }
    else if (x >= _width || y >= _height) {
      return false;
    }
    return true;
  }
  
}
