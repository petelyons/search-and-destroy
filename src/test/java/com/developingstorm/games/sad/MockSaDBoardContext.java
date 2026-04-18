package com.developingstorm.games.sad;

import com.developingstorm.games.hexboard.HexBoardContext;

class MockSaDBoardContext implements HexBoardContext {

    private static final int MAP_W = 50;
    private static final int MAP_H = 50;

    @Override
    public int getWidth() {
        return MAP_W;
    }

    @Override
    public int getHeight() {
        return MAP_H;
    }

    @Override
    public int getHexSide() {
        return 24;
    }

    @Override
    public int getTerrainImageSelector(int x, int y) {
        return 0;
    }

    @Override
    public int getUnexploredImageSelector() {
        return 0;
    }

    @Override
    public int getPrototypeHex() {
        return 0;
    }

    @Override
    public int getZs() {
        return 3;
    }

    @Override
    public java.awt.Color getBorderColor() {
        return java.awt.Color.GRAY;
    }

    @Override
    public java.awt.Color getSelectionColor() {
        return java.awt.Color.YELLOW;
    }

    @Override
    public java.awt.Color getXorColor() {
        return null;
    }

    @Override
    public boolean showBorder() {
        return true;
    }

    @Override
    public java.awt.Image[] getImages() {
        return new java.awt.Image[0];
    }
}
