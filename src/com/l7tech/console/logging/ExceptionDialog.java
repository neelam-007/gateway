package com.l7tech.console.logging;


import com.l7tech.console.panels.Utilities;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;

import javax.swing.*;


/**
 * Class ExceptionDialog
 *
 * @author <a href="mailto:jeff@shiftone.org">Jeff Drost</a>
 * @version $Revision$
 */
public class ExceptionDialog extends JDialog implements ActionListener {
    private JPanel main = new JPanel();
    private JPanel buttons = new JPanel();
    private JTabbedPane tabPane = new JTabbedPane();
    private JTextArea textArea = new JTextArea();
    private JScrollPane scrollPane = new JScrollPane(textArea);
    private UIDefaults uiDefaults = UIManager.getLookAndFeelDefaults();
    private Icon errorIcon = uiDefaults.getIcon("OptionPane.errorIcon");
    private Icon warningIcon = uiDefaults.getIcon("OptionPane.warningIcon");
    private Icon informationIcon = uiDefaults.getIcon("OptionPane.informationIcon");
    private JLabel messageLabel = null;
    private JLabel iconLabel = null;
    private JButton close = new JButton("Close");

    /**
     * Constructor ExceptionDialog
     *
     * @param parent
     * @param title
     * @param message
     * @param throwable
     */
    public ExceptionDialog(Frame parent, String title, String message, Throwable throwable, Level level) {
        super(parent, true);
        initialize(title, message, throwable, level);
    }

    /**
     * Initialize the dialog
     *
     * @param title .
     * @param message .
     * @param throwable .
     */
    private void initialize(String title, String message, Throwable throwable, Level level) {
        Container pane = getContentPane();
        setTitle(title);
        if (JDialog.isDefaultLookAndFeelDecorated()) {
            boolean supportsWindowDecorations =
              UIManager.getLookAndFeel().getSupportsWindowDecorations();
            if (supportsWindowDecorations) {
                setUndecorated(true);
                getRootPane().setWindowDecorationStyle(getDecorationStyle(level));
            }
        }
        setResizable(false);
        messageLabel = new JLabel(message, SwingConstants.CENTER);
        iconLabel = levelIcon(level);
        iconLabel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        close.addActionListener(this);
        buttons.add(close);

        //
        textArea.setText(stackTrace(throwable));
        textArea.setEditable(false);
        textArea.setCaretPosition(0);

        //
        main.setLayout(new BorderLayout());
        main.add(iconLabel, BorderLayout.WEST);
        main.add(buttons, BorderLayout.SOUTH);
        main.add(messageLabel, BorderLayout.CENTER);

        //
        tabPane.add(main, "Error");
        tabPane.add(scrollPane, "Details");

        //
        pane.setLayout(new BorderLayout());
        pane.add(tabPane, BorderLayout.CENTER);
        pane.add(buttons, BorderLayout.SOUTH);
        Utilities.centerOnScreen(this);

        setSize(650, 250);
    }

    private int getDecorationStyle(Level level) {
        if (level == Level.SEVERE) {
            return JRootPane.ERROR_DIALOG;
        } else if (level == Level.WARNING) {
            return JRootPane.WARNING_DIALOG;
        } else {
            return JRootPane.PLAIN_DIALOG;
        }

    }

    private JLabel levelIcon(Level level) {
        if (level == Level.SEVERE) {
            return new JLabel(errorIcon);
        } else if (level == Level.WARNING) {
            return new JLabel(warningIcon);
        } else {
            return new JLabel(informationIcon);
        }
    }

    private String stackTrace(Throwable throwable) {
        String value = null;
        if (throwable == null) {
            value = "There are no additional mesaage details.";
            return value;
        }

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        throwable.printStackTrace(printWriter);
        printWriter.flush();
        value = stringWriter.toString();

        return value;
    }


    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == close) {
            this.dispose();
        }
    }

    public static void main(String[] args) {
        ExceptionDialog d = new ExceptionDialog(null, "Error", "This is a messich", new Exception(), Level.WARNING);
        d.pack();
        d.show();

    }
}
