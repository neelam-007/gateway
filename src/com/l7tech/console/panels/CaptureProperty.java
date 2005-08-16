/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 16, 2005<br/>
 */
package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Simple dialog to capture a property (key+value)
 *
 * @author flascelles@layer7-tech.com
 */
public class CaptureProperty extends JDialog {
    private JPanel mainPanel;
    private JTextField valueField;
    private JTextField keyField;
    private JButton cancelButton;
    private JButton okButton;

    private String initialKey;
    private String initialValue;
    private String title;
    private boolean oked = false;

    public CaptureProperty(JDialog parent, String title, String initialKey, String initialValue) {
        super(parent, true);
        this.title = title;
        this.initialKey = initialKey;
        this.initialValue = initialValue;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle(title);
        keyField.setText(initialKey);
        valueField.setText(initialValue);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (newKey() == null || newKey().length() < 1 || newValue() == null || newValue().length() < 1) {
                    JOptionPane.showMessageDialog(CaptureProperty.this,
                                                  "Key and Value cannot be empty",
                                                  "Invalid Property Key or Value",
                                                  JOptionPane.ERROR_MESSAGE);
                } else {
                    oked = true;
                    dispose();
                }
            }
        });
    }

    public boolean wasOked() {
        return oked;
    }

    public String newKey() {
        return keyField.getText();
    }

    public String newValue() {
        return valueField.getText();
    }
}
