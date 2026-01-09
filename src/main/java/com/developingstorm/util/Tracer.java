package com.developingstorm.util;

import java.io.*;

public class Tracer {

  public static final Tracer INSTANCE = new Tracer();
  private PrintStream log = null;
  
  public Tracer() {
  }
  
  public Tracer(String filename) throws IOException {
    PrintStream ps = new PrintStream(filename);
    setLogStream(ps);
  }

  public void setLogStream(PrintStream ps) throws IOException {
    log = ps;
  }

  public void logln(Object s) {
    if (this.log != null)
      this.log.println(s);
  }
  
  public void log(Object s) {
    if (this.log != null)
      this.log.print(s);
  }

  public void term() {
    if (this.log != null) {
      this.log.close();
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
