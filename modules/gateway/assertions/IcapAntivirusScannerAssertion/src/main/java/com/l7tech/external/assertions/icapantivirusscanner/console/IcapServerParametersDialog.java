package com.l7tech.external.assertions.icapantivirusscanner.console;

import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * <p>The GUI to add/modify the server parameters.</p>
 * @author Ken Diep
 */
public class IcapServerParametersDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField paramNameField;
    private JTextField paramValueField;
    private JLabel paramNameLabel;
    private JLabel paramValueLabel;

    private boolean confirmed;

    public IcapServerParametersDialog(final Window owner, final String title) {
        super(owner, title);
        initializeComponents();
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setTitle(title);
        confirmed = false;

    }

    private void initializeComponents() {
        Utilities.setEscKeyStrokeDisposes(this);
        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });
    }

    private void onOK() {
        if (paramNameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter valid parameter name.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    /**
     * @return true if the user clicked OK, false otherwise.
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * @return the parameter name.
     */
    public String getParameterName() {
        return paramNameField.getText().trim();
    }

    /**
     * @return the parameter value.
     */
    public String getParameterValue() {
        return paramValueField.getText().trim();
    }

    /**
     * @param parameterName the parameter name to set.
     */
    public void setParameterName(String parameterName) {
        this.paramNameField.setText(parameterName);
    }

    /**
     * @param parameterValue the parameter value to set.
     */
    public void setParameterValue(String parameterValue) {
        this.paramValueField.setText(parameterValue);
    }
}
