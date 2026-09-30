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
    private HBox healthPipsRow;
    private Label statsLabel;
    private Label locationLabel;
    private Label orderLabel;
    private VBox alsoHereSection;
    private Label alsoHereTitle;
    private VBox alsoHereList;

    private java.util.function.Consumer<Unit> unitSelectHandler;
    private java.util.function.Consumer<com.developingstorm.games.sad.City> citySelectHandler;

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

        // Health pips (one block per hit point)
        healthPipsRow = new HBox(3);
        healthPipsRow.setAlignment(Pos.CENTER_LEFT);

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

        alsoHereTitle = new Label("Also here");
        alsoHereTitle.setFont(Font.font("System", FontWeight.BOLD, 11));
        alsoHereTitle.setTextFill(Color.LIGHTGRAY);
        alsoHereList = new VBox(2);
        alsoHereSection = new VBox(3, alsoHereTitle, alsoHereList);
        alsoHereSection.setPadding(new Insets(6, 0, 0, 0));
        alsoHereSection.setVisible(false);
        alsoHereSection.setManaged(false);

        getChildren().addAll(
            iconRow,
            nameLabel,
            typeLabel,
            healthPipsRow,
            statsLabel,
            locationLabel,
            orderLabel,
            alsoHereSection
        );

        setUnit(null);
    }

    public void setUnitSelectHandler(java.util.function.Consumer<Unit> h) {
        this.unitSelectHandler = h;
    }

    public void setCitySelectHandler(
        java.util.function.Consumer<com.developingstorm.games.sad.City> h
    ) {
        this.citySelectHandler = h;
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
            healthPipsRow.getChildren().clear();
            statsLabel.setText("");
            locationLabel.setText("");
            orderLabel.setText("");
            alsoHereList.getChildren().clear();
            alsoHereSection.setVisible(false);
            alsoHereSection.setManaged(false);
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

            // Health pips - one block per hit point
            int hits = unit.life().hits;
            int maxHits = unit.getType().getHits();
            healthPipsRow.getChildren().clear();
            Color filledColor = pipColor(hits, maxHits);
            Color emptyColor = Color.rgb(60, 60, 60);
            Color borderColor = Color.rgb(30, 30, 30);
            double pipW = 12;
            double pipH = 14;
            for (int i = 0; i < maxHits; i++) {
                Rectangle pip = new Rectangle(pipW, pipH);
                pip.setFill(i < hits ? filledColor : emptyColor);
                pip.setStroke(borderColor);
                pip.setStrokeWidth(1);
                pip.setArcWidth(2);
                pip.setArcHeight(2);
                healthPipsRow.getChildren().add(pip);
            }

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

            populateAlsoHere(unit);
        }
    }

    private void populateAlsoHere(Unit unit) {
        alsoHereList.getChildren().clear();

        com.developingstorm.games.sad.City city = query.getCityAtLocation(
            unit.getLocation()
        );
        java.util.List<Unit> units = query.getUnitsAtLocation(
            unit.getLocation()
        );

        boolean any = false;

        if (city != null) {
            alsoHereList.getChildren().add(buildCityRow(city));
            any = true;
        }

        if (units != null) {
            for (Unit u : units) {
                if (u == unit) continue;
                alsoHereList.getChildren().add(buildUnitRow(u));
                any = true;
            }
        }

        alsoHereSection.setVisible(any);
        alsoHereSection.setManaged(any);
    }

    private Region buildUnitRow(Unit u) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 4, 2, 4));
        row.setStyle("-fx-cursor: hand;");

        Image img = TerrainImages.getInstance().getUnitImageWithStatus(u);
        ImageView iv = new ImageView();
        iv.setFitWidth(20);
        iv.setFitHeight(20);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        if (img != null) iv.setImage(img);

        StackPane iconBox = new StackPane(iv);
        iconBox.setPrefSize(24, 24);
        iconBox.setMinSize(24, 24);
        iconBox.setMaxSize(24, 24);
        iconBox.setBackground(
            new Background(
                new BackgroundFill(
                    getPlayerColor(u.getOwner()),
                    new CornerRadii(2),
                    Insets.EMPTY
                )
            )
        );
        iconBox.setAlignment(Pos.CENTER);

        String label = (u.name != null ? u.name : u.getType().toString());
        Label lbl = new Label(label);
        lbl.setTextFill(Color.WHITE);
        lbl.setFont(Font.font("System", 11));

        row.getChildren().addAll(iconBox, lbl);
        row.setOnMouseClicked(e -> {
            if (unitSelectHandler != null) unitSelectHandler.accept(u);
        });
        addHoverHighlight(row);
        return row;
    }

    private Region buildCityRow(com.developingstorm.games.sad.City city) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 4, 2, 4));
        row.setStyle("-fx-cursor: hand;");

        Rectangle marker = new Rectangle(16, 16);
        marker.setArcWidth(3);
        marker.setArcHeight(3);
        if (city.getOwner() != null) {
            marker.setFill(getPlayerColor(city.getOwner()));
        } else {
            marker.setFill(Color.rgb(160, 160, 160));
        }
        marker.setStroke(Color.BLACK);

        String name = city.getName() != null ? city.getName() : "City";
        Label lbl = new Label("City: " + name);
        lbl.setTextFill(Color.WHITE);
        lbl.setFont(Font.font("System", 11));

        row.getChildren().addAll(marker, lbl);
        row.setOnMouseClicked(e -> {
            if (citySelectHandler != null) citySelectHandler.accept(city);
        });
        addHoverHighlight(row);
        return row;
    }

    private void addHoverHighlight(Region row) {
        Background hover = new Background(
            new BackgroundFill(
                Color.rgb(255, 255, 255, 0.12),
                new CornerRadii(3),
                Insets.EMPTY
            )
        );
        row.setOnMouseEntered(e -> row.setBackground(hover));
        row.setOnMouseExited(e -> row.setBackground(Background.EMPTY));
    }

    private Color pipColor(int hits, int maxHits) {
        double ratio = maxHits > 0 ? (double) hits / maxHits : 0;
        if (ratio > 0.6) {
            return Color.rgb(90, 138, 90); // muted green
        } else if (ratio > 0.3) {
            return Color.rgb(176, 128, 64); // muted amber
        } else {
            return Color.rgb(160, 74, 74); // muted red
        }
    }
}
