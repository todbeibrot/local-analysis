package la;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.text.DocumentFilter;
import javax.swing.text.InternationalFormatter;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.util.DigitOnlyFilter;

public class LocalAnalysisDialog extends JDialog {
	final private static char[] alphabet = {'a','b','c','d','e','f','g','h','j','k','l','m','n','o','p','q','r','s','t', 'u'};
	
	public JButton okButton;
	public JButton cancelButton;
	public JRadioButton rdoPtmBlack;
	public JRadioButton rdoPtmWhite;
	public JRadioButton rdoBlack;
	public JRadioButton rdoWhite;
	public JTextField txtCoordinate1;
	public JTextField txtCoordinate2;
	public JFormattedTextField txtBlackKoThreats;
	
    private int playerInside;
    private int playerToMove;
	private int blackKoThreats;
	private String coordinate1;
	private String coordinate2;
	private boolean allowKoThreats;
	
	public LocalAnalysisDialog(String coords1, String coords2) {
//		if(coords1 == null || coords1.length != 2 
//				|| coords2 == null || coords2.length != 2) {
//			//ERROR
//		}
//		coordinate1 = "" + String.valueOf(alphabet[coords1[0]]) + coords1[1];
//		coordinate2 = "" + String.valueOf(alphabet[coords2[0]]) + coords2[1];
		
		coordinate1 = coords1;
		coordinate2 = coords2;
		allowKoThreats = Lizzie.frame.localAnalysisFrame.tools.allowKoThreats;
		
	    setTitle("Local Analysis");
	    setModalityType(ModalityType.APPLICATION_MODAL);
	    if (isWindows()) { // avoid suspicious behavior on Linux (#616)
	        setType(Type.POPUP);
	      }
	    setBounds(100, 100, 500, 250);
	    getContentPane().setLayout(new BorderLayout());
	    JPanel buttonPane = new JPanel();
	    buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
	    getContentPane().add(buttonPane, BorderLayout.SOUTH);
	    
	    okButton = new JButton("Ok");
	    okButton.addActionListener(
	        new ActionListener() {
	          public void actionPerformed(ActionEvent e) {
	            setVisible(false);
	            startAnalysis();
	          }
	        });
	    okButton.setActionCommand("OK");
	    okButton.setEnabled(false);
	    buttonPane.add(okButton);
	    getRootPane().setDefaultButton(okButton);
	    
	    cancelButton = new JButton("Cancel");
	    cancelButton.addActionListener(
	        new ActionListener() {
	          public void actionPerformed(ActionEvent e) {
	        	Lizzie.frame.localAnalysisFrame.coordinate1 = null;
	        	Lizzie.frame.localAnalysisFrame.coordinate2 = null;
	            setVisible(false);
	          }
	        });
	    cancelButton.setActionCommand("Cancel");
	    buttonPane.add(cancelButton);
		
		JLabel lblInsertCoordinates = new JLabel("Please insert two coordinates! (e.g. a1 and j12)");
		lblInsertCoordinates.setBounds(10, 10, 250, 16);
	 	add(lblInsertCoordinates);
	 	lblInsertCoordinates.setVisible(true);
	 	
	 	JLabel Coordinate1 = new JLabel("Coordinate 1:");
		Coordinate1.setBounds(10, 33, 157, 16);
	 	add(Coordinate1);
		
	 	txtCoordinate1 = new JTextField(10);
	 	txtCoordinate1.setBounds(110, 30, 52, 26);
	 	add(txtCoordinate1);
	 	txtCoordinate1.setText(coordinate1);
	 	
	 	JLabel Coordinate2 = new JLabel("Coordinate 2:");
		Coordinate2.setBounds(180, 33, 157, 16);
	 	add(Coordinate2);
	 	
	 	txtCoordinate2 = new JTextField(10);
	 	txtCoordinate2.setBounds(270, 30, 52, 26);
	 	add(txtCoordinate2);
	 	txtCoordinate2.setText(coordinate2);

		if(Lizzie.frame.localAnalysisFrame.tools.allowKoThreats) {
			JLabel lblBlackKoThreats =
			 	    new JLabel("Ko Threats For Black");
			 	lblBlackKoThreats.setBounds(10, 67, 157, 16);
			 	add(lblBlackKoThreats);
			 	NumberFormat nf = NumberFormat.getIntegerInstance();
			 	nf.setGroupingUsed(false);
			 	txtBlackKoThreats =
			 	    new JFormattedTextField(
			 		    new InternationalFormatter(nf) {
			 		        protected DocumentFilter getDocumentFilter() {
			 		            return filter;
			 		        }
	
			 		        private DocumentFilter filter = new DigitOnlyFilter();
			 		    });
			 	txtBlackKoThreats.setBounds(180, 65, 40, 26);
			 	add(txtBlackKoThreats);
			 	txtBlackKoThreats.setColumns(10);
			 	txtBlackKoThreats.setText(String.valueOf(Lizzie.frame.localAnalysisFrame.tools.getBlackKoThreats()));
		}
		else {
			blackKoThreats = 0;
		}
		playerInside = Lizzie.frame.localAnalysisFrame.tools.getPlayerInside();
		if( playerInside == 0) {
			JLabel lblPlayerInside = new JLabel("Inside Player");
			lblPlayerInside.setBounds(10, 100, 157, 16);
			add(lblPlayerInside);
			rdoBlack = new JRadioButton("Black");
			rdoBlack.setBounds(177, 100, 69, 23);
			add(rdoBlack);
	
			rdoWhite = new JRadioButton("White");
			rdoWhite.setBounds(260, 100, 92, 23);
			add(rdoWhite);
	
			ButtonGroup piGroup = new ButtonGroup();
			piGroup.add(rdoBlack);
			piGroup.add(rdoWhite);
			rdoBlack.setSelected(true);
		}
		playerToMove = Lizzie.frame.localAnalysisFrame.tools.getPlayerToMove();
		if(playerToMove == 0) {
			JLabel lblPlayerToMove = new JLabel("Player to move first");
			lblPlayerToMove.setBounds(10, 120, 157, 16);
			 	add(lblPlayerToMove);
			 	rdoPtmBlack = new JRadioButton("Black");
			    rdoPtmBlack.setBounds(177, 120, 69, 23);
			    add(rdoPtmBlack);
	
			    rdoPtmWhite = new JRadioButton("White");
			    rdoPtmWhite.setBounds(260, 120, 92, 23);
			    add(rdoPtmWhite);
	
			    ButtonGroup ptmGroup = new ButtonGroup();
			    ptmGroup.add(rdoPtmBlack);
			    ptmGroup.add(rdoPtmWhite);
			    rdoPtmBlack.setSelected(true);
		}
		
		JLabel nothing = new JLabel("");	//necessary for some reasons. else the last element will be at the wrong place
		nothing.setBounds(10, 140, 1, 1);	//and other weird things happen
		add(nothing);
		
		okButton.setEnabled(true);
		setLocationRelativeTo(getOwner());
		if(!Lizzie.config.showLocalAnalysisStartDialog) {
			startAnalysis();
		}
	}
	
