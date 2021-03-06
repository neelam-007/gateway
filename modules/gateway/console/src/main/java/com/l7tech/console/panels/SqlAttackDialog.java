package com.l7tech.console.panels;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.policy.assertion.SqlAttackAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author megery
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class SqlAttackDialog extends AssertionPropertiesEditorSupport<SqlAttackAssertion> {
    private static final String PROTECTION_KEY = "PROT.KEY";
    private static final String PROTECTION_DESCRIPTION = "PROT.DESCRIPTION";

    private JPanel mainPanel;
    private JTextArea descriptionText;
    private JPanel protectionsPanel;

    private JCheckBox urlQueryStringCheckBox;
    private JCheckBox bodyCheckBox;

    private SqlAttackAssertion sqlAssertion;
    private JButton okButton;
    private JButton cancelButton;
    private JCheckBox urlPathCheckBox;

    private boolean confirmed = false;
    private List<JCheckBox> protectionCheckBoxes;

    private static final Logger logger = Logger.getLogger(SqlAttackDialog.class.getName());

    private static final String NO_LABEL_TEXT = "No label";
    private static final String NO_DESCRIPTION_TEXT = "No description";

    public SqlAttackDialog( final Window owner, final SqlAttackAssertion assertion) {
        super(owner, assertion);
        doInit(assertion);
    }

    private void doInit(SqlAttackAssertion assertion) {
        protectionsPanel.setLayout(new BoxLayout(protectionsPanel, BoxLayout.Y_AXIS));
        protectionCheckBoxes = new ArrayList<>();

        this.sqlAssertion = assertion;
        getContentPane().add(mainPanel);

        protectionsPanel.setBackground(mainPanel.getBackground());
        descriptionText.setBackground(mainPanel.getBackground());
        Utilities.equalizeButtonSizes(okButton, cancelButton);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doSave();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doCancel();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                doCancel();
            }
        });

        urlPathCheckBox.setSelected(sqlAssertion.isIncludeUrlPath());
        urlQueryStringCheckBox.setSelected(sqlAssertion.isIncludeUrlQueryString());

        bodyCheckBox.setSelected(sqlAssertion.isIncludeBody());

        Utilities.setEscKeyStrokeDisposes(this);
        populateProtectionList();
        initAssertionTargetComponents();
    }

    private void initAssertionTargetComponents() {
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
    }

    @Override
    protected void configureView() {
        enableOkButton();
    }

    /**
     * Enable/disable the OK button based on acceptability of settings.
     */
    private void enableOkButton() {
        boolean ok = false;

        // Ensures at least one protection type has been selected.
        for (JCheckBox checkBox : protectionCheckBoxes) {
            if (checkBox.isSelected()) {
                ok = true;
                break;
            }
        }

        ok &= (urlPathCheckBox.isSelected() || urlQueryStringCheckBox.isSelected() || bodyCheckBox.isSelected());

        okButton.setEnabled(ok && !isReadOnly());
    }

    private void doCancel() {
        confirmed = false;
        dispose();
    }

    private void doSave() {
        sqlAssertion.setIncludeUrlPath(urlPathCheckBox.isSelected());
        sqlAssertion.setIncludeUrlQueryString(urlQueryStringCheckBox.isSelected());
        sqlAssertion.setIncludeBody(bodyCheckBox.isSelected());

        for (JCheckBox checkBox : protectionCheckBoxes) {
            String key = (String) checkBox.getClientProperty(PROTECTION_KEY);

            if (checkBox.isSelected()) {
                sqlAssertion.setProtection(key);
            } else {
                sqlAssertion.removeProtection(key);
            }
        }

        confirmed = true;
        dispose();
    }

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData(SqlAttackAssertion assertion) {
        sqlAssertion = assertion;
    }

    @Override
    public SqlAttackAssertion getData(SqlAttackAssertion assertion) {
        return sqlAssertion;
    }

    private void populateProtectionList() {
        List<String> protections = getAvailableProtections();

        if (protections != null) {
            for (String protection : protections) {
                addProtectionToList(protection, sqlAssertion);
            }
        }
    }

    private void addProtectionToList(String theProtection, SqlAttackAssertion sqlAssertion) {
        String theLabel;
        String theDescription;

        Set enabledProtections = sqlAssertion.getProtections();

        try {
            theLabel = SqlAttackAssertion.getProtectionLabel(theProtection);
            theDescription = SqlAttackAssertion.getProtectionDescription(theProtection);

            if(null == theLabel) {
                logger.log(Level.WARNING, "Could not find label for protection: " + theProtection);
                theLabel = NO_LABEL_TEXT;
            }

            if(null == theDescription) {
                logger.log(Level.WARNING, "Could not find description for protection: " + theProtection);
                theDescription = NO_DESCRIPTION_TEXT;
            }

            JCheckBox theCheckBox = new JCheckBox(theLabel);

            theCheckBox.addMouseMotionListener(new MouseInputAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    updateDescription((JCheckBox)e.getComponent());
                }
            });

            theCheckBox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    enableOkButton();
                }
            });

            theCheckBox.putClientProperty(PROTECTION_KEY, theProtection);
            theCheckBox.putClientProperty(PROTECTION_DESCRIPTION, theDescription);
            theCheckBox.setSelected(enabledProtections.contains(theProtection));
            protectionCheckBoxes.add(theCheckBox);
            protectionsPanel.add(theCheckBox);
        } catch (PolicyAssertionException e) {
            logger.log(Level.WARNING,
                    "Error adding protection: " + e.getMessage(), ExceptionUtils.getDebugException(e));
            DialogDisplayer.showMessageDialog(SqlAttackDialog.this,
                    "Could not populate protection list.", "Error", JOptionPane.ERROR_MESSAGE, null);
        }

    }

    private void updateDescription(JCheckBox checkbox) {
        String theDescription = (String) checkbox.getClientProperty(PROTECTION_DESCRIPTION);
        descriptionText.setText(theDescription);
        repaint();
    }

    private List<String> getAvailableProtections() {
        return SqlAttackAssertion.getAllProtections();
    }
}
