package la;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.FlowLayout;
import java.awt.Window.Type;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

public class LocalAnalysisOptionDialog extends JDialog {
  JButton okButton;
  JButton cancelButton;

  public LocalAnalysisOptionDialog() {
    setTitle("Options");
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
            // startAnalysis();
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
            setVisible(false);
          }
        });
    cancelButton.setActionCommand("Cancel");
    buttonPane.add(cancelButton);
  }

  public boolean isWindows() {
    String osName = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
    return osName != null && !osName.contains("darwin") && osName.contains("win");
  }
}
