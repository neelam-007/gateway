package com.l7tech.console.panels;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.SslAssertion;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

/**
 * This class is the SSL properties editor.
 *
 * @author Emil Marceta
 * @version 1.0
 */
public class EditSslAssertionPropertiesDialog extends LegacyAssertionPropertyDialog {
    static final Logger log = Logger.getLogger(EditSslAssertionPropertiesDialog.class.getName());
    private SslAssertion sslAssertion;
    private JCheckBox requireClientCertificateCheckBox;
    private JButton okButton;
    private JButton cancelButton;
    private JRadioButton sslRequiredRadioButton;
    private JRadioButton sslOptionalRadioButton;
    private JRadioButton sslForbiddenRadioButton;
    private JPanel mainPanel;
    private JCheckBox certValidityCheckBox;

    /**
     * Create a new Ssl Editor Dialog
     *
     * @param parent the parent Frame. May be <B>null</B>
     */
    public EditSslAssertionPropertiesDialog(Frame parent, SslAssertion sslAssertion, boolean readOnly) {
        super(parent, sslAssertion, true);
        this.sslAssertion = sslAssertion;
        initialize(readOnly);
        pack();
        Utilities.centerOnScreen(this);
    }

    private void initialize(boolean readOnly) {
        Container pane = getContentPane();
        pane.setLayout(new BorderLayout());
        /** Set content pane */
        pane.add(mainPanel, BorderLayout.CENTER);

        ButtonGroup group = new ButtonGroup();
        group.add(sslForbiddenRadioButton);
        group.add(sslOptionalRadioButton);
        group.add(sslRequiredRadioButton);
        final ChangeListener enableOrDisableListener = new RunOnChangeListener() {
            @Override
            public void run() {
                enableOrDisable();
            }
        };
        sslRequiredRadioButton.addChangeListener(enableOrDisableListener);
        requireClientCertificateCheckBox.addChangeListener(enableOrDisableListener);

        final SslAssertion.Option sslOption = sslAssertion.getOption();

        Utilities.equalizeButtonSizes(new AbstractButton[]{
                                          okButton,
                                          cancelButton
                                        });

        Utilities.enableGrayOnDisabled(certValidityCheckBox);

        if (sslOption.equals(SslAssertion.REQUIRED)) {
              sslRequiredRadioButton.setSelected(true);
            if (sslAssertion.isRequireClientAuthentication()) {
                requireClientCertificateCheckBox.setSelected(true);
            }
          } else if (sslOption.equals(SslAssertion.OPTIONAL)) {
              sslOptionalRadioButton.setSelected(true);
          } else if (sslOption.equals(SslAssertion.FORBIDDEN)) {
              sslForbiddenRadioButton.setSelected(true);
          }

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        okButton.setEnabled( !readOnly );
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (sslRequiredRadioButton.isSelected()) {
                    sslAssertion.setOption(SslAssertion.REQUIRED);
                    if (requireClientCertificateCheckBox.isSelected()) {
                        sslAssertion.setRequireClientAuthentication(true);
                        sslAssertion.setCheckCertValidity(certValidityCheckBox.isSelected());
                    }
                } else if (sslOptionalRadioButton.isSelected()) {
                    sslAssertion.setOption(SslAssertion.OPTIONAL);
                    sslAssertion.setRequireClientAuthentication(false);
                    sslAssertion.setCheckCertValidity(true);
                } else if (sslForbiddenRadioButton.isSelected()) {
                    sslAssertion.setOption(SslAssertion.FORBIDDEN);
                    sslAssertion.setRequireClientAuthentication(false);
                    sslAssertion.setCheckCertValidity(true);
                }
                dispose();
            }
        });
        requireClientCertificateCheckBox.setEnabled(sslRequiredRadioButton.isSelected());

        enableOrDisable();
    }

    private void enableOrDisable() {
        requireClientCertificateCheckBox.setEnabled(sslRequiredRadioButton.isSelected());
        certValidityCheckBox.setEnabled(requireClientCertificateCheckBox.isEnabled() && requireClientCertificateCheckBox.isSelected());
    }
}
