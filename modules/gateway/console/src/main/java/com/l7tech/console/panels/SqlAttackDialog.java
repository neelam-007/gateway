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
 */
public class SqlAttackDialog extends AssertionPropertiesEditorSupport<SqlAttackAssertion> {
    private static final String PROTECTION_KEY = "PROT.KEY";
    private static final String PROTECTION_DESCRIPTION = "PROT.DESCRIPTION";

    private JPanel mainPanel;
    private JTextArea attackDescription;
    private JPanel attackNameList;

    private JCheckBox urlCheckBox;
    private JCheckBox bodyCheckBox;

    private SqlAttackAssertion sqlAssertion;
    private JButton okButton;
    private JButton cancelButton;

    private boolean confirmed = false;
    private List<JCheckBox> attacks;

    private static final Logger logger = Logger.getLogger(SqlAttackDialog.class.getName());

    private static final String NO_LABEL_TEXT = "No label";
    private static final String NO_DESCRIPTION_TEXT = "No description";

    public SqlAttackDialog( final Window owner, final SqlAttackAssertion assertion) {
        super(owner, assertion);
        doInit(assertion);
    }

    private void doInit(SqlAttackAssertion assertion) {
        attackNameList.setLayout(new BoxLayout(attackNameList, BoxLayout.Y_AXIS));
        attacks = new ArrayList<JCheckBox>();

        this.sqlAssertion = assertion;
        getContentPane().add(mainPanel);

        attackNameList.setBackground(mainPanel.getBackground());
        attackDescription.setBackground(mainPanel.getBackground());
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

        urlCheckBox.setSelected(sqlAssertion.isIncludeUrl());
        bodyCheckBox.setSelected(sqlAssertion.isIncludeBody());

        Utilities.setEscKeyStrokeDisposes(this);
        populateProtectionList();
        initAssertionTargetComponents();
    }

    private void initAssertionTargetComponents() {

        urlCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                enableOkButton();
            }
        });

        urlCheckBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                attackDescription.setText("Scan parameter values in URL query string.");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                attackDescription.setText("");
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
                attackDescription.setText("Scan request message body.");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                attackDescription.setText("");
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
        for (JCheckBox checkBox : attacks) {
            if (checkBox.isSelected()) {
                ok = true;
                break;
            }
        }

        ok &= (urlCheckBox.isSelected() || bodyCheckBox.isSelected());

        okButton.setEnabled(ok && !isReadOnly());
    }

    private void doCancel() {
        confirmed = false;
        dispose();
    }

    private void doSave() {
        sqlAssertion.setIncludeUrl(urlCheckBox.isSelected());
        sqlAssertion.setIncludeBody(bodyCheckBox.isSelected());

        for (JCheckBox checkBox : attacks) {
            String thekey = (String) checkBox.getClientProperty(PROTECTION_KEY);
            if (checkBox.isSelected()) {
                sqlAssertion.setProtection(thekey);
            } else {
                sqlAssertion.removeProtection(thekey);
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
        List protections = getAvailableProtections();
        if (protections != null) {
            for (Object protection : protections) {
                String theProtection = (String) protection;
                addProtectionToList(theProtection, sqlAssertion);
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
            attacks.add(theCheckBox);
            attackNameList.add(theCheckBox);
        } catch (PolicyAssertionException e) {
            logger.log(Level.WARNING,
                    "Error adding protection: " + e.getMessage(), ExceptionUtils.getDebugException(e));
            DialogDisplayer.showMessageDialog(SqlAttackDialog.this,
                    "Could not populate protection list.", "Error", JOptionPane.ERROR_MESSAGE, null);
        }

    }

    private void updateDescription(JCheckBox checkbox) {
        String theDescription = (String) checkbox.getClientProperty(PROTECTION_DESCRIPTION);
        attackDescription.setText(theDescription);
        repaint();
    }

    private List getAvailableProtections() {
        return SqlAttackAssertion.getAllProtections();
    }
}
