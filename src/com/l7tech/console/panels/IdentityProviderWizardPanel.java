package com.l7tech.console.panels;

import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.util.IconManager;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


/**
 * <code>IdentityProviderWizardPanel</code> that represent a step in the wizard
 * <code>WizardStepPanel</code> that collects the published service identities
 * info.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class IdentityProviderWizardPanel extends WizardStepPanel {
    private DefaultComboBoxModel providersComboBoxModel;

    /** Creates new form IdentityProviderWizardPanel */
    public IdentityProviderWizardPanel() {
        super(null);
        initComponents();
        equalizeButtons();
        populateIdentityTables();
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents() {
        providersComboBox = new JComboBox();
        identitiesjPanel = new JPanel();
        identitiesOutScrollPane = new JScrollPane();
        identitiesOutTable = new JTable();
        usersLabelPanel = new JPanel();
        buttonAdd = new JButton();
        buttonAddAll = new JButton();
        buttonRemove = new JButton();
        buttonRemoveAll = new JButton();
        identitiesInScrollPane = new JScrollPane();
        identitiesInTable = new JTable();
        buttonPanel = new JPanel();

        credentialsLocationComboBox = new JComboBox();
        credentialsLocationComboBox.setModel(Components.getCredentialsLocationComboBoxModelNonAnonymous());

        sslCheckBox = new JCheckBox();

        setLayout(new GridBagLayout());

        providersComboBox.setModel(getProvidersComboBoxModel());
        providersComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(
              JList list,
              Object value,
              int index,
              boolean isSelected,
              boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                IdentityProvider ip = (IdentityProvider)value;
                setText(ip.getConfig().getName());
                return c;
            }
        });
        providersComboBox.addActionListener(new ActionListener() {
            Object oldSelectedItem = null;

            /** Invoked when an action occurs.  */
            public void actionPerformed(ActionEvent e) {
                Object newSelectedItem = providersComboBox.getSelectedItem();
                if (newSelectedItem != oldSelectedItem)
                    populateIdentityTables();
                oldSelectedItem = newSelectedItem;
            }
        });

        identitiesjPanel.setLayout(new BoxLayout(identitiesjPanel, BoxLayout.X_AXIS));

        identitiesjPanel.setBorder(new EmptyBorder(new Insets(20, 5, 10, 5)));
        identitiesOutScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        identitiesOutScrollPane.setPreferredSize(new java.awt.Dimension(150, 50));
        identitiesOutTable.setModel(getIdentitiesOutTableModel());

        identitiesOutTable.setShowHorizontalLines(false);
        identitiesOutTable.setShowVerticalLines(false);
        identitiesOutTable.setDefaultRenderer(Object.class, tableRenderer);
        identitiesOutScrollPane.getViewport().setBackground(identitiesOutTable.getBackground());
        identitiesOutScrollPane.setViewportView(identitiesOutTable);

        identitiesjPanel.add(identitiesOutScrollPane);

        usersLabelPanel.setLayout(new BoxLayout(usersLabelPanel, BoxLayout.Y_AXIS));

        usersLabelPanel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 5)));
        buttonAdd.setText("Add");
        buttonAdd.setHorizontalTextPosition(SwingConstants.RIGHT);
        buttonAdd.setHorizontalAlignment(SwingConstants.RIGHT);
        buttonAdd.setIcon(IconManager.getInstance().getIconAdd());

        buttonAdd.addActionListener(new ActionListener() {
            /** Invoked when an action occurs. */
            public void actionPerformed(ActionEvent e) {
                int[] rows = identitiesOutTable.getSelectedRows();
                Collection toAdd = new ArrayList();
                java.util.List listOut = getIdentitiesOutTableModel().getDataVector();

                for (int i = 0; i < rows.length; i++) {
                    toAdd.add(listOut.get(rows[i]));
                }
                getIdentitiesOutTableModel().getDataVector().removeAll(toAdd);
                getIdentitiesOutTableModel().fireTableDataChanged();
                getIdentitiesInTableModel().getDataVector().addAll(toAdd);
                getIdentitiesInTableModel().fireTableDataChanged();

            }
        });
        usersLabelPanel.add(buttonAdd);

        buttonAddAll.setText("Add All");
        buttonAddAll.setHorizontalTextPosition(SwingConstants.RIGHT);
        buttonAddAll.setHorizontalAlignment(SwingConstants.RIGHT);
        buttonAddAll.setIcon(IconManager.getInstance().getIconAddAll());

        buttonAddAll.addActionListener(new ActionListener() {
            /** Invoked when an action occurs. */
            public void actionPerformed(ActionEvent e) {
                java.util.List listOut = getIdentitiesOutTableModel().getDataVector();

                getIdentitiesInTableModel().getDataVector().addAll(listOut);
                getIdentitiesOutTableModel().getDataVector().clear();
                getIdentitiesOutTableModel().fireTableDataChanged();
                getIdentitiesInTableModel().fireTableDataChanged();

            }
        });
        usersLabelPanel.add(buttonAddAll);

        buttonRemove.setText("Remove");
        buttonRemove.setHorizontalTextPosition(SwingConstants.RIGHT);
        buttonRemove.setHorizontalAlignment(SwingConstants.RIGHT);
        buttonRemove.setIcon(IconManager.getInstance().getIconRemove());

        buttonRemove.addActionListener(new ActionListener() {
            /** Invoked when an action occurs. */
            public void actionPerformed(ActionEvent e) {
                int[] rows = identitiesInTable.getSelectedRows();
                Collection toRemove = new ArrayList();
                java.util.List listIn = getIdentitiesInTableModel().getDataVector();

                for (int i = 0; i < rows.length; i++) {
                    toRemove.add(listIn.get(rows[i]));
                }
                getIdentitiesInTableModel().getDataVector().removeAll(toRemove);
                getIdentitiesInTableModel().fireTableDataChanged();
                getIdentitiesOutTableModel().getDataVector().addAll(toRemove);
                getIdentitiesOutTableModel().fireTableDataChanged();
            }
        });

        usersLabelPanel.add(buttonRemove);

        buttonRemoveAll.setText("Remove all");
        buttonRemoveAll.setHorizontalTextPosition(SwingConstants.RIGHT);
        buttonRemoveAll.setHorizontalAlignment(SwingConstants.RIGHT);
        buttonRemoveAll.setIcon(IconManager.getInstance().getIconRemoveAll());

        buttonRemoveAll.addActionListener(new ActionListener() {
            /** Invoked when an action occurs. */
            public void actionPerformed(ActionEvent e) {
                java.util.List listIn = getIdentitiesInTableModel().getDataVector();
                getIdentitiesOutTableModel().getDataVector().addAll(listIn);
                getIdentitiesInTableModel().getDataVector().clear();
                getIdentitiesOutTableModel().fireTableDataChanged();
                getIdentitiesInTableModel().fireTableDataChanged();
            }
        });

        usersLabelPanel.add(buttonRemoveAll);

        identitiesjPanel.add(usersLabelPanel);

        identitiesInScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        identitiesInScrollPane.setPreferredSize(new java.awt.Dimension(150, 50));
        identitiesInTable.setModel(getIdentitiesInTableModel());

        identitiesInTable.setShowHorizontalLines(false);
        identitiesInTable.setShowVerticalLines(false);
        identitiesInTable.setDefaultRenderer(Object.class, tableRenderer);
        identitiesInScrollPane.getViewport().setBackground(identitiesInTable.getBackground());
        identitiesInScrollPane.setViewportView(identitiesInTable);

        identitiesjPanel.add(identitiesInScrollPane);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        identitiesjPanel.add(buttonPanel);



        sslCheckBox.setText("Require SSL/TLS encryption");
        add(sslCheckBox,
            new GridBagConstraints(0, 0, GridBagConstraints.REMAINDER, 1, 1.0, 0.0,
                                   GridBagConstraints.WEST,
                                   GridBagConstraints.BOTH,
                                   new Insets(0, 0, 8, 0), 0, 0));


        authButtonGroup = new ButtonGroup();
        getAnonRadio().setSelected(true);
        authButtonGroup.add(getAnonRadio());
        add(getAnonRadio(),
            new GridBagConstraints(0, 1, GridBagConstraints.REMAINDER, 1, 1.0, 0.0,
                                   GridBagConstraints.SOUTHWEST,
                                   GridBagConstraints.HORIZONTAL,
                                   new Insets(0, 0, 0, 0), 0, 0));


        authButtonGroup.add(getAuthRadio());
        add(getAuthRadio(),
            new GridBagConstraints(0, 2, GridBagConstraints.REMAINDER, 1, 1.0, 0.0,
                                   GridBagConstraints.NORTHWEST,
                                   GridBagConstraints.HORIZONTAL,
                                   new Insets(0, 0, 5, 0), 0, 0));

        add(new JLabel("Authentication method:"),
            new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                                   GridBagConstraints.EAST,
                                   GridBagConstraints.NONE,
                                   new Insets(0, 25, 4, 5), 0, 0));
        add(credentialsLocationComboBox,
            new GridBagConstraints(1, 3, GridBagConstraints.REMAINDER, 1, 1.0, 0.0,
                                   GridBagConstraints.WEST,
                                   GridBagConstraints.HORIZONTAL,
                                   new Insets(0, 0, 4, 5), 0, 0));

        add(new JLabel("Identity provider:"),
            new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                                   GridBagConstraints.EAST,
                                   GridBagConstraints.NONE,
                                   new Insets(0, 25, 5, 5), 0, 0));
        add(providersComboBox,
            new GridBagConstraints(1, 4, GridBagConstraints.REMAINDER, 1, 1.0, 0.0,
                                   GridBagConstraints.WEST,
                                   GridBagConstraints.HORIZONTAL,
                                   new Insets(0, 0, 5, 5), 0, 0));

        add(identitiesjPanel,
            new GridBagConstraints(0, 5, GridBagConstraints.REMAINDER, 1, 1.0, 100.0,
                                   GridBagConstraints.CENTER,
                                   GridBagConstraints.BOTH,
                                   new Insets(0, 0, 5, 0), 0, 0));


    }

    private void populateIdentityTables() {
        enableOrDisableIdentityTables();

        if (isAnonymous()) {
            clearIdentitiesTables();
            return;
        }

        try {
            IdentityProvider ip = (IdentityProvider)getProvidersComboBoxModel().getSelectedItem();
            DefaultTableModel modelOut = getIdentitiesOutTableModel();
            modelOut.getDataVector().clear();

            Iterator i = ip.getUserManager().findAllHeaders().iterator();
            while (i.hasNext()) {
                modelOut.addRow(new Object[]{i.next()});
            }
            i = ip.getGroupManager().findAllHeaders().iterator();
            while (i.hasNext()) {
                modelOut.addRow(new Object[]{i.next()});
            }
            DefaultTableModel modelIn = getIdentitiesInTableModel();
            modelIn.getDataVector().clear();
            getIdentitiesOutTableModel().fireTableDataChanged();
            getIdentitiesInTableModel().fireTableDataChanged();
        } catch (FindException ex) {
            ex.printStackTrace();  //todo: fix this with better, general exception management
        }
    }

    private void enableOrDisableIdentityTables() {
        boolean enable = !isAnonymous();
        buttonAdd.setEnabled(enable);
        buttonAddAll.setEnabled(enable);
        buttonRemove.setEnabled(enable);
        buttonRemoveAll.setEnabled(enable);
        identitiesInTable.setEnabled(enable);
        identitiesInTable.tableChanged(null);
        identitiesOutTable.setEnabled(enable);
        identitiesOutTable.tableChanged(null);
        providersComboBox.setEnabled(enable);
        credentialsLocationComboBox.setEnabled(enable);
    }

    private void clearIdentitiesTables() {
        DefaultTableModel modelIn = getIdentitiesInTableModel();
        DefaultTableModel modelOut = getIdentitiesOutTableModel();
        modelIn.getDataVector().clear();
        modelOut.getDataVector().clear();
        getIdentitiesOutTableModel().fireTableDataChanged();
        getIdentitiesInTableModel().fireTableDataChanged();
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
                Object o = providers.next();
                providersComboBoxModel.addElement(o);
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
    private DefaultTableModel getIdentitiesInTableModel() {
        if (identitiesInTableModel != null)
            return identitiesInTableModel;

        identitiesInTableModel = new DefaultTableModel() {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        identitiesInTableModel.addColumn("Have permission");
        return identitiesInTableModel;
    }

    /**
     * @return the table model representing the identities that
     *         are not permitted to use the service.
     */
    private DefaultTableModel getIdentitiesOutTableModel() {
        if (identitiesOutTableModel != null)
            return identitiesOutTableModel;
        identitiesOutTableModel = new DefaultTableModel() {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        identitiesOutTableModel.addColumn("No permission");
        return identitiesOutTableModel;
    }

    /**
     * Provides the wizard with the current data
     *
     * @param settings the object representing wizard panel state
     * @exception IllegalArgumentException if the the data provided
     * by the wizard are not valid.
     */
    public void readSettings(Object settings) throws IllegalArgumentException {
        PublishServiceWizard.ServiceAndAssertion
          collect = (PublishServiceWizard.ServiceAndAssertion)settings;
        if (isSharedPolicy()) {
            applySharedPolicySettings(collect);
        } else {
            applyIndividualPolicySettings(collect);
        }

    }

    /**
     * Provides the wizard with the current data as shared policy
     *
     * @param pa the object representing wizard panel state
     * @exception IllegalArgumentException if the the data provided
     * by the wizard are not valid.
     */
    private void applySharedPolicySettings(PublishServiceWizard.ServiceAndAssertion pa) {
        IdentityProvider ip = (IdentityProvider)getProvidersComboBoxModel().getSelectedItem();

        java.util.List allAssertions = new ArrayList();
        java.util.List identityAssertions = new ArrayList();
        pa.setSharedPolicy(true);
        if (sslCheckBox.isSelected()) {
            allAssertions.add(new SslAssertion());
        }

        if (!isAnonymous()) {
            Iterator it = getIdentitiesInTableModel().getDataVector().iterator();
            while (it.hasNext()) {
                java.util.List row = (java.util.List)it.next();
                EntityHeader eh = (EntityHeader)row.get(0);
                if (EntityType.USER.equals(eh.getType())) {
                    UserBean u = new UserBean();
                    u.setName(eh.getName());
                    u.setLogin(eh.getName());
                    identityAssertions.add(new SpecificUser(ip.getConfig().getOid(), u.getLogin()));
                } else if (EntityType.GROUP.equals(eh.getType())) {
                    GroupBean g = new GroupBean();
                    g.setName(eh.getName());
                    g.getName();
                    MemberOfGroup ma = new MemberOfGroup(ip.getConfig().getOid(), g.getName());
                    identityAssertions.add(ma);
                }
            }
            // crenedtials location, safe
            Object o = credentialsLocationComboBox.getSelectedItem();
            if (o != null) {
                Assertion ca = (Assertion)Components.getCredentialsLocationMap().get(o);
                if (ca != null && !(ca instanceof TrueAssertion)) // trueassertion is anonymous
                    allAssertions.add(ca);
            }
        }

        if (!allAssertions.isEmpty()) {
            allAssertions.add(new OneOrMoreAssertion(identityAssertions));
            pa.setAssertion(new AllAssertion(allAssertions));
        } else {
            pa.setAssertion(new AllAssertion());
        }
    }

    /**
     * Provides the wizard with the current data as individual policies
     *
     * @param pa the object representing wizard panel state
     * @exception IllegalArgumentException if the the data provided
     * by the wizard are not valid.
     */
    private void applyIndividualPolicySettings(PublishServiceWizard.ServiceAndAssertion pa) {
        IdentityProvider ip = (IdentityProvider)getProvidersComboBoxModel().getSelectedItem();
        pa.setSharedPolicy(false);
        java.util.List allAssertions = new ArrayList();


        Iterator it = getIdentitiesInTableModel().getDataVector().iterator();
        while (it.hasNext()) {

            java.util.List identityAssertion = new ArrayList();


            if (sslCheckBox.isSelected()) {
                identityAssertion.add(new SslAssertion());
            }

            // crenedtials location, safe
            Object o = credentialsLocationComboBox.getSelectedItem();
            if (o != null && !isAnonymous()) {
                Assertion ca = (Assertion)Components.getCredentialsLocationMap().get(o);
                if (ca != null && !(ca instanceof TrueAssertion)) // trueassertion is anonymous
                    identityAssertion.add(ca);
            }


            java.util.List row = (java.util.List)it.next();
            EntityHeader eh = (EntityHeader)row.get(0);
            if (EntityType.USER.equals(eh.getType())) {
                UserBean u = new UserBean();
                u.setName(eh.getName());
                u.setLogin(eh.getName());
                identityAssertion.add(new SpecificUser(ip.getConfig().getOid(), u.getLogin()));
            } else if (EntityType.GROUP.equals(eh.getType())) {
                GroupBean g = new GroupBean();
                g.setName(eh.getName());
                g.getName();
                MemberOfGroup ma = new MemberOfGroup(ip.getConfig().getOid(), g.getName());
                identityAssertion.add(ma);
            }
            allAssertions.add(new AllAssertion(identityAssertion));
        }
        pa.setAssertion(new OneOrMoreAssertion(allAssertions));

    }


    /** @return the wizard step label    */
    public String getStepLabel() {
        return "Access control";
    }

    private boolean isAnonymous() {
        return getAnonRadio().isSelected();
    }

    private boolean isSharedPolicy() {
        return true;
    }


    private void equalizeButtons() {
        JButton buttons[] = new JButton[]{
            buttonAdd,
            buttonAddAll,
            buttonRemove,
            buttonRemoveAll
        };
        Utilities.equalizeComponentSizes(buttons);
//        JComponent cboxes[] = new JComponent[]{
//            sharedPolicyCheckBox,
//            sslCheckBox
//        };
//        Utilities.equalizeComponentSizes(cboxes);
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
              if (!table.isEnabled()) {
                  this.setEnabled(false);
              } else {
                  this.setEnabled(true);
                  if (iss) {
                      this.setBackground(table.getSelectionBackground());
                      this.setForeground(table.getSelectionForeground());
                  } else {
                      this.setBackground(table.getBackground());
                      this.setForeground(table.getForeground());
                  }
              }

              this.setFont(new Font("Dialog", Font.PLAIN, 12));
              EntityHeader h = (EntityHeader)value;
              EntityType type = h.getType();
              ImageIcon icon = IdentityProviderWizardPanel.this.getIcon(type);
              if (icon != null) setIcon(icon);
              setText(h.getName());
              return this;
          }
      };

    /**
     * Get the Icon for the Class passed.
     *
     * @param type   the entity type enum
     * @return ImageIcon for the given node
     */
    private ImageIcon getIcon(EntityType type) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        if (type.equals(EntityType.GROUP)) {
            return new ImageIcon(ImageCache.getInstance().getIcon(GroupPanel.GROUP_ICON_RESOURCE));
        } else if (type.equals(EntityType.USER)) {
            return new ImageIcon(ImageCache.getInstance().getIcon(UserPanel.USER_ICON_RESOURCE));
        }
        return null;
    }

    private JTable identitiesInTable;
    private JTable identitiesOutTable;
    private DefaultTableModel identitiesInTableModel;
    private DefaultTableModel identitiesOutTableModel;

    private JScrollPane identitiesOutScrollPane;

    private JButton buttonAddAll;
    private JPanel identitiesjPanel;
    private JButton buttonRemove;
    private JButton buttonRemoveAll;
    private JComboBox providersComboBox;
    private JScrollPane identitiesInScrollPane;
    private JButton buttonAdd;
    private JComboBox credentialsLocationComboBox;
    private JPanel buttonPanel;
    private JPanel usersLabelPanel;
    private JRadioButton anonRadio;
    private JRadioButton authRadio;
    private ButtonGroup authButtonGroup;

    private JCheckBox sslCheckBox;

    private JRadioButton getAnonRadio() {
        if (anonRadio == null) {
            anonRadio = new JRadioButton("Allow anonymous access");
            anonRadio.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    populateIdentityTables();
                }
            });
        }
        return anonRadio;
    }

    private JRadioButton getAuthRadio() {
        if (authRadio == null) {
            authRadio = new JRadioButton("Require users to authenticate");
            authRadio.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    populateIdentityTables();
                }
            });
        }
        return authRadio;
    }
}
