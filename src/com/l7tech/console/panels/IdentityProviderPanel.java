package com.l7tech.console.panels;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.EmptyBorder;


/**
 * <code>IdentityProviderPanel</code> that represent a step in the wizard
 * <code>WizardStepPanel</code> that collects the published service identities
 * info.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class IdentityProviderPanel extends WizardStepPanel {
    
    /** Creates new form IdentityProviderPanel */
    public IdentityProviderPanel() {
        initComponents();
        equalizeButtons();
    }
    
    /**
     * This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        providerSelectorjPanel = new JPanel();
        selecProviderjLabel = new JLabel();
        providersjComboBox = new JComboBox();
        identitiesPanel = new JPanel();
        identitiesjPanel = new JPanel();
        identitiesOutjScrollPane = new JScrollPane();
        usersOutjTable = new JTable();
        usersLablejPanel = new JPanel();
        jButtonAdd = new JButton();
        jButtonAddAll = new JButton();
        jButtonRemove = new JButton();
        jButtonRemoveAll = new JButton();
        identitiesInjScrollPane = new JScrollPane();
        usersInjTable = new JTable();
        buttonjPanel = new JPanel();
        jPanel1 = new JPanel();
        jPanel2 = new JPanel();
        credentialsjLabel = new JLabel();
        credentialsLocationjComboBox = new JComboBox();
        jCheckBox1 = new JCheckBox();
        jPanel3 = new JPanel();

        setLayout(new java.awt.BorderLayout());

        providerSelectorjPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        selecProviderjLabel.setText("Select the identity provider:");
        selecProviderjLabel.setBorder(new EmptyBorder(new java.awt.Insets(2, 2, 2, 2)));
        providerSelectorjPanel.add(selecProviderjLabel);

        providersjComboBox.setModel(new DefaultComboBoxModel(new String[] { "Internal provider", "LDAP provider (Corporate LDAP)", "NTLM (Windows corporate netzwerke)" }));
        providerSelectorjPanel.add(providersjComboBox);

        add(providerSelectorjPanel, java.awt.BorderLayout.NORTH);

        identitiesPanel.setLayout(new java.awt.BorderLayout());

        identitiesjPanel.setLayout(new BoxLayout(identitiesjPanel, BoxLayout.X_AXIS));

        identitiesjPanel.setBorder(new EmptyBorder(new java.awt.Insets(20, 5, 10, 5)));
        identitiesOutjScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        identitiesOutjScrollPane.setPreferredSize(new java.awt.Dimension(150, 50));
        usersOutjTable.setModel(new DefaultTableModel(
            new Object [][] {
                {"bob"},
                {"fred"},
                {"bunky"},
                {"spock"},
                {"stuart"},
                {null},
                {null},
                {null},
                {null},
                {null}
            },
            new String [] {
                "Non Users"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        usersOutjTable.setMinimumSize(new java.awt.Dimension(15, 15));
        usersOutjTable.setPreferredSize(new java.awt.Dimension(50, 50));
        usersOutjTable.setShowHorizontalLines(false);
        usersOutjTable.setShowVerticalLines(false);
        identitiesOutjScrollPane.setViewportView(usersOutjTable);

        identitiesjPanel.add(identitiesOutjScrollPane);

        usersLablejPanel.setLayout(new BoxLayout(usersLablejPanel, BoxLayout.Y_AXIS));

        usersLablejPanel.setBorder(new EmptyBorder(new java.awt.Insets(5, 5, 5, 5)));
        jButtonAdd.setText("Add");
        usersLablejPanel.add(jButtonAdd);

        jButtonAddAll.setText("Add All");
        usersLablejPanel.add(jButtonAddAll);

        jButtonRemove.setText("Remove");
        usersLablejPanel.add(jButtonRemove);

        jButtonRemoveAll.setText("Remove all");
        usersLablejPanel.add(jButtonRemoveAll);

        identitiesjPanel.add(usersLablejPanel);

        identitiesInjScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        identitiesInjScrollPane.setPreferredSize(new java.awt.Dimension(150, 50));
        usersInjTable.setModel(new DefaultTableModel(
            new Object [][] {
                {"alice"},
                {"helmut"},
                {"marketing-group"},
                {"development-group"},
                {null},
                {null},
                {null},
                {null},
                {null},
                {null}
            },
            new String [] {
                "Service users"
            }
        ));
        usersInjTable.setShowHorizontalLines(false);
        usersInjTable.setShowVerticalLines(false);
        identitiesInjScrollPane.setViewportView(usersInjTable);

        identitiesjPanel.add(identitiesInjScrollPane);

        buttonjPanel.setLayout(new BoxLayout(buttonjPanel, BoxLayout.Y_AXIS));

        identitiesjPanel.add(buttonjPanel);

        identitiesPanel.add(identitiesjPanel, java.awt.BorderLayout.EAST);

        add(identitiesPanel, java.awt.BorderLayout.WEST);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel1.setBorder(new EmptyBorder(new java.awt.Insets(20, 5, 10, 5)));
        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        credentialsjLabel.setText("Credentials");
        credentialsjLabel.setBorder(new EmptyBorder(new java.awt.Insets(1, 1, 1, 10)));
        jPanel2.add(credentialsjLabel);

        credentialsLocationjComboBox.setModel(new DefaultComboBoxModel(new String[] { "HTTP Basic", "HTTP Digest", "Message Basic" }));
        jPanel2.add(credentialsLocationjComboBox);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(jPanel2, gridBagConstraints);

        jCheckBox1.setText("SSL/TLS");
        jCheckBox1.setHorizontalTextPosition(SwingConstants.LEADING);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        jPanel1.add(jCheckBox1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel1.add(jPanel3, gridBagConstraints);

        add(jPanel1, java.awt.BorderLayout.CENTER);

    }//GEN-END:initComponents
    
    public String getDescription() {
        return "Identity provider selection";
    }
    
    private void equalizeButtons() {
        JButton buttons[] = new JButton[] {
            jButtonAdd,
            jButtonAddAll,
            jButtonRemove,
            jButtonRemoveAll
        };
        Utilities.equalizeButtonSizes(buttons);
        
        
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JTable usersInjTable;
    private JScrollPane identitiesOutjScrollPane;
    private JPanel jPanel3;
    private JLabel credentialsjLabel;
    private JLabel selecProviderjLabel;
    private JButton jButtonAddAll;
    private JPanel identitiesjPanel;
    private JPanel jPanel2;
    private JButton jButtonRemove;
    private JPanel providerSelectorjPanel;
    private JPanel identitiesPanel;
    private JButton jButtonRemoveAll;
    private JComboBox providersjComboBox;
    private JScrollPane identitiesInjScrollPane;
    private JButton jButtonAdd;
    private JComboBox credentialsLocationjComboBox;
    private JPanel jPanel1;
    private JPanel buttonjPanel;
    private JPanel usersLablejPanel;
    private JTable usersOutjTable;
    private JCheckBox jCheckBox1;
    
}
