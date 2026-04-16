package com.developingstorm.games.sad.fx;

import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.controller.GameController;
import com.developingstorm.games.sad.controller.GameQueryService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Panel displaying information about the selected unit.
 */
public class UnitInfoPanel extends VBox {

    private final GameController controller;
    private final GameQueryService query;

    private Unit currentUnit;

    // UI elements
    private HBox iconRow;
    private StackPane iconPanel;
    private ImageView iconView;
    private FlowPane cargoPane;
    private Label nameLabel;
    private Label typeLabel;
    private HBox healthBarRow;
    private Rectangle healthBarFill;
    private Rectangle healthBarEmpty;
    private Label healthText;
    private Label statsLabel;
    private Label locationLabel;
    private Label orderLabel;

    public UnitInfoPanel(GameController controller, GameQueryService query) {
        this.controller = controller;
        this.query = query;

        initializeUI();
    }

    private void initializeUI() {
        setSpacing(6);
        setPadding(new Insets(10));
        setAlignment(Pos.TOP_LEFT);

        // Icon row: main unit icon + cargo icons
        iconView = new ImageView();
        iconView.setFitWidth(48);
        iconView.setFitHeight(48);
        iconView.setPreserveRatio(true);
        iconView.setSmooth(true);

        iconPanel = new StackPane(iconView);
        iconPanel.setPrefSize(64, 64);
        iconPanel.setMinSize(64, 64);
        iconPanel.setMaxSize(64, 64);
        iconPanel.setStyle("-fx-border-color: black; -fx-border-width: 2;");
        iconPanel.setAlignment(Pos.CENTER);

        cargoPane = new FlowPane(2, 2);
        cargoPane.setAlignment(Pos.CENTER_LEFT);
        cargoPane.setPrefWrapLength(80);

        iconRow = new HBox(8);
        iconRow.setAlignment(Pos.CENTER_LEFT);
        iconRow.getChildren().addAll(iconPanel, cargoPane);

        // Name
        nameLabel = new Label();
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        nameLabel.setTextFill(Color.WHITE);

        // Type
        typeLabel = new Label();
        typeLabel.setFont(Font.font("System", 11));
        typeLabel.setTextFill(Color.LIGHTGRAY);

        // Health bar
        healthBarFill = new Rectangle(0, 12);
        healthBarEmpty = new Rectangle(0, 12);
        healthBarEmpty.setFill(Color.rgb(60, 60, 60));
        healthText = new Label();
        healthText.setFont(Font.font("System", 11));
        healthText.setTextFill(Color.LIGHTGRAY);

        healthBarRow = new HBox(6);
        healthBarRow.setAlignment(Pos.CENTER_LEFT);
        HBox barContainer = new HBox(0);
        barContainer.getChildren().addAll(healthBarFill, healthBarEmpty);
        healthBarRow.getChildren().addAll(barContainer, healthText);

        // Stats (moves, fuel)
        statsLabel = new Label();
        statsLabel.setFont(Font.font("System", 11));
        statsLabel.setTextFill(Color.LIGHTGRAY);

        // Location
        locationLabel = new Label();
        locationLabel.setFont(Font.font("System", 11));
        locationLabel.setTextFill(Color.LIGHTGRAY);

        // Order
        orderLabel = new Label();
        orderLabel.setFont(Font.font("System", 11));
        orderLabel.setTextFill(Color.LIGHTGRAY);

        getChildren().addAll(
            iconRow,
            nameLabel,
            typeLabel,
            healthBarRow,
            statsLabel,
            locationLabel,
            orderLabel
        );

        setUnit(null);
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
     */
    public void setUnit(Unit unit) {
        this.currentUnit = unit;

        if (unit == null) {
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
            cargoPane.getChildren().clear();
            nameLabel.setText("No unit selected");
            typeLabel.setText("");
            healthBarFill.setWidth(0);
            healthBarEmpty.setWidth(0);
            healthText.setText("");
            statsLabel.setText("");
            locationLabel.setText("");
            orderLabel.setText("");
        } else {
            // Main unit icon with status
            Image fxImage = TerrainImages.getInstance().getUnitImageWithStatus(
                unit
            );
            if (fxImage != null) {
                iconView.setImage(fxImage);
            }

            // Player color background
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

            // Carried unit icons
            cargoPane.getChildren().clear();
            if (unit.carries != null && !unit.carries.isEmpty()) {
                for (Unit carried : unit.carries) {
                    Image cargoImage = TerrainImages.getInstance().getUnitImage(
                        carried.getType()
                    );
                    if (cargoImage != null) {
                        ImageView cargoView = new ImageView(cargoImage);
                        cargoView.setFitWidth(20);
                        cargoView.setFitHeight(20);
                        cargoView.setPreserveRatio(true);
                        cargoView.setSmooth(true);

                        StackPane cargoIcon = new StackPane(cargoView);
                        cargoIcon.setPrefSize(24, 24);
                        cargoIcon.setMinSize(24, 24);
                        cargoIcon.setMaxSize(24, 24);
                        cargoIcon.setBackground(
                            new Background(
                                new BackgroundFill(
                                    playerColor.deriveColor(0, 1, 0.8, 1),
                                    new CornerRadii(2),
                                    Insets.EMPTY
                                )
                            )
                        );
                        cargoIcon.setAlignment(Pos.CENTER);
                        cargoPane.getChildren().add(cargoIcon);
                    }
                }
            }

            // Name
            nameLabel.setText(
                unit.name != null ? unit.name : "Unit #" + unit.id
            );

            // Type
            typeLabel.setText(unit.getType().toString());

            // Health bar
            int hits = unit.life().hits;
            int maxHits = unit.getType().getHits();
            double ratio = maxHits > 0 ? (double) hits / maxHits : 0;
            double barWidth = 140;
            double fillWidth = barWidth * ratio;

            Color barColor;
            if (ratio > 0.6) {
                barColor = Color.LIMEGREEN;
            } else if (ratio > 0.3) {
                barColor = Color.YELLOW;
            } else {
                barColor = Color.RED;
            }
            healthBarFill.setWidth(fillWidth);
            healthBarFill.setHeight(12);
            healthBarFill.setFill(barColor);
            healthBarEmpty.setWidth(barWidth - fillWidth);
            healthBarEmpty.setHeight(12);
            healthText.setText(hits + "/" + maxHits);

            // Stats: moves and fuel for air units
            int movesLeft = unit.life().movesLeft();
            int maxMoves = unit.getType().getDist();
            String stats = "Moves: " + movesLeft + "/" + maxMoves;
            if (unit.getType().getTravel() == Travel.AIR) {
                int fuel = unit.life().getFuel();
                int maxFuel = unit.life().getMaxFuel();
                stats += "  Fuel: " + fuel + "/" + maxFuel;
            }
            statsLabel.setText(stats);

            // Location
            locationLabel.setText(
                "(" +
                    unit.getLocation().getX() +
                    ", " +
                    unit.getLocation().getY() +
                    ")"
            );

            // Order
            if (unit.getOrder() != null) {
                orderLabel.setText(unit.getOrder().toString());
            } else {
                orderLabel.setText("(none)");
            }
        }
    }
}
