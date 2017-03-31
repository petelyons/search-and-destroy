package com.developingstorm.games.sad;

import java.util.ArrayList;
import java.util.List;

import com.developingstorm.games.sad.edicts.AirPatrol;
import com.developingstorm.games.sad.edicts.AutoSentry;
import com.developingstorm.games.sad.edicts.SendAirUnits;
import com.developingstorm.games.sad.edicts.SendLandUnits;
import com.developingstorm.games.sad.edicts.SendSeaUnits;



/**
 * Each city has an EdictGovernor that executes its assigned edicts.
 */
public class EdictGovernor {
  
  
  private final City _c;
  private final Player _player;
  private volatile SendAirUnits _airPath;
  private volatile SendLandUnits _landPath;
  private volatile SendSeaUnits _seaPath;
  private volatile AirPatrol _airPatrol;
  private volatile AutoSentry _autoSentry;
  
  private final Game _game;

  EdictGovernor(Player player, City c) {
    _c = c;
    _player = player;
    _game = _c.getGame();
  }
  
  public void setAirPathDest(City c) {
    _airPath = _player.edictFactory().sendAirUnits(_c, c);
    _airPath.execute(_game);
  }

  public void setLandPathDest(City c) {
    _landPath = _player.edictFactory().sendLandUnits(_c, c);
    _landPath.execute(_game);
  }

  public void setSeaPathDest(City c) {
    _seaPath = _player.edictFactory().sendSeaUnits(_c, c);
    _seaPath.execute(_game);
  }

  public void setAirPatrol() {
    _airPatrol = _player.edictFactory().airPatrol(_c);
    _airPatrol.execute(_game);
  }
  public void setAutoSentry() {
    _autoSentry = _player.edictFactory().autoSentry(_c);
    _autoSentry.execute(_game);
  }

  public City getAirPathDest() {
    if (_airPath != null) {
      return _airPath.destination();
    }
    return null;
  }

  public City getLandPathDest() {
    if (_landPath != null) {
      return _landPath.destination();
    }
    return null;
  }
  public City getSeaPathDest() {
    if (_seaPath != null) {
      return _seaPath.destination();
    }
    return null;
  }
  
  public boolean hastAirPath() {
    return (_airPath != null);
  }

  public boolean hasLandPath() {
    return(_landPath != null);
  }
  public boolean hasSeaPath() {
    return (_seaPath != null);
  }
  
  public boolean hasAirPatrol() {
    return _airPatrol != null;
  }
    
  public boolean hasAutoSentry() {
    return _autoSentry != null;
  }
  
  private List<Edict> getEdicts() {
    List<Edict> edicts = new ArrayList<Edict>();
    if (_airPatrol != null) {
      edicts.add(_airPatrol);
    }
    if (_autoSentry != null) {
      edicts.add(_autoSentry);
    }
    if (_seaPath != null) {
      edicts.add(_seaPath);
    }
    if (_landPath != null) {
      edicts.add(_landPath);
    }
    if (_airPath != null) {
      edicts.add(_airPath);
    }
    return edicts;
  }

  public void execute() {
    List<Edict> edicts = getEdicts();
    for (Edict e : edicts) {
      e.execute(_game);
    }
  }

  public void clearSeaPath() {
    _seaPath = null;
    
  }

  public void clearAirPath() {
    _airPath = null;
    
  }

  public void clearLandPath() {
    _landPath = null;
    
  }

  public void clearAirPatrol() {
    _airPatrol = null;
    
  }

  public void clearAutoSenty() {
    _autoSentry = null;
    
  }

  public Object toJson() {
    // TODO Auto-generated method stub
    return null;
  }

}
