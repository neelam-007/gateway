package com.l7tech.console.panels;

import com.l7tech.policy.assertion.HttpPassthroughRule;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Dialog to describe an http header or parameter to pass through the securespan gateway
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 5, 2007<br/>
 */
public class HttpRuleDialog extends JDialog {
    private String subjectPrefix;
    private boolean wasOKed = false;
    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton okButton;
    private JTextField valueField;
    private JTextField nameField;
    private JLabel nameLabel;
    private JRadioButton customRadio;
    private JRadioButton originalRadio;
    private HttpPassthroughRule data;

    public HttpRuleDialog(String subjectprefix, JDialog owner, HttpPassthroughRule data) {
        super(owner, true);
        this.subjectPrefix = subjectprefix;
        this.data = data;
        if (data == null) {
            this.data = new HttpPassthroughRule();
        }
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle("Custom " + subjectPrefix + " Setting");
        nameLabel.setText(subjectPrefix + " Name:");
        originalRadio.setSelected(true);

        ActionListener modecheck = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enableValueFieldAsAppropriate();
            }
        };
        originalRadio.addActionListener(modecheck);
        customRadio.addActionListener(modecheck);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        nameField.setText(data.getName());
        valueField.setText(data.getCustomizeValue());
        if (data.isUsesCustomizedValue()) {
            customRadio.setSelected(true);
        } else {
            originalRadio.setSelected(true);
        }

        enableValueFieldAsAppropriate();
    }

    private void cancel() {
        dispose();
    }

    public HttpPassthroughRule getData() {
        return data;
    }

    public boolean wasOKed() {
        return wasOKed;
    }

    private void ok() {
        wasOKed = true;
        // save data
        String tmp = nameField.getText();
        if (tmp != null) tmp = tmp.trim();
        data.setName(tmp);
        if (customRadio.isSelected()) {
            data.setUsesCustomizedValue(true);
        } else {
            data.setUsesCustomizedValue(false);
        }
        tmp = valueField.getText();
        if (tmp != null) tmp = tmp.trim();
        data.setCustomizeValue(tmp);
        cancel();
    }
    
    private void enableValueFieldAsAppropriate() {
        if (originalRadio.isSelected()) {
            valueField.setEnabled(false);
        } else {
            valueField.setEnabled(true);
        }
    }
}
