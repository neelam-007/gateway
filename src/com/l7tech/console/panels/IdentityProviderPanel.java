package com.l7tech.console.panels;

import com.l7tech.console.util.IconManager;
import com.l7tech.console.util.IconManager2;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
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
        providerSelectorjPanel = new JPanel();
        selecProviderjLabel = new JLabel();
        providersjComboBox = new JComboBox();
        identitiesPanel = new JPanel();
        identitiesjPanel = new JPanel();
        identitiesOutjScrollPane = new JScrollPane();
        identitiesOutjTable = new JTable();
        usersLabeljPanel = new JPanel();
        jButtonAdd = new JButton();
        jButtonAddAll = new JButton();
        jButtonRemove = new JButton();
        jButtonRemoveAll = new JButton();
        identitiesInjScrollPane = new JScrollPane();
        identitiesInjTable = new JTable();
        buttonjPanel = new JPanel();
        credentialsAndTransportjPanel = new JPanel();
        credentialsLocationjPanel = new JPanel();
        credentialsLocationjComboBox = new JComboBox();
        ssljCheckBox = new JCheckBox();
        ssljPanel = new JPanel();
        anonymousAccessCheckBox = new JCheckBox();

        setLayout(new BorderLayout());

        providerSelectorjPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        selecProviderjLabel.setText("Select the identity provider:");
        selecProviderjLabel.setBorder(new EmptyBorder(new java.awt.Insets(2, 2, 2, 2)));
        providerSelectorjPanel.add(selecProviderjLabel);


        providersjComboBox.setModel(getProvidersComboBoxModel());
        providersjComboBox.setRenderer(new ListCellRenderer() {
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

        providerSelectorjPanel.add(providersjComboBox);
        anonymousAccessCheckBox.setText("Anonymous access");
        anonymousAccessCheckBox.setHorizontalTextPosition(SwingConstants.TRAILING);
        anonymousAccessCheckBox.
          addActionListener(new ActionListener() {
              /** Invoked when an action occurs. */
              public void actionPerformed(ActionEvent e) {
                  JCheckBox cb = (JCheckBox)e.getSource();
                  boolean enable = !cb.isSelected();
                  jButtonAdd.setEnabled(enable);
                  jButtonAddAll.setEnabled(enable);
                  jButtonRemove.setEnabled(enable);
                  jButtonRemoveAll.setEnabled(enable);
                  identitiesInjTable.setEnabled(enable);
                  identitiesInjTable.tableChanged(null);
                  identitiesOutjTable.setEnabled(enable);
                  identitiesOutjTable.tableChanged(null);
                  credentialsLocationjComboBox.setEnabled(enable);
                  providersjComboBox.setEnabled(enable);
              }
          });
        providerSelectorjPanel.add(anonymousAccessCheckBox);

        add(providerSelectorjPanel, BorderLayout.NORTH);


        identitiesPanel.setLayout(new BorderLayout());

        identitiesjPanel.setLayout(new BoxLayout(identitiesjPanel, BoxLayout.X_AXIS));

        identitiesjPanel.setBorder(new EmptyBorder(new Insets(20, 5, 10, 5)));
        identitiesOutjScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        identitiesOutjScrollPane.setPreferredSize(new java.awt.Dimension(150, 50));
        identitiesOutjTable.setModel(getIdentitiesOutTableModel());

        identitiesOutjTable.setShowHorizontalLines(false);
        identitiesOutjTable.setShowVerticalLines(false);
        identitiesOutjTable.setDefaultRenderer(Object.class, tableRenderer);
        identitiesOutjScrollPane.getViewport().setBackground(identitiesOutjTable.getBackground());
        identitiesOutjScrollPane.setViewportView(identitiesOutjTable);

        identitiesjPanel.add(identitiesOutjScrollPane);

        usersLabeljPanel.setLayout(new BoxLayout(usersLabeljPanel, BoxLayout.Y_AXIS));

        usersLabeljPanel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 5)));
        jButtonAdd.setText("Add");
        jButtonAdd.setHorizontalTextPosition(SwingConstants.RIGHT);
        jButtonAdd.setHorizontalAlignment(SwingConstants.RIGHT);
        jButtonAdd.setIcon(IconManager.getInstance().getIconAdd());

        jButtonAdd.addActionListener(new ActionListener() {
            /** Invoked when an action occurs. */
            public void actionPerformed(ActionEvent e) {
                int[] rows = identitiesOutjTable.getSelectedRows();
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
        usersLabeljPanel.add(jButtonAdd);

        jButtonAddAll.setText("Add All");
        jButtonAddAll.setHorizontalTextPosition(SwingConstants.RIGHT);
        jButtonAddAll.setHorizontalAlignment(SwingConstants.RIGHT);
        jButtonAddAll.setIcon(IconManager.getInstance().getIconAddAll());

        jButtonAddAll.addActionListener(new ActionListener() {
            /** Invoked when an action occurs. */
            public void actionPerformed(ActionEvent e) {
                java.util.List listOut = getIdentitiesOutTableModel().getDataVector();

                getIdentitiesInTableModel().getDataVector().addAll(listOut);
                getIdentitiesOutTableModel().getDataVector().clear();
                getIdentitiesOutTableModel().fireTableDataChanged();
                getIdentitiesInTableModel().fireTableDataChanged();

            }
        });
        usersLabeljPanel.add(jButtonAddAll);

        jButtonRemove.setText("Remove");
        jButtonRemove.setHorizontalTextPosition(SwingConstants.RIGHT);
        jButtonRemove.setHorizontalAlignment(SwingConstants.RIGHT);
        jButtonRemove.setIcon(IconManager.getInstance().getIconRemove());

        jButtonRemove.addActionListener(new ActionListener() {
            /** Invoked when an action occurs. */
            public void actionPerformed(ActionEvent e) {
                int[] rows = identitiesInjTable.getSelectedRows();
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

        usersLabeljPanel.add(jButtonRemove);

        jButtonRemoveAll.setText("Remove all");
        jButtonRemoveAll.setHorizontalTextPosition(SwingConstants.RIGHT);
        jButtonRemoveAll.setHorizontalAlignment(SwingConstants.RIGHT);
        jButtonRemoveAll.setIcon(IconManager.getInstance().getIconRemoveAll());

        jButtonRemoveAll.addActionListener(new ActionListener() {
            /** Invoked when an action occurs. */
            public void actionPerformed(ActionEvent e) {
                java.util.List listIn = getIdentitiesInTableModel().getDataVector();
                getIdentitiesOutTableModel().getDataVector().addAll(listIn);
                getIdentitiesInTableModel().getDataVector().clear();
                getIdentitiesOutTableModel().fireTableDataChanged();
                getIdentitiesInTableModel().fireTableDataChanged();

            }
        });

        usersLabeljPanel.add(jButtonRemoveAll);

        identitiesjPanel.add(usersLabeljPanel);

        identitiesInjScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        identitiesInjScrollPane.setPreferredSize(new java.awt.Dimension(150, 50));
        identitiesInjTable.setModel(getIdentitiesInTableModel());

        identitiesInjTable.setShowHorizontalLines(false);
        identitiesInjTable.setShowVerticalLines(false);
        identitiesInjTable.setDefaultRenderer(Object.class, tableRenderer);
        identitiesInjScrollPane.getViewport().setBackground(identitiesInjTable.getBackground());
        identitiesInjScrollPane.setViewportView(identitiesInjTable);

        identitiesjPanel.add(identitiesInjScrollPane);

        buttonjPanel.setLayout(new BoxLayout(buttonjPanel, BoxLayout.Y_AXIS));

        identitiesjPanel.add(buttonjPanel);

        identitiesPanel.add(identitiesjPanel, java.awt.BorderLayout.EAST);

        add(identitiesPanel, BorderLayout.WEST);
        credentialsAndTransportjPanel.setLayout(new BoxLayout(credentialsAndTransportjPanel, BoxLayout.Y_AXIS));

        credentialsAndTransportjPanel.setBorder(new TitledBorder("Credentials/transport"));
        credentialsLocationjPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        ssljPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        credentialsLocationjComboBox.setModel(getCredentialsLocationComboBoxModel());

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
                    identityAssertions.add(new SpecificUser(ip, u));
                } else if (EntityType.GROUP.equals(eh.getType())) {
                    Group g = new Group();
                    g.setName(eh.getName());
                    identityAssertions.add(new MemberOfGroup(ip, g));
                }
            }
            // crenedtials location, safe
            Object o = credentialsLocationjComboBox.getSelectedItem();
            if (o != null) {
                CredentialSourceAssertion ca =
                  (CredentialSourceAssertion)credentialsLocationMap.get(o);
                if (ca != null)
                    allAssertions.add(ca);
            }
        }


        if (ssljCheckBox.isSelected()) {
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
            jButtonAdd,
            jButtonAddAll,
            jButtonRemove,
            jButtonRemoveAll
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
        ClassLoader cl = type.getClass().getClassLoader();
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

    private JTable identitiesInjTable;
    private JTable identitiesOutjTable;
    private DefaultTableModel identitiesInTableModel;
    private DefaultTableModel identitiesOutTableModel;

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
    private JPanel usersLabeljPanel;

    private JCheckBox ssljCheckBox;
}
