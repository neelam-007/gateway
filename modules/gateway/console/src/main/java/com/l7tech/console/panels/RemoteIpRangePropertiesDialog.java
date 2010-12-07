package com.l7tech.console.panels;

import com.l7tech.util.InetAddressUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.console.action.Actions;
import com.l7tech.policy.assertion.RemoteIpRange;
import com.l7tech.policy.assertion.AssertionMetadata;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;
import java.util.ResourceBundle;

import static com.l7tech.policy.assertion.RemoteIpRange.IPV4_MAX_PREFIX;
import static com.l7tech.policy.assertion.RemoteIpRange.IPV6_MAX_PREFIX;

/**
 * Dialog for viewing and editing a RemoteIpRange assertion.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * @author flascell<br/>
 */
public class RemoteIpRangePropertiesDialog extends LegacyAssertionPropertyDialog {
    private JButton okButton;
    private JButton cancelButton;
    private JButton helpButton;
    private JFormattedTextField address;
    private JFormattedTextField suffix;
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
        okButton.setEnabled( !readOnly );
        Utilities.equalizeButtonSizes(okButton, cancelButton, helpButton);
        Utilities.setEscKeyStrokeDisposes(this);

        includeExcludeCombo.setModel(new DefaultComboBoxModel(new String[] {
                                                          resources.getString("includeExcludeCombo.include"),
                                                          resources.getString("includeExcludeCombo.exclude")}));

        suffix.setFormatterFactory(new DefaultFormatterFactory(new NumberFormatter() {{
            setMinimum(1);
            setMaximum(IPV6_MAX_PREFIX);
        }}
        ));

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
        // get address
        String addressStr = address.getText();
        // get prefix
        Integer prefix;
        try {
            prefix = Integer.parseInt(suffix.getText());
        } catch (NumberFormatException e) {
            bark(resources.getString("error.badmask"));
            return;
        }

        boolean isIpv4 = InetAddressUtil.isValidIpv4Address(addressStr);
        boolean isIpv6 = InetAddressUtil.isValidIpv6Address(addressStr);
        if ( ! isIpv4 && ! isIpv6 ) {
            bark(resources.getString("error.badaddress"));
            return;
        }

        if (isIpv4 && prefix > IPV4_MAX_PREFIX || prefix > IPV6_MAX_PREFIX) {
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
            subject.setAddressRange(addressStr, prefix);
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
            if (subject.getNetworkMask() == 0) {
                subject.setNetworkMask(IPV4_MAX_PREFIX);
            }
            suffix.setText("" + subject.getNetworkMask());
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
            contextVarField.setText("request.http.header.remoteip");
        }
    }
}
