/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.gui.widgets;

import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A dialog box that prompts the user for a single IP address.
 */
public class GetIpDialog extends JDialog {
    private static final String ADD_IP = "Add IP Address";
    private String retval = null;
    private SquigglyTextField ipRangeTextField = null;
    private JButton okButton;
    private boolean validateIPFormat = true;
    private JLabel mainLabel;

    /** Create modal dialog using the specified title. */
    public GetIpDialog(Frame owner, String title) {
        super(owner, title, true);
        init();
    }

    /** Create modal dialog using the specified title. */
    public GetIpDialog(Dialog owner, String title) {
        super(owner, title, true);
        init();
    }

    /** Create modal dialog using the title "Add IP Address". */
    public GetIpDialog(Frame owner) {
        this(owner, ADD_IP);
    }

    /** Create modal dialog using the title "Add IP Address". */
    public GetIpDialog(Dialog owner) {
        this(owner, ADD_IP);
    }

    /** @return the address that was entered, if Ok button was pressed; else null. */
    public String getAddress() {
        return retval;
    }

    private void init() {
        Container p = getContentPane();
        p.setLayout(new GridBagLayout());
        mainLabel = new JLabel("IP Address: ");
        p.add(mainLabel,
              new GridBagConstraints(0, 0, 3, 1, 1.0, 1.0,
                                     GridBagConstraints.NORTHWEST,
                                     GridBagConstraints.BOTH,
                                     new Insets(5, 5, 5, 5), 0, 0));
        p.add(getIpRangeTextField(),
              new GridBagConstraints(0, 1, 3, 1, 1.0, 1.0,
                                     GridBagConstraints.NORTHWEST,
                                     GridBagConstraints.BOTH,
                                     new Insets(0, 5, 0, 5), 0, 0));

        okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        Action buttonAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == okButton) {
                    String s = getIpRangeTextField().getText();
                    if (isValidIp(s))
                        retval = s;
                }
                GetIpDialog.this.dispose();
            }
        };
        okButton.addActionListener(buttonAction);
        cancelButton.addActionListener(buttonAction);
        p.add(Box.createGlue(),
              new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0,
                                     GridBagConstraints.EAST,
                                     GridBagConstraints.HORIZONTAL,
                                     new Insets(0, 20, 0, 0), 0, 0));
        Utilities.equalizeButtonSizes(new AbstractButton[] { okButton, cancelButton });
        p.add(okButton,
              new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                                     GridBagConstraints.EAST,
                                     GridBagConstraints.NONE,
                                     new Insets(5, 0, 5, 0), 0, 0));
        p.add(cancelButton,
              new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
                                     GridBagConstraints.EAST,
                                     GridBagConstraints.NONE,
                                     new Insets(5, 5, 5, 5), 0, 0));
        Utilities.runActionOnEscapeKey(getIpRangeTextField(), buttonAction);
        getRootPane().setDefaultButton(okButton);
        pack();
        checkValid();
    }

    private static final Pattern ipPattern = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)");
    private static final Pattern badIpChars = Pattern.compile("[^0-9.]");

    /** Check for valid IP, attempting to narrow in on the invalid portion if it isn't valid. */
    private boolean isValidIp(String s) {
        if (!validateIPFormat) {
            getIpRangeTextField().setNone();
            return true;// we dont care
        }
        boolean ret = false;
        int pos = -1;
        int end = -1;
        try {
            Matcher matcher = ipPattern.matcher(s);
            if (!matcher.matches())
                return false;

            for (int i = 0; i < 4; ++i) {
                final String matched = matcher.group(i + 1);
                pos = matcher.start(i + 1);
                end = matcher.end(i + 1);
                int c = Integer.parseInt(matched);
                if (c < 0 || c > 255)
                    return false;
            }

            getIpRangeTextField().setNone();
            ret = true;
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        } finally {
            if (ret) {
                getIpRangeTextField().setNone();
            } else {
                if (pos < 0)
                    getIpRangeTextField().setAll();
                else
                    getIpRangeTextField().setRange(pos, end);
            }
        }
    }

    private void checkValid() {
        if (validateIPFormat) {
            okButton.setEnabled(isValidIp(getIpRangeTextField().getText()));
        } else {
            okButton.setEnabled(getIpRangeTextField().getText() != null && getIpRangeTextField().getText().length() > 0);
        }
    }

    private SquigglyTextField getIpRangeTextField() {
        if (ipRangeTextField == null) {
            ipRangeTextField = new SquigglyTextField();
            // Block bad inserts immediately
            ipRangeTextField.getDocument().addUndoableEditListener(new UndoableEditListener() {
                public void undoableEditHappened(UndoableEditEvent e) {
                    if (validateIPFormat) {
                        if (badIpChars.matcher(ipRangeTextField.getText()).find())
                            e.getEdit().undo();
                    }
                    checkValid();
                }
            });
        }
        return ipRangeTextField;
    }

    public void noValidateFormat() {
        validateIPFormat = false;
        mainLabel.setText("URL");
    }
}
