package com.l7tech.console.panels;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;


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
        credentialsAndTransportjPanel = new JPanel();
        credentialsLocationjPanel = new JPanel();
        credentialsLocationjComboBox = new JComboBox();
        ssljCheckBox = new JCheckBox();
        ssljPanel = new JPanel();

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
        identitiesOutjScrollPane.getViewport().setBackground(usersOutjTable.getBackground());
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
        identitiesInjScrollPane.getViewport().setBackground(usersInjTable.getBackground());
        identitiesInjScrollPane.setViewportView(usersInjTable);

        identitiesjPanel.add(identitiesInjScrollPane);

        buttonjPanel.setLayout(new BoxLayout(buttonjPanel, BoxLayout.Y_AXIS));

        identitiesjPanel.add(buttonjPanel);

        identitiesPanel.add(identitiesjPanel, java.awt.BorderLayout.EAST);

        add(identitiesPanel, java.awt.BorderLayout.WEST);
        credentialsAndTransportjPanel.setLayout(new BoxLayout(credentialsAndTransportjPanel, BoxLayout.Y_AXIS));

        credentialsAndTransportjPanel.setBorder(new TitledBorder("Credentials/transport"));
        credentialsLocationjPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        ssljPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        credentialsLocationjComboBox.setModel(new DefaultComboBoxModel(new String[] { "HTTP Basic", "HTTP Digest", "Message Basic" }));
        credentialsLocationjPanel.add(credentialsLocationjComboBox);
        credentialsAndTransportjPanel.add(credentialsLocationjPanel);

        ssljCheckBox.setText("SSL/TLS");
        ssljCheckBox.setHorizontalTextPosition(SwingConstants.LEADING);
        ssljPanel.add(ssljCheckBox);
        credentialsAndTransportjPanel.add(ssljPanel);
        JPanel ra = new JPanel();
        ra.setLayout(new BorderLayout());
        ra.add(Box.createGlue());
        credentialsAndTransportjPanel.add(ra);

        add(credentialsAndTransportjPanel, java.awt.BorderLayout.CENTER);
    }

    public String getDescription() {
        return "Select the identities (users, groups) that are allowed to access the published service"+
                " Specify where the credentials are located and transport layewr security";
    }

    /** @return the wizard step label    */
    public String getStepLabel() {
        return "Acces control";
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
    private JPanel ssljPanel;

    private JLabel selecProviderjLabel;
    private JButton jButtonAddAll;
    private JPanel identitiesjPanel;
    private JPanel credentialsLocationjPanel;
    private JButton jButtonRemove;
    private JPanel providerSelectorjPanel;
    private JPanel identitiesPanel;
    private JButton jButtonRemoveAll;
    private JComboBox providersjComboBox;
    private JScrollPane identitiesInjScrollPane;
    private JButton jButtonAdd;
    private JComboBox credentialsLocationjComboBox;
    private JPanel credentialsAndTransportjPanel;
    private JPanel buttonjPanel;
    private JPanel usersLablejPanel;
    private JTable usersOutjTable;
    private JCheckBox ssljCheckBox;
    
}
