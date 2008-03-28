package com.l7tech.common.gui;

import com.l7tech.common.gui.util.*;
import com.l7tech.common.gui.widgets.WrappingLabel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

/**
 * @author: ghuang
 */
public class ErrorMessageDialog extends JDialog implements ActionListener {

    private final static int DIALOG_LENGTH = 500;
    private final static int DIALOG_WIDTH = 320;

    private JPanel mainPanel;
    private JButton okButton;
    private JButton closeManagerButton;
    private JButton reportButton;
    private JLabel iconLabel;
    private WrappingLabel messageLabel;

    private Throwable throwable;
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
        messageLabel.setText(errorMessage);

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
        setSize(DIALOG_LENGTH, DIALOG_WIDTH);
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
            try {
                new DefaultSaveErrorStrategy(this, throwable).saveErrorReportFile();
            } catch (Exception ex) {
                new UntrustedAppletSaveErrorStrategy(this, throwable).saveErrorReportFile();
            }
        }
    }
}