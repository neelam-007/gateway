package com.l7tech.external.assertions.ntlm.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.ntlm.NtlmAuthenticationAssertion;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: ymoiseyenko
 * Date: 11/23/11
 * Time: 9:09 AM
 * To change this template use File | Settings | File Templates.
 */
public class NtlmAuthenticationPropertiesDialog extends AssertionPropertiesOkCancelSupport<NtlmAuthenticationAssertion> {
    private JRadioButton maxDurationDefaultRadioButton;
    private JRadioButton maxDurationCustomRadioButton;
    private JTextField maxDurationSecondsTextField;
    private JRadioButton maxIdleCustomRadioButton;
    private JRadioButton maxIdleDefaultRadioButton;
    private JTextField maxIdleSecondsTextField;
    private JPanel propertyPanel;
    private TargetVariablePanel targetVariablePanel;
    private JComboBox ldapServerComboBox;
    private JLabel ldapIdentityProviderLabel;
    private JLabel variablePrefixLabel;
    private final long defaultMaxConnectionDuration = NtlmAuthenticationAssertion.DEFAULT_MAX_CONNECTION_DURATION;
    private final long defaultMaxIdleTimeout = NtlmAuthenticationAssertion.DEFAULT_MAX_IDLE_TIMEOUT;
    private final InputValidator inputValidator;


    public NtlmAuthenticationPropertiesDialog(final Frame owner, final NtlmAuthenticationAssertion assertion) {
        super(NtlmAuthenticationAssertion.class, owner, assertion, true);
        inputValidator = new InputValidator( this, getTitle() );
        initComponents();
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        //--------------------------------------------
        try {
            DefaultComboBoxModel model = new DefaultComboBoxModel();
            if(Registry.getDefault() != null && Registry.getDefault().getIdentityAdmin() != null) {
                IdentityAdmin identityAdmin = Registry.getDefault().getIdentityAdmin();
                for(EntityHeader entityHeader : identityAdmin.findAllIdentityProviderConfig()) {
                    IdentityProviderConfig cfg = identityAdmin.findIdentityProviderConfigByID(entityHeader.getOid());
                    if (IdentityProviderType.fromVal(cfg.getTypeVal()) == IdentityProviderType.LDAP) {
                        LdapIdentityProviderConfig ldapConfig = (LdapIdentityProviderConfig)cfg ;
                        Map<String, String> props = ldapConfig.getNtlmAuthenticationProviderProperties();
                        if(props.size() > 0 && Boolean.TRUE.toString().equals(props.get("enabled"))) {
                            model.addElement(new LdapServerEntry(entityHeader.getOid(), entityHeader.getName()));
                        }
                    }
                }
            }
            ldapServerComboBox.setModel(model);
        } catch(FindException e) {
            //TODO: capture this failure in the log
        }

        maxDurationDefaultRadioButton.setSelected(true);
        maxIdleDefaultRadioButton.setSelected(true);
        targetVariablePanel.setVariable(NtlmAuthenticationAssertion.DEFAULT_PREFIX);
        targetVariablePanel.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                enableDisableComponents();
            }
        });
        targetVariablePanel.setAcceptEmpty(true);

        maxIdleCustomRadioButton.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                enableDisableComponents();
            }
        });

        maxDurationCustomRadioButton.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                enableDisableComponents();
            }
        });
        // the value entered must be from 0 to max integer 2^31-1
        inputValidator.constrainTextFieldToNumberRange("Max Duration Custom", maxDurationSecondsTextField, 0L, Integer.MAX_VALUE, false);
        inputValidator.constrainTextFieldToNumberRange("Max Idle Time", maxIdleSecondsTextField, 0L, Integer.MAX_VALUE, false);
    }

    private void enableDisableComponents() {
        maxDurationSecondsTextField.setEnabled(maxDurationCustomRadioButton.isSelected());
        maxIdleSecondsTextField.setEnabled(maxIdleCustomRadioButton.isSelected());
    }

    @Override
    public void setData(NtlmAuthenticationAssertion assertion) {

       targetVariablePanel.setVariable(assertion.getVariablePrefix());
       long maxConnectionDuration = assertion.getMaxConnectionDuration();
       if(maxConnectionDuration == defaultMaxConnectionDuration) {
           maxDurationDefaultRadioButton.setSelected(true);
       }
       else {
           maxDurationCustomRadioButton.setSelected(true);
           maxDurationSecondsTextField.setText(String.valueOf(maxConnectionDuration));
       }
       long maxIdleTimeout = assertion.getMaxConnectionIdleTime();
       if(maxIdleTimeout == defaultMaxIdleTimeout) {
           maxIdleDefaultRadioButton.setSelected(true);
       }
       else {
           maxIdleCustomRadioButton.setSelected(true);
           maxIdleSecondsTextField.setText(String.valueOf(maxIdleTimeout));
       }

        long ldapProviderOid = assertion.getLdapProviderOid();
        for(int i = 0;i < ldapServerComboBox.getItemCount();i++) {
            LdapServerEntry entry = (LdapServerEntry) ldapServerComboBox.getItemAt(i);
            if(entry.getOid() == ldapProviderOid) {
                ldapServerComboBox.setSelectedIndex(i);
                break;
            }
        }

    }

    @Override
    public NtlmAuthenticationAssertion getData(NtlmAuthenticationAssertion assertion) throws ValidationException {

        assertion.setVariablePrefix(targetVariablePanel.getVariable());

        if(maxDurationDefaultRadioButton.isSelected()) {
            assertion.setMaxConnectionDuration(defaultMaxConnectionDuration);
        }
        else {
            assertion.setMaxConnectionDuration(getNumericValue(maxDurationSecondsTextField.getText()));
        }

        if(maxIdleDefaultRadioButton.isSelected()){
            assertion.setMaxConnectionIdleTime(defaultMaxIdleTimeout);
        }
        else {
            assertion.setMaxConnectionIdleTime(getNumericValue(maxIdleSecondsTextField.getText()));
        }

        LdapServerEntry entry = (LdapServerEntry) ldapServerComboBox.getSelectedItem();
        if(entry != null) {
            assertion.setLdapProviderOid(entry.getOid());
        }

        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return propertyPanel;
    }

    private long getNumericValue(String str) {
        long n = 0;

        try {
            if(StringUtils.isNotBlank(str)) {
                 n = Long.parseLong(str);
            }
           else {
                throw new ValidationException("Invalid numeric value: the string cannot be empty!");
            }
        } catch (NumberFormatException e) {
           throw new ValidationException("Invalid numeric value: " + str);
        }

        return n;
    }

    private static class LdapServerEntry {
        private long oid;
        private String name;

        public LdapServerEntry(long oid, String name) {
            this.oid = oid;
            this.name = name;
        }

        public String toString() {
            return name;
        }

        public long getOid() {
            return oid;
        }
    }
}
