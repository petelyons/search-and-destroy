package com.developingstorm.games.sad.fx;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Unit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * "Reports": newest-first cards for battles and patrol sightings, drawn in the "facing" layout
 * (the two units side by side with the action between them).
 */
public class ReportsPanel extends VBox {

    static final int MAX_REPORTS = 50;

    private enum Filter { ALL, BATTLES, SIGHTINGS }

    private static final String GREEN = "#55cc55";
    private static final String RED = "#dd5555";
    private static final String AMBER = "#e9b13c";
    private static final double NAME_WIDTH = 92;

    private final List<ReportEntry> entries = new ArrayList<>();
    private final VBox list = new VBox(6);
    private final ScrollPane scroll;
    private final List<Label> chips = new ArrayList<>();
    private final Consumer<Location> onSelect;
    private final Consumer<Unit> onResume;
    private Filter filter = Filter.ALL;

    /**
     * @param onSelect called with the event's hex when a card is clicked
     * @param onResume called with the patrolling unit when its card's Resume button is pressed
     */
    public ReportsPanel(Consumer<Location> onSelect, Consumer<Unit> onResume) {
        this.onSelect = onSelect;
        this.onResume = onResume;
        setSpacing(5);
        setPadding(new Insets(10));
        setStyle("-fx-background-color: #2b2b2b;");

        Label title = new Label("Reports");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));
        title.setTextFill(Color.WHITE);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(4, title, spacer,
            chip("All", Filter.ALL), chip("Battles", Filter.BATTLES), chip("Sightings", Filter.SIGHTINGS));
        header.setAlignment(Pos.CENTER_LEFT);

        Region separator = new Region();
        separator.setPrefHeight(1);
        separator.setStyle("-fx-background-color: #555;");

        list.setPadding(new Insets(5));
        scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #2b2b2b; -fx-background-color: #2b2b2b;");
        scroll.setPrefHeight(200);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(header, separator, scroll);
        updateChips();
        rebuild();
    }

    /** Add a card at the top, dropping the oldest beyond {@link #MAX_REPORTS}. */
    public void add(ReportEntry entry) {
        entries.add(0, entry);
        while (entries.size() > MAX_REPORTS) {
            entries.remove(entries.size() - 1);
        }
        rebuild();
        scroll.setVvalue(0);
    }

    /** Re-draw the cards, e.g. so Resume buttons reflect units that have since moved on. */
    public void refresh() {
        rebuild();
    }

    private Label chip(String text, Filter f) {
        Label chip = new Label(text);
        chip.setFont(Font.font("System", 10));
        chip.setPadding(new Insets(2, 7, 2, 7));
        chip.setCursor(Cursor.HAND);
        chip.setUserData(f);
        chip.setOnMouseClicked(e -> {
            filter = f;
            updateChips();
            rebuild();
        });
        chips.add(chip);
        return chip;
    }

    private void updateChips() {
        for (Label c : chips) {
            boolean on = c.getUserData() == filter;
            c.setStyle("-fx-background-radius: 10; -fx-background-color: " + (on ? "#5a5a5a" : "#3a3a3a") + ";");
            c.setTextFill(on ? Color.WHITE : Color.web("#aaaaaa"));
        }
    }

    private boolean shown(ReportEntry e) {
        switch (filter) {
            case BATTLES:
                return !e.kind().isPatrol();
            case SIGHTINGS:
                return e.kind().isPatrol();
            default:
                return true;
        }
    }

    private void rebuild() {
        list.getChildren().clear();
        for (ReportEntry e : entries) {
            if (shown(e)) {
                list.getChildren().add(card(e));
            }
        }
        if (list.getChildren().isEmpty()) {
            Label empty = new Label("No reports yet");
            empty.setFont(Font.font("System", FontWeight.LIGHT, 12));
            empty.setTextFill(Color.GRAY);
            empty.setPadding(new Insets(20));
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            list.getChildren().add(empty);
        }
    }

    private VBox card(ReportEntry e) {
        String edge = e.kind() == ReportEntry.Kind.BATTLE_WON ? GREEN
            : e.kind() == ReportEntry.Kind.BATTLE_LOST ? RED : AMBER;
        String bg = e.kind().isPatrol() ? "#3a3424" : "#333333";

        Label header = new Label(ReportText.header(e));
        header.setFont(Font.font("System", 10));
        header.setTextFill(Color.web("#999999"));
        header.setTextOverrun(OverrunStyle.ELLIPSIS);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(4, header, spacer, badge(e, edge));
        top.setAlignment(Pos.CENTER_LEFT);

        Label verb = new Label(ReportText.verb(e));
        verb.setFont(Font.font("System", 10));
        verb.setTextFill(Color.web("#aaaaaa"));
        verb.setWrapText(true);
        verb.setAlignment(Pos.CENTER);
        HBox.setHgrow(verb, Priority.ALWAYS);
        verb.setMaxWidth(Double.MAX_VALUE);

        HBox body = new HBox(6, column(e.left(), e), verb, column(e.right(), e));
        body.setAlignment(Pos.CENTER);

        VBox card = new VBox(5, top, body);
        card.setPadding(new Insets(7, 8, 7, 8));
        card.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 5;"
            + " -fx-border-color: transparent transparent transparent " + edge + ";"
            + " -fx-border-width: 0 0 0 4; -fx-border-radius: 5;");

        if (e.kind().isPatrol() && e.patroller() != null) {
            Button resume = new Button("Resume patrol");
            resume.setFont(Font.font("System", 10));
            Unit p = e.patroller();
            resume.setDisable(p.isDead() || p.getSuspendedPatrol() == null);
            resume.setOnAction(a -> {
                if (!p.isDead() && p.getSuspendedPatrol() != null) {
                    onResume.accept(p);
                }
                resume.setDisable(true);
            });
            card.getChildren().add(resume);
        }

        if (onSelect != null && e.location() != null) {
            card.setCursor(Cursor.HAND);
            card.setOnMouseClicked(m -> onSelect.accept(e.location()));
        }
        return card;
    }

    private static Label badge(ReportEntry e, String colour) {
        Label b = new Label(ReportText.badge(e));
        b.setFont(Font.font("System", FontWeight.BOLD, 9));
        b.setTextFill(Color.web(colour));
        b.setPadding(new Insets(1, 5, 1, 5));
        b.setStyle("-fx-background-color: derive(" + colour + ", -60%); -fx-background-radius: 3;");
        return b;
    }

    /** One unit: icon on its owner's colour, name (cut short with a tooltip), then type or health. */
    private static VBox column(ReportEntry.Side side, ReportEntry e) {
        VBox col = new VBox(2);
        col.setAlignment(Pos.TOP_CENTER);
        col.setMinWidth(NAME_WIDTH);
        col.setPrefWidth(NAME_WIDTH);
        col.setMaxWidth(NAME_WIDTH);
        if (side == null) {
            return col;
        }

        ImageView icon = new ImageView(TerrainImages.getInstance().getUnitImage(side.type()));
        icon.setFitWidth(22);
        icon.setFitHeight(22);
        icon.setPreserveRatio(true);
        StackPane iconPane = new StackPane(icon);
        iconPane.setMinSize(26, 26);
        iconPane.setMaxSize(26, 26);
        iconPane.setStyle("-fx-background-color: " + ownerColour(side.owner()) + "; -fx-background-radius: 5;");

        String full = ReportText.displayName(side.name(), side.type());
        Label name = new Label(full);
        name.setFont(Font.font("System", FontWeight.BOLD, 11));
        name.setTextFill(Color.WHITE);
        name.setTextOverrun(OverrunStyle.ELLIPSIS);
        name.setMaxWidth(NAME_WIDTH);
        name.setTooltip(new Tooltip(full));

        col.getChildren().addAll(iconPane, name);
        if (side.hitFraction() != null) {
            col.getChildren().add(healthBar(side.hitFraction()));
        } else {
            Label type = new Label(side.type().getName());
            type.setFont(Font.font("System", 10));
            type.setTextFill(Color.web("#999999"));
            col.getChildren().add(type);
        }
        return col;
    }

    private static StackPane healthBar(double fraction) {
        double width = 48;
        Region track = new Region();
        track.setMinSize(width, 4);
        track.setMaxSize(width, 4);
        track.setStyle("-fx-background-color: #444; -fx-background-radius: 2;");
        Region fill = new Region();
        double w = width * Math.max(0, Math.min(1, fraction));
        fill.setMinSize(w, 4);
        fill.setMaxSize(w, 4);
        String colour = fraction > 0.5 ? "#66cc66" : fraction > 0 ? "#dd8844" : "transparent";
        fill.setStyle("-fx-background-color: " + colour + "; -fx-background-radius: 2;");
        StackPane bar = new StackPane(track, fill);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setMaxWidth(width);
        return bar;
    }

    static String ownerColour(Player owner) {
        return PlayerColors.css(owner);
    }
}
