package com.developingstorm.games.gridmap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.util.RandomUtil;

/**
 * Routines to load and save a grid of integer values.
 * 
 */
public class GridMap {

  private int _width;
  private int _height;
  private int[][] _data;

  public GridMap(int width, int height) {
    _width = width;
    _height = height;
    _data = new int[_width][_height];
  }

  public int[][] getData() {
    return _data;
  }

  public int getData(int x, int y) {
    return _data[x][y];
  }

  public int getWidth() {
    return _width;
  }

  public int getHeight() {
    return _width;
  }

  public Location random() {
    int nx = RandomUtil.getInt(_width);
    int ny = RandomUtil.getInt(_height);
    return Location.get(nx, ny);
  }

  public void saveMap(Writer writer) throws IOException {

    writer.write("TYPE=GRID\r\n");
    writer.write("VERSION=1\r\n");
    writer.write("WIDTH=" + _width + "\r\n");
    writer.write("HEIGHT=" + _height + "\r\n");
    writer.write("DATA BEGIN\r\n");

    for (int y = 0; y < _height; y++) {
      for (int x = 0; x < _width; x++) {
        writer.write("" + _data[x][y] + "\r\n");
      }
    }
  }

  public void saveMap(String filename) throws IOException {
    FileWriter fw = null;

    try {
      fw = new FileWriter(filename);
      saveMap(fw);
    } finally {
      if (fw != null) {
        fw.close();
      }
    }
  }

  public static GridMap loadMap(String filename) throws IOException,
      InvalidMapException {
    FileReader fr = null;

    try {
      fr = new FileReader(filename);
      return loadMap(fr);
    } finally {
      if (fr != null) {
        fr.close();
      }
    }
  }

  public static GridMap loadMap(Reader reader) throws IOException,
      InvalidMapException {
    int i = 0;
    String line;
    int width;
    int height;
    BufferedReader br;

    if (reader instanceof BufferedReader) {
      br = (BufferedReader) reader;
    } else {
      br = new BufferedReader(reader);
    }

    line = br.readLine(); // TYPE
    line = br.readLine(); // version
    line = br.readLine(); // width
    String prefix = "WIDTH=";
    if (!line.startsWith(prefix)) {
      throw new InvalidMapException();
    } else {
      line = line.substring(prefix.length());
      width = Integer.parseInt(line);
    }
    line = br.readLine(); // height
    prefix = "HEIGHT=";
    if (!line.startsWith(prefix)) {
      throw new InvalidMapException();
    } else {
      line = line.substring(prefix.length());
      height = Integer.parseInt(line);
    }

    line = br.readLine(); // data

    GridMap map = new GridMap(width, height);
    int[][] data = map.getData();

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        line = br.readLine(); // data
        data[x][y] = (byte) Integer.parseInt(line);
      }
    }
    return map;
  }

}
