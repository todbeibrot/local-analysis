package la;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.gui.*;
import featurecat.lizzie.util.WindowPosition;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;
import org.json.JSONArray;

public class LAGtpConsolePane extends JDialog {
  private static final ResourceBundle resourceBundle = MainFrame.resourceBundle;

  // Display Comment
  private HTMLDocument htmlDoc;
  private LizziePane.HtmlKit htmlKit;
  private StyleSheet htmlStyle;
  private JScrollPane scrollPane;
  private JTextPane console;
  private String command;
  private boolean isAnalyzeCommand = false;
  private final JTextField txtCommand = new JTextField();
  private JLabel lblCommand = new JLabel();
  private JPanel pnlCommand = new JPanel();
  private final int MAX_HTML_LENGTH = 10000;

  /** Creates a Gtp Console Window */
  public LAGtpConsolePane(Window owner) {
    super(owner);
    setTitle("Gtp Console");

    JSONArray pos = WindowPosition.gtpWindowPos();
    if (pos != null) {
      this.setBounds(pos.getInt(0), pos.getInt(1), pos.getInt(2), pos.getInt(3));
    } else {
      Insets oi = owner.getInsets();
      setBounds(
          0,
          owner.getY() - oi.top,
          Math.max(owner.getX() - oi.left, 400),
          Math.max(owner.getHeight() + oi.top + oi.bottom, 300));
    }

    htmlKit = new LizziePane.HtmlKit();
    htmlDoc = (HTMLDocument) htmlKit.createDefaultDocument();
    htmlStyle = htmlKit.getStyleSheet();
    htmlStyle.addRule(Lizzie.config.gtpConsoleStyle);

    console = new JTextPane();
    console.setBorder(BorderFactory.createEmptyBorder());
    console.setEditable(false);
    console.setEditorKit(htmlKit);
    console.setDocument(htmlDoc);
    scrollPane = new JScrollPane();
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    txtCommand.setBackground(Color.DARK_GRAY);
    txtCommand.setForeground(Color.WHITE);
    lblCommand.setFont(new Font("Tahoma", Font.BOLD, 11));
    lblCommand.setOpaque(true);
    lblCommand.setBackground(Color.DARK_GRAY);
    lblCommand.setForeground(Color.WHITE);
    lblCommand.setText(
        Lizzie.frame.localAnalysisFrame.leelaz == null
            ? "GTP>"
            : Lizzie.frame.localAnalysisFrame.leelaz.currentShortWeight() + ">");
    pnlCommand.setLayout(new BorderLayout(0, 0));
    pnlCommand.add(lblCommand, BorderLayout.WEST);
    pnlCommand.add(txtCommand);
    getContentPane().add(scrollPane, BorderLayout.CENTER);
    getContentPane().add(pnlCommand, BorderLayout.SOUTH);
    scrollPane.setViewportView(console);
    getRootPane().setBorder(BorderFactory.createEmptyBorder());
    getRootPane().setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);

