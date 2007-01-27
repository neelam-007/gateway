package com.l7tech.console.panels;

import com.l7tech.common.gui.NumberField;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.HexUtils;
import com.l7tech.policy.assertion.RateLimitAssertion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Properties for RateLimitAssertion.
 */
public class RateLimitAssertionPropertiesDialog extends JDialog implements ActionListener {
    private static final int DEFAULT_CONCURRENCY_LIMIT = 10;

    private JPanel topPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField counterNameField;
    private JTextField maxRequestsPerSecondField;
    private JRadioButton shapingOffRb;
    private JRadioButton shapingOnRb;
    private JRadioButton concurrencyLimitOffRb;
    private JRadioButton concurrencyLimitOnRb;
    private JTextField concurrencyLimitField;
    private JCheckBox burstTrafficCb;
    private JComboBox counterCb;

    private boolean confirmed = false;
    private String uuid[] = { makeUuid() };
    private String expr = "";

    private static final String PRESET_DEFAULT = "User or client IP";
    private static final String PRESET_GLOBAL = "Gateway node";
    private static final String PRESET_CUSTOM = "Custom:";
    private static final Map<String, String> counterNameTypes = new LinkedHashMap<String, String>() {{
        put(PRESET_DEFAULT, "${request.clientid}");
        put("Authenticated user", "${request.authenticateduser}");
        put("Client IP", "${request.tcp.remoteip}");
        put("SOAP operation", "${request.soap.operationname}");
        put("SOAP namespace", "${request.soap.namespace}");
        put(PRESET_GLOBAL, "");
        put(PRESET_CUSTOM, makeUuid());
    }};
    private static final Pattern presetFinder = Pattern.compile("^PRESET\\(([a-fA-F0-9]{16})\\)(.*)$");
    private static final Pattern defaultCustomExprFinder = Pattern.compile("^([a-fA-F0-9]{8})-?(.*)$");

    private static String makeDefaultCustomExpr(String uuid, String expr) {
        return (uuid != null ? uuid : makeUuid()).substring(8) + (expr == null || expr.length() < 1 ? "" : "-" + expr);
    }

    private static boolean isDefaultCustomExpr(String rawExpr) {
        Matcher matcher = defaultCustomExprFinder.matcher(rawExpr);
        if (!matcher.matches())
            return false;
        String expr = matcher.group(2);
        return counterNameTypes.containsValue(expr);
    }

    /**
     * Generate a new counter name corresponding to the default preset (clientid).
     * @return a counter name similar to "PRESET(deadbeefcafebabe)${request.clientid}".  Never null or empty.
     */
    public static String makeDefaultCounterName() {
        return findRawCounterName(PRESET_DEFAULT, makeUuid(), null);
    }

    /**
     * See if the specified raw counter name happens to match any of the user friendly presets we provide.
     * If a counter key is matched, this updates {@link #uuid} as a side effect.
     *
     * @param rawCounterName  raw counter name ot look up, ie "foo bar blatz ${mumble}"
     * @param uuidOut  An optional single-element String array to receive the UUID.
     *                 Any UUID found will be copied into the first element of this array, if present.
     * @return the key in counterNameTypes corresponding to the given raw counter name, or null for {@link #PRESET_CUSTOM}.
     * for example given "PRESET(deadbeefcafebabe)${request.clientid}" this will return "User or client IP";
     * when given        "PRESET(abcdefabcdefabcd)" this will return "Gateway node (global)"; and
     * when given        "RateLimit-${request.clientid}" this will return null.
     */
    public static String findCounterNameKey(String rawCounterName, String[] uuidOut) {
        String foundKey = null;
        String foundUuid = null;

        Matcher matcher = presetFinder.matcher(rawCounterName);
        if (matcher.matches()) {
            String expr = matcher.group(2);
            if ((foundKey = findKeyForValue(counterNameTypes, expr)) != null)
                foundUuid = matcher.group(1);
        } else {
            matcher = defaultCustomExprFinder.matcher(rawCounterName);
            if (matcher.matches()) {
                String expr = matcher.group(2);
                if ((foundKey = findKeyForValue(counterNameTypes, expr)) != null)
                    foundUuid = makeUuid().substring(8) + matcher.group(1);
            }
        }

        if (foundUuid != null && uuidOut != null && uuidOut.length > 0) uuidOut[0] = foundUuid;
        return PRESET_CUSTOM.equals(foundKey) ? null : foundKey;
    }

