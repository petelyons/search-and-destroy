package com.developingstorm.games.sad.fx;

import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.controller.GameController;
import com.developingstorm.games.sad.controller.GameQueryService;
import com.developingstorm.games.sad.fx.TerrainImages;
import com.developingstorm.games.sad.orders.Explore;
import com.developingstorm.games.sad.orders.HeadHome;
import com.developingstorm.games.sad.orders.Sentry;
import com.developingstorm.games.sad.orders.SkipTurn;
import com.developingstorm.games.sad.orders.Unload;
import java.awt.image.BufferedImage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Panel displaying information about the selected unit.
 */
public class UnitInfoPanel extends VBox {

    private final GameController controller;
    private final GameQueryService query;

    private Unit currentUnit;

    private Label titleLabel;
    private StackPane iconPanel;
    private ImageView iconView;
    private Label turnLabel;
    private Label nameLabel;
    private Label typeLabel;
    private Label ownerLabel;
    private Label healthLabel;
    private Label locationLabel;
    private Label movedLabel;
    private Label carriesLabel;
    private Label orderLabel;
    private Label movesLabel;

    public UnitInfoPanel(GameController controller, GameQueryService query) {
        this.controller = controller;
        this.query = query;

        initializeUI();
    }

    private void initializeUI() {
        setSpacing(10);
        setPadding(new Insets(10));
        setAlignment(Pos.TOP_LEFT);

        // Title
        titleLabel = new Label("Unit Information");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.WHITE);

        // Icon panel with background color (matches Swing version)
        iconView = new ImageView();
        iconView.setFitWidth(48);
        iconView.setFitHeight(48);
        iconView.setPreserveRatio(true);
        iconView.setSmooth(true);

        iconPanel = new StackPane(iconView);
        iconPanel.setPrefSize(64, 64);
        iconPanel.setMinSize(64, 64);
        iconPanel.setMaxSize(64, 64);
        iconPanel.setStyle(
            "-fx-border-color: black; -fx-border-width: 2; -fx-background-color: lightgray;"
        );
        iconPanel.setAlignment(Pos.CENTER);

        // Info labels
        turnLabel = createInfoLabel();
        nameLabel = createInfoLabel();
        typeLabel = createInfoLabel();
        ownerLabel = createInfoLabel();
        healthLabel = createInfoLabel();
        locationLabel = createInfoLabel();
        movedLabel = createInfoLabel();
        carriesLabel = createInfoLabel();
        orderLabel = createInfoLabel();
        movesLabel = createInfoLabel();

        // Add all to panel
        getChildren().addAll(
            titleLabel,
            createSeparator(),
            iconPanel,
            createSeparator(),
            turnLabel,
            nameLabel,
            typeLabel,
            ownerLabel,
            createSeparator(),
            healthLabel,
            movesLabel,
            movedLabel,
            carriesLabel,
            locationLabel,
            createSeparator(),
            orderLabel
        );

