package com.developingstorm.games.sad.fx;

import javafx.scene.image.Image;

/**
 * Loads and manages terrain images for JavaFX rendering.
 * Corresponds to GameIcons in the Swing version.
 */
public class TerrainImages {

    private static final String IMAGE_PATH = "/images/";

    // Terrain images
    private final Image waterImage;
    private final Image landImage;
    private final Image cityImage;
    private final Image unexploredImage;

    // Unit images
    private final Image infantryImage;
    private final Image armorImage;
    private final Image fighterImage;
    private final Image bomberImage;
    private final Image transportImage;
    private final Image cargoImage;
    private final Image destroyerImage;
    private final Image submarineImage;
    private final Image cruiserImage;
    private final Image carrierImage;
    private final Image battleshipImage;

    // Status overlay images
    private final Image sentryArmyImage;
    private final Image sentryTankImage;
    private final Image fullTransportImage;
    private final Image fullCargoImage;
    private final Image fullCarrierImage;
    private final Image anchorImage;

    // Singleton instance
    private static TerrainImages instance;

    public static TerrainImages getInstance() {
        if (instance == null) {
            instance = new TerrainImages();
        }
        return instance;
    }

    private TerrainImages() {
        // Load terrain images
        waterImage = loadImage("water.gif");
        landImage = loadImage("land.gif");
        cityImage = loadImage("city.gif");
        unexploredImage = loadImage("unexplored.gif");

        // Load unit images
        infantryImage = loadImage("army.gif");
        armorImage = loadImage("tank.gif");
        fighterImage = loadImage("fighter.gif");
        bomberImage = loadImage("bomber.gif");
        transportImage = loadImage("transport.gif");
        cargoImage = loadImage("cargo.gif");
        destroyerImage = loadImage("destroyer.gif");
        submarineImage = loadImage("sub.gif");
        cruiserImage = loadImage("cruiser.gif");
        carrierImage = loadImage("carrier.gif");
        battleshipImage = loadImage("battleship.gif");

        // Load status overlay images
        sentryArmyImage = loadImage("tent.gif");
        sentryTankImage = loadImage("sentrytank.gif");
        fullTransportImage = loadImage("fulltransport.gif");
        fullCargoImage = loadImage("fullcargo.gif");
        fullCarrierImage = loadImage("fullcarrier.gif");
        anchorImage = loadImage("anchor.gif");
    }

    private Image loadImage(String filename) {
        try {
            String path = IMAGE_PATH + filename;
            java.io.InputStream stream = getClass().getResourceAsStream(path);
            if (stream == null) {
                System.err.println("Could not find image: " + path);
                return null;
            }
            return new Image(stream);
        } catch (Exception e) {
            System.err.println(
                "Error loading image " + filename + ": " + e.getMessage()
            );
            return null;
        }
    }

    /**
     * Get the image for a terrain type.
     * @param terrainType 0 = water, 1 = land
     * @return The terrain image
     */
    public Image getTerrainImage(int terrainType) {
        if (terrainType == 0) {
            return waterImage;
        } else {
            return landImage;
        }
    }

    public Image getWaterImage() {
        return waterImage;
    }

    public Image getLandImage() {
        return landImage;
    }

    public Image getCityImage() {
        return cityImage;
    }

    public Image getUnexploredImage() {
        return unexploredImage;
    }

    /**
     * Get the width of terrain images.
     */
    public double getImageWidth() {
        if (waterImage != null) {
            return waterImage.getWidth();
        }
        return 42; // fallback
    }

    /**
     * Get the height of terrain images.
     */
    public double getImageHeight() {
        if (waterImage != null) {
            return waterImage.getHeight();
        }
        return 48; // fallback
    }

    /**
     * Get unit image by type (basic, without status).
     */
    public Image getUnitImage(com.developingstorm.games.sad.Type type) {
        if (type == null) return null;

        switch (type) {
            case INFANTRY:
                return infantryImage;
            case ARMOR:
                return armorImage;
            case FIGHTER:
                return fighterImage;
            case BOMBER:
                return bomberImage;
            case TRANSPORT:
                return transportImage;
            case CARGO:
                return cargoImage;
            case DESTROYER:
                return destroyerImage;
            case SUBMARINE:
                return submarineImage;
            case CRUISER:
                return cruiserImage;
            case CARRIER:
                return carrierImage;
            case BATTLESHIP:
                return battleshipImage;
            default:
                return null;
        }
    }

    /**
     * Get unit image with status (sentry, loaded, etc).
     * Matches Swing BoardCanvas.getUnitImage() logic.
     */
    public Image getUnitImageWithStatus(
        com.developingstorm.games.sad.Unit unit
    ) {
        if (unit == null) return null;

        com.developingstorm.games.sad.Type type = unit.getType();

        // Check for sentry infantry (army)
        if (
            type == com.developingstorm.games.sad.Type.INFANTRY &&
            unit.inSentryMode()
        ) {
            return sentryArmyImage;
        }

        // Check for sentry armor
        if (
            type == com.developingstorm.games.sad.Type.ARMOR &&
            unit.inSentryMode()
        ) {
            return sentryTankImage;
        }

        // Check for loaded transport
        if (
            type == com.developingstorm.games.sad.Type.TRANSPORT &&
            unit.carriedWeight() > 0
        ) {
            return fullTransportImage;
        }

        // Check for loaded cargo plane
        if (
            type == com.developingstorm.games.sad.Type.CARGO &&
            unit.carriedWeight() > 0
        ) {
            return fullCargoImage;
        }

        // Check for loaded carrier
        if (
            type == com.developingstorm.games.sad.Type.CARRIER &&
            unit.carriedWeight() > 0
        ) {
            return fullCarrierImage;
        }

        // Default: return basic unit image
        return getUnitImage(type);
    }

    /**
     * Get anchor overlay image (for sea units in sentry mode).
     */
    public Image getAnchorImage() {
        return anchorImage;
    }
}
