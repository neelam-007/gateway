package com.l7tech.gui;

import com.l7tech.gui.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

/**
 * @author: ghuang
 */
public class ErrorMessageDialog extends JDialog implements ActionListener {

    private final static int DIALOG_WIDTH = 500;
    private final static int DIALOG_HEIGHT = 320;

    private JPanel mainPanel;
    private JButton okButton;
    private JButton closeManagerButton;
    private JButton reportButton;
    private JLabel iconLabel;
    private JTextPane messagePane;

    private Throwable throwable;
    private SaveErrorStrategy defaultSaveStrategy = new DefaultSaveErrorStrategy();
    private static SaveErrorStrategy browserSaveStrategy;
    private static Runnable shutdownHandler;
    public static final ResourceBundle resources;
    static {
        DialogDisplayer.suppressSheetDisplay(ErrorMessageDialog.class);
        resources = ResourceBundle.getBundle("com/l7tech/common/resources/ErrorMessageDialog");
        shutdownHandler = new Runnable() {public void run(){System.exit(-1);}};
    }

    /**
     * Constructor ErrorMessageDialog for error messages
     */
    public ErrorMessageDialog(final Frame parent, final String errorMessage, final Throwable throwable) {
        super(parent, true);
        this.throwable = throwable;
        initialize(errorMessage);
    }

    /**
     * Constructor ErrorMessageDialog for error messages
     */
    public ErrorMessageDialog(final Dialog parent, final String errorMessage, final Throwable throwable) {
        super(parent, true);
        this.throwable = throwable;
        initialize(errorMessage);
    }

    /**
     * If the SSM is an untrusted applet, we need one other save strategy - BrowserSaveErrorStrategy.
     * @param strategy The browser save strategy to be set in ErrorMessageDialog.
     */
    public static void setBrowserSaveErrorStrategy(SaveErrorStrategy strategy) {
        browserSaveStrategy = strategy;
    }

    public synchronized static void setShutdownHandler(Runnable handler) {
        if (handler == null)
            throw new IllegalArgumentException("handler must not be null");

        shutdownHandler = handler;
    }

    /**
     * Initialize the dialog
     *
     * @param errorMessage .
     */
    private void initialize(String errorMessage) {
        setContentPane(mainPanel);
        setTitle(resources.getString("error.dialog.title"));
        iconLabel.setIcon(UIManager.getLookAndFeelDefaults().getIcon("OptionPane.warningIcon"));
        if ( errorMessage != null ) {
            if ( errorMessage.toLowerCase().startsWith("<html") ) {
                messagePane.setText(errorMessage);
            } else {
                messagePane.setText("<html>" + errorMessage + "</html>");                
            }
            messagePane.setCaretPosition(0);
        }

        okButton.addActionListener(this);
        reportButton.addActionListener(this);
        closeManagerButton.addActionListener(this);

        // Set okButton as a default button
        getRootPane().setDefaultButton(okButton);

        if (JDialog.isDefaultLookAndFeelDecorated()) {
            boolean supportsWindowDecorations =
                    UIManager.getLookAndFeel().getSupportsWindowDecorations();
            if (supportsWindowDecorations) {
                setUndecorated(true);
                getRootPane().setWindowDecorationStyle(JRootPane.WARNING_DIALOG);
            }
        }

        setResizable(true);
        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        Utilities.setEscKeyStrokeDisposes(this);
        Utilities.setAlwaysOnTop(this, true);
        Utilities.centerOnScreen(this);
    }

    public void actionPerformed(final ActionEvent e) {
        final Object source = e.getSource();
        if (source == okButton) {
            //setVisible(false);
            dispose();
        } else if (source == closeManagerButton) {
            shutdownHandler.run();
            //setVisible(false);
            dispose();
        } else if (source == reportButton) {
            // Try to use the default save strategy first.  If there exists any problem, try the browser save strategy.
            try {
                defaultSaveStrategy.setErrorMessageDialog(this);
                defaultSaveStrategy.setThrowable(throwable);
                defaultSaveStrategy.saveErrorReportFile();
            } catch (Exception ex1) {
                if (browserSaveStrategy == null) {
                    JOptionPane.showMessageDialog(this,
                            "Cannot save a report due to an internal error.  Please contact to your administrator.",
                            "Warning",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                try {
                    browserSaveStrategy.setErrorMessageDialog(this);
                    browserSaveStrategy.setThrowable(throwable);
                    browserSaveStrategy.saveErrorReportFile();
                } catch (Exception ex2) {
                    // Any problems have been handled in BrowserSaveErrorStrategy.
                }
            }
        }
    }
}