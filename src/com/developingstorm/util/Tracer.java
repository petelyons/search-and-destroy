package com.developingstorm.util;

import java.io.*;

public class Tracer {

  public static final Tracer INSTANCE = new Tracer();
  private PrintStream _log = null;
  
  public Tracer() {
  }
  
  public Tracer(String filename) throws IOException {
    PrintStream ps = new PrintStream(filename);
    setLogStream(ps);
  }

  public void setLogStream(PrintStream ps) throws IOException {
    _log = ps;
  }

  public void logln(Object s) {
    if (_log != null)
      _log.println(s);
  }
  
  public void log(Object s) {
    if (_log != null)
      _log.print(s);
  }

  public void term() {
    if (_log != null) {
      _log.close();
    }
  }

  public void println(Object s) {
    logln(s);
    System.out.println(s);
  }

  public void print(Object s) {
    log(s);
    System.out.print(s);
  }
  

}
