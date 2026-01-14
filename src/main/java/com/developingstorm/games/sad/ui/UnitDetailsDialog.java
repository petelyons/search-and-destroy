package com.developingstorm.games.sad.ui;

import com.developingstorm.games.sad.Unit;
import java.awt.*;
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
    private SaDBoardContext context;

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
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(3, 3, 3, 3);

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

        // Add icon panel at the top, centered, spanning both columns
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 10, 0);
        panel.add(this.iconPanel, gbc);

        // Reset for regular rows
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(3, 3, 3, 3);

        // Add rows (starting from row 1 since icon is at row 0)
        addRow(panel, gbc, 1, "Name:", this.nameLabel);
        addRow(panel, gbc, 2, "Type:", this.typeLabel);
        addRow(panel, gbc, 3, "Owner:", this.ownerLabel);
        addRow(panel, gbc, 4, "Location:", this.locationLabel);
        addRow(panel, gbc, 5, "Health:", this.healthLabel);
        addRow(panel, gbc, 6, "Movement:", this.movementLabel);
        addRow(panel, gbc, 7, "Order:", this.orderLabel);
        addRow(panel, gbc, 8, "Cargo:", this.cargoLabel);

        add(panel, BorderLayout.CENTER);
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
        this.movementLabel.setText(
            unit.life().movesLeft() + "/" + unit.getType().getDist()
        );

        String orderText = "None";
        if (unit.getOrder() != null) {
            orderText = unit.getOrder().getType().toString();
        }
        this.orderLabel.setText(orderText);

        // Show cargo information
        String cargoText = getCargoText(unit);
        this.cargoLabel.setText(cargoText);

        // Panel is always visible when docked
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
