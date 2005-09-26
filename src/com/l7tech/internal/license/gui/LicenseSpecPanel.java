/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.internal.license.gui;

import com.l7tech.internal.license.LicenseSpec;
import com.l7tech.common.util.ISO8601Date;
import com.l7tech.common.util.Background;
import com.l7tech.common.BuildInfo;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.util.*;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.text.ParseException;
import java.security.SecureRandom;

/**
 * Panel for editing a LicenseSpec in the internal L7 license builder GUI.
 */
public class LicenseSpecPanel extends JPanel {
    /**
     * Property whose PropertyChangeEvent signals that a field has been edited.  Call getSpec() to pick up the changes.
     */
    public static final String PROPERTY_LICENSE_SPEC = "licenseSpec";
    public static final Color BAD_FIELD_BG = new Color(255, 200, 200);

    /**
     * We fire an update when they've made a change then been idle for two seconds.
     * We wait this long because firing an update can cause the cursor to move to the end of the text field.
     */
    private static final long INACTIVITY_UPDATE_MILLIS = 2000;


    private JPanel rootPanel;
    private JTextField idField;
    private JButton randomIdButton;
    private JTextField descriptionField;
    private JTextField licenseeEmailField;
    private JTextField licenseeNameField;
    private JTextField startField;
    private JButton startTodayButton;
    private JTextField expiryField;
    private JButton expireNextYearButton;
    private JTextField productField;
    private JButton currentProductButton;
    private JButton anyProductButton;
    private JTextField majorVersionField;
    private JButton currentMajorVersionButton;
    private JButton anyMajorVersionButton;
    private JTextField minorVersionField;
    private JButton currentMinorVersionButton;
    private JButton anyMinorVersionButton;
    private JTextField hostField;
    private JButton anyHostButton;
    private JTextField ipField;
    private JButton anyIpButton;

    private JTextField defaultTextField = new JTextField();
    private FocusListener focusListener;
    private DocumentListener documentListener;
    private Map oldFieldValues = new HashMap();
    private Random random = new SecureRandom();

    private boolean fieldChanged = false;
    private long fieldChangedWhen = 0;

