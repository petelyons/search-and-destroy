package com.developingstorm.games.sad.ui;

import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Unit;
import java.awt.*;
import java.util.List;
import javax.swing.*;

/**
 * Panel that displays details about the currently selected unit.
 * Can be docked to the side of the main frame.
 */
public class UnitDetailsDialog extends JPanel {

    private JPanel iconPanel;
    private JLabel iconLabel;
    private JLabel nameLabel;
    private JLabel typeLabel;
    private JLabel ownerLabel;
    private JLabel locationLabel;
    private JLabel healthLabel;
    private JLabel movementLabel;
    private JLabel orderLabel;
    private JLabel cargoLabel;
    private JLabel productionLabel;
    private JLabel turnsLabel;
    private JLabel unitsHereLabel;
    private SaDBoardContext context;
    private JPanel detailPanel;

    public UnitDetailsDialog(SaDBoardContext context) {
        super();
        this.context = context;

        // Set layout and border for the panel
        setLayout(new BorderLayout());
        setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 2, 0, 0, Color.GRAY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            )
        );

        initComponents();

        // Set a reasonable size for the docked panel
        setPreferredSize(new Dimension(280, 0)); // Height will match parent
        setMinimumSize(new Dimension(250, 300));
    }

    private void initComponents() {
        this.detailPanel = new JPanel(new GridBagLayout());
        this.detailPanel.setBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );

        // Initialize labels
        this.iconLabel = new JLabel();
        this.iconLabel.setOpaque(false);

        // Create a panel to hold the icon with colored background
        this.iconPanel = new JPanel(new BorderLayout());
        this.iconPanel.setOpaque(true);
        this.iconPanel.add(this.iconLabel, BorderLayout.CENTER);
        this.iconPanel.setBorder(
            BorderFactory.createLineBorder(Color.BLACK, 2)
        );

        this.nameLabel = new JLabel();
        this.typeLabel = new JLabel();
        this.ownerLabel = new JLabel();
        this.locationLabel = new JLabel();
        this.healthLabel = new JLabel();
        this.movementLabel = new JLabel();
        this.orderLabel = new JLabel();
        this.cargoLabel = new JLabel();
        this.productionLabel = new JLabel();
        this.turnsLabel = new JLabel();
        this.unitsHereLabel = new JLabel();

        add(this.detailPanel, BorderLayout.CENTER);
    }

    private void addRow(
        JPanel panel,
        GridBagConstraints gbc,
        int row,
        String labelText,
        JLabel valueLabel
    ) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        JLabel label = new JLabel(labelText);
        Font boldFont = label.getFont().deriveFont(Font.BOLD);
        label.setFont(boldFont);
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(valueLabel, gbc);
    }

    /**
     * Updates the dialog with information about the given unit.
     */
    public void updateUnit(Unit unit) {
        updateUnit(unit, null);
    }

    /**
     * Updates the dialog with information about the given unit and optional city.
     */
    public void updateUnit(Unit unit, City cityAtLocation) {
        System.out.println(
            "UnitDetailsDialog.updateUnit called with unit: " + unit
        );
        if (unit == null) {
            clearUnit();
            return;
        }
        System.out.println(
            "Updating dialog for unit: " + unit.name + " (id=" + unit.id + ")"
        );

        // Check if the unit is in a city - if so, show both unit and city info
        if (cityAtLocation != null) {
            updateUnitAndCity(unit, cityAtLocation);
            return;
        }

        // Clear and rebuild panel for unit display
        this.detailPanel.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(3, 3, 3, 3);

        // Set unit icon with player color background
        int iconIndex = unit.getType().getIcon();
        Image iconImage = GameIcons.get().getImages()[iconIndex];
        this.iconLabel.setIcon(new ImageIcon(iconImage));

        // Set background color to player's color
        Color playerColor = this.context.getPlayerColor(unit.getOwner());
        this.iconPanel.setBackground(playerColor);

        this.nameLabel.setText(
            unit.name != null ? unit.name : "Unit #" + unit.id
        );
        this.typeLabel.setText(unit.getType().toString());
        this.ownerLabel.setText(unit.getOwner().toString());
        this.locationLabel.setText(unit.getLocation().toString());
        this.healthLabel.setText(
            unit.life().hits + "/" + unit.getType().getHits()
        );
        // Show movement and fuel for air units
        String movementText =
            unit.life().movesLeft() + "/" + unit.getType().getDist();
        if (unit.getTravel() == com.developingstorm.games.sad.Travel.AIR) {
            movementText +=
                " [Fuel: " +
                unit.life().remainingFuel() +
                "/" +
                unit.getMaxTravel() +
                "]";
        }
        this.movementLabel.setText(movementText);

        String orderText = "None";
        if (unit.getOrder() != null) {
            orderText = unit.getOrder().getType().toString();
        }
        this.orderLabel.setText(orderText);

        // Show cargo information
        String cargoText = getCargoText(unit);
        this.cargoLabel.setText(cargoText);

        // Add icon panel at the top
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 10, 0);
        this.detailPanel.add(this.iconPanel, gbc);

        // Reset for regular rows
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(3, 3, 3, 3);

        // Add unit information rows
        addRow(this.detailPanel, gbc, 1, "Name:", this.nameLabel);
        addRow(this.detailPanel, gbc, 2, "Type:", this.typeLabel);
        addRow(this.detailPanel, gbc, 3, "Owner:", this.ownerLabel);
        addRow(this.detailPanel, gbc, 4, "Location:", this.locationLabel);
        addRow(this.detailPanel, gbc, 5, "Health:", this.healthLabel);
        addRow(this.detailPanel, gbc, 6, "Movement:", this.movementLabel);
        addRow(this.detailPanel, gbc, 7, "Order:", this.orderLabel);
        addRow(this.detailPanel, gbc, 8, "Cargo:", this.cargoLabel);

        // Panel is always visible when docked
        revalidate();
        repaint();
    }

    /**
     * Updates the dialog with information about both a unit and the city it's in.
     */
    public void updateUnitAndCity(Unit unit, City city) {
        // Clear and rebuild panel for combined display
        this.detailPanel.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(3, 3, 3, 3);

        // Set unit icon with player color background
        int iconIndex = unit.getType().getIcon();
        Image iconImage = GameIcons.get().getImages()[iconIndex];
        this.iconLabel.setIcon(new ImageIcon(iconImage));
        Color playerColor = this.context.getPlayerColor(unit.getOwner());
        this.iconPanel.setBackground(playerColor);

        // Add icon panel at the top
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 10, 0);
        this.detailPanel.add(this.iconPanel, gbc);

        // Reset for regular rows
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(3, 3, 3, 3);

        // Unit section
        this.nameLabel.setText(
            unit.name != null ? unit.name : "Unit #" + unit.id
        );
        this.typeLabel.setText(unit.getType().toString());
        this.ownerLabel.setText(unit.getOwner().toString());
        this.locationLabel.setText(unit.getLocation().toString());
        this.healthLabel.setText(
            unit.life().hits + "/" + unit.getType().getHits()
        );

        String movementText =
            unit.life().movesLeft() + "/" + unit.getType().getDist();
        if (unit.getTravel() == com.developingstorm.games.sad.Travel.AIR) {
            movementText +=
                " [Fuel: " +
                unit.life().remainingFuel() +
                "/" +
                unit.getMaxTravel() +
                "]";
        }
        this.movementLabel.setText(movementText);

        String orderText = "None";
        if (unit.getOrder() != null) {
            orderText = unit.getOrder().getType().toString();
        }
        this.orderLabel.setText(orderText);
        this.cargoLabel.setText(getCargoText(unit));

        int row = 1;
        addRow(this.detailPanel, gbc, row++, "Unit:", this.nameLabel);
        addRow(this.detailPanel, gbc, row++, "Type:", this.typeLabel);
        addRow(this.detailPanel, gbc, row++, "Owner:", this.ownerLabel);
        addRow(this.detailPanel, gbc, row++, "Health:", this.healthLabel);
        addRow(this.detailPanel, gbc, row++, "Movement:", this.movementLabel);
        addRow(this.detailPanel, gbc, row++, "Order:", this.orderLabel);
        addRow(this.detailPanel, gbc, row++, "Cargo:", this.cargoLabel);

        // Add separator
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 10, 0);
        this.detailPanel.add(new javax.swing.JSeparator(), gbc);

        // Reset
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(3, 3, 3, 3);

        // City section
        this.productionLabel.setText(
            city.getProduction() != null
                ? city.getProduction().toString()
                : "None"
        );
        String turnsText = "-";
        if (city.getProduction() != null) {
            turnsText = "Building...";
        }
        this.turnsLabel.setText(turnsText);

        addRow(
            this.detailPanel,
            gbc,
            row++,
            "City:",
            new JLabel(city.getName() != null ? city.getName() : "Unnamed")
        );
        addRow(
            this.detailPanel,
            gbc,
            row++,
            "Production:",
            this.productionLabel
        );
        addRow(this.detailPanel, gbc, row++, "Turns:", this.turnsLabel);

        revalidate();
        repaint();
    }

    /**
     * Updates the dialog with information about the given city.
     */
    public void updateCity(City city, List<Unit> unitsAtLocation) {
        if (city == null) {
            clearUnit();
            return;
        }

        // Clear and rebuild panel for city display
        this.detailPanel.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(3, 3, 3, 3);

        // Set city icon
        int iconIndex = 16; // City icon index
        Image iconImage = GameIcons.get().getImages()[iconIndex];
        this.iconLabel.setIcon(new ImageIcon(iconImage));

        // Set background color to owner's color or gray if unowned
        Color cityColor = Color.LIGHT_GRAY;
        if (city.getOwner() != null) {
            cityColor = this.context.getPlayerColor(city.getOwner());
        }
        this.iconPanel.setBackground(cityColor);

        // Set city information
        this.nameLabel.setText(
            city.getName() != null ? city.getName() : "Unnamed City"
        );
        this.ownerLabel.setText(
            city.getOwner() != null ? city.getOwner().toString() : "Neutral"
        );
        this.locationLabel.setText(city.getLocation().toString());
        this.productionLabel.setText(
            city.getProduction() != null
                ? city.getProduction().toString()
                : "None"
        );

        // Calculate turns until next unit - for now just show if producing
        String turnsText = "-";
        if (city.getProduction() != null) {
            turnsText = "Building...";
        }
        this.turnsLabel.setText(turnsText);

        // Count units at this location
        int unitCount = unitsAtLocation != null ? unitsAtLocation.size() : 0;
        this.unitsHereLabel.setText(String.valueOf(unitCount));

        // Add icon panel at the top
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 10, 0);
        this.detailPanel.add(this.iconPanel, gbc);

        // Reset for regular rows
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(3, 3, 3, 3);

        // Add city information rows
        addRow(this.detailPanel, gbc, 1, "Name:", this.nameLabel);
        addRow(this.detailPanel, gbc, 2, "Owner:", this.ownerLabel);
        addRow(this.detailPanel, gbc, 3, "Location:", this.locationLabel);
        addRow(this.detailPanel, gbc, 4, "Production:", this.productionLabel);
        addRow(this.detailPanel, gbc, 5, "Turns:", this.turnsLabel);
        addRow(this.detailPanel, gbc, 6, "Units Here:", this.unitsHereLabel);

        revalidate();
        repaint();
    }

    /**
     * Generates a text description of the unit's cargo.
     */
    private String getCargoText(Unit unit) {
        if (!unit.canCarry()) {
            return "N/A";
        }

        if (unit.carries == null || unit.carries.isEmpty()) {
            return "0/" + unit.carriableWeight() + " (Empty)";
        }

        // Count units by type
        java.util.Map<String, Integer> typeCounts = new java.util.HashMap<>();
        for (Unit carried : unit.carries) {
            String typeName = carried.getType().toString();
            typeCounts.put(typeName, typeCounts.getOrDefault(typeName, 0) + 1);
        }

        // Build display string
        StringBuilder sb = new StringBuilder();
        sb
            .append(unit.carriedWeight())
            .append("/")
            .append(unit.carriableWeight());
        sb.append(" (");

        boolean first = true;
        for (java.util.Map.Entry<
            String,
            Integer
        > entry : typeCounts.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(entry.getValue()).append(" ").append(entry.getKey());
            first = false;
        }

        sb.append(")");
        return sb.toString();
    }

    /**
     * Clears the unit details.
     */
    public void clearUnit() {
        this.iconLabel.setIcon(null);
        this.iconPanel.setBackground(Color.LIGHT_GRAY);
        this.nameLabel.setText("-");
        this.typeLabel.setText("-");
        this.ownerLabel.setText("-");
        this.locationLabel.setText("-");
        this.healthLabel.setText("-");
        this.movementLabel.setText("-");
        this.orderLabel.setText("-");
        this.cargoLabel.setText("-");
    }
}
