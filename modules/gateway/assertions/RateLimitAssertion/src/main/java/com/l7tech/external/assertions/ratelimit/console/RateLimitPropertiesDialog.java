package com.l7tech.external.assertions.ratelimit.console;

import com.l7tech.gui.NumberField;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.external.assertions.ratelimit.RateLimitAssertion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

/**
 * Properties for RateLimitAssertion.
 */
public class RateLimitPropertiesDialog extends AssertionPropertiesEditorSupport<RateLimitAssertion> implements ActionListener {

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
    private String uuid[] = {RateLimitAssertion.PresetInfo.makeUuid()};
    private String expr = "";


    public RateLimitPropertiesDialog(Window owner, RateLimitAssertion rla) throws HeadlessException {
        super(owner, rla);
        initialize(rla);
    }

    private void initialize(RateLimitAssertion rla) {
        setContentPane(topPanel);

        okButton.addActionListener(this);
        cancelButton.addActionListener(this);

        maxRequestsPerSecondField.setDocument(new NumberField(5, 90000));
        concurrencyLimitField.setDocument(new NumberField(5, 90000));
        concurrencyLimitField.setText(String.valueOf(RateLimitAssertion.PresetInfo.DEFAULT_CONCURRENCY_LIMIT));

        ActionListener concListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateConcurrencyEnableState();
            }
        };
        concurrencyLimitOnRb.addActionListener(concListener);
        concurrencyLimitOffRb.addActionListener(concListener);

        counterCb.setModel(new DefaultComboBoxModel(new Vector<String>(RateLimitAssertion.PresetInfo.counterNameTypes.keySet())));
        counterCb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateCounterNameEnableState();
            }
        });

        Utilities.enableGrayOnDisabled(concurrencyLimitField);
        Utilities.setEscKeyStrokeDisposes(this);
        getRootPane().setDefaultButton(okButton);
        Utilities.equalizeButtonSizes(new JButton[]{okButton, cancelButton});
        pack();
        if (rla != null) setData(rla);
    }

    @Override
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
        if (e && getViewConcurrency() < 1)
            concurrencyLimitField.setText(String.valueOf(RateLimitAssertion.PresetInfo.DEFAULT_CONCURRENCY_LIMIT));
        if (e) {
            concurrencyLimitField.selectAll();
            concurrencyLimitField.requestFocusInWindow();
        }
    }

    private void updateCounterNameEnableState() {
        String counterNameKey = (String)counterCb.getSelectedItem();
        String nameField = counterNameField.getText().trim();
        if (RateLimitAssertion.PresetInfo.PRESET_CUSTOM.equals(counterNameKey)) {
            counterNameField.setVisible(true);
            counterNameField.setEnabled(true);
            if (nameField == null || nameField.length() < 1)
                counterNameField.setText(RateLimitAssertion.PresetInfo.makeDefaultCustomExpr(uuid[0], expr));
            counterNameField.selectAll();
            counterNameField.requestFocusInWindow();
        } else {
            counterNameField.setEnabled(false);
            expr = RateLimitAssertion.PresetInfo.counterNameTypes.get(counterNameKey);
            if (nameField == null || nameField.length() < 1)
                counterNameField.setVisible(false);
            else if (RateLimitAssertion.PresetInfo.isDefaultCustomExpr(nameField))
                counterNameField.setText(RateLimitAssertion.PresetInfo.makeDefaultCustomExpr(uuid[0], expr));
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

        if (RateLimitAssertion.PresetInfo.PRESET_CUSTOM.equals(counterCb.getSelectedItem()) && counterNameField.getText().trim().length() < 1)
            err = "Custom rate limiter name must not be empty.";

        if (getViewConcurrency() < 1)
            err = "Concurrency limit must be positive.";

        if (err != null)
            DialogDisplayer.showMessageDialog(this, err, "Error", JOptionPane.ERROR_MESSAGE, null);

        return null == err;
    }

    @Override
    public void setData(RateLimitAssertion rla) {
        String rawCounterName = rla.getCounterName();

        /** Freely overwrite the default counter name with a better one. */
        if (new RateLimitAssertion().getCounterName().equals(rawCounterName))
            rawCounterName = RateLimitAssertion.PresetInfo.findRawCounterName(RateLimitAssertion.PresetInfo.PRESET_DEFAULT, uuid[0] = RateLimitAssertion.PresetInfo.makeUuid(), null);

        String cnk = RateLimitAssertion.PresetInfo.findCounterNameKey(rawCounterName, uuid);
        if (cnk == null) {
            counterCb.setSelectedItem(RateLimitAssertion.PresetInfo.PRESET_CUSTOM);
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

    @Override
    public RateLimitAssertion getData(RateLimitAssertion rla) {
        String counterNameKey = (String)counterCb.getSelectedItem();
        String rawCounterName = RateLimitAssertion.PresetInfo.findRawCounterName(counterNameKey, uuid[0], counterNameField.getText().trim());
        rla.setCounterName(rawCounterName);
        rla.setMaxRequestsPerSecond(Integer.parseInt(maxRequestsPerSecondField.getText()));
        rla.setShapeRequests(shapingOnRb.isSelected());
        rla.setMaxConcurrency(concurrencyLimitOnRb.isSelected() ? getViewConcurrency() : 0);
        rla.setHardLimit(!burstTrafficCb.isSelected());
        return rla;
    }

    /**
     * @return true if the dialog was dismissed by the user pressing the Ok button.
     */
    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    protected void configureView() {
        okButton.setEnabled( !isReadOnly() );
    }
}
