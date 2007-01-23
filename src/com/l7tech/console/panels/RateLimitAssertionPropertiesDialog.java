package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.NumberField;
import com.l7tech.policy.assertion.RateLimitAssertion;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

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

    private boolean confirmed = false;

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

        ActionListener updateListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateConcurrencyEnableState();
            }
        };
        concurrencyLimitOnRb.addActionListener(updateListener);
        concurrencyLimitOffRb.addActionListener(updateListener);

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
            concurrencyLimitField.requestFocus();
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

        if (counterNameField.getText().trim().length() < 1)
            err = "Counter name must not be empty.";

        if (getViewConcurrency() < 1)
            err = "Concurrency limit must be positive.";

        if (err != null)
            DialogDisplayer.showMessageDialog(this, err, "Error", JOptionPane.ERROR_MESSAGE, null);

        return null == err;
    }

    public void setData(RateLimitAssertion rla) {
        counterNameField.setText(rla.getCounterName());

        maxRequestsPerSecondField.setText(String.valueOf(rla.getMaxRequestsPerSecond()));

        shapingOnRb.setSelected(rla.isShapeRequests());
        shapingOffRb.setSelected(!rla.isShapeRequests());

        int maxConc = rla.getMaxConcurrency();
        boolean concLimit = maxConc > 0;
        concurrencyLimitOnRb.setSelected(concLimit);
        concurrencyLimitOffRb.setSelected(!concLimit);
        if (concLimit) concurrencyLimitField.setText(String.valueOf(maxConc));
        updateConcurrencyEnableState();
    }

    public RateLimitAssertion getData(RateLimitAssertion rla) {
        rla.setCounterName(counterNameField.getText());
        rla.setMaxRequestsPerSecond(Integer.parseInt(maxRequestsPerSecondField.getText()));
        rla.setShapeRequests(shapingOnRb.isSelected());
        rla.setMaxConcurrency(concurrencyLimitOnRb.isSelected() ? getViewConcurrency() : 0);
        return rla;
    }

    /** @return true if the dialog was dismissed by the user pressing the Ok button. */
    public boolean isConfirmed() {
        return confirmed;
    }
}
