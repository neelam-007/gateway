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
public class SqlAttackDialog extends JDialog {
    private static final String PROTECTION_KEY = "PROT.KEY";
    private static final String PROTECTION_DESCRIPTION = "PROT.DESCRIPTION";

    private JPanel mainPanel;
    private JTextArea attackDescription;
    private JPanel attackNameList;

    private final boolean readOnly;
    private SqlAttackAssertion sqlAssertion;
    private JButton okButton;
    private JButton cancelButton;
    private boolean modified;
    private boolean confirmed = false;
    private ArrayList attacks;

    public boolean wasConfirmed() {
        return confirmed;
    }

    public SqlAttackDialog(Frame owner, SqlAttackAssertion assertion, boolean modal, boolean readOnly) throws HeadlessException {
        super(owner, "Configure SQL Attack Protection", modal);
        this.readOnly = readOnly;
        doInit(assertion);
    }

    private void doInit(SqlAttackAssertion assertion) {
        attackNameList.setLayout(new BoxLayout(attackNameList, BoxLayout.Y_AXIS));
        attacks = new ArrayList();

        this.sqlAssertion = assertion;
        getContentPane().add(mainPanel);

        attackNameList.setBackground(mainPanel.getBackground());
        attackDescription.setBackground(mainPanel.getBackground());
        Utilities.equalizeButtonSizes(new AbstractButton[]{okButton, cancelButton});

        okButton.setEnabled( !readOnly );
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doSave();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doCancel();
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);
        populateProtectionList();

    }

    private void doCancel() {
        confirmed = false;
        modified = false;
        dispose();
    }

    private void doSave() {
        Iterator iter = attacks.iterator();
        while (iter.hasNext()) {
            JCheckBox checkBox = (JCheckBox) iter.next();
            String thekey = (String) checkBox.getClientProperty(PROTECTION_KEY);
            if (checkBox.isSelected()) {
                sqlAssertion.setProtection(thekey);
            } else {
                sqlAssertion.removeProtection(thekey);
            }
        }
        confirmed = true;
        modified = true;
        dispose();
    }

    public boolean isModified() {
        return modified;
    }

    public SqlAttackAssertion getAssertion() {
        return sqlAssertion;
    }

    private void populateProtectionList() {
        List protections = getAvailableProtections();
        if (protections != null) {
            Iterator iter = protections.iterator();
            while (iter.hasNext()) {
                String theProtection = (String) iter.next();
                addProtectionToList(theProtection, sqlAssertion);
            }
        }
    }

    private void addProtectionToList(String theProtection, SqlAttackAssertion sqlAssertion) {
        String theLabel = null;
        String theDescription = null;

        Set enabledProtections = sqlAssertion.getProtections();

        try {
            theLabel = SqlAttackAssertion.getProtectionLabel(theProtection);
            theDescription = SqlAttackAssertion.getProtectionDescription(theProtection);

            JCheckBox theCheckBox = new JCheckBox(theLabel);

            theCheckBox.addMouseMotionListener(new MouseInputAdapter() {
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
