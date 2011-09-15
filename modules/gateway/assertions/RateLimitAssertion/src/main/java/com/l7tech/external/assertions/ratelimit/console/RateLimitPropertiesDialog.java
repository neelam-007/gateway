package com.l7tech.external.assertions.ratelimit.console;

import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.external.assertions.ratelimit.RateLimitAssertion;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.variable.Syntax;

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
    private JTextField burstWindowSizeField;
    private JComboBox counterCb;
    private JCheckBox blackoutForCheckBox;
    private JTextField blackoutSecField;
    private JCheckBox splitConcurrencyAcrossNodes;
    private JCheckBox splitRateAcrossNodes;

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

        maxRequestsPerSecondField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableOrDisableOkButton();
            }
        }));

        concurrencyLimitField.setText(String.valueOf(RateLimitAssertion.PresetInfo.DEFAULT_CONCURRENCY_LIMIT));

        ActionListener concListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String errorMsg = RateLimitAssertion.validateMaxConcurrency(maxRequestsPerSecondField.getText());
                if (errorMsg != null) {
                    DialogDisplayer.showMessageDialog(RateLimitPropertiesDialog.this, errorMsg, "Invalid value", JOptionPane.ERROR_MESSAGE, null);
                } else {
                    updateConcurrencyEnableState();
                }
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

        final ActionListener updateBlackoutAndWindowListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateBlackoutAndWindowSizeFieldEnableState();
            }
        };

        burstWindowSizeField.setText("1");
        Utilities.enableGrayOnDisabled(burstWindowSizeField);
        burstTrafficCb.addActionListener(updateBlackoutAndWindowListener);

        blackoutSecField.setText("1");
        Utilities.enableGrayOnDisabled(blackoutSecField);
        blackoutForCheckBox.addActionListener(updateBlackoutAndWindowListener);

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
        boolean selected = concurrencyLimitOnRb.isSelected();
        concurrencyLimitField.setEnabled(selected);
        splitConcurrencyAcrossNodes.setEnabled(selected);

        final String stringConcurrency = concurrencyLimitField.getText();
        final String[] referencedVars = Syntax.getReferencedNames(stringConcurrency);
        int concurrency = 0;
        if(referencedVars.length == 0){
            try {
                concurrency = Integer.parseInt(stringConcurrency);
            } catch (NumberFormatException e) {
                concurrency = 0;//text field may have an invalid value, just reset to the default
            }
        }

        if (selected && concurrency < 1 && referencedVars.length == 0) //set to 10 if value is enabled for the first time e.g. it's previous value was 0, which is the default
            concurrencyLimitField.setText(String.valueOf(RateLimitAssertion.PresetInfo.DEFAULT_CONCURRENCY_LIMIT));
        if (selected) {
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

    private void updateBlackoutAndWindowSizeFieldEnableState() {
        burstWindowSizeField.setEnabled(burstTrafficCb.isSelected());
        blackoutSecField.setEnabled(blackoutForCheckBox.isSelected());
    }

    private boolean checkValidity() {
        String err = null;

        if (RateLimitAssertion.PresetInfo.PRESET_CUSTOM.equals(counterCb.getSelectedItem()) && counterNameField.getText().trim().length() < 1)
            err = "Custom rate limiter name must not be empty.";

        if(err == null && concurrencyLimitOnRb.isSelected()) err = RateLimitAssertion.validateMaxConcurrency(concurrencyLimitField.getText());
        if(err == null) err = RateLimitAssertion.validateMaxRequestsPerSecond(maxRequestsPerSecondField.getText());

        if (err == null && burstWindowSizeField.isEnabled() && !validNumOrVar(burstWindowSizeField.getText()))
            err = "Burst traffic window size must be at least 1 second (if not using variables)";

        if (err == null && blackoutForCheckBox.isEnabled() && !validNumOrVar(blackoutSecField.getText()))
            err = "Blackout time must be a least 1 second (if not using variables)";

        if (err != null)
            DialogDisplayer.showMessageDialog(this, err, "Error", JOptionPane.ERROR_MESSAGE, null);

        return null == err;
    }

    private boolean validNumOrVar(String text) {
        if (Syntax.getReferencedNames(text).length > 0)
            return true;
        try {
            return Long.valueOf(text) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void enableOrDisableOkButton() {
        String maxRequests = maxRequestsPerSecondField.getText();
        okButton.setEnabled(maxRequests != null && !maxRequests.trim().isEmpty());
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

        maxRequestsPerSecondField.setText(rla.getMaxRequestsPerSecond());

        shapingOnRb.setSelected(rla.isShapeRequests());
        shapingOffRb.setSelected(!rla.isShapeRequests());

        burstTrafficCb.setSelected(!rla.isHardLimit());
        burstWindowSizeField.setText(String.valueOf(rla.getWindowSizeInSeconds()));

        String blackoutSec = rla.getBlackoutPeriodInSeconds();
        boolean useBlackout = blackoutSec != null && blackoutSec.trim().length() > 0;
        blackoutSecField.setText(useBlackout ? blackoutSec : "1");
        blackoutForCheckBox.setSelected(useBlackout);

        splitRateAcrossNodes.setSelected(rla.isSplitRateLimitAcrossNodes());
        splitConcurrencyAcrossNodes.setSelected(rla.isSplitConcurrencyLimitAcrossNodes());

        String maxConc = rla.getMaxConcurrency();
        boolean concLimit = true;
        if( Syntax.getReferencedNames(maxConc).length == 0){
            //value must be numeric, no context variable referenced
            final int maxConcInt = Integer.parseInt(maxConc);
            if(maxConcInt > 0) concurrencyLimitField.setText(maxConc);
            else concLimit = false;
        }else{
            concurrencyLimitField.setText(maxConc);
        }

        concurrencyLimitOnRb.setSelected(concLimit);
        concurrencyLimitOffRb.setSelected(!concLimit);
        updateConcurrencyEnableState();
        updateCounterNameEnableState();
        updateBlackoutAndWindowSizeFieldEnableState();
    }

    @Override
    public RateLimitAssertion getData(RateLimitAssertion rla) {
        String counterNameKey = (String)counterCb.getSelectedItem();
        String rawCounterName = RateLimitAssertion.PresetInfo.findRawCounterName(counterNameKey, uuid[0], counterNameField.getText().trim());
        rla.setCounterName(rawCounterName);
        rla.setMaxRequestsPerSecond(maxRequestsPerSecondField.getText());
        rla.setShapeRequests(shapingOnRb.isSelected());
        rla.setMaxConcurrency(concurrencyLimitOnRb.isSelected() ? concurrencyLimitField.getText() : "0");
        rla.setHardLimit(!burstTrafficCb.isSelected());
        rla.setWindowSizeInSeconds(burstWindowSizeField.getText());
        rla.setBlackoutPeriodInSeconds(blackoutForCheckBox.isSelected() ? blackoutSecField.getText() : null);
        rla.setSplitConcurrencyLimitAcrossNodes(splitConcurrencyAcrossNodes.isSelected());
        rla.setSplitRateLimitAcrossNodes(splitRateAcrossNodes.isSelected());
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
