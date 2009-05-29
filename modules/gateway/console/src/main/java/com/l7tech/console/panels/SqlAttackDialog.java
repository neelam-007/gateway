package com.l7tech.console.panels;

import com.l7tech.policy.assertion.SqlAttackAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Sep 28, 2005
 * Time: 9:42:34 AM
 */
public class SqlAttackDialog extends AssertionPropertiesEditorSupport<SqlAttackAssertion> {
    private static final String PROTECTION_KEY = "PROT.KEY";
    private static final String PROTECTION_DESCRIPTION = "PROT.DESCRIPTION";

    private JPanel mainPanel;
    private JTextArea attackDescription;
    private JPanel attackNameList;

    private SqlAttackAssertion sqlAssertion;
    private JButton okButton;
    private JButton cancelButton;
    private boolean confirmed = false;
    private List<JCheckBox> attacks;

    public SqlAttackDialog( final Window owner, final SqlAttackAssertion assertion) {
        super(owner, "Configure SQL Attack Protection");
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

        Utilities.setEscKeyStrokeDisposes(this);
        populateProtectionList();
    }

    @Override
    protected void configureView() {
        okButton.setEnabled( !isReadOnly() );
    }

    private void doCancel() {
        confirmed = false;
        dispose();
    }

    private void doSave() {
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

            JCheckBox theCheckBox = new JCheckBox(theLabel);

            theCheckBox.addMouseMotionListener(new MouseInputAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    updateDescription((JCheckBox)e.getComponent());
                }
            });

            theCheckBox.putClientProperty(PROTECTION_KEY, theProtection);
            theCheckBox.putClientProperty(PROTECTION_DESCRIPTION, theDescription);
            theCheckBox.setSelected(enabledProtections.contains(theProtection));
            attacks.add(theCheckBox);
            attackNameList.add(theCheckBox);
        } catch (PolicyAssertionException e) {
            e.printStackTrace();
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
