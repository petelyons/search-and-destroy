package com.developingstorm.games.sad.ui;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JRadioButton;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.Font;

public class NewGameDialog extends JDialog {

  public static class NewGameValues {
    public int exitButton = -1; // 0 = OK, 1=cancel -1=unknown
    public String player1Name;
    public String player2Name;
    public int player1Type;
    public int player2Type;
    public int gameSize; //0=small, 1=medium, 2=large
    
  };

  
  private final JPanel contentPanel = new JPanel();
  private JLabel lblPlayer1;
  private JComboBox comboBox1;
  private JLabel lblPlayer2;
  private JComboBox comboBox2;
  private JRadioButton rdbtnSmallMap;
  private JRadioButton rdbtnMediumMap;
  private JRadioButton rdbtnLargeMap;
  private final NewGameValues _values = new NewGameValues();
  /**
   * Launch the application.
   */
//  public static void main(String[] args) {
//    try {
//      NewGameDialog dialog = new NewGameDialog();
//      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
//      dialog.setVisible(true);
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }

  /**
   * Create the dialog.
   */
  public NewGameDialog(Window owner, String title, Dialog.ModalityType modalityType) {
    
    super(owner, title, modalityType);
    
    
    setBounds(100, 100, 465, 403);
    getContentPane().setLayout(new BorderLayout());
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    {
      comboBox1 = new JComboBox();
      comboBox1.addItem("Human");
      comboBox1.addItem("Robot");
      comboBox1.setSelectedIndex(0);
    }
    {
      comboBox2 = new JComboBox();
      comboBox2.addItem("Human");
      comboBox2.addItem("Robot");
      comboBox2.setSelectedIndex(1);
    }
    {
      lblPlayer1 = new JLabel("Player 1");
    }
    {
      lblPlayer2 = new JLabel("Player 2");
    }

    {
      rdbtnSmallMap = new JRadioButton("Small Map");
    }
    {
      rdbtnMediumMap = new JRadioButton("Medium Map");
      rdbtnMediumMap.setSelected(true);
    }
    {
      rdbtnLargeMap = new JRadioButton("Large Map");
    }
    
    JLabel lblMapImg = new JLabel("");
    
    JLabel lblCreateANew = new JLabel("Create a new Game.  Pick the players and the map size.");
    lblCreateANew.setFont(new Font("Tahoma", Font.BOLD, 13));
    GroupLayout gl_contentPanel = new GroupLayout(contentPanel);
    gl_contentPanel.setHorizontalGroup(
      gl_contentPanel.createParallelGroup(Alignment.LEADING)
        .addGroup(gl_contentPanel.createSequentialGroup()
          .addContainerGap()
          .addGroup(gl_contentPanel.createParallelGroup(Alignment.TRAILING)
            .addGroup(gl_contentPanel.createSequentialGroup()
              .addComponent(lblCreateANew)
              .addGap(164))
            .addGroup(gl_contentPanel.createSequentialGroup()
              .addGroup(gl_contentPanel.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_contentPanel.createSequentialGroup()
                  .addGroup(gl_contentPanel.createParallelGroup(Alignment.TRAILING)
                    .addGroup(gl_contentPanel.createSequentialGroup()
                      .addComponent(lblPlayer1, GroupLayout.PREFERRED_SIZE, 112, GroupLayout.PREFERRED_SIZE)
                      .addGap(5))
                    .addGroup(gl_contentPanel.createSequentialGroup()
                      .addGroup(gl_contentPanel.createParallelGroup(Alignment.LEADING)
                        .addComponent(rdbtnSmallMap, GroupLayout.PREFERRED_SIZE, 85, GroupLayout.PREFERRED_SIZE)
                        .addComponent(rdbtnMediumMap, GroupLayout.PREFERRED_SIZE, 85, GroupLayout.PREFERRED_SIZE)
                        .addComponent(rdbtnLargeMap, GroupLayout.PREFERRED_SIZE, 85, GroupLayout.PREFERRED_SIZE))
                      .addGap(31)))
                  .addGroup(gl_contentPanel.createParallelGroup(Alignment.LEADING)
                    .addGroup(gl_contentPanel.createParallelGroup(Alignment.TRAILING, false)
                      .addComponent(comboBox1, Alignment.LEADING, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                      .addComponent(comboBox2, Alignment.LEADING, 0, 73, Short.MAX_VALUE))
                    .addComponent(lblMapImg, GroupLayout.PREFERRED_SIZE, 265, GroupLayout.PREFERRED_SIZE)))
                .addComponent(lblPlayer2, GroupLayout.PREFERRED_SIZE, 112, GroupLayout.PREFERRED_SIZE))
              .addContainerGap(239, Short.MAX_VALUE))))
    );
    gl_contentPanel.setVerticalGroup(
      gl_contentPanel.createParallelGroup(Alignment.LEADING)
        .addGroup(gl_contentPanel.createSequentialGroup()
          .addGap(8)
          .addComponent(lblCreateANew)
          .addGroup(gl_contentPanel.createParallelGroup(Alignment.LEADING)
            .addGroup(gl_contentPanel.createSequentialGroup()
              .addGap(21)
              .addComponent(lblPlayer1))
            .addGroup(gl_contentPanel.createSequentialGroup()
              .addGap(18)
              .addComponent(comboBox1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
          .addPreferredGap(ComponentPlacement.RELATED)
          .addGroup(gl_contentPanel.createParallelGroup(Alignment.LEADING)
            .addComponent(lblPlayer2)
            .addComponent(comboBox2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
          .addGap(18)
          .addGroup(gl_contentPanel.createParallelGroup(Alignment.BASELINE)
            .addGroup(gl_contentPanel.createSequentialGroup()
              .addComponent(rdbtnSmallMap)
              .addGap(1)
              .addComponent(rdbtnMediumMap)
              .addGap(1)
              .addComponent(rdbtnLargeMap))
            .addComponent(lblMapImg, GroupLayout.PREFERRED_SIZE, 190, GroupLayout.PREFERRED_SIZE))
          .addContainerGap(81, Short.MAX_VALUE))
    );
    contentPanel.setLayout(gl_contentPanel);
    {
      JPanel buttonPane = new JPanel();
      buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
      getContentPane().add(buttonPane, BorderLayout.SOUTH);
      {
        JButton okButton = new JButton("OK");
        okButton.setActionCommand("OK");
        buttonPane.add(okButton);
        getRootPane().setDefaultButton(okButton);
        okButton.addActionListener(new ActionListener() {

          @Override
          public void actionPerformed(ActionEvent e) {
            
            _values.exitButton = 0;
            _values.player1Type = playerType((String) comboBox1.getSelectedItem());
            _values.player2Type = playerType((String) comboBox2.getSelectedItem());
            
            if (rdbtnSmallMap.isSelected()) {
              _values.gameSize = 0;
            } else if (rdbtnMediumMap.isSelected()) {
              _values.gameSize = 1;
            } else {
              _values.gameSize = 2;
            }
            
            NewGameDialog.this.setVisible(false);
            
          }

          private int playerType(String selectedItem) {
            if (selectedItem.equals("Robot")) {
              return 1;
            }
            return 0;
          }});
      }
      {
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand("Cancel");
        buttonPane.add(cancelButton);
        cancelButton.addActionListener(new ActionListener() {

          @Override
          public void actionPerformed(ActionEvent e) {
            
            _values.exitButton = 1;
            
            NewGameDialog.this.setVisible(false);
            
          }});
      }
    }
    
    
  }

  public NewGameValues getValues() {
    return _values;
  }
  
}
