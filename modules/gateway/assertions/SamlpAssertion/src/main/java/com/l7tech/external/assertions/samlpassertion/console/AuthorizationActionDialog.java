package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * Dialog for changing a password.
 *
 * <p>This dialog is modal and by default disposes of itself when closed.</p>
 *
 * @author Steve Jones
 */
public class AuthorizationActionDialog extends JDialog {

    //- PUBLIC

    /**
     * Create a password change dialog with the given values.
     *
     * @param owner             The owner for the dialog
     * @param action            Action value (may be null)
     * @param namespace         Namespace value (may be null)
     */
    public AuthorizationActionDialog(Frame owner, String action, String namespace) {
        super(owner, TITLE, true);
        this.action = action;
        this.actionNamespace = namespace;
        initComponents();
    }

    /**
     * Create a password change dialog with the given values.
     *
     * @param owner             The owner for the dialog
     * @param action            Action value (may be null)
     * @param namespace         Namespace value (may be null)
     */
    public AuthorizationActionDialog(Dialog owner, String action, String namespace) {
        super(owner, TITLE, true);
        this.action = action;
        this.actionNamespace = namespace;
        initComponents();
    }

    public String getAction() {
        return action;
    }

    public String getActionNamespace() {
        return actionNamespace;
    }

    /**
     * Check if the dialog was OK'd
     *
     * @return true if OK was selected.
     */
    public boolean wasOk() {
        return ok;
    }

    public boolean isCancel() {
        return cancel;
    }

    //- PRIVATE

    private static final String TITLE = "Authorization Action and Namespace";

    private JTextField actionText;
    private JTextField actionNamespaceText;
    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;

    private String action;
    private String actionNamespace;
    private boolean ok;
    private boolean cancel;

    private void initComponents() {
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setResizable(false);
        this.add(mainPanel);
        this.actionText.setText(action);
        this.actionNamespaceText.setText(actionNamespace);

        ok = false;
        cancel = false;

        RunOnChangeListener rocl = new RunOnChangeListener(new Runnable(){
            public void run() {
                updateButtons();
            }
        });

        // register Esc keystroke
        mainPanel.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        actionText.getDocument().addDocumentListener(rocl);
        actionNamespaceText.getDocument().addDocumentListener(rocl);

        okButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                action = actionText.getText();
                actionNamespace = actionNamespaceText.getText();
                ok = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });
        getRootPane().setDefaultButton(okButton);
        updateButtons();
        this.pack();
        Utilities.centerOnScreen(this);
    }

    private void updateButtons() {
        okButton.setEnabled( actionText.getText() != null && actionText.getText().length() > 0 );
    }

    private void onCancel() {
        cancel = true;
        dispose();
    }
}