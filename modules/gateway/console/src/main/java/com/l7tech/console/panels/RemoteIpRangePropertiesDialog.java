package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.RemoteIpRange;
import com.l7tech.util.InetAddressUtil;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;
import java.util.ResourceBundle;

import static com.l7tech.policy.assertion.RemoteIpRange.IPV4_MAX_NETWORK_MASK;
import static com.l7tech.policy.assertion.RemoteIpRange.IPV6_MAX_NETWORK_MASK;

/**
 * Dialog for viewing and editing a RemoteIpRange assertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * @author flascell<br/>
 */
public class RemoteIpRangePropertiesDialog extends LegacyAssertionPropertyDialog {
    private JButton okButton;
    private JButton cancelButton;
    private JButton helpButton;
    private JFormattedTextField address;
    private JFormattedTextField networkMask;
    private JComboBox includeExcludeCombo;
    private JPanel mainPanel;

    private ResourceBundle resources;
    private boolean oked;
    private RemoteIpRange subject;
    private JRadioButton tcpRadio;
    private JRadioButton contextVarRadio;
    private JTextField contextVarField;

    public RemoteIpRangePropertiesDialog(Frame owner, boolean modal, boolean readOnly, RemoteIpRange subject) {
        super(owner, subject, modal);
        this.subject = subject;
        initialize(readOnly);
        oked = false;
    }

    /**
     * @return false if the dialog was dismissed or canceled, true if it was oked.
     */
    public boolean wasOked() {
        return oked;
    }

    private void initialize(boolean readOnly) {
        setModal(true);
        initResources();
        setContentPane(mainPanel);
        okButton.setEnabled(!readOnly);
        Utilities.equalizeButtonSizes(okButton, cancelButton, helpButton);

        includeExcludeCombo.setModel(new DefaultComboBoxModel(new String[]{
                resources.getString("includeExcludeCombo.include"),
                resources.getString("includeExcludeCombo.exclude")}));

        setCallbacks();
        setInitialValues();
    }

    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.RemoteIpRangePropertiesDialog", locale);
    }

    private void cancel() {
        RemoteIpRangePropertiesDialog.this.dispose();
    }

    private void ok() {
        String contextval = null;
        if (contextVarRadio.isSelected()) {
            contextval = contextVarField.getText();
            if (contextval == null || contextval.length() < 1) {
                bark("The source for the requestor ip should either be tcp or be a context variable");
                return;
            }
        }

        // get rule
        int index = includeExcludeCombo.getSelectedIndex();
        // get address and network mask
        String addressStrOrig = address.getText();
        String networkMaskStrOrig = networkMask.getText();

        //since we really can't know the value of the variables until run time, replace them accordingly
        //at least we can test a valid format (assuming the variables resolve to a proper value on runtime)
        //before the user actually saves the values of this assertion
        String addressStr = RemoteIpRange.formatStartIpStringWithDefaultValue(addressStrOrig);
        String networkMaskStr = RemoteIpRange.formatNetworkMaskStringWithDefaultValue(networkMaskStrOrig);

        // get networkMask
        Integer networkMask;
        try {
            networkMask = Integer.parseInt(networkMaskStr);
        } catch (NumberFormatException e) {
            bark(resources.getString("error.badmask"));
            return;
        }

        boolean isIpv4 = InetAddressUtil.isValidIpv4Address(addressStr);
        boolean isIpv6 = InetAddressUtil.isValidIpv6Address(addressStr);
        if (!isIpv4 && !isIpv6) {
            bark(resources.getString("error.badaddress"));
            return;
        }

        // DE296405 : Invalidate if network mask is negative or zero or greater than the permissive maximum value
        if (networkMask <= 0 || networkMask > (isIpv4 ? IPV4_MAX_NETWORK_MASK : IPV6_MAX_NETWORK_MASK)) {
            bark(resources.getString("error.badmask"));
            return;
        }

        if (subject != null) {
            // all is good. record values and get out o here
            switch (index) {
                case 0:
                    subject.setAllowRange(true);
                    break;
                case 1:
                    subject.setAllowRange(false);
                    break;
            }
            subject.setAddressRange(addressStrOrig, networkMaskStrOrig);
            subject.setIpSourceContextVariable(contextval);
            oked = true;
        }
        RemoteIpRangePropertiesDialog.this.dispose();
    }

    private void bark(String woof) {
        JOptionPane.showMessageDialog(this, woof, subject.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME).toString(), JOptionPane.ERROR_MESSAGE);
    }

    private void setInitialValues() {
        contextVarRadio.setSelected(false);
        tcpRadio.setSelected(true);
        contextVarField.setEnabled(false);
        if (subject != null) {
            // get values to populate with
            int index = subject.isAllowRange() ? 0 : 1;
            includeExcludeCombo.setSelectedIndex(index);
            includeExcludeCombo.setPreferredSize(new Dimension(100, 25));
            this.address.setText(subject.getStartIp());

            // bug 4796: any existing 0's should be changed to 32 when edited.
            if ("0".equals(subject.getNetworkMask())) {
                subject.setNetworkMask(String.valueOf(IPV4_MAX_NETWORK_MASK));
            }
            networkMask.setText(subject.getNetworkMask());
            if (subject.getIpSourceContextVariable() == null) {
                contextVarRadio.setSelected(false);
                tcpRadio.setSelected(true);
                contextVarField.setEnabled(false);
            } else {
                contextVarRadio.setSelected(true);
                tcpRadio.setSelected(false);
                contextVarField.setEnabled(true);
                contextVarField.setText(subject.getIpSourceContextVariable());
            }
        }
    }

    private void setCallbacks() {
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });
        helpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(RemoteIpRangePropertiesDialog.this);
            }
        });
        tcpRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                contextVarRadio.setSelected(!tcpRadio.isSelected());
                contextVarField.setEnabled(!tcpRadio.isSelected());
                addDefaultCntxVar();
            }
        });
        contextVarRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tcpRadio.setSelected(!contextVarRadio.isSelected());
                contextVarField.setEnabled(contextVarRadio.isSelected());
                addDefaultCntxVar();
            }
        });
    }

    private void addDefaultCntxVar() {
        if (contextVarField.isEnabled() && (contextVarField.getText() == null || contextVarField.getText().length() < 1)) {
            contextVarField.setText("request.tcp.remoteAddress");
        }
    }
}
