package com.developingstorm.games.sad.ui;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.CombatResult;
import com.developingstorm.games.sad.Player;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * Panel that displays a scrollable history of recent combat encounters.
 * Each battle is shown as a single row with unit tiles for attacker and defender.
 */
public class BattleHistoryPanel extends JPanel {

    private final SaDBoardContext context;
    private final List<CombatResult> battleHistory;
    private final JPanel battleListPanel;
    private final BattleSelectionListener selectionListener;

    public interface BattleSelectionListener {
        void battleSelected(Location location);
    }

    public BattleHistoryPanel(
        SaDBoardContext context,
        BattleSelectionListener selectionListener
    ) {
        super(new BorderLayout());
        this.context = context;
        this.battleHistory = new ArrayList<>();
        this.selectionListener = selectionListener;

        setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 0, 0, Color.GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            )
        );

        // Title
        JLabel titleLabel = new JLabel("Battle History", SwingConstants.CENTER);
        Font titleFont = titleLabel.getFont().deriveFont(Font.BOLD, 14f);
        titleLabel.setFont(titleFont);
        add(titleLabel, BorderLayout.NORTH);

        // Battle list panel with vertical layout
        battleListPanel = new JPanel();
        battleListPanel.setLayout(
            new BoxLayout(battleListPanel, BoxLayout.Y_AXIS)
        );
        battleListPanel.setBackground(Color.WHITE);

        // Add scroll pane
        JScrollPane scrollPane = new JScrollPane(battleListPanel);
        scrollPane.setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );
        scrollPane.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Set reasonable size
        setPreferredSize(new Dimension(280, 200));
        setMinimumSize(new Dimension(250, 150));

        // Initially show "No battles yet"
        showEmptyState();
    }

    private void showEmptyState() {
        battleListPanel.removeAll();
        JLabel emptyLabel = new JLabel("No battles yet", SwingConstants.CENTER);
        emptyLabel.setFont(emptyLabel.getFont().deriveFont(Font.ITALIC));
        emptyLabel.setForeground(Color.GRAY);
        emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        battleListPanel.add(Box.createVerticalGlue());
        battleListPanel.add(emptyLabel);
        battleListPanel.add(Box.createVerticalGlue());
        revalidate();
        repaint();
    }

    public void addBattle(CombatResult result) {
        if (result == null) {
            return;
        }

        // If this is the first battle, clear the empty state
        if (battleHistory.isEmpty()) {
            battleListPanel.removeAll();
        }

        // Add to history
        battleHistory.add(result);

        // Create battle row panel
        JPanel battleRow = createBattleRow(result);

        // Add to the top of the list (most recent first)
        battleListPanel.add(battleRow, 0);

        // Limit history to 50 battles
        if (battleHistory.size() > 50) {
            battleHistory.remove(0);
            battleListPanel.remove(battleListPanel.getComponentCount() - 1);
        }

        revalidate();
        repaint();

        // Auto-scroll to top to show the new battle
        SwingUtilities.invokeLater(() -> {
            battleListPanel.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
        });
    }

    private JPanel createBattleRow(CombatResult result) {
        JPanel row = new JPanel();
        row.setLayout(new BorderLayout(5, 0));
        row.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(8, 5, 8, 5)
            )
        );
        row.setBackground(Color.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70)); // Increased height for text

        // Left side: Attacker unit tile with name
        JPanel attackerSection = new JPanel();
        attackerSection.setLayout(
            new BoxLayout(attackerSection, BoxLayout.Y_AXIS)
        );
        attackerSection.setBackground(Color.WHITE);

        JPanel attackerTile = createUnitTile(
            result.getAttackerIconIndex(),
            result.getAttackerOwner(),
            result.attackerWon()
        );
        attackerTile.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel attackerName = createNameLabel(result.getAttackerName());
        attackerName.setAlignmentX(Component.CENTER_ALIGNMENT);

        attackerSection.add(attackerTile);
        attackerSection.add(Box.createVerticalStrut(2));
        attackerSection.add(attackerName);

        // Center: VS indicator
        JLabel vsLabel = new JLabel("VS", SwingConstants.CENTER);
        vsLabel.setFont(vsLabel.getFont().deriveFont(Font.BOLD, 10f));
        vsLabel.setForeground(Color.DARK_GRAY);
        JPanel vsPanel = new JPanel(new BorderLayout());
        vsPanel.setBackground(Color.WHITE);
        vsPanel.add(vsLabel, BorderLayout.CENTER);
        vsPanel.setPreferredSize(new Dimension(30, 40));

        // Right side: Defender unit tile with name
        JPanel defenderSection = new JPanel();
        defenderSection.setLayout(
            new BoxLayout(defenderSection, BoxLayout.Y_AXIS)
        );
        defenderSection.setBackground(Color.WHITE);

        JPanel defenderTile = createUnitTile(
            result.getDefenderIconIndex(),
            result.getDefenderOwner(),
            !result.attackerWon()
        );
        defenderTile.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel defenderName = createNameLabel(result.getDefenderName());
        defenderName.setAlignmentX(Component.CENTER_ALIGNMENT);

        defenderSection.add(defenderTile);
        defenderSection.add(Box.createVerticalStrut(2));
        defenderSection.add(defenderName);

        // Assemble row
        row.add(attackerSection, BorderLayout.WEST);
        row.add(vsPanel, BorderLayout.CENTER);
        row.add(defenderSection, BorderLayout.EAST);

        // Make clickable
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.addMouseListener(
            new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (selectionListener != null) {
                        selectionListener.battleSelected(
                            result.getBattleLocation()
                        );
                    }
                }

                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    row.setBackground(new Color(240, 240, 255));
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    row.setBackground(Color.WHITE);
                }
            }
        );

        return row;
    }

    private JLabel createNameLabel(String name) {
        // Create label with HTML to enable text wrapping
        JLabel label = new JLabel(
            "<html><div style='text-align: center; width: 80px;'>" +
                name +
                "</div></html>"
        );
        label.setFont(label.getFont().deriveFont(9f));
        label.setForeground(Color.DARK_GRAY);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    private JPanel createUnitTile(
        int iconIndex,
        Player owner,
        boolean isVictor
    ) {
        JPanel tile = new JPanel(new BorderLayout());
        tile.setOpaque(true);

        // Get the unit icon image
        Image iconImage = GameIcons.get().getImages()[iconIndex];
        JLabel iconLabel = new JLabel(new ImageIcon(iconImage));
        iconLabel.setOpaque(false);

        // Set background color to player's color
        Color playerColor = context.getPlayerColor(owner);
        tile.setBackground(playerColor);

        // Add border - thicker/golden for victor
        if (isVictor) {
            tile.setBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(255, 215, 0), 3), // Gold border
                    BorderFactory.createLineBorder(Color.BLACK, 1)
                )
            );
        } else {
            tile.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        }

        tile.add(iconLabel, BorderLayout.CENTER);

        // Set preferred size to match icon dimensions plus border
        int borderSize = isVictor ? 4 : 2;
        tile.setPreferredSize(
            new Dimension(
                iconImage.getWidth(null) + borderSize * 2,
                iconImage.getHeight(null) + borderSize * 2
            )
        );

        return tile;
    }

    public void clearHistory() {
        battleHistory.clear();
        showEmptyState();
    }

    public int getBattleCount() {
        return battleHistory.size();
    }
}
