package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.IconManager;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;


/**
 * <code>IdentityProviderPanel</code> that represent a step in the wizard
 * <code>WizardStepPanel</code> that collects the published service identities
 * info.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class IdentityProviderPanel extends WizardStepPanel {
    private DefaultComboBoxModel providersComboBoxModel;

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


        providersjComboBox.setModel(getProvidersComboBoxModel());
        providersjComboBox.setRenderer(new ListCellRenderer () {
            public Component getListCellRendererComponent(
                    JList list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {
                IdentityProvider ip = (IdentityProvider)value;
                return new JLabel(ip.getConfig().getDescription());
            }
        });
        providersjComboBox.addActionListener(new ActionListener() {
            /** Invoked when an action occurs.  */
            public void actionPerformed(ActionEvent e) {
                e.getSource();
                try {
                    IdentityProvider ip = (IdentityProvider)providersComboBoxModel.getSelectedItem();
                    DefaultTableModel modelOut = new DefaultTableModel();
                    modelOut.addColumn("No permission");
                    Iterator i = ip.getUserManager().findAllHeaders().iterator();
                    while(i.hasNext()) {
                        modelOut.addRow(new Object[] {i.next()});
                    }
                    i = ip.getGroupManager().findAllHeaders().iterator();
                    while(i.hasNext()) {
                        modelOut.addRow(new Object[] {i.next()});
                    }
                    usersOutjTable.setModel(modelOut);
                    DefaultTableModel modelIn = new DefaultTableModel();
                    modelIn.addColumn("Have permission");
                    usersInjTable.setModel(modelIn);
                } catch (FindException ex) {
                    ex.printStackTrace();  //todo: fix this with better, general exception management
                }
            }
        });
        providerSelectorjPanel.add(providersjComboBox);

        add(providerSelectorjPanel, java.awt.BorderLayout.NORTH);

        identitiesPanel.setLayout(new java.awt.BorderLayout());

        identitiesjPanel.setLayout(new BoxLayout(identitiesjPanel, BoxLayout.X_AXIS));

        identitiesjPanel.setBorder(new EmptyBorder(new Insets(20, 5, 10, 5)));
        identitiesOutjScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        identitiesOutjScrollPane.setPreferredSize(new java.awt.Dimension(150, 50));
        usersOutjTable.setModel(getUsersOutTableModel());

        /*usersOutjTable.setMinimumSize(new Dimension(15, 15));
        usersOutjTable.setPreferredSize(new Dimension(50, 50));*/
        usersOutjTable.setShowHorizontalLines(false);
        usersOutjTable.setShowVerticalLines(false);
        usersOutjTable.setDefaultRenderer(Object.class, tableRenderer);
        identitiesOutjScrollPane.getViewport().setBackground(usersOutjTable.getBackground());
        identitiesOutjScrollPane.setViewportView(usersOutjTable);

        identitiesjPanel.add(identitiesOutjScrollPane);

        usersLablejPanel.setLayout(new BoxLayout(usersLablejPanel, BoxLayout.Y_AXIS));

        usersLablejPanel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 5)));
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
        usersInjTable.setModel(getUsersInTableModel());

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
        credentialsLocationjComboBox.setModel(new DefaultComboBoxModel(new String[]{"HTTP Basic", "HTTP Digest", "Message Basic"}));
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

    /**
     * @return the list of providers combo box model
     */
    private DefaultComboBoxModel getProvidersComboBoxModel() {
        if (providersComboBoxModel != null)
            return providersComboBoxModel;

        providersComboBoxModel = new DefaultComboBoxModel();
        try {
            Iterator providers =
                    Registry.getDefault().getProviderConfigManager().findAllIdentityProviders().iterator();
            while (providers.hasNext()) {
                providersComboBoxModel.addElement(providers.next());
            }
        } catch (FindException e) {
             e.printStackTrace();  //todo: fix this with better, general exception management
        }
        return providersComboBoxModel;
    }

    /**
     * @return the table model representing the identities that
     *         have permisison to use the service.
     */
    private DefaultTableModel getUsersInTableModel() {
        if (usersInTableMode != null)
            return usersInTableMode;

        usersInTableMode = new DefaultTableModel();
        return usersInTableMode;
    }

    /**
     * @return the table model representing the identities that
     *         are not permitted to use the service.
     */
    private DefaultTableModel getUsersOutTableModel() {
        if (usersOutTableMode != null)
            return usersOutTableMode;
        usersOutTableMode = new DefaultTableModel();
        return usersOutTableMode;
    }

    public String getDescription() {
        return "Select the identities (users, groups) that are allowed to access the published service" +
                " Specify where the credentials are located and transport layewr security";
    }

    /** @return the wizard step label    */
    public String getStepLabel() {
        return "Acces control";
    }

    private void equalizeButtons() {
        JButton buttons[] = new JButton[]{
            jButtonAdd,
            jButtonAddAll,
            jButtonRemove,
            jButtonRemoveAll
        };
        Utilities.equalizeButtonSizes(buttons);
    }


    private final TableCellRenderer
            tableRenderer = new DefaultTableCellRenderer() {
                /* This is the only method defined by ListCellRenderer.  We just
                 * reconfigure the Jlabel each time we're called.
                 */
                public Component
                        getTableCellRendererComponent(JTable table,
                                                      Object value,
                                                      boolean iss,
                                                      boolean hasFocus,
                                                      int row, int column) {
                    if (iss) {
                        this.setBackground(table.getSelectionBackground());
                        this.setForeground(table.getSelectionForeground());
                    } else {
                        this.setBackground(table.getBackground());
                        this.setForeground(table.getForeground());
                    }
                    this.setFont(new Font("Dialog", Font.PLAIN, 12));
                        EntityHeader h = (EntityHeader) value;
                        EntityType type = h.getType();
                        ImageIcon icon = IconManager.getInstance().getIcon(type);
                        if (icon != null) setIcon(icon);
                        setText(h.getName());


                    return this;
                }

            };

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JTable usersInjTable;
    private JTable usersOutjTable;
    private DefaultTableModel usersInTableMode;
    private DefaultTableModel usersOutTableMode;

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

    private JCheckBox ssljCheckBox;

}
