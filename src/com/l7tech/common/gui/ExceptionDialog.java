package com.l7tech.common.gui;


import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.widgets.HyperlinkLabel;
import com.l7tech.common.util.ExceptionUtils;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTML;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.logging.Level;


/**
 * Class ExceptionDialog is the generic eror/exception dialog for SSL
 * policy editor.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ExceptionDialog extends JDialog implements ActionListener {
    private static final String OPEN_HTML = "<html>";
    private static final String CLOSE_HTML = "</html>";
    private JPanel main = new JPanel();
    private JPanel messagePanel = new JPanel();
    private JPanel internalErroLabelPanel = null;
    private JPanel buttons = new JPanel();
    private JTabbedPane tabPane = new JTabbedPane();
    private JTextArea textArea = new JTextArea();
    private JScrollPane scrollPane = new JScrollPane(textArea);
    private UIDefaults uiDefaults = UIManager.getLookAndFeelDefaults();
    private Icon errorIcon = uiDefaults.getIcon("OptionPane.errorIcon");
    private Icon warningIcon = uiDefaults.getIcon("OptionPane.warningIcon");
    private Icon informationIcon = uiDefaults.getIcon("OptionPane.informationIcon");
    private JLabel internalErrorLabel = null;
    private JLabel messageLabel = null;
    private JLabel exceptionMessageLabel = null;
    private JLabel iconLabel = null;
    private JButton close = new JButton("Close");

    private JButton shutdown = new JButton("Shutdown");
    private JButton ignore = new JButton("Ignore");
    private String internalErrorLabelText = "An severe error has occurred.  You may need to restart the application";
    private boolean allowShutdown = true;

    public ExceptionDialog(Frame parent, String title, String labelMessage, String message, Throwable throwable, Level level) {
        super(parent, true);
        if (labelMessage != null) {
            internalErrorLabelText = labelMessage;
            allowShutdown = false;
        }
        initialize(title, message, throwable, level);
    }

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
     * Constructor ExceptionDialog
     *
     * @param parent
     * @param message
     * @param throwable
     */
    public ExceptionDialog(Frame parent, String message, Throwable throwable, Level level) {
        super(parent, true);
        initialize(getDialogTitle(level), message, throwable, level);
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
        setResizable(true);
        messageLabel = new JLabel(createMessage(message), SwingConstants.CENTER);
        iconLabel = levelIcon(level);
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 0));

        if (level == Level.SEVERE) {
            shutdown.addActionListener(this);
            ignore.addActionListener(this);
            buttons.add(shutdown);
            buttons.add(ignore);
            Utilities.equalizeButtonSizes(
              new AbstractButton[] {
                  shutdown,
                  ignore
            });
        } else {
            close.addActionListener(this);
            buttons.add(close);
        }


        //
        textArea.setText(stackTrace(throwable));
        textArea.setEditable(false);
        textArea.setCaretPosition(0);


        try {
            exceptionMessageLabel = getExceptionMessageLabel(throwable);
        } catch (MalformedURLException e) {
            e.printStackTrace(System.err);
        }

        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        messagePanel.add(Box.createGlue());
        messagePanel.add(messageLabel);

        if (exceptionMessageLabel != null) {
            messagePanel.add(exceptionMessageLabel);
        }
        messagePanel.add(Box.createGlue());

        main.setLayout(new BorderLayout());
        main.add(iconLabel, BorderLayout.WEST);
        main.add(messagePanel, BorderLayout.CENTER);

        if (level == Level.SEVERE) {
            internalErroLabelPanel = new JPanel();
            internalErroLabelPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
            internalErroLabelPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5 ,0));
            internalErrorLabel = new JLabel(internalErrorLabelText);
            internalErroLabelPanel.add(internalErrorLabel);
            main.add(internalErroLabelPanel, BorderLayout.NORTH);
        }

        main.add(buttons, BorderLayout.SOUTH);

        tabPane.add(main, "Error");
        tabPane.add(scrollPane, "Details");
        tabPane.setPreferredSize(new Dimension(650, 175));

        pane.setLayout(new BorderLayout());
        pane.add(tabPane, BorderLayout.CENTER);
        pane.add(buttons, BorderLayout.SOUTH);

        if (!allowShutdown) {
            shutdown.setEnabled(false);
            shutdown.setVisible(false);
        }
    }

    private JLabel getExceptionMessageLabel(Throwable t)
      throws MalformedURLException {
        HyperlinkLabel label = null;
        if (t == null || (t.getMessage() == null || "".equals(t.getMessage())))
            return new JLabel();
        Throwable cause = ExceptionUtils.unnestToRoot(t);
        label = new HyperlinkLabel(cause.getMessage(), null, "file://", SwingConstants.CENTER);
        label.addHyperlinkListener(new HyperlinkListener() {
            /**
             * Called when a hypertext link is updated.
             *
             * @param e the event responsible for the update
             */
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (HyperlinkEvent.EventType.ACTIVATED != e.getEventType())
                    return;
                if (tabPane.getTabCount() > 1)
                    tabPane.setSelectedIndex(1);
            }

        });
        return label;
    }

    private String createMessage(String message) {
        StringBuffer sb = new StringBuffer();
        sb.append(OPEN_HTML + openTag(HTML.Tag.CENTER) + message + closeTag(HTML.Tag.CENTER));
        sb.append(CLOSE_HTML);
        return sb.toString();
    }


    private String openTag(HTML.Tag t) {
        return "<" + t + ">";
    }

    private String closeTag(HTML.Tag t) {
        return "</" + t + ">";
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


    private String getDialogTitle(Level level) {
        if (level == Level.SEVERE) {
            return "Error";
        } else if (level == Level.WARNING) {
            return "Warning";
        } else {
            return "Info";
        }
    }


    private String stackTrace(Throwable throwable) {
        String value = null;
        if (throwable == null) {
            value = "There are no additional mesaage details.";
            return value;
        }
        Throwable cause = ExceptionUtils.unnestToRoot(throwable);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        cause.printStackTrace(printWriter);
        printWriter.flush();
        value = stringWriter.toString();

        return value;
    }


    public void actionPerformed(ActionEvent e) {
        final Object source = e.getSource();
        if (source == close || source == ignore) {
            this.dispose();
        } else if (source == shutdown) {
            System.exit(-1);
        }
    }

    private void calculateDialogSize() {
          //todo: dynamically resize
        double height = messagePanel.getSize().getHeight();
        if (internalErroLabelPanel != null) {
            height += internalErroLabelPanel.getSize().getHeight();
        }
        height += buttons.getSize().getHeight();
        setSize(new Dimension(650, (int)height));
    }

    public static void main(String[] args) {
        ExceptionDialog d = new
          ExceptionDialog(null,
            "There was problem that caused this messich. The program will now exit.",
            new Exception("Exception message"), Level.SEVERE);
        d.pack();
        d.show();
        System.exit(-1);

    }
}