    private static <K, V> K findKeyForValue(Map<K, V> map, V value) {
        for (Map.Entry<K, V> entry : map.entrySet())
            if (entry.getValue().equals(value))
                return entry.getKey();
        return null;
    }

    /**
     * Create the raw counter name to save in the assertion instance given the specified counterNameKey.
     *
     * @param counterNameKey a counter name key from {@link #counterNameTypes}, or null to use {@link #PRESET_CUSTOM}.
     * @param uuid  the UUID to use to create unique counters.  Should be an 8-digit hex string.  Mustn't be null.
     * @param customExpr the custom string to use if counterNameKey is CUSTOM or null or can't be found in the map.
     *                    May be empty.  May only be null if counterNameKey is in the map and isn't CUSTOM.
     * @return the raw counter name to store into the assertion.  Never null or otherwise invalid.
     */
    public static String findRawCounterName(String counterNameKey, String uuid, String customExpr) {
        // If it claims to be custom, but looks just like a generated example, use whatever preset it was generated from
        if (counterNameKey == null || PRESET_CUSTOM.equals(counterNameKey))
            if (isDefaultCustomExpr(customExpr))
                counterNameKey = findCounterNameKey(customExpr, null);

        String presetExpr = counterNameKey == null ? null : counterNameTypes.get(counterNameKey);
        if (presetExpr == null || PRESET_CUSTOM.equals(counterNameKey))
            return customExpr;
        return "PRESET(" + uuid + ")" + presetExpr;
    }

    private static String makeUuid() {
        Random random = new Random();
        byte[] bytes = new byte[8];
        random.nextBytes(bytes);
        return HexUtils.hexDump(bytes);
    }

    public RateLimitAssertionPropertiesDialog(Frame owner, RateLimitAssertion rla) throws HeadlessException {
        super(owner, true);
        initialize(rla);
    }

    public RateLimitAssertionPropertiesDialog(Dialog owner, RateLimitAssertion rla) throws HeadlessException {
        super(owner, true);
        initialize(rla);
    }

