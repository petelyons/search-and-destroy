package com.developingstorm.games.hexboard;

import com.developingstorm.exceptions.InvalidMapException;
import com.developingstorm.games.sad.SaDException;
import com.developingstorm.util.RandomUtil;
import com.developingstorm.util.ResourceUtil;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;

/**
 * Routines to load and save a grid of integer values.
 *
 */
public class HexBoardMap {

    private int width;
    private int height;
    private int[][] data;

    public HexBoardMap(int width, int height) {
        this.width = width;
        this.height = height;
        data = new int[this.width][this.height];
        LocationMap.init(this.width, this.height);
    }

    public int[][] getData() {
        return data;
    }

    public int getData(int x, int y) {
        return this.data[x][y];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return width;
    }

    public Location random() {
        int nx = RandomUtil.getInt(this.width);
        int ny = RandomUtil.getInt(this.height);
        return Location.get(nx, ny);
    }

    public void saveMap(Writer writer) throws IOException {
        writer.write("TYPE=GRID\r\n");
        writer.write("VERSION=1\r\n");
        writer.write("WIDTH=" + this.width + "\r\n");
        writer.write("HEIGHT=" + this.height + "\r\n");
        writer.write("DATA BEGIN\r\n");

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writer.write("" + this.data[x][y] + "\r\n");
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

    public static HexBoardMap loadMap(String filename)
        throws IOException, InvalidMapException {
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

    public static HexBoardMap loadMapAsResource(
        Object container,
        String resourceName
    ) {
        try {
            InputStream is = ResourceUtil.openResourceStream(
                container.getClass().getClassLoader(),
                resourceName
            ); //"MedMap.sdm"
            InputStreamReader sr = new InputStreamReader(is);
            return HexBoardMap.loadMap(sr);
        } catch (Exception e) {
            e.printStackTrace();
            throw new SaDException("Bad Map:" + e);
        }
    }

    public static HexBoardMap loadMap(Reader reader)
        throws IOException, InvalidMapException {
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

        HexBoardMap map = new HexBoardMap(width, height);
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
