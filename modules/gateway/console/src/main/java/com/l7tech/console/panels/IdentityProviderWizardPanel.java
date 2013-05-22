package com.l7tech.console.panels;

import com.l7tech.console.util.*;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.gateway.common.admin.IdentityAdmin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.Principal;
import java.util.*;


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
    private Map credentialsLocationMap = null;
    private boolean isSoap = true;

    /**
     * Creates new form IdentityProviderWizardPanel
     */
    public IdentityProviderWizardPanel(boolean isSoap) {
        super(null);
        this.isSoap = isSoap;
        credentialsLocationMap = CredentialsLocation.newCredentialsLocationMap(isSoap);
        initComponents();
        equalizeButtons();
        populateIdentityTables();
    }

    @Override
    public String getDescription() {
        if (isSoap) {
            return "Specify Web Service access security and permissions.";
        } else {
            return "Specify non-SOAP application access security and permissions.";
        }
    }

    public IdentityProviderWizardPanel(WizardStepPanel next, boolean isSoap) {
        super(next);
        this.isSoap = isSoap;
        credentialsLocationMap = CredentialsLocation.newCredentialsLocationMap(isSoap);
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
        final JPanel identitiesjPanel = new JPanel();
        final JScrollPane identitiesOutScrollPane = new JScrollPane();
        identitiesOutTable = new JTable();
        final JPanel usersLabelPanel = new JPanel();
        buttonAdd = new JButton();
        buttonAddAll = new JButton();
        buttonRemove = new JButton();
        buttonRemoveAll = new JButton();
        final JScrollPane identitiesInScrollPane = new JScrollPane();
        identitiesInTable = new JTable();
        final JPanel buttonPanel = new JPanel();

        ToolTipManager.sharedInstance().registerComponent(identitiesInTable);
        ToolTipManager.sharedInstance().registerComponent(identitiesOutTable);

        credentialsLocationComboBox = new JComboBox();
        credentialsLocationComboBox.setModel(CredentialsLocation.getCredentialsLocationComboBoxModelNonAnonymous(isSoap));

        sslCheckBox = new JCheckBox();

        setLayout(new GridBagLayout());

        providersComboBox.setModel(getProvidersComboBoxModel());
        providersComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    IdentityProviderConfig ipc = (IdentityProviderConfig) value;
                    setText(ipc.getName());
                }
                return c;
            }
        });
        providersComboBox.addActionListener(new ActionListener() {
            Object oldSelectedItem = null;

            /**
             * Invoked when an action occurs.
             */
            @Override
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
        buttonAdd.setIcon(IconManager.getInstance().getIconAdd());

        buttonAdd.addActionListener(new ActionListener() {
            /**
             * Invoked when an action occurs.
             */
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] rows = identitiesOutTable.getSelectedRows();
                Object[] toAdd = new Object[rows.length];
                Object[] listOut = getIdentitiesOutTableModel().getDataSet();

                for (int i = 0; i < rows.length; i++) {
                    toAdd[i] = listOut[rows[i]];
                }
                getIdentitiesOutTableModel().removeRows(toAdd);
                getIdentitiesOutTableModel().fireTableDataChanged();
                getIdentitiesInTableModel().addRows(toAdd);
                getIdentitiesInTableModel().fireTableDataChanged();

            }
        });
        usersLabelPanel.add(buttonAdd);

        buttonAddAll.setText("Add All");
        buttonAddAll.setHorizontalTextPosition(SwingConstants.RIGHT);
        buttonAddAll.setIcon(IconManager.getInstance().getIconAddAll());

        buttonAddAll.addActionListener(new ActionListener() {
            /**
             * Invoked when an action occurs.
             */
            @Override
            public void actionPerformed(ActionEvent e) {
                Object[] listOut = getIdentitiesOutTableModel().getDataSet();

                getIdentitiesInTableModel().addRows(listOut);
                getIdentitiesOutTableModel().clearDataSet();
                getIdentitiesOutTableModel().fireTableDataChanged();
                getIdentitiesInTableModel().fireTableDataChanged();

            }
        });
        usersLabelPanel.add(buttonAddAll);

        buttonRemove.setText("Remove");
        buttonRemove.setHorizontalTextPosition(SwingConstants.RIGHT);
        buttonRemove.setIcon(IconManager.getInstance().getIconRemove());

        buttonRemove.addActionListener(new ActionListener() {
            /**
             * Invoked when an action occurs.
             */
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] rows = identitiesInTable.getSelectedRows();
                Object[] toRemove = new Object[rows.length];
                Object[] listIn = getIdentitiesInTableModel().getDataSet();

                for (int i = 0; i < rows.length; i++) {
                    toRemove[i] = listIn[rows[i]];
                }
                getIdentitiesInTableModel().removeRows(toRemove);
                getIdentitiesInTableModel().fireTableDataChanged();
                getIdentitiesOutTableModel().addRows(toRemove);
                getIdentitiesOutTableModel().fireTableDataChanged();
            }
        });

        usersLabelPanel.add(buttonRemove);

        buttonRemoveAll.setText("Remove All");
        buttonRemoveAll.setHorizontalTextPosition(SwingConstants.RIGHT);
        buttonRemoveAll.setIcon(IconManager.getInstance().getIconRemoveAll());

        buttonRemoveAll.addActionListener(new ActionListener() {
            /**
             * Invoked when an action occurs.
             */
            @Override
            public void actionPerformed(ActionEvent e) {
                Object[] listIn = getIdentitiesInTableModel().getDataSet();
                getIdentitiesOutTableModel().addRows(listIn);
                getIdentitiesInTableModel().clearDataSet();
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

        sslCheckBox.setText("Require SSL/TLS Encryption");
        add(sslCheckBox,
          new GridBagConstraints(0, 0, GridBagConstraints.REMAINDER, 1, 1.0, 0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.VERTICAL,
            new Insets(0, 0, 8, 0), 0, 0));


        final ButtonGroup authButtonGroup = new ButtonGroup();
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

        add(new JLabel("Authentication Method:"),
          new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, 25, 4, 5), 0, 0));
        add(credentialsLocationComboBox,
          new GridBagConstraints(1, 3, GridBagConstraints.REMAINDER, 1, 1.0, 0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL,
            new Insets(0, 0, 4, 5), 0, 0));

        add(new JLabel("Identity Provider:"),
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


        zoneControl = new SecurityZoneWidget();
        zoneControl.configure(EntityType.SERVICE, OperationType.CREATE, null);
        add(zoneControl,
                new GridBagConstraints(0, 6, GridBagConstraints.REMAINDER, 1, 1.0, 0.0,
                        GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL,
                        new Insets(0, 0, 0, 0), 0, 0));
    }

    private void populateIdentityTables() {
        enableOrDisableIdentityTables();

        if (isAnonymous()) {
            clearIdentitiesTables();
            return;
        }

        try {
            IdentityProviderConfig ipc = (IdentityProviderConfig)getProvidersComboBoxModel().getSelectedItem();
            SortedSingleColumnTableModel modelOut = getIdentitiesOutTableModel();
            modelOut.clearDataSet();

            IdentityAdmin admin = Registry.getDefault().getIdentityAdmin();
            EntityHeaderSet<IdentityHeader> identities = admin.findAllUsers(ipc.getOid());

            for(IdentityHeader header : identities) {
                User u = admin.findUserByID(ipc.getOid(), header.getStrId());
                modelOut.addRow(u);
            }

            EntityHeaderSet<IdentityHeader> groups = admin.findAllGroups(ipc.getOid());
            for (IdentityHeader header : groups) {
                Group g = admin.findGroupByID(ipc.getOid(), header.getStrId());
                modelOut.addRow(g);
            }

            if (identities.isMaxExceeded() || groups.isMaxExceeded()) {
                JOptionPane.showMessageDialog(identitiesInTable, "Not all identities can be displayed " +
                                                                 "because the search criterion is too wide");
            }

            //SortedSingleColumnTableModel modelIn = getIdentitiesInTableModel();
            //modelIn.clearDataSet();

            getIdentitiesOutTableModel().fireTableDataChanged();
            getIdentitiesInTableModel().fireTableDataChanged();
        } catch (Exception ex) {
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
        SortedSingleColumnTableModel modelIn = getIdentitiesInTableModel();
        SortedSingleColumnTableModel modelOut = getIdentitiesOutTableModel();
        modelIn.clearDataSet();
        modelOut.clearDataSet();
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
            final IdentityAdmin admin = Registry.getDefault().getIdentityAdmin();
            EntityHeader[] headers = admin.findAllIdentityProviderConfig();
            for ( EntityHeader header : headers ) {
                providersComboBoxModel.addElement( admin.findIdentityProviderConfigByID( header.getOid() ) );
            }
        } catch (Exception e) {
            e.printStackTrace();  //todo: fix this with better, general exception management
        }
        return providersComboBoxModel;
    }

    /**
     * @return the table model representing the identities that
     *         have permisison to use the service.
     */
    private SortedSingleColumnTableModel getIdentitiesInTableModel() {
        if (identitiesInTableModel != null)
            return identitiesInTableModel;

        identitiesInTableModel = new SortedSingleColumnTableModel(new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                Principal e1 = (Principal)o1;
                Principal e2 = (Principal)o2;

                return e1.getName().compareToIgnoreCase(e2.getName());
            }
        }) {

            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        identitiesInTableModel.addColumn("Have Permission");
        return identitiesInTableModel;
    }

    /**
     * @return the table model representing the identities that
     *         are not permitted to use the service.
     */
    private SortedSingleColumnTableModel getIdentitiesOutTableModel() {
        if (identitiesOutTableModel != null)
            return identitiesOutTableModel;

        identitiesOutTableModel = new SortedSingleColumnTableModel(new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                Principal e1 = (Principal)o1;
                Principal e2 = (Principal)o2;

                return e1.getName().compareToIgnoreCase(e2.getName());
            }
        }) {

            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        identitiesOutTableModel.addColumn("No Permission");
        return identitiesOutTableModel;
    }

    /**
     * Provides the wizard with the current data
     * 
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    @SuppressWarnings({ "unchecked" })
    @Override
    public void readSettings(Object settings) throws IllegalArgumentException {
        if (settings instanceof PublishServiceWizard.ServiceAndAssertion) {
            PublishServiceWizard.ServiceAndAssertion
              collect = (PublishServiceWizard.ServiceAndAssertion)settings;
            applySharedPolicySettings(collect);
        } else if (settings instanceof ArrayList) {
            populateAssertions((ArrayList<Assertion>)settings);
        }
    }

    @Override
    public void storeSettings(Object settings) throws IllegalArgumentException {
        readSettings(settings);
    }

    public SecurityZone getSelectedSecurityZone() {
        return zoneControl.getSelectedZone();
    }

    private void populateAssertions(ArrayList<Assertion> allAssertions) {
        java.util.List<Assertion> identityAssertions = new ArrayList<Assertion>();
        if (sslCheckBox.isSelected()) {
            allAssertions.add(new SslAssertion());
        }

        if (!isAnonymous()) {
            Iterator it = getIdentitiesInTableModel().getDataIterator();
            while (it.hasNext()) {
                Principal p = (Principal)it.next();
                if (p instanceof User) {
                    User u = (User)p;
                    identityAssertions.add(new SpecificUser(u.getProviderId(), u.getLogin(), u.getId(), u.getName()));
                } else if (p instanceof Group) {
                    Group g = (Group)p;
                    MemberOfGroup ma = new MemberOfGroup(g.getProviderId(), g.getName(), g.getId());
                    identityAssertions.add(ma);
                }
            }
            // crenedtials location, safe
            Object o = credentialsLocationComboBox.getSelectedItem();
            if (o != null) {
                Assertion ca = (Assertion)credentialsLocationMap.get(o);
                if (ca != null && !(ca instanceof TrueAssertion)) // trueassertion is anonymous
                    allAssertions.add(ca);
            }
        }

        if (!allAssertions.isEmpty()) {
            allAssertions.add(new OneOrMoreAssertion(identityAssertions));
        }
    }

    /**
     * Provides the wizard with the current data as shared policy
     *
     * @param pa the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    private void applySharedPolicySettings(PublishServiceWizard.ServiceAndAssertion pa) {
        java.util.ArrayList<Assertion> assertionList = new ArrayList<Assertion>();
        populateAssertions(assertionList);
        pa.setSharedPolicy(true);
        if (!assertionList.isEmpty()) {
            pa.setAssertion(new AllAssertion(assertionList));
        } else {
            pa.setAssertion(new AllAssertion());
        }
        if (pa.getService() != null) {
            pa.getService().setSecurityZone(zoneControl.getSelectedZone());
            if (pa.getService().getPolicy() != null) {
                // service policy inherits same security zone as the service
                pa.getService().getPolicy().setSecurityZone(zoneControl.getSelectedZone());
            }
        }
    }

    /**
     * @return the wizard step label
     */
    @Override
    public String getStepLabel() {
        return "Access Control";
    }

    private boolean isAnonymous() {
        return getAnonRadio().isSelected();
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
          @Override
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
              Principal p = (Principal)value;
              ImageIcon icon = IdentityProviderWizardPanel.this.getIcon(p);
              if (icon != null) setIcon(icon);
              setText(p.getName());
              setToolTipText(null);

              if (p instanceof Group) {
                  // assume that the strid is a valuable piece of information if it;s something else than a number
                  String strid = ((Group)p).getId();
                  String tt;
                  try {
                      Long.parseLong(strid);
                      tt = null;
                  } catch (NumberFormatException nfe) {
                      tt = strid;
                  }
                  setToolTipText(tt);
              }
              return this;
          }
      };

    /**
     * Get the Icon for the Class passed.
     * 
     * @param p the entity type enum
     * @return ImageIcon for the given node
     */
    private ImageIcon getIcon(Principal p) {
        if (p == null) {
            throw new NullPointerException("type");
        }
        if (p instanceof Group) {
            return new ImageIcon(ImageCache.getInstance().getIcon(GroupPanel.GROUP_ICON_RESOURCE));
        } else if (p instanceof User) {
            return new ImageIcon(ImageCache.getInstance().getIcon(UserPanel.USER_ICON_RESOURCE));
        }
        return null;
    }

    private JTable identitiesInTable;
    private JTable identitiesOutTable;
    private SortedSingleColumnTableModel identitiesInTableModel;
    private SortedSingleColumnTableModel identitiesOutTableModel;

    private JButton buttonAddAll;
    private JButton buttonRemove;
    private JButton buttonRemoveAll;
    private JComboBox providersComboBox;
    private JButton buttonAdd;
    private JComboBox credentialsLocationComboBox;
    private JRadioButton anonRadio;
    private JRadioButton authRadio;
    private SecurityZoneWidget zoneControl;
    private JCheckBox sslCheckBox;

    private JRadioButton getAnonRadio() {
        if (anonRadio == null) {
            anonRadio = new JRadioButton("Allow Anonymous Access");
            anonRadio.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    populateIdentityTables();
                }
            });
        }
        return anonRadio;
    }

    private JRadioButton getAuthRadio() {
        if (authRadio == null) {
            authRadio = new JRadioButton("Require Users to Authenticate:");
            authRadio.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    populateIdentityTables();
                }
            });
        }
        return authRadio;
    }
}
