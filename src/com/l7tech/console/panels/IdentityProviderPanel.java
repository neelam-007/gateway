package com.l7tech.console.panels;

import com.l7tech.console.util.IconManager;
import com.l7tech.console.util.IconManager2;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.credential.wss.WssDigest;
import com.l7tech.policy.assertion.credential.wss.WssClientCert;
import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.security.Principal;


/**
 * <code>IdentityProviderPanel</code> that represent a step in the wizard
 * <code>WizardStepPanel</code> that collects the published service identities
 * info.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class IdentityProviderPanel extends WizardStepPanel {
    private DefaultComboBoxModel providersComboBoxModel;
    private ComboBoxModel credentialsLocationComboBoxModel;
    private JCheckBox anonymousAccessCheckBox;

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
        providerSelectorPanel = new JPanel();
        providersComboBox = new JComboBox();
        identitiesPanel = new JPanel();
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
        credentialsAndTransportPanel = new JPanel();
        credentialsLocationjPanel = new JPanel();
        credentialsLocationComboBox = new JComboBox();
        sslCheckBox = new JCheckBox();
        sslPanel = new JPanel();
        anonymousAccessCheckBox = new JCheckBox();

        setLayout(new BorderLayout());

        providerSelectorPanel.setLayout(new BorderLayout());


        providersComboBox.setModel(getProvidersComboBoxModel());
        providersComboBox.setRenderer(new ListCellRenderer() {
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
        providersComboBox.addActionListener(new ActionListener() {
            /** Invoked when an action occurs.  */
            public void actionPerformed(ActionEvent e) {
                try {
                    IdentityProvider ip = (IdentityProvider)providersComboBoxModel.getSelectedItem();
                    if (ip == NO_PROVIDER) {
                        DefaultTableModel modelIn = getIdentitiesInTableModel();
                        DefaultTableModel modelOut = getIdentitiesOutTableModel();
                        modelIn.getDataVector().clear();
                        modelOut.getDataVector().clear();
                        getIdentitiesOutTableModel().fireTableDataChanged();
                        getIdentitiesInTableModel().fireTableDataChanged();
                        return;
                    }
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
        });

        providerSelectorPanel.add(providersComboBox, BorderLayout.WEST);
        anonymousAccessCheckBox.setText("Anonymous access");
        anonymousAccessCheckBox.setHorizontalTextPosition(SwingConstants.TRAILING);
        anonymousAccessCheckBox.
          addActionListener(new ActionListener() {
              /** Invoked when an action occurs. */
              public void actionPerformed(ActionEvent e) {
                  JCheckBox cb = (JCheckBox)e.getSource();
                  boolean enable = !cb.isSelected();
                  buttonAdd.setEnabled(enable);
                  buttonAddAll.setEnabled(enable);
                  buttonRemove.setEnabled(enable);
                  buttonRemoveAll.setEnabled(enable);
                  identitiesInTable.setEnabled(enable);
                  identitiesInTable.tableChanged(null);
                  identitiesOutTable.setEnabled(enable);
                  identitiesOutTable.tableChanged(null);
                  credentialsLocationComboBox.setEnabled(enable);
                  providersComboBox.setEnabled(enable);
              }
          });
        providerSelectorPanel.add(anonymousAccessCheckBox, BorderLayout.EAST);

        add(providerSelectorPanel, BorderLayout.NORTH);


        identitiesPanel.setLayout(new BorderLayout());

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

        identitiesPanel.add(identitiesjPanel, BorderLayout.EAST);

        add(identitiesPanel, BorderLayout.WEST);
        credentialsAndTransportPanel.setLayout(new BoxLayout(credentialsAndTransportPanel, BoxLayout.Y_AXIS));

        credentialsAndTransportPanel.setBorder(new TitledBorder("Credentials/transport"));
        credentialsLocationjPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        sslPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        credentialsLocationComboBox.setModel(getCredentialsLocationComboBoxModel());

        credentialsLocationjPanel.add(credentialsLocationComboBox);
        credentialsAndTransportPanel.add(credentialsLocationjPanel);

        sslCheckBox.setText("SSL/TLS");
        sslCheckBox.setHorizontalTextPosition(SwingConstants.LEADING);
        sslPanel.add(sslCheckBox);
        credentialsAndTransportPanel.add(sslPanel);
        JPanel ra = new JPanel();
        ra.setLayout(new BorderLayout());
        ra.add(Box.createGlue());
        credentialsAndTransportPanel.add(ra);

        add(credentialsAndTransportPanel, java.awt.BorderLayout.CENTER);
    }

    /**
     * create the credentials location (http, ws message) locaiton combo model
     *
     * @return the <code>ComboBoxModel</code> with credentials location list
     */
    private ComboBoxModel getCredentialsLocationComboBoxModel() {
        if (credentialsLocationComboBoxModel != null)
            return credentialsLocationComboBoxModel;

        credentialsLocationComboBoxModel =
          new DefaultComboBoxModel(credentialsLocationMap.keySet().toArray());
        return credentialsLocationComboBoxModel;
    }

    /**
     * @return the list of providers combo box model
     */
    private DefaultComboBoxModel getProvidersComboBoxModel() {
        if (providersComboBoxModel != null)
            return providersComboBoxModel;

        providersComboBoxModel = new DefaultComboBoxModel();
        try {

            providersComboBoxModel.addElement(NO_PROVIDER);
            providersComboBoxModel.addElement(Registry.getDefault().getInternalProvider());
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
    private DefaultTableModel getIdentitiesInTableModel() {
        if (identitiesInTableModel != null)
            return identitiesInTableModel;

        identitiesInTableModel = new DefaultTableModel();
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
        identitiesOutTableModel = new DefaultTableModel();
        identitiesOutTableModel.addColumn("No permission");
        return identitiesOutTableModel;
    }

    public String getDescription() {
        return "Select the identities (users, groups) that are allowed to access the published service" +
          " Specify where the credentials are located and transport layewr security";
    }

    /**
     * Test whether the step is finished and it is safe to proceed to the next
     * one.
     * If the step is valid, the "Next" (or "Finish") button will be enabled.
     *
     * @return true if the panel is valid, false otherwis
     */
    public boolean isValid() {
        return true;
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
        IdentityProvider ip = (IdentityProvider)providersComboBoxModel.getSelectedItem();

        java.util.List allAssertions = new ArrayList();
        java.util.List identityAssertions = new ArrayList();

        if (!anonymousAccessCheckBox.isSelected()) {
            Iterator it = getIdentitiesInTableModel().getDataVector().iterator();
            while (it.hasNext()) {
                java.util.List row = (java.util.List)it.next();
                EntityHeader eh = (EntityHeader)row.get(0);
                if (EntityType.USER.equals(eh.getType())) {
                    User u = new User();
                    u.setName(eh.getName());
                    u.setLogin(eh.getName());
                    identityAssertions.add(new SpecificUser(ip.getConfig().getOid(), u.getLogin() ));
                } else if (EntityType.GROUP.equals(eh.getType())) {
                    Group g = new Group();
                    g.setName(eh.getName());
                    identityAssertions.add(new MemberOfGroup(ip.getConfig().getOid(), g.getName() ));
                }
            }
            // crenedtials location, safe
            Object o = credentialsLocationComboBox.getSelectedItem();
            if (o != null) {
                CredentialSourceAssertion ca =
                  (CredentialSourceAssertion)credentialsLocationMap.get(o);
                if (ca != null)
                    allAssertions.add(ca);
            }
        }


        if (sslCheckBox.isSelected()) {
            allAssertions.add(new SslAssertion());
        }

        if (!allAssertions.isEmpty()) {
            allAssertions.add(new OneOrMoreAssertion(identityAssertions));
            collect.setAssertion(new AllAssertion(allAssertions));
        }
    }

    /** @return the wizard step label    */
    public String getStepLabel() {
        return "Access control";
    }

    private void equalizeButtons() {
        JButton buttons[] = new JButton[]{
            buttonAdd,
            buttonAddAll,
            buttonRemove,
            buttonRemoveAll
        };
        Utilities.equalizeComponentSizes(buttons);
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
              ImageIcon icon = IdentityProviderPanel.this.getIcon(type);
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
            return new ImageIcon(IconManager2.getInstance().getIcon(GroupPanel.GROUP_ICON_RESOURCE));
        } else if (type.equals(EntityType.USER)) {
            return new ImageIcon(IconManager2.getInstance().getIcon(UserPanel.USER_ICON_RESOURCE));
        }
        return null;
    }

    static Map credentialsLocationMap = new TreeMap();

    // maping assertion to tree nodes to
    static {
        credentialsLocationMap.put("HTTP basic", new HttpBasic());
        credentialsLocationMap.put("HTTP digest", new HttpDigest());
        // credentialsLocationMap.put("HTTP client cert", new HttpClientCert());

        credentialsLocationMap.put("WSS token basic", new WssBasic());
        credentialsLocationMap.put("WSS token digest", new WssDigest());
        // credentialsLocationMap.put("WSS client cert", new WssClientCert());
    }

    /**
     * the <b>no provider</b> anonymous class, used for combo selection tghat
     * indicates no provder is selected
     */
    private static final IdentityProvider
      NO_PROVIDER = new IdentityProvider() {
          public void initialize(IdentityProviderConfig config) {
          }

          public IdentityProviderConfig getConfig() { return config; }

          public UserManager getUserManager() { return null; }

          public GroupManager getGroupManager() { return null; }

          public boolean authenticate( User user, byte[] credentials) { return false; }

          public boolean isReadOnly() { return true; }

          IdentityProviderConfig config = new IdentityProviderConfig();

          {
              config.setDescription("No provider selected ");
          }

      };
    private JTable identitiesInTable;
    private JTable identitiesOutTable;
    private DefaultTableModel identitiesInTableModel;
    private DefaultTableModel identitiesOutTableModel;

    private JScrollPane identitiesOutScrollPane;
    private JPanel sslPanel;

    private JButton buttonAddAll;
    private JPanel identitiesjPanel;
    private JPanel credentialsLocationjPanel;
    private JButton buttonRemove;
    private JPanel providerSelectorPanel;
    private JPanel identitiesPanel;
    private JButton buttonRemoveAll;
    private JComboBox providersComboBox;
    private JScrollPane identitiesInScrollPane;
    private JButton buttonAdd;
    private JComboBox credentialsLocationComboBox;
    private JPanel credentialsAndTransportPanel;
    private JPanel buttonPanel;
    private JPanel usersLabelPanel;

    private JCheckBox sslCheckBox;
}
