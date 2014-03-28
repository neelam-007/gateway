package com.l7tech.console.panels;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.CodeInjectionProtectionAssertion;
import com.l7tech.policy.assertion.CodeInjectionProtectionType;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SqlAttackAssertion;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Properties dialog for the Code Injection Protection Assertion.
 *
 * @author rmak
 * @author Jamie Williams - jamie.williams2@ca.com
 * @since SecureSpan 3.7
 * @see com.l7tech.policy.assertion.CodeInjectionProtectionAssertion
 */
public class CodeInjectionProtectionAssertionDialog extends AssertionPropertiesEditorSupport<CodeInjectionProtectionAssertion> {
    private JPanel contentPane;
    private JCheckBox urlPathCheckBox;
    private JCheckBox urlQueryStringCheckBox;
    private JCheckBox bodyCheckBox;
    private List<JCheckBox> protectionCheckBoxes = new ArrayList<>();
    private JTextArea descriptionText;
    private JButton okButton;
    private JButton cancelButton;
    private JPanel protectionsPanel;

    private CodeInjectionProtectionAssertion _assertion;
    private boolean _confirmed;

    public CodeInjectionProtectionAssertionDialog(final Window owner, final CodeInjectionProtectionAssertion assertion) throws HeadlessException {
        super(owner, assertion);
        _assertion = assertion;

        protectionsPanel.setLayout(new BoxLayout(protectionsPanel, BoxLayout.Y_AXIS));
        protectionsPanel.setBackground(contentPane.getBackground());

        protectionCheckBoxes = new ArrayList<>();

        urlPathCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                enableOkButton();
            }
        });

        urlPathCheckBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                descriptionText.setText("Scan URL path.");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                descriptionText.setText("");
            }
        });

        urlQueryStringCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                enableOkButton();
            }
        });

        urlQueryStringCheckBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                descriptionText.setText("Scan parameter values in URL query string.");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                descriptionText.setText("");
            }
        });

        bodyCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                enableOkButton();
            }
        });

        bodyCheckBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                descriptionText.setText("Scan message body.");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                descriptionText.setText("");
            }
        });

        descriptionText.setMargin(new Insets(0, 10, 0, 10));

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        urlPathCheckBox.setSelected(_assertion.isIncludeUrlPath());
        urlQueryStringCheckBox.setSelected(_assertion.isIncludeUrlQueryString());
        bodyCheckBox.setSelected(_assertion.isIncludeBody());

        setContentPane(contentPane);
        setMinimumSize( getContentPane().getMinimumSize() );
        getRootPane().setDefaultButton(okButton);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);
        Utilities.equalizeButtonSizes(okButton, cancelButton);

        populateProtectionList();
    }

    private void populateProtectionList() {
        final List<CodeInjectionProtectionType> protectionsToApply = Arrays.asList(_assertion.getProtections());

        for (CodeInjectionProtectionType protection : CodeInjectionProtectionType.values()) {
            final JCheckBox checkbox = new JCheckBox(protection.getDisplayName());
            checkbox.setActionCommand(protection.getWspName());
            checkbox.setSelected(protectionsToApply.contains(protection));

            checkbox.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    final String action = ((JCheckBox) e.getComponent()).getActionCommand();
                    final CodeInjectionProtectionType protection = CodeInjectionProtectionType.fromWspName(action);
                    descriptionText.setText(protection.getDescription());
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    descriptionText.setText("");
                }
            });

            checkbox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    enableOkButton();
                }
            });

            protectionsPanel.add(checkbox);
            protectionCheckBoxes.add(checkbox);
        }
    }

    @Override
    protected void configureView() {
        enableOkButton();
    }

    private void onOK() {
        _assertion.setIncludeUrlPath(urlPathCheckBox.isSelected());
        _assertion.setIncludeUrlQueryString(urlQueryStringCheckBox.isSelected());
        _assertion.setIncludeBody(bodyCheckBox.isSelected());

        final List<CodeInjectionProtectionType> protectionsToApply = new ArrayList<>();
        for (JCheckBox checkBox : protectionCheckBoxes) {
            if (checkBox.isEnabled() && checkBox.isSelected()) {
                final String name = checkBox.getActionCommand();
                final CodeInjectionProtectionType protection = CodeInjectionProtectionType.fromWspName(name);
                protectionsToApply.add(protection);
            }
        }
        _assertion.setProtections(protectionsToApply.toArray(new CodeInjectionProtectionType[protectionsToApply.size()]));

        _confirmed = true;
        dispose();
    }

    private void onCancel() {
        _confirmed = false;
        dispose();
    }

    /**
     * Enable/disable the OK button if all settings are OK.
     */
    private void enableOkButton() {
        boolean ok = false;

        // Ensures at least one protection type has been selected.
        for (JCheckBox checkBox : protectionCheckBoxes) {
            if (checkBox.isEnabled() && checkBox.isSelected()) {
                ok = true;
                break;
            }
        }

        ok &= (urlPathCheckBox.isSelected() || urlQueryStringCheckBox.isSelected() || bodyCheckBox.isSelected());

        okButton.setEnabled(ok && !isReadOnly());
    }

    @Override
    public boolean isConfirmed() {
        return _confirmed;
    }

    @Override
    public void setData(CodeInjectionProtectionAssertion assertion) {
        _assertion = assertion;
    }

    @Override
    public CodeInjectionProtectionAssertion getData(CodeInjectionProtectionAssertion assertion) {
        return _assertion;
    }
}
