package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.action.Actions;
import com.l7tech.policy.assertion.SslAssertion;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

/**
 * This class is the SSL properties editor.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class EditSslAssertionPropertiesDialog extends JDialog {
    static final Logger log = Logger.getLogger(EditSslAssertionPropertiesDialog.class.getName());
    private SslAssertion sslAssertion;
    private JCheckBox requireClientCertificateCheckBox;
    private JButton okButton;
    private JButton cancelButton;
    private JRadioButton sslRequiredRadioButton;
    private JRadioButton sslOptionalRadioButton;
    private JRadioButton sslForbiddenRadioButton;
    private JPanel mainPanel;

    /**
     * Create a new Ssl Editor Dialog
     *
     * @param parent the parent Frame. May be <B>null</B>
     */
    public EditSslAssertionPropertiesDialog(Frame parent, SslAssertion sslAssertion) {
        super(parent, true);
        this.sslAssertion = sslAssertion;
        initialize();
        pack();
        Utilities.centerOnScreen(this);
        Actions.setEscKeyStrokeDisposes(this);
    }

    private void initialize() {
        setTitle("SSL or TLS options");
        Container pane = getContentPane();
        pane.setLayout(new BorderLayout());
        /** Set content pane */
        pane.add(mainPanel, BorderLayout.CENTER);

        ButtonGroup group = new ButtonGroup();
        group.add(sslForbiddenRadioButton);
        group.add(sslOptionalRadioButton);
        group.add(sslRequiredRadioButton);
        sslRequiredRadioButton.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                requireClientCertificateCheckBox.setEnabled(sslRequiredRadioButton.isSelected());
            }
        });

        final SslAssertion.Option sslOption = sslAssertion.getOption();

        Utilities.equalizeButtonSizes(new AbstractButton[]{
                                          okButton,
                                          cancelButton
                                        });

        if (sslOption.equals(SslAssertion.REQUIRED)) {
              sslRequiredRadioButton.setSelected(true);
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

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (sslRequiredRadioButton.isSelected()) {
                    sslAssertion.setOption(SslAssertion.REQUIRED);
                    if (requireClientCertificateCheckBox.isSelected()) {
                        sslAssertion.setRequireClientAuthentication(true);
                    }
                } else if (sslOptionalRadioButton.isSelected()) {
                    sslAssertion.setOption(SslAssertion.OPTIONAL);
                    sslAssertion.setRequireClientAuthentication(false);
                } else if (sslForbiddenRadioButton.isSelected()) {
                    sslAssertion.setOption(SslAssertion.FORBIDDEN);
                    sslAssertion.setRequireClientAuthentication(false);
                }
                dispose();
            }
        });
    }
}