	private void startAnalysis() {
		coordinate1 = txtCoordinate1.getText();
		coordinate2 = txtCoordinate2.getText();
		if(coordinate1 == null || coordinate2 == null) {
			return;
		}
		if(playerInside == 0) {
			if(rdoBlack.isSelected()) {
				playerInside = 2;
			}
			else {
				playerInside = 3;
			}
		}
		if(playerToMove == 0) {
			if(rdoPtmBlack.isSelected()) {
				playerInside = 2;
			}
			else {
				playerInside = 3;
			}
		}
		
		if(allowKoThreats) {
			blackKoThreats = txtFieldIntValue(txtBlackKoThreats);
		}
		else {
			blackKoThreats = 0;
		}
		
		Lizzie.frame.localAnalysisFrame.localAnalysis 
				= new LocalAnalysis(coordinate1, coordinate2, playerInside, playerToMove, blackKoThreats);
		Lizzie.frame.localAnalysisFrame.tools.showTools();
	}
	
	  private Integer txtFieldIntValue(JTextField txt) {
		    if (txt.getText().trim().isEmpty()) {
		      return 0;
		    } else {
		      return Integer.parseInt(txt.getText().trim());
		    }
		 }
	  
	  public boolean isWindows() {
		  String osName = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
		  return osName != null && !osName.contains("darwin") && osName.contains("win");
	 }

}