    txtCommand.addActionListener(e -> postCommand(e));
  }

  public void addCommand(String command, int commandNumber) {
    if (command == null || command.trim().length() == 0) {
      return;
    }
    lblCommand.setText(
        Lizzie.frame.localAnalysisFrame.leelaz == null
            ? "GTP>"
            : Lizzie.frame.localAnalysisFrame.leelaz.currentShortWeight() + ">");
    this.command = command;
    this.isAnalyzeCommand =
        command.startsWith("lz-analyze")
            || command.startsWith("kata-analyze")
            || command.startsWith("lz-genmove_analyze");
    addText(formatCommand(command, commandNumber));
  }

  public void addLine(String line) {
    if (line == null || line.trim().length() == 0 || isAnalyzeCommand) {
      return;
    }
    addText(format(line));
  }

  public void addLineForce(String line) {
    if (line == null || line.trim().length() == 0) {
      return;
    }
    addText(format(line));
  }

  public void addZenCommand(String command, int commandNumber) {
    if (command == null || command.trim().length() == 0) {
      return;
    }
    addText(formatZenCommand(command, commandNumber));
  }

  private void addText(String text) {
    try {
      htmlKit.insertHTML(htmlDoc, htmlDoc.getLength(), text, 0, 0, null);
      removeOldText();
      console.setCaretPosition(htmlDoc.getLength());
    } catch (BadLocationException | IOException e) {
      e.printStackTrace();
    }
  }

  private void removeOldText() {
    Element body =
        htmlDoc.getElement(
            htmlDoc.getDefaultRootElement(), StyleConstants.NameAttribute, HTML.Tag.BODY);
    while (htmlDoc.getLength() > MAX_HTML_LENGTH) {
      ElementIterator it = new ElementIterator(body);
      it.first();
      Element e = it.next();
      if (e == null) {
        break;
      }
      htmlDoc.removeElement(e);
    }
  }

  public String formatZenCommand(String command, int commandNumber) {
    return String.format(
        "<span class=\"command\">" + ("YAZenGtp") + "> %d %s </span><br />",
        commandNumber,
        command);
  }

  public String formatCommand(String command, int commandNumber) {
    return String.format(
        "<span class=\"command\">"
            + (Lizzie.frame.localAnalysisFrame.leelaz == null
                ? "GTP"
                : Lizzie.frame.localAnalysisFrame.leelaz.currentShortWeight())
            + "> %d %s </span><br />",
        commandNumber,
        command);
  }

  public String format(String text) {
    StringBuilder sb = new StringBuilder();
    // TODO need better performance
    text =
        text.replaceAll("\\b([0-9]{1,3}\\.*[0-9]{0,2}%)", "<span class=\"winrate\">$1</span>")
            .replaceAll("\\b([A-HJ-Z][1-9][0-9]?)\\b", "<span class=\"coord\">$1</span>")
            .replaceAll(" (info move)", "<br />$1")
            .replaceAll("(\r\n)|(\n)", "<br />")
            .replaceAll(" ", "&nbsp;");
    sb.append("<b>   </b>").append(text);
    return sb.toString();
  }

  private void postCommand(ActionEvent e) {
    if (txtCommand.getText() == null || txtCommand.getText().trim().isEmpty()) {
      return;
    }
    String command = txtCommand.getText().trim();
    txtCommand.setText("");

    if (Lizzie.frame.localAnalysisFrame.leelaz != null) {
      if (command.startsWith("genmove")
          || command.startsWith("lz-genmove")
          || command.startsWith("play")) {
        String cmdParams[] = command.split(" ");
        if (cmdParams.length >= 2) {
          String param1 = cmdParams[1].toUpperCase();
          boolean needPass =
              (Lizzie.frame.localAnalysisFrame.board.getData().blackToPlay != "B".equals(param1));
          if (needPass) {
            Lizzie.frame.localAnalysisFrame.board.pass();
          }
          if (command.startsWith("genmove") || command.startsWith("lz-genmove")) {
            if (!Lizzie.frame.localAnalysisFrame.leelaz.isThinking) {
              Lizzie.frame.localAnalysisFrame.leelaz.time_settings();
              Lizzie.frame.localAnalysisFrame.leelaz.isInputCommand = true;
              if (command.startsWith("genmove")) {
                Lizzie.frame.localAnalysisFrame.leelaz.genmove(param1);
              } else {
                Lizzie.frame.localAnalysisFrame.leelaz.genmove_analyze(param1);
              }
            }
          } else {
            if (cmdParams.length >= 3) {
              String param2 = cmdParams[2].toUpperCase();
              Lizzie.frame.localAnalysisFrame.board.place(param2);
            }
          }
        }
      } else if ("clear_board".equals(command)) {
        Lizzie.frame.localAnalysisFrame.board.clear();
      } else if ("undo".equals(command)) {
        Input.undo();
      } else if (command.startsWith("boardsize")) {
        String cmdParams[] = command.split(" ");
        if (cmdParams.length >= 2) {
          int width = Integer.parseInt(cmdParams[1]);
          int height = width;
          if (cmdParams.length >= 3) {
            height = Integer.parseInt(cmdParams[2]);
          }
          Lizzie.frame.localAnalysisFrame.board.reopen(width, height);
        }
      } else if (command.startsWith("komi")) {
        String cmdParams[] = command.split(" ");
        if (cmdParams.length >= 2) {
          try {
            double komi = Double.parseDouble(cmdParams[1]);
            Lizzie.frame.localAnalysisFrame.leelaz.komi(komi);
          } catch (Exception ex) {
          }
        }
      } else {
        Lizzie.frame.localAnalysisFrame.leelaz.sendCommand(command);
      }
    }
  }
}
