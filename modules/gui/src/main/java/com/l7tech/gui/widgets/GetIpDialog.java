/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.gui.widgets;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.*;

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
    private JTextField ipRangeTextField = null;
    private JButton okButton;
    private boolean validateIPFormat = true;
    private JLabel mainLabel;
    private Functions.Unary<Boolean, String> contextVariableValidator; // to check if a URL contains context variable

    public GetIpDialog(Frame owner, String title) {
        super(owner, title, true);
        init();
    }

    public GetIpDialog(Dialog owner, String title) {
        super(owner, title, true);
        init();
    }

    /**
     * Create modal dialog using the title "Add IP Address".
     *
     * @param owner the <code>Frame</code> from which the dialog is displayed
     */
    public GetIpDialog(Frame owner) {
        this(owner, ADD_IP);
    }

    /**
     * Create modal dialog using the title "Add IP Address".
     *
     * @param owner the <code>Frame</code> from which the dialog is displayed
     */
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
              new GridBagConstraints(0, 0, 3, 1, 0.0, 0.0,
                                     GridBagConstraints.NORTHWEST,
                                     GridBagConstraints.HORIZONTAL,
                                     new Insets(5, 5, 5, 5), 0, 0));
        p.add(getIpRangeTextField(),
              new GridBagConstraints(0, 1, 3, 1, 0.0, 0.0,
                                     GridBagConstraints.NORTHWEST,
                                     GridBagConstraints.HORIZONTAL,
                                     new Insets(0, 5, 0, 5), 0, 0));

        p.add(Box.createGlue(),
                new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0,
                                       GridBagConstraints.NORTHWEST,
                                       GridBagConstraints.BOTH,
                                       new Insets(0, 0, 0, 0), 0, 0));

        okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        Action buttonAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == okButton) {
                    String s = getIpRangeTextField().getText();
                    if (isValidIp(s)) {
                        retval = s;
                    } else {
                        DialogDisplayer.showMessageDialog(GetIpDialog.this, "Please enter a valid address.", null);
                        return;
                    }
                }
                GetIpDialog.this.dispose();
            }
        };
        okButton.addActionListener(buttonAction);
        cancelButton.addActionListener(buttonAction);
        p.add(Box.createGlue(),
              new GridBagConstraints(0, 3, 1, 1, 1.0, 1.0,
                                     GridBagConstraints.EAST,
                                     GridBagConstraints.HORIZONTAL,
                                     new Insets(0, 20, 0, 0), 0, 0));
        Utilities.equalizeButtonSizes(okButton, cancelButton);
        p.add(okButton,
              new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                                     GridBagConstraints.SOUTHEAST,
                                     GridBagConstraints.NONE,
                                     new Insets(5, 0, 5, 0), 0, 0));
        p.add(cancelButton,
              new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
                                     GridBagConstraints.SOUTHEAST,
                                     GridBagConstraints.NONE,
                                     new Insets(5, 5, 5, 5), 0, 0));
        Utilities.runActionOnEscapeKey(getIpRangeTextField(), buttonAction);
        getRootPane().setDefaultButton(okButton);
        pack();
    }

    private static final Pattern ipv4Pattern = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)$");
    private static final Pattern badIpChars = Pattern.compile("[^0-9a-fA-F.:]");

    /**
     * Check if the provided string is a valid IP address or URL based on the validateIPFormat member flag.
     * If invalid IP, attempts to determine the bad character range and accordingly squiggly-set the text field.
     *
     * @param s the string to examine.  Required.
     * @return true if the the provided string is a valid IP or URL, false otherwise
     */
    private boolean isValidIp(String s) {
        if (!validateIPFormat) {
            // Check if it is a valid URL.
            return validateUrl(s); 
        }

        Pair<Integer,Integer> badRange = getBadRange(s);
        return badRange == null;
    }

    /**
     * Attempt to determine the bad character range that makes the provided string an invalid IP address.
     *
     * @param s the string to examine.  Required.
     * @return null if the provided string is a valid IP address,
     *         the start and end positions of the detected invalid range, or
     *         Pair<-1,-1> if the invalid range could not be determined.
     */
    private Pair<Integer, Integer> getBadRange(String s) {
        if ( InetAddressUtil.isValidIpv4Address(s) || InetAddressUtil.isValidIpv6Address(s) ) {
            return null;
        }

        Matcher matcher4 = ipv4Pattern.matcher(s);
        if (matcher4.matches()) {
            // looks like a IPv4 address
            int start = -1;
            int end = -1;
            for (int i = 0; i < 4; ++i) {
                final String matched = matcher4.group(i + 1);
                start = matcher4.start(i + 1);
                end = matcher4.end(i + 1);
                try {
                    int c = Integer.parseInt(matched);
                    if (c < 0 || c > 255)
                        break;
                } catch (NumberFormatException nfe) {
                    break;
                }
            }
            return new Pair<Integer, Integer>(start, end);
        } else {
            // assume IPv6 format
            int start = -1;
            int end = -1;
            boolean wasEmpty = false;
            String[] nibbles = s.split(":");
            for(int i = 0; i < nibbles.length; i++) {
                start = end + 1;
                end += nibbles[i].length() + 1;
                if ( i < 8 && isValidIpv6Nibbles(nibbles[i], ! wasEmpty) || (i == nibbles.length-1 && InetAddressUtil.isValidIpv4Address(nibbles[i])) ) {
                    wasEmpty |= nibbles[i].length() == 0;
                    continue;
                }
                break;
            }
            return new Pair<Integer, Integer>(start, end);
        }
    }

    private boolean isValidIpv6Nibbles(String nibble, boolean allowEmpty) {
        if (nibble == null) return false;
        if (nibble.length() == 0) return allowEmpty;
        if (nibble.length() > 4) return false;
        for(char c : nibble.toCharArray()) {
            if (Character.digit(c, 16) == -1) return false;
        }
        return true;
    }

    public void setContextVariableValidator(Functions.Unary<Boolean, String> contextVariableValidator) {
        this.contextVariableValidator = contextVariableValidator;
    }

    /**
     * Check if a URL is valid or not.
     * @param s: the string of the URL
     * @return true if the URL contains context variable or is a valid URL.
     */
    private boolean validateUrl(String s) {
        if (s == null || s.isEmpty()) return false;

        // If the URL contains context variable, we just can't check semantic
        if (contextVariableValidator != null) {
            boolean hasContextVariable = contextVariableValidator.call(s);
            if (hasContextVariable) return true;
        }

        // The URL doesn't contain any context variables and check if the url is valid or not.
        return ValidationUtils.isValidUrl(s, false, CollectionUtils.caseInsensitiveSet( "http", "https" ));
    }

    private JTextField getIpRangeTextField() {
        if (ipRangeTextField == null) {
            ipRangeTextField = new JTextField();
            // Block bad inserts immediately
            ipRangeTextField.getDocument().addUndoableEditListener(new UndoableEditListener() {
                @Override
                public void undoableEditHappened(UndoableEditEvent e) {
                    if (validateIPFormat) {
                        if (badIpChars.matcher(ipRangeTextField.getText()).find())
                            e.getEdit().undo();
                    }
                }
            });
        }
        return ipRangeTextField;
    }

    public void noValidateFormat() {
        validateIPFormat = false;
        mainLabel.setText("URL");
    }

    /**
     * Sets the dialog's text field.  It will validate input text value.
     *
     * @param text  Text to be set into the text field.
     */
    public void setTextField(String text) {
        ipRangeTextField.setText(text);
    }
}
