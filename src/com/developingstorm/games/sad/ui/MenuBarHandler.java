package com.developingstorm.games.sad.ui;

public interface MenuBarHandler {
 
  void onExit();
  void onSave();
  void onSaveAs();
  void onOpen();
  void onAbout();
  void onCenter();
  void onNew();
  
  void onDebugAstar(boolean v);
  void onDebugExplore(boolean v);
  void onDebugGodLens(boolean v);
  void onDebugContinentNumbers(boolean selected);
  
  void onDebugDump();
  
  void onGameMode();
  void onPathsMode();
 
}