    private void initialize(RateLimitAssertion rla) {
        setTitle("Rate Limit Properties");
        setContentPane(topPanel);

        okButton.addActionListener(this);
        cancelButton.addActionListener(this);

        maxRequestsPerSecondField.setDocument(new NumberField(8));
        concurrencyLimitField.setDocument(new NumberField(8));
        concurrencyLimitField.setText(String.valueOf(DEFAULT_CONCURRENCY_LIMIT));

        ActionListener concListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateConcurrencyEnableState();
            }
        };
        concurrencyLimitOnRb.addActionListener(concListener);
        concurrencyLimitOffRb.addActionListener(concListener);

        counterCb.setModel(new DefaultComboBoxModel(new Vector<String>(counterNameTypes.keySet())));
        counterCb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateCounterNameEnableState();
            }
        });

        Utilities.enableGrayOnDisabled(concurrencyLimitField);
        Utilities.setEscKeyStrokeDisposes(this);
        getRootPane().setDefaultButton(okButton);
        Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });
        pack();
        setData(rla);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(okButton.getActionCommand())) {
            if (!checkValidity())
                return;
            confirmed = true;
        }
        dispose();
    }

    private void updateConcurrencyEnableState() {
        boolean e = concurrencyLimitOnRb.isSelected();
        concurrencyLimitField.setEnabled(e);
        if (e && getViewConcurrency() < 1) concurrencyLimitField.setText(String.valueOf(DEFAULT_CONCURRENCY_LIMIT));
        if (e) {
            concurrencyLimitField.selectAll();
            concurrencyLimitField.requestFocusInWindow();
        }
    }

    private void updateCounterNameEnableState() {
        String counterNameKey = (String)counterCb.getSelectedItem();
        String nameField = counterNameField.getText().trim();
        if (PRESET_CUSTOM.equals(counterNameKey)) {
            counterNameField.setVisible(true);
            counterNameField.setEnabled(true);
            if (nameField == null || nameField.length() < 1)
                counterNameField.setText(makeDefaultCustomExpr(uuid[0], expr));
            counterNameField.selectAll();
            counterNameField.requestFocusInWindow();
        } else {
            counterNameField.setEnabled(false);
            expr = counterNameTypes.get(counterNameKey);
            if (nameField == null || nameField.length() < 1)
                counterNameField.setVisible(false);
            else if (isDefaultCustomExpr(nameField))
                counterNameField.setText(makeDefaultCustomExpr(uuid[0], expr));
        }
    }

    private int getViewConcurrency() {
        try {
            return Integer.parseInt(concurrencyLimitField.getText());
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    private boolean checkValidity() {
        String err = null;

        if (PRESET_CUSTOM.equals(counterCb.getSelectedItem()) && counterNameField.getText().trim().length() < 1)
            err = "Custom counter name must not be empty.";

        if (getViewConcurrency() < 1)
            err = "Concurrency limit must be positive.";

        if (err != null)
            DialogDisplayer.showMessageDialog(this, err, "Error", JOptionPane.ERROR_MESSAGE, null);

        return null == err;
    }

    public void setData(RateLimitAssertion rla) {
        String rawCounterName = rla.getCounterName();

        /** Freely overwrite the default counter name with a better one. */
        if (new RateLimitAssertion().getCounterName().equals(rawCounterName))
            rawCounterName = findRawCounterName(PRESET_DEFAULT, uuid[0] = makeUuid(), null);

        String cnk = findCounterNameKey(rawCounterName, uuid);
        if (cnk == null) {
            counterCb.setSelectedItem(PRESET_CUSTOM);
            counterNameField.setText(rawCounterName);
        } else {
            counterCb.setSelectedItem(cnk);
            counterNameField.setText("");
        }

        maxRequestsPerSecondField.setText(String.valueOf(rla.getMaxRequestsPerSecond()));

        shapingOnRb.setSelected(rla.isShapeRequests());
        shapingOffRb.setSelected(!rla.isShapeRequests());

        burstTrafficCb.setSelected(!rla.isHardLimit());

        int maxConc = rla.getMaxConcurrency();
        boolean concLimit = maxConc > 0;
        concurrencyLimitOnRb.setSelected(concLimit);
        concurrencyLimitOffRb.setSelected(!concLimit);
        if (concLimit) concurrencyLimitField.setText(String.valueOf(maxConc));
        updateConcurrencyEnableState();
        updateCounterNameEnableState();
    }

    public RateLimitAssertion getData(RateLimitAssertion rla) {
        String counterNameKey = (String)counterCb.getSelectedItem();
        String rawCounterName = findRawCounterName(counterNameKey, uuid[0], counterNameField.getText().trim());
        rla.setCounterName(rawCounterName);
        rla.setMaxRequestsPerSecond(Integer.parseInt(maxRequestsPerSecondField.getText()));
        rla.setShapeRequests(shapingOnRb.isSelected());
        rla.setMaxConcurrency(concurrencyLimitOnRb.isSelected() ? getViewConcurrency() : 0);
        rla.setHardLimit(!burstTrafficCb.isSelected());
        return rla;
    }

    /** @return true if the dialog was dismissed by the user pressing the Ok button. */
    public boolean isConfirmed() {
        return confirmed;
    }
}