        // Set no unit selected initially
        setUnit(null);
    }

    private Label createInfoLabel() {
        Label label = new Label();
        label.setTextFill(Color.LIGHTGRAY);
        label.setWrapText(true);
        return label;
    }

    private Label createSeparator() {
        Label sep = new Label("─".repeat(30));
        sep.setTextFill(Color.GRAY);
        return sep;
    }

    /**
     * Convert AWT BufferedImage to JavaFX Image.
     */
    private Image convertToFXImage(java.awt.Image awtImage) {
        if (awtImage == null) {
            System.err.println("convertToFXImage: awtImage is null");
            return null;
        }

        // If it's not a BufferedImage, we need to convert it
        BufferedImage bImg;
        if (!(awtImage instanceof BufferedImage)) {
            System.out.println(
                "convertToFXImage: Converting Image to BufferedImage"
            );
            int width = awtImage.getWidth(null);
            int height = awtImage.getHeight(null);
            if (width <= 0 || height <= 0) {
                System.err.println(
                    "convertToFXImage: Invalid image dimensions: " +
                        width +
                        "x" +
                        height
                );
                return null;
            }
            bImg = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_ARGB
            );
            java.awt.Graphics2D g = bImg.createGraphics();
            g.drawImage(awtImage, 0, 0, null);
            g.dispose();
        } else {
            bImg = (BufferedImage) awtImage;
        }

        int width = bImg.getWidth();
        int height = bImg.getHeight();
        System.out.println(
            "convertToFXImage: Converting " + width + "x" + height + " image"
        );

        WritableImage fxImage = new WritableImage(width, height);
        PixelWriter pixelWriter = fxImage.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = bImg.getRGB(x, y);
                pixelWriter.setArgb(x, y, argb);
            }
        }

        System.out.println("convertToFXImage: Successfully converted image");
        return fxImage;
    }

    /**
     * Get player color (matches MapCanvas logic).
     */
    private Color getPlayerColor(Player player) {
        int playerId = player.getId();
        if (playerId == 1) {
            return Color.rgb(250, 100, 100); // Red
        } else if (playerId == 2) {
            return Color.rgb(150, 150, 250); // Blue
        } else {
            return Color.rgb(100, 250, 100); // Green
        }
    }

    /**
     * Get the unit currently displayed in this panel.
     */
    public Unit getUnit() {
        return currentUnit;
    }

    /**
     * Update the panel to show information about the given unit.
     * Uses helper methods from Unit class to match Swing formatting.
     */
    public void setUnit(Unit unit) {
        this.currentUnit = unit;

        // Always show turn number
        turnLabel.setText("Turn: " + query.getGame().getTurn());

        if (unit == null) {
            // No unit selected
            iconView.setImage(null);
            iconPanel.setBackground(
                new Background(
                    new BackgroundFill(
                        Color.LIGHTGRAY,
                        CornerRadii.EMPTY,
                        Insets.EMPTY
                    )
                )
            );
            nameLabel.setText("No unit selected");
            typeLabel.setText("");
            ownerLabel.setText("");
            healthLabel.setText("");
            locationLabel.setText("");
            movedLabel.setText("");
            carriesLabel.setText("");
            orderLabel.setText("");
            movesLabel.setText("");
        } else {
            // Set unit icon with status indicators (loaded, fuel, etc.)
            Image fxImage = TerrainImages.getInstance().getUnitImageWithStatus(
                unit
            );
            if (fxImage != null) {
                iconView.setImage(fxImage);
            } else {
                System.err.println(
                    "Failed to get icon for unit type: " + unit.getType()
                );
            }

            // Set background color to player's color
            Color playerColor = getPlayerColor(unit.getOwner());
            iconPanel.setBackground(
                new Background(
                    new BackgroundFill(
                        playerColor,
                        CornerRadii.EMPTY,
                        Insets.EMPTY
                    )
                )
            );

            // Show unit info using helper methods (matches Swing)
            nameLabel.setText("Unit: " + unit.name);
            typeLabel.setText("Type: " + unit.typeDesc());
            ownerLabel.setText("Owner: " + unit.getOwner().toString());

            // Use helper method for health
            healthLabel.setText("Health: " + unit.life().healthDesc());

            // Show max moves
            int maxMoves = unit.getType().getDist();
            movesLabel.setText("Max Moves: " + maxMoves);

            // Use helper method for moved status
            movedLabel.setText("Moved: " + unit.life().moveDesc());

            // Use helper method for cargo
            carriesLabel.setText("Carries: " + unit.carriesDesc());

            // Use helper method for location
            String locDesc = unit.locationDesc();
            System.out.println(
                "UnitInfoPanel: Unit " +
                    unit.name +
                    " location=" +
                    unit.getLocation() +
                    " desc=" +
                    locDesc
            );
            locationLabel.setText("Location: " + locDesc);

            // Show order status (full order toString, not just type)
            String orderText = "Status: ";
            if (unit.getOrder() != null) {
                orderText += unit.getOrder().toString();
            } else {
                orderText += "(none)";
            }
            orderLabel.setText(orderText);
        }
    }
}
