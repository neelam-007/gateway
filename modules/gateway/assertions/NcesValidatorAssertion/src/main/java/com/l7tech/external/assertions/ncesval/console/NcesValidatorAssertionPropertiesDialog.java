/**
 * Copyright (C) 2007-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.ncesval.console;

import com.l7tech.console.panels.TargetMessagePanel;
import com.l7tech.security.types.CertificateValidationType;
import com.l7tech.console.panels.*;
import com.l7tech.external.assertions.ncesval.NcesValidatorAssertion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

/** @author alex */
public class NcesValidatorAssertionPropertiesDialog extends AssertionPropertiesEditorSupport<NcesValidatorAssertion> {

    //- PUBLIC

    public NcesValidatorAssertionPropertiesDialog(Window owner) {
        super(owner, "NCES Validator Properties");
        initialize();
    }

    public boolean isConfirmed() {
        return ok;
    }

    public void setData(NcesValidatorAssertion assertion) {
        this.assertion = assertion;
        targetMessagePanel.setModel(assertion);
        samlCheckbox.setSelected(assertion.isSamlRequired());
        validationOptionComboBox.setSelectedItem(assertion.getCertificateValidationType());                
        trustedCertsPanel.setCertificateInfos( assertion.getTrustedCertificateInfo() );
        trustedCertIssuersPanel.setCertificateInfos( assertion.getTrustedIssuerCertificateInfo() );
    }

    public NcesValidatorAssertion getData(NcesValidatorAssertion assertion) {
        targetMessagePanel.updateModel(assertion);
        assertion.setSamlRequired(samlCheckbox.isSelected());
        assertion.setCertificateValidationType((CertificateValidationType) validationOptionComboBox.getSelectedItem());
        assertion.setTrustedCertificateInfo(trustedCertsPanel.getCertificateInfos());
        assertion.setTrustedIssuerCertificateInfo(trustedCertIssuersPanel.getCertificateInfos());

        return assertion;
    }

    //- PRIVATE

    private static final String RES_VALTYPE_PREFIX = "validation.option.";
    private static final String RES_VALTYPE_DEFAULT = "default";

    private JCheckBox samlCheckbox;
    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;

    private JPanel targetMessagePanelHolder;
    private TargetMessagePanel targetMessagePanel = new TargetMessagePanel();
    private JComboBox validationOptionComboBox;

    private JPanel trustedCertsPanelHolder;
    private JPanel trustedCertIssuersPanelHolder;

    private TrustedCertsPanel trustedCertsPanel;
    private TrustedCertsPanel trustedCertIssuersPanel;

    private volatile boolean ok = false;
    private NcesValidatorAssertion assertion;

    private void initialize() {
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok = true;
                targetMessagePanel.updateModel(assertion);
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok = false;
                dispose();
            }
        });

        targetMessagePanelHolder.add(targetMessagePanel);
        targetMessagePanel.addPropertyChangeListener("valid", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                okButton.setEnabled(Boolean.TRUE.equals(evt.getNewValue()));
            }
        });

        DefaultComboBoxModel model = new DefaultComboBoxModel(
            new Object[]{null, CertificateValidationType.PATH_VALIDATION, CertificateValidationType.REVOCATION}
        );
        validationOptionComboBox.setModel(model);
        validationOptionComboBox.setRenderer(new CertificateValidationTypeRenderer());

        trustedCertsPanel = new TrustedCertsPanel(false, 0, null);
        trustedCertsPanelHolder.add( trustedCertsPanel, BorderLayout.CENTER );

        trustedCertIssuersPanel = new TrustedCertsPanel(false, 0, null);
        trustedCertIssuersPanelHolder.add( trustedCertIssuersPanel, BorderLayout.CENTER );

        add(mainPanel);
    }

    /**
     * Renderer for CertificateValidationType
     */
    private final class CertificateValidationTypeRenderer extends JLabel implements ListCellRenderer {
        private Map<String,String> names = new HashMap<String,String>();

        public CertificateValidationTypeRenderer() {
            names.put("validation.option.CERTIFICATE_ONLY","Validate Certificate Path");
            names.put("validation.option.PATH_VALIDATION","Validate Certificate Path");
            names.put("validation.option.REVOCATION","Revocation Checking");
            names.put("validation.option.default","Use Default");
        }

        public Component getListCellRendererComponent( JList list,
                                                       Object value,
                                                       int index,
                                                       boolean isSelected,
                                                       boolean cellHasFocus)
        {
            CertificateValidationType type = (CertificateValidationType) value;

            String labelKey = RES_VALTYPE_PREFIX + RES_VALTYPE_DEFAULT;
            if (type != null) {
                labelKey = RES_VALTYPE_PREFIX + type.name();
            }

            setText(names.get(labelKey));

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
                setOpaque(true);
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
                setOpaque(false);
            }

            setEnabled(list.isEnabled());
            setFont(list.getFont());

            return this;
        }
    }

    public static void main(String[] args) {
        NcesValidatorAssertionPropertiesDialog dlg = new NcesValidatorAssertionPropertiesDialog(new JFrame());
        dlg.setData(new NcesValidatorAssertion());
        dlg.pack();
        dlg.setVisible(true);
    }
}