    public LicenseSpecPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(rootPanel);
        init();
    }

    private void init() {
        idField.addFocusListener(getFocusListener());
        descriptionField.addFocusListener(getFocusListener());
        licenseeEmailField.addFocusListener(getFocusListener());
        licenseeNameField.addFocusListener(getFocusListener());
        startField.addFocusListener(getFocusListener());
        expiryField.addFocusListener(getFocusListener());
        productField.addFocusListener(getFocusListener());
        majorVersionField.addFocusListener(getFocusListener());
        minorVersionField.addFocusListener(getFocusListener());
        hostField.addFocusListener(getFocusListener());
        ipField.addFocusListener(getFocusListener());

        idField.getDocument().addDocumentListener(getDocumentListener());
        descriptionField.getDocument().addDocumentListener(getDocumentListener());
        licenseeEmailField.getDocument().addDocumentListener(getDocumentListener());
        licenseeNameField.getDocument().addDocumentListener(getDocumentListener());
        startField.getDocument().addDocumentListener(getDocumentListener());
        expiryField.getDocument().addDocumentListener(getDocumentListener());
        productField.getDocument().addDocumentListener(getDocumentListener());
        majorVersionField.getDocument().addDocumentListener(getDocumentListener());
        minorVersionField.getDocument().addDocumentListener(getDocumentListener());
        hostField.getDocument().addDocumentListener(getDocumentListener());
        ipField.getDocument().addDocumentListener(getDocumentListener());

        Background.schedule(new TimerTask() {
            public void run() {
                if (!fieldChanged) return;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (!fieldChanged)
                            return;
                        final long now = System.currentTimeMillis();
                        if (now - fieldChangedWhen < INACTIVITY_UPDATE_MILLIS)
                            return;
                        checkForChangedField();
                    }
                });
            }
        }, 500, 500);

        randomIdButton.addActionListener(new RandomIdAction());
        startTodayButton.addActionListener(new StartTodayAction());
        expireNextYearButton.addActionListener(new ExpireNextYearAction());
        currentProductButton.addActionListener(new CurrentProductAction());
        anyProductButton.addActionListener(blankFieldAction(productField));
        currentMajorVersionButton.addActionListener(new CurrentMajorVersionAction());
        anyMajorVersionButton.addActionListener(blankFieldAction(majorVersionField));
        currentMinorVersionButton.addActionListener(new CurrentMinorVersionAction());
        anyMinorVersionButton.addActionListener(blankFieldAction(minorVersionField));
        anyHostButton.addActionListener(blankFieldAction(hostField));
        anyIpButton.addActionListener(blankFieldAction(ipField));

        setSpec(null);
    }

    private void checkForChangedField() {
        if (!fieldChanged)
            return;
        fieldChangedWhen = System.currentTimeMillis();
        fireUpdate();
        fieldChanged = false;
    }

    private DocumentListener getDocumentListener() {
        if (documentListener != null) return documentListener;
        DocumentListener listener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                changeCheck();
            }

            public void removeUpdate(DocumentEvent e) {
                changeCheck();
            }

            public void changedUpdate(DocumentEvent e) {
                changeCheck();
            }

            private void changeCheck() {
                fieldChanged = true;
                fieldChangedWhen = System.currentTimeMillis();
            }
        };
        return documentListener = listener;
    }

    private ActionListener blankFieldAction(final JTextField field) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                field.setText("");
                fireUpdate();
            }
        };
    }

    /** Initialize all fields with useful default values, except licensee name which has to be set. */
    public void setDefaults() {
        setSpec(new LicenseSpec());
        new RandomIdAction().actionPerformed(null);
        new StartTodayAction().actionPerformed(null);
        new ExpireNextYearAction().actionPerformed(null);
        new CurrentProductAction().actionPerformed(null);
        new CurrentMajorVersionAction().actionPerformed(null);
    }

    private FocusListener getFocusListener() {
        if (focusListener != null) return focusListener;
        FocusListener listener = new FocusListener() {
            public void focusGained(FocusEvent e) {}
            public void focusLost(FocusEvent e) {
                Component c = e.getComponent();
                // See if this is one of our text fields changing.  If so, we'll need to fire an update
                if (c instanceof JTextField) {
                    // Heh, this is such a smelly hack.  I'm so proud of it
                    JTextField field = (JTextField)c;
                    String currentValue = field.getText();
                    String oldValue = (String)oldFieldValues.get(field);
                    if (oldValue == null || !(oldValue.equals(currentValue))) {
                        oldFieldValues.put(field, currentValue);
                        fireUpdate();
                    }
                }
            }
        };
        return focusListener = listener;
    }

    private void fireUpdate() {
        firePropertyChange(PROPERTY_LICENSE_SPEC, null, null);
    }

    /** Updates the view to correspond with this license spec. */
    public void setSpec(LicenseSpec spec) {
        fieldChanged = false;
        if (spec == null) spec = new LicenseSpec();
        setText(idField, tt(spec.getLicenseId()));
        setText(descriptionField, tt(spec.getDescription()));
        setText(licenseeEmailField, tt(spec.getLicenseeContactEmail()));
        setText(licenseeNameField, tt(spec.getLicenseeName()));

        setText(startField, tt(spec.getStartDate()));
        setText(expiryField, tt(spec.getExpiryDate()));

        setText(productField, tt(spec.getProduct()));
        setText(majorVersionField, tt(spec.getVersionMajor()));
        setText(minorVersionField, tt(spec.getVersionMinor()));

        setText(hostField, tt(spec.getHostname()));
        setText(ipField, tt(spec.getIp()));
        fieldChanged = false;
        getSpec(); // update all field colors
    }

    private void setText(JTextField field, String val) {
        field.setText(val);
        oldFieldValues.put(field, val);
    }

    /**
     * Read the view and build a LicenseSpec out of it.
     */
    public LicenseSpec getSpec() {
        LicenseSpec spec = new LicenseSpec();
        spec.setLicenseId(ftid(idField));
        spec.setDescription(fts(descriptionField));
        spec.setLicenseeName(ftsReq(licenseeNameField));
        spec.setLicenseeContactEmail(fts(licenseeEmailField));
        spec.setStartDate(ftd(startField));
        spec.setExpiryDate(ftd(expiryField));
        spec.setProduct(fts(productField));
        spec.setVersionMajor(fts(majorVersionField));
        spec.setVersionMinor(fts(minorVersionField));
        spec.setHostname(fts(hostField));
        spec.setIp(fts(ipField));
        return spec;
    }

    /** from text to date. */
    private Date ftd(JTextField f) {
        boolean good = true;
        try {
            String s = f.getText();
            if (s == null || s.length() < 1) return null;
            final Date date = ISO8601Date.parse(s);
            if (date.getTime() == 0 || date.getTime() == -1) {
                good = false; // flag this invalid date in red
                return null;
            }
            return date;
        } catch (ParseException e) {
            good = false;
            return null;
        } finally {
            f.setBackground(good ? defaultTextField.getBackground() : BAD_FIELD_BG);
        }
    }

    /** from text to string. */
    private String fts(JTextField f) {
        String s = f.getText();
        return s == null || s.trim().length() < 1 ? null : s.trim();
    }

    /** from text to required string. */
    private String ftsReq(JTextField f) {
        boolean good = false;
        try {
            String s = f.getText();
            if (s == null || s.trim().length() < 1) {
                return null;
            } else {
                good = true;
                return s.trim();
            }
        } finally {
            f.setBackground(good ? defaultTextField.getBackground() : BAD_FIELD_BG);
        }
    }

    /** from text to license id. */
    private long ftid(JTextField f) {
        boolean good = false;
        try {
            final String s = f.getText();
            if (s == null || s.length() < 1)
                return 0;
            final long n = Long.parseLong(s);
            if (n < 1)
                return 0;
            good = true;
            return n;
        } catch (NumberFormatException nfe) {
            return 0;
        } finally {
            f.setBackground(good ? defaultTextField.getBackground() : BAD_FIELD_BG);
        }
    }

    /** to text from date. */
    private String tt(Date d) {
        return d == null ? "" : ISO8601Date.format(d);
    }

    /** to text from long. */
    private String tt(long n) {
        return n == 0 ? "" : String.valueOf(n);
    }

    /** to text from string. */
    private String tt(String s) {
        return s == null || "*".equals(s) ? "" : s;
    }

    private class RandomIdAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            long rand;
            do {
                rand = Math.abs(random.nextLong());
            } while (rand == 0); // reroll zeros
            idField.setText(String.valueOf(rand));
            fireUpdate();
        }
    }

    private class StartTodayAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            startField.setText(ISO8601Date.format(new Date()));
            fireUpdate();
        }
    }

    private class ExpireNextYearAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR, 1);
            expiryField.setText(ISO8601Date.format(cal.getTime()));
            fireUpdate();
        }
    }

    private class CurrentProductAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            productField.setText(BuildInfo.getProductName());
            fireUpdate();
        }
    }

    private class CurrentMajorVersionAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            majorVersionField.setText(BuildInfo.getProductVersionMajor());
            fireUpdate();
        }
    }

    private class CurrentMinorVersionAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            minorVersionField.setText(BuildInfo.getProductVersionMinor());
            fireUpdate();
        }
    }
}
