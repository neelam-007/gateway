package com.l7tech.console.panels;

import com.l7tech.policy.assertion.SqlAttackAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.action.Actions;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Sep 28, 2005
 * Time: 9:42:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class SqlAttackDialog extends JDialog {
    private JPanel mainPanel;
    private JTextArea attackDescription;
    private JList attackNameList;

    ArrayList availableAttacks = new ArrayList();
    private JList list1;
    private JTextArea textArea1;
    private DefaultListModel listModel;

    SqlAttackAssertion sqlAssertion;
    HashMap buttonToStringMap = new HashMap();
    private static final String PROTECTION_KEY = "PROT.KEY";
    private static final String PROTECTION_DESCRIPTION = "PROT.DESCRIPTION";
    private JButton okButton;
    private JButton cancelButton;

    protected class MyCellRenderer implements ListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JCheckBox checkbox = (JCheckBox) value;
            checkbox.setEnabled(isEnabled());
            checkbox.setFont(getFont());
            checkbox.setFocusPainted(false);
            checkbox.setBorderPainted(true);
            checkbox.setBorder(isSelected ?
            UIManager.getBorder("List.focusCellHighlightBorder") : new EmptyBorder(1, 1, 1, 1));
            return checkbox;
        }
    }

    public SqlAttackDialog(Frame owner, SqlAttackAssertion assertion, boolean modal) throws HeadlessException {
        super(owner, "Configure SQL Injection Protection", modal);
        doInit(assertion);
    }

    private void doInit(SqlAttackAssertion assertion) {
        this.sqlAssertion = assertion;
        getContentPane().add(mainPanel);
        listModel = new DefaultListModel();

        attackNameList.setBackground(mainPanel.getBackground());
        attackDescription.setBackground(mainPanel.getBackground());
        Utilities.equalizeButtonSizes(new AbstractButton[]{okButton, cancelButton});

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveToAssertion();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        
        Actions.setEscKeyStrokeDisposes(this);
        populateProtectionList();

    }

    private void saveToAssertion() {
        Enumeration items = listModel.elements();
        while (items.hasMoreElements()) {
            JCheckBox checkBox = (JCheckBox) items.nextElement();
            String thekey = (String) checkBox.getClientProperty(PROTECTION_KEY);
            if (checkBox.isSelected()) {
                sqlAssertion.setProtection(thekey);
            } else {
                sqlAssertion.removeProtection(thekey);
            }
        }
        setVisible(false);
    }

    private void populateProtectionList() {
        setupListRendering();

        List protections = getAvailableProtections();
        if (protections != null) {
            attackNameList.setModel(listModel);
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
            theLabel = sqlAssertion.getProtectionLabel(theProtection);
            theDescription = sqlAssertion.getProtectionDescription(theProtection);

            JCheckBox theCheckBox = new JCheckBox(theLabel);
            theCheckBox.putClientProperty(PROTECTION_KEY, theProtection);
            theCheckBox.putClientProperty(PROTECTION_DESCRIPTION, theDescription);

            theCheckBox.setSelected(enabledProtections.contains(theProtection));
            listModel.addElement(theCheckBox);
        } catch (PolicyAssertionException e) {
            e.printStackTrace();
        }

    }

    private void updateDescription(JCheckBox checkbox) {
        String theDescription = (String) checkbox.getClientProperty(PROTECTION_DESCRIPTION);
        attackDescription.setText(theDescription);
    }

    private void setupListRendering() {
        attackNameList.setCellRenderer(new MyCellRenderer());
        attackNameList.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e)
                {
                   int index = attackNameList.locationToIndex(e.getPoint());

                   if (index != -1) {
                      JCheckBox checkbox = (JCheckBox)
                                  attackNameList.getModel().getElementAt(index);
                      checkbox.setSelected(
                                         !checkbox.isSelected());
                      updateDescription(checkbox);
                      repaint();
                   }
                }
        });

        attackNameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    private List getAvailableProtections() {
        return sqlAssertion.getAllProtections();
    }

    public static void main(String[] args) {
        JDialog dlg = new JDialog();
        dlg.getContentPane().setLayout(new BorderLayout());
        dlg.getContentPane().add(new JList());


        dlg.pack();
        dlg.setVisible(true);
    }
}
