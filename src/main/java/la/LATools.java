package la;

import featurecat.lizzie.*;
import featurecat.lizzie.util.DigitOnlyFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DocumentFilter;
import javax.swing.text.InternationalFormatter;

public class LATools extends JTabbedPane {
  private static final char[] alphabet = {
    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
    'u'
  };

  private JPanel tools1Tab;
  private JPanel tools2Tab;
  private JPanel startTab;

  public boolean allowKoThreats;
  public boolean showStartDialog;
  public String coordinate1;
  public String coordinate2;

  // tools1Tab
  public JRadioButton rdoPlay;
  public JRadioButton rdoSetStone;
  public JRadioButton rdoDeleteStone;
  public JRadioButton rdoSetGroupVertical;
  public JRadioButton rdoSetGroupHorizontal;
  public JRadioButton rdoSetLine;

  // tools2Tab
  public JRadioButton rdoPlayerInsideBlack;
  public JRadioButton rdoPlayerInsideWhite;
  public JRadioButton rdoPlayerToMoveBlack;
  public JRadioButton rdoPlayerToMoveWhite;
  public JRadioButton rdoAllowKoYes;
  public JRadioButton rdoAllowKoNo;
  public JButton bMirror;
  public JButton bAdjustPoints;
  public JButton bAddPointsPlus1;
  public JButton bAddPointsMinus1;
  public JButton bAddPointsPlus5;
  public JButton bAddPointsMinus5;
  public JTextField txtAddPoints;
  public JButton bKoThreatsPlus1;
  public JButton bKoThreatsMinus1;
  public JButton bKoThreatsMax;
  public JButton bKoThreatsMin;
  public JTextField txtMarkAlive;
  public JTextField txtWallDistance;

  // startTab
  public JRadioButton rdoPtmBlack;
  public JRadioButton rdoPtmWhite;
  public JRadioButton rdoPiBlack;
  public JRadioButton rdoPiWhite;
  public JTextField txtCoordinate1;
  public JTextField txtCoordinate2;
  public JFormattedTextField txtBlackKoThreats;
  public JLabel lblBlackKoThreats;
  private JFormattedTextField txtWallDistance2;
  private JCheckBox chkAllowKo;
  private JCheckBox chkShowStartDialog;

