package com.developingstorm.games.sad.ui;

import com.developingstorm.games.sad.CombatResult;
import java.awt.*;
import javax.swing.*;

/**
 * Panel that displays the results of the most recent combat encounter.
 */
public class CombatResultPanel extends JPanel {

    private JPanel attackerPanel;
    private JPanel defenderPanel;
    private JLabel titleLabel;
    private SaDBoardContext context;

    public CombatResultPanel(SaDBoardContext context) {
        super();
        this.context = context;
        setLayout(new BorderLayout());
        setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 0, 0, Color.GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            )
        );

        initComponents();

        // Set reasonable size
        setPreferredSize(new Dimension(280, 200));
        setMinimumSize(new Dimension(250, 150));
    }

    private void initComponents() {
        // Title
        titleLabel = new JLabel("Last Combat", SwingConstants.CENTER);
        Font titleFont = titleLabel.getFont().deriveFont(Font.BOLD, 14f);
        titleLabel.setFont(titleFont);
        add(titleLabel, BorderLayout.NORTH);

        // Combat details panel
        JPanel combatPanel = new JPanel(new GridLayout(2, 1, 5, 5));

        attackerPanel = createUnitPanel("Attacker", Color.LIGHT_GRAY);
        defenderPanel = createUnitPanel("Defender", Color.LIGHT_GRAY);

        combatPanel.add(attackerPanel);
        combatPanel.add(defenderPanel);

        add(combatPanel, BorderLayout.CENTER);

        // Initially show "No combat yet"
        clearCombat();
    }

    private JPanel createUnitPanel(String label, Color bgColor) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(bgColor);
        panel.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            )
        );

        JLabel titleLabel = new JLabel(label);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(titleLabel);

        return panel;
    }

    public void updateCombat(CombatResult result) {
        if (result == null) {
            clearCombat();
            return;
        }

        // Update attacker panel
        attackerPanel.removeAll();
        attackerPanel.setBackground(
            result.attackerWon()
                ? new Color(200, 255, 200)
                : new Color(255, 200, 200)
        );

        JLabel atkTitle = new JLabel(
            "Attacker" + (result.attackerWon() ? " (VICTOR)" : "")
        );
        atkTitle.setFont(atkTitle.getFont().deriveFont(Font.BOLD));
        atkTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        attackerPanel.add(atkTitle);

        // Add colored icon for attacker
        JPanel atkIconPanel = createIconPanel(
            result.getAttackerIconIndex(),
            result.getAttackerOwner()
        );
        atkIconPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        attackerPanel.add(atkIconPanel);

        attackerPanel.add(
            createInfoLabel(
                result.getAttackerType() + " - " + result.getAttackerOwner()
            )
        );
        attackerPanel.add(
            createInfoLabel("Damage: " + result.getAttackerDamage() + " hits")
        );
        attackerPanel.add(
            createInfoLabel(
                "Final: " +
                    result.getAttackerFinalHits() +
                    "/" +
                    result.getAttackerMaxHits()
            )
        );

        // Update defender panel
        defenderPanel.removeAll();
        defenderPanel.setBackground(
            !result.attackerWon()
                ? new Color(200, 255, 200)
                : new Color(255, 200, 200)
        );

        JLabel defTitle = new JLabel(
            "Defender" + (!result.attackerWon() ? " (VICTOR)" : "")
        );
        defTitle.setFont(defTitle.getFont().deriveFont(Font.BOLD));
        defTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        defenderPanel.add(defTitle);

        // Add colored icon for defender
        JPanel defIconPanel = createIconPanel(
            result.getDefenderIconIndex(),
            result.getDefenderOwner()
        );
        defIconPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        defenderPanel.add(defIconPanel);

        defenderPanel.add(
            createInfoLabel(
                result.getDefenderType() + " - " + result.getDefenderOwner()
            )
        );
        defenderPanel.add(
            createInfoLabel("Damage: " + result.getDefenderDamage() + " hits")
        );
        defenderPanel.add(
            createInfoLabel(
                "Final: " +
                    result.getDefenderFinalHits() +
                    "/" +
                    result.getDefenderMaxHits()
            )
        );

        revalidate();
        repaint();
    }

    private JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(11f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JPanel createIconPanel(
        int iconIndex,
        com.developingstorm.games.sad.Player owner
    ) {
        JPanel iconPanel = new JPanel(new BorderLayout());
        iconPanel.setOpaque(true);

        // Get the unit icon image
        Image iconImage = GameIcons.get().getImages()[iconIndex];
        JLabel iconLabel = new JLabel(new ImageIcon(iconImage));
        iconLabel.setOpaque(false);

        // Set background color to player's color
        Color playerColor = this.context.getPlayerColor(owner);
        iconPanel.setBackground(playerColor);
        iconPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

        iconPanel.add(iconLabel, BorderLayout.CENTER);

        // Set preferred size to match icon dimensions
        iconPanel.setPreferredSize(
            new Dimension(
                iconImage.getWidth(null) + 4,
                iconImage.getHeight(null) + 4
            )
        );
        iconPanel.setMaximumSize(
            new Dimension(
                iconImage.getWidth(null) + 4,
                iconImage.getHeight(null) + 4
            )
        );

        return iconPanel;
    }

    public void clearCombat() {
        attackerPanel.removeAll();
        attackerPanel.setBackground(Color.LIGHT_GRAY);
        JLabel noDataLabel = new JLabel("No combat yet");
        noDataLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        attackerPanel.add(Box.createVerticalGlue());
        attackerPanel.add(noDataLabel);
        attackerPanel.add(Box.createVerticalGlue());

        defenderPanel.removeAll();
        defenderPanel.setBackground(Color.LIGHT_GRAY);
        defenderPanel.add(Box.createVerticalGlue());

        revalidate();
        repaint();
    }
}