  LATools() {
    loadConfigs();

    setTabPlacement(JTabbedPane.TOP);
    tools1Tab = new JPanel();
    tools2Tab = new JPanel();
    startTab = new JPanel();
    setBorder(BorderFactory.createEmptyBorder());

    // tools1Tab
    rdoPlay = new JRadioButton("Play");
    rdoPlay.setBounds(10, 10, 200, 30);
    rdoPlay.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.localAnalysisFrame.tool1 = 1;
          }
        });
    tools1Tab.add(rdoPlay);

    JRadioButton rdoSetStone = new JRadioButton("Set Stone");
    rdoSetStone.setBounds(10, 50, 200, 30);
    rdoSetStone.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.localAnalysisFrame.tool1 = 2;
          }
        });
    tools1Tab.add(rdoSetStone);

    rdoDeleteStone = new JRadioButton("Delete Stone");
    rdoDeleteStone.setBounds(10, 90, 200, 30);
    rdoDeleteStone.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.localAnalysisFrame.tool1 = 3;
          }
        });
    tools1Tab.add(rdoDeleteStone);

    rdoSetGroupVertical = new JRadioButton("Set Group Vertical");
    rdoSetGroupVertical.setBounds(10, 130, 200, 30);
    rdoSetGroupVertical.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.localAnalysisFrame.tool1 = 4;
          }
        });
    tools1Tab.add(rdoSetGroupVertical);

    rdoSetGroupHorizontal = new JRadioButton("Set Group Horizontal");
    rdoSetGroupHorizontal.setBounds(10, 170, 200, 30);
    rdoSetGroupHorizontal.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.localAnalysisFrame.tool1 = 5;
          }
        });
    tools1Tab.add(rdoSetGroupHorizontal);

    //		rdoSetLine = new JRadioButton("Set Line");
    //		rdoSetLine.setBounds(10, 210, 200, 30);
    //		rdoSetLine.addActionListener(new ActionListener() {
    //	        @Override
    //	        public void actionPerformed(ActionEvent e) {
    //	        	Lizzie.frame.localAnalysisFrame.tool1 = 6;
    //	        }
    //	    });
    //		tools1Tab.add(rdoSetLine);

    ButtonGroup tools1group = new ButtonGroup();
    tools1group.add(rdoPlay);
    tools1group.add(rdoSetStone);
    tools1group.add(rdoDeleteStone);
    tools1group.add(rdoSetGroupVertical);
    tools1group.add(rdoSetGroupHorizontal);
    // tools1group.add(rdoSetLine);
    rdoPlay.setSelected(true);

    JLabel chooseColor = new JLabel("Change Color With Caps Lock Key");
    chooseColor.setBounds(15, 240, 200, 30);
    tools1Tab.add(chooseColor);

    // tools2Tab
    JLabel playerInside = new JLabel("Player Inside");
    playerInside.setBounds(10, 10, 120, 30);
    tools2Tab.add(playerInside);
    rdoPlayerInsideBlack = new JRadioButton("Black");
    rdoPlayerInsideBlack.setBounds(130, 10, 100, 30);
    rdoPlayerInsideBlack.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (!Lizzie.frame.localAnalysisFrame.localAnalysis.isPlayerInsideBlack()) {
              Lizzie.frame.localAnalysisFrame.localAnalysis.changePlayerInsideBlack();
            }
            ;
          }
        });
    tools2Tab.add(rdoPlayerInsideBlack);
    rdoPlayerInsideWhite = new JRadioButton("White");
    rdoPlayerInsideWhite.setBounds(240, 10, 100, 30);
    rdoPlayerInsideWhite.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (Lizzie.frame.localAnalysisFrame.localAnalysis.isPlayerInsideBlack()) {
              Lizzie.frame.localAnalysisFrame.localAnalysis.changePlayerInsideBlack();
            }
            ;
          }
        });
    tools2Tab.add(rdoPlayerInsideWhite);
    ButtonGroup bgPlayerInside = new ButtonGroup();
    bgPlayerInside.add(rdoPlayerInsideBlack);
    bgPlayerInside.add(rdoPlayerInsideWhite);

    JLabel playerToMove = new JLabel("Player To Move First");
    playerToMove.setBounds(10, 40, 120, 30);
    tools2Tab.add(playerToMove);
    rdoPlayerToMoveBlack = new JRadioButton("Black");
    rdoPlayerToMoveBlack.setBounds(130, 40, 100, 30);
    rdoPlayerToMoveBlack.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (!Lizzie.frame.localAnalysisFrame.localAnalysis.isBlackToMove()) {
              Lizzie.frame.localAnalysisFrame.localAnalysis.changeBlackToMove();
            }
            ;
          }
        });
    tools2Tab.add(rdoPlayerToMoveBlack);
    rdoPlayerToMoveWhite = new JRadioButton("White");
    rdoPlayerToMoveWhite.setBounds(240, 40, 100, 30);
    rdoPlayerToMoveWhite.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (Lizzie.frame.localAnalysisFrame.localAnalysis.isBlackToMove()) {
              Lizzie.frame.localAnalysisFrame.localAnalysis.changeBlackToMove();
            }
            ;
          }
        });
    tools2Tab.add(rdoPlayerToMoveWhite);
    ButtonGroup bgPlayerToMove = new ButtonGroup();
    bgPlayerToMove.add(rdoPlayerToMoveBlack);
    bgPlayerToMove.add(rdoPlayerToMoveWhite);

    JLabel allowKo = new JLabel("Allow Ko ");
    allowKo.setBounds(10, 70, 120, 30);
    tools2Tab.add(allowKo);
    rdoAllowKoYes = new JRadioButton("Yes");
    rdoAllowKoYes.setBounds(130, 70, 100, 30);
    rdoAllowKoYes.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (!Lizzie.frame.localAnalysisFrame.localAnalysis.isAllowKo()) {
              Lizzie.frame.localAnalysisFrame.localAnalysis.changeAllowKo();
              lblBlackKoThreats.setVisible(true);
              txtBlackKoThreats.setVisible(true);
              bKoThreatsPlus1.setVisible(true);
              bKoThreatsMinus1.setVisible(true);
              bKoThreatsMax.setVisible(true);
              bKoThreatsMin.setVisible(true);
            }
            ;
          }
        });
    tools2Tab.add(rdoAllowKoYes);
    rdoAllowKoNo = new JRadioButton("No");
    rdoAllowKoNo.setBounds(240, 70, 100, 30);
    rdoAllowKoNo.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (Lizzie.frame.localAnalysisFrame.localAnalysis.isAllowKo()) {
              Lizzie.frame.localAnalysisFrame.localAnalysis.changeAllowKo();
              lblBlackKoThreats.setVisible(false);
              txtBlackKoThreats.setVisible(false);
              bKoThreatsPlus1.setVisible(false);
              bKoThreatsMinus1.setVisible(false);
              bKoThreatsMax.setVisible(false);
              bKoThreatsMin.setVisible(false);
            }
            ;
          }
        });
    tools2Tab.add(rdoAllowKoNo);
    ButtonGroup bgAllowKo = new ButtonGroup();
    bgAllowKo.add(rdoAllowKoYes);
    bgAllowKo.add(rdoAllowKoNo);
    rdoAllowKoYes.setSelected(true);

    JLabel changePoints = new JLabel("Change Points");
    changePoints.setBounds(10, 100, 120, 30);
    tools2Tab.add(changePoints);
    bAddPointsPlus1 = new JButton("+1");
    bAddPointsPlus1.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.localAnalysisFrame.localAnalysis.moveBorder(1);
          }
        });
    bAddPointsPlus1.setEnabled(true);
    bAddPointsPlus1.setBounds(130, 100, 50, 30);
    tools2Tab.add(bAddPointsPlus1);
    bAddPointsMinus1 = new JButton("-1");
    bAddPointsMinus1.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.localAnalysisFrame.localAnalysis.moveBorder(-1);
          }
        });
    bAddPointsMinus1.setEnabled(true);
    bAddPointsMinus1.setBounds(180, 100, 50, 30);
    tools2Tab.add(bAddPointsMinus1);
    bAddPointsPlus5 = new JButton("+5");
    bAddPointsPlus5.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.localAnalysisFrame.localAnalysis.moveBorder(5);
          }
        });
    bAddPointsPlus5.setEnabled(true);
    bAddPointsPlus5.setBounds(230, 100, 50, 30);
    tools2Tab.add(bAddPointsPlus5);
    bAddPointsMinus5 = new JButton("-5");
    bAddPointsMinus5.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.localAnalysisFrame.localAnalysis.moveBorder(-5);
          }
        });
    bAddPointsMinus5.setEnabled(true);
    bAddPointsMinus5.setBounds(280, 100, 50, 30);
    tools2Tab.add(bAddPointsMinus5);
    NumberFormat nf = NumberFormat.getIntegerInstance();
    nf.setGroupingUsed(false);
    txtAddPoints =
        new JFormattedTextField(
            new InternationalFormatter(nf) {
              protected DocumentFilter getDocumentFilter() {
                return filter;
              }

              private DocumentFilter filter = new DigitOnlyFilter();
            });
    txtAddPoints.setBounds(340, 100, 40, 30);
    tools2Tab.add(txtAddPoints);

    JLabel changeKoThreats = new JLabel("Change Ko Threats");
    changeKoThreats.setBounds(10, 140, 120, 30);
    tools2Tab.add(changeKoThreats);
    bKoThreatsPlus1 = new JButton("+1");
    bKoThreatsPlus1.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.localAnalysisFrame.localAnalysis.changeBlackKoThreats(1);
          }
        });
    bKoThreatsPlus1.setEnabled(true);
    bKoThreatsPlus1.setBounds(130, 140, 60, 30);
    tools2Tab.add(bKoThreatsPlus1);
    bKoThreatsMinus1 = new JButton("-1");
    bKoThreatsMinus1.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.localAnalysisFrame.localAnalysis.changeBlackKoThreats(-1);
          }
        });
    bKoThreatsMinus1.setEnabled(true);
    bKoThreatsMinus1.setBounds(190, 140, 60, 30);
    tools2Tab.add(bKoThreatsMinus1);
    bKoThreatsMax = new JButton("Max");
    bKoThreatsMax.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.localAnalysisFrame.localAnalysis.setBlackKoThreatsMax();
          }
        });
    bKoThreatsMax.setEnabled(true);
    bKoThreatsMax.setBounds(250, 140, 60, 30);
    tools2Tab.add(bKoThreatsMax);
    bKoThreatsMin = new JButton("Min");
    bKoThreatsMin.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.localAnalysisFrame.localAnalysis.setBlackKoThreatsMin();
          }
        });
    bKoThreatsMin.setEnabled(true);
    bKoThreatsMin.setBounds(310, 140, 60, 30);
    tools2Tab.add(bKoThreatsMin);

    // JTextField txtMarkAlive;
    //		txtMarkAlive = new JFormattedTextField(
    //		    new InternationalFormatter(nf) {
    //			        protected DocumentFilter getDocumentFilter() {
    //			            return filter;
    //			        }
    //			        private DocumentFilter filter = new DigitOnlyFilter();
    //			    });
    //	 	txtMarkAlive.setBounds(0, 0, 0, 0);
    //	 	tools2Tab.add(txtMarkAlive);

    JLabel wallDistance = new JLabel("Wall Distance");
    wallDistance.setBounds(10, 175, 120, 30);
    tools2Tab.add(wallDistance);
    txtWallDistance =
        new JFormattedTextField(
            new InternationalFormatter(nf) {
              protected DocumentFilter getDocumentFilter() {
                return filter;
              }

              private DocumentFilter filter = new DigitOnlyFilter();
            });
    txtWallDistance.setBounds(130, 178, 40, 25);
    txtWallDistance.setText("3");
    tools2Tab.add(txtWallDistance);

    bMirror = new JButton("Mirror");
    bMirror.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.localAnalysisFrame.localAnalysis.mirror();
          }
        });
    bMirror.setEnabled(true);
    bMirror.setBounds(130, 215, 100, 30);
    tools2Tab.add(bMirror);

    bAdjustPoints = new JButton("Adjust Points");
    bAdjustPoints.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Lizzie.frame.localAnalysisFrame.adjustPoints();
          }
        });
    bAdjustPoints.setEnabled(true);
    bAdjustPoints.setBounds(230, 215, 100, 30);
    tools2Tab.add(bAdjustPoints);

    // startTab
    JLabel lblInsertCoordinates = new JLabel("Please Insert Two Coordinates! (e.g. a1 and j12)");
    lblInsertCoordinates.setBounds(10, 10, 250, 30);
    startTab.add(lblInsertCoordinates);

    JLabel Coordinate1 = new JLabel("Coordinate 1:");
    Coordinate1.setBounds(10, 40, 100, 30);
    startTab.add(Coordinate1);

    txtCoordinate1 = new JTextField(10);
    txtCoordinate1.setBounds(110, 43, 40, 25);
    startTab.add(txtCoordinate1);

    JLabel Coordinate2 = new JLabel("Coordinate 2:");
    Coordinate2.setBounds(170, 40, 90, 30);
    startTab.add(Coordinate2);

    txtCoordinate2 = new JTextField(10);
    txtCoordinate2.setBounds(260, 43, 40, 25);
    startTab.add(txtCoordinate2);

    lblBlackKoThreats = new JLabel("Ko Threats For Black");
    lblBlackKoThreats.setBounds(10, 220, 100, 30);
    startTab.add(lblBlackKoThreats);
    if (!allowKoThreats) {
      lblBlackKoThreats.setVisible(false);
    }
    txtBlackKoThreats =
        new JFormattedTextField(
            new InternationalFormatter(nf) {
              protected DocumentFilter getDocumentFilter() {
                return filter;
              }

              private DocumentFilter filter = new DigitOnlyFilter();
            });
    txtBlackKoThreats.setBounds(110, 223, 40, 25);
    startTab.add(txtBlackKoThreats);
    if (!allowKoThreats) {
      txtBlackKoThreats.setVisible(false);
    }
    txtBlackKoThreats.setColumns(10);
    txtBlackKoThreats.setText(String.valueOf(0));

    JLabel lblPlayerInside = new JLabel("Inside Player");
    lblPlayerInside.setBounds(10, 130, 100, 30);
    startTab.add(lblPlayerInside);
    rdoPiBlack = new JRadioButton("Black");
    rdoPiBlack.setBounds(110, 130, 50, 30);
    startTab.add(rdoPiBlack);

    rdoPiWhite = new JRadioButton("White");
    rdoPiWhite.setBounds(160, 130, 70, 30);
    startTab.add(rdoPiWhite);

    ButtonGroup piGroup = new ButtonGroup();
    piGroup.add(rdoPiBlack);
    piGroup.add(rdoPiWhite);

    JLabel lblPlayerToMove = new JLabel("Player To Move First");
    lblPlayerToMove.setBounds(10, 160, 157, 30);
    startTab.add(lblPlayerToMove);
    rdoPtmBlack = new JRadioButton("Black");
    rdoPtmBlack.setBounds(110, 160, 50, 30);
    startTab.add(rdoPtmBlack);

    rdoPtmWhite = new JRadioButton("White");
    rdoPtmWhite.setBounds(160, 160, 70, 30);
    startTab.add(rdoPtmWhite);

    ButtonGroup ptmGroup = new ButtonGroup();
    ptmGroup.add(rdoPtmBlack);
    ptmGroup.add(rdoPtmWhite);

    JLabel lblAllowKo = new JLabel("Allow Ko");
    lblAllowKo.setBounds(10, 70, 100, 30);
    startTab.add(lblAllowKo);
    chkAllowKo = new JCheckBox("");
    if (allowKoThreats) {
      chkAllowKo.setSelected(true);
    }
    chkAllowKo.setEnabled(true);
    chkAllowKo.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            changeAllowKoThreats();
          }
        });
    chkAllowKo.setBounds(110, 70, 30, 30);
    startTab.add(chkAllowKo);

    JLabel lblShowStartDialog = new JLabel("Show Start Dialog");
    lblShowStartDialog.setBounds(10, 100, 100, 30);
    startTab.add(lblShowStartDialog);
    chkShowStartDialog = new JCheckBox("");
    if (showStartDialog) {
      chkShowStartDialog.setSelected(true);
    }
    chkShowStartDialog.setEnabled(true);
    chkShowStartDialog.addChangeListener(
        new ChangeListener() {
          public void stateChanged(ChangeEvent e) {
            showStartDialog = !showStartDialog;
          }
        });
    chkShowStartDialog.setBounds(110, 100, 30, 30);
    startTab.add(chkShowStartDialog);
    chkShowStartDialog.setSelected(Lizzie.config.showLocalAnalysisStartDialog);

    JLabel lblWallDistance = new JLabel("Wall Distance");
    lblWallDistance.setBounds(10, 190, 100, 25);
    startTab.add(lblWallDistance);
    txtWallDistance =
        new JFormattedTextField(
            new InternationalFormatter(nf) {
              protected DocumentFilter getDocumentFilter() {
                return filter;
              }

              private DocumentFilter filter = new DigitOnlyFilter();
            });
    txtWallDistance.setColumns(10);
    txtWallDistance.setBounds(110, 193, 40, 25);
    startTab.add(txtWallDistance);
    txtWallDistance.setText(String.valueOf(Lizzie.config.la.getInt("wall-distance")));

    addTab("Start Settings", null, startTab, null);
    startTab.setLayout(null);
  }

  private void loadConfigs() {
    allowKoThreats = Lizzie.config.allowKo;
  }

  public void showTools() {
    setVisible(false);
    remove(startTab);
    addTab("tools1", null, tools1Tab, null);
    tools1Tab.setLayout(null);
    addTab("tools2", null, tools2Tab, null);
    tools2Tab.setLayout(null);
    setVisible(true);
    if (Lizzie.frame.localAnalysisFrame.localAnalysis.isPlayerInsideBlack()) {
      rdoPlayerInsideBlack.setSelected(true);
    } else {
      rdoPlayerInsideWhite.setSelected(true);
    }
    if (Lizzie.frame.localAnalysisFrame.localAnalysis.isBlackToMove()) {
      rdoPlayerToMoveBlack.setSelected(true);
    } else {
      rdoPlayerToMoveWhite.setSelected(true);
    }
    if (Lizzie.frame.localAnalysisFrame.localAnalysis.isBlackToMove()) {
      rdoAllowKoYes.setSelected(true);
      lblBlackKoThreats.setVisible(true);
      txtBlackKoThreats.setVisible(true);
      bKoThreatsPlus1.setVisible(true);
      bKoThreatsMinus1.setVisible(true);
      bKoThreatsMax.setVisible(true);
      bKoThreatsMin.setVisible(true);
    } else {
      rdoAllowKoNo.setSelected(true);
      lblBlackKoThreats.setVisible(false);
      txtBlackKoThreats.setVisible(false);
      bKoThreatsPlus1.setVisible(false);
      bKoThreatsMinus1.setVisible(false);
      bKoThreatsMax.setVisible(false);
      bKoThreatsMin.setVisible(false);
    }
    txtWallDistance.setText(
        String.valueOf(Lizzie.frame.localAnalysisFrame.localAnalysis.getWallDistance()));
  }

  public void setCoordinate1Text(int[] coords) {
    if (coords == null || coords.length != 2) {
      // ERROR
    }
    coordinate1 = "" + String.valueOf(alphabet[coords[0]]) + coords[1];
    txtCoordinate1.setText(coordinate1);
  }

  public void setCoordinate2Text(int[] coords) {
    if (coords == null || coords.length != 2) {
      // ERROR
    }
    coordinate2 = "" + String.valueOf(alphabet[coords[0]]) + coords[1];
    txtCoordinate2.setText(coordinate2);
  }

  private void changeAllowKoThreats() {
    allowKoThreats = !allowKoThreats;
    if (allowKoThreats) {
      lblBlackKoThreats.setVisible(true);
      txtBlackKoThreats.setVisible(true);
    } else {
      lblBlackKoThreats.setVisible(false);
      txtBlackKoThreats.setVisible(false);
    }
  }

  public int getPlayerInside() {
    if (rdoPiBlack.isSelected()) {
      return 2;
    }
    if (rdoPiWhite.isSelected()) {
      return 3;
    }
    return Lizzie.config.playerInside;
  }

  public int getPlayerToMove() {
    if (rdoPtmBlack.isSelected()) {
      return 2;
    }
    if (rdoPtmWhite.isSelected()) {
      return 3;
    }
    return Lizzie.config.playerToMove;
  }

  public int getBlackKoThreats() {
    return txtFieldIntValue(txtBlackKoThreats);
  }

  private Integer txtFieldIntValue(JTextField txt) {
    if (txt.getText().trim().isEmpty()) {
      return 0;
    } else {
      return Integer.parseInt(txt.getText().trim());
    }
  }
}
