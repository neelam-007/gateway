package com.l7tech.console.panels;

import com.l7tech.console.util.IconManager;
import com.l7tech.console.util.IconManager2;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.WindowManager;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.tree.policy.AssertionTreeNodeFactory;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.credential.wss.WssDigest;
import com.l7tech.policy.assertion.credential.wss.WssClientCert;
import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.credential.PrincipalCredentials;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.Principal;


/**
 * <code>PolicyAddIdentitiesDialog</code> is the policy identities editor for
 * existing policies.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class PolicyAddIdentitiesDialog extends JDialog {
    private DefaultComboBoxModel providersComboBoxModel;
    private JButton addSelectedIdentitiesButton;
    AssertionTreeNode targetNode;
    private static final Logger log = Logger.getLogger(PolicyAddIdentitiesDialog.class.getName());

    /** Creates new form PolicyAddIdentitiesDialog */
    public PolicyAddIdentitiesDialog(Frame owner, AssertionTreeNode node) {
        super(owner, true);
        setTitle("Add identity asssertion(s)");
        targetNode = node;
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


        getContentPane().setLayout(new BorderLayout());

        providerSelectorPanel.setLayout(new BorderLayout());
        providerSelectorPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 0));

        providersComboBox.setModel(getProvidersComboBoxModel());
        providersComboBox.setRenderer(new ListCellRenderer() {
            public Component getListCellRendererComponent(
              JList list,
              Object value,
              int index,
              boolean isSelected,
              boolean cellHasFocus) {
                IdentityProvider ip = (IdentityProvider)value;
                return new JLabel(ip.getConfig().getName());
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

        getContentPane().add(providerSelectorPanel, BorderLayout.NORTH);


        identitiesPanel.setLayout(new BorderLayout());

        identitiesjPanel.setLayout(new BoxLayout(identitiesjPanel, BoxLayout.X_AXIS));

        identitiesjPanel.setBorder(new EmptyBorder(new Insets(20, 5, 10, 5)));
        identitiesOutScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        identitiesOutScrollPane.setPreferredSize(new Dimension(150, 50));
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
        identitiesInScrollPane.setPreferredSize(new Dimension(150, 50));
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
        getContentPane().add(identitiesPanel, BorderLayout.WEST);
        getContentPane().add(createButtonPanel(), BorderLayout.SOUTH);

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

    /**
     * Creates the panel of buttons that goes along the bottom
     * of the dialog
     *
     * Sets the variable loginButton
     *
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.TRAILING));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        // login button (global variable)
        addSelectedIdentitiesButton = new JButton();
        addSelectedIdentitiesButton.setText("Save");
        addSelectedIdentitiesButton.setToolTipText("Save assertions");

        addSelectedIdentitiesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                IdentityProvider ip = (IdentityProvider)providersComboBoxModel.getSelectedItem();

                java.util.List identityAssertions = new ArrayList();

                Iterator it = getIdentitiesInTableModel().getDataVector().iterator();
                while (it.hasNext()) {
                    java.util.List row = (java.util.List)it.next();
                    EntityHeader eh = (EntityHeader)row.get(0);
                    if (EntityType.USER.equals(eh.getType())) {
                        User u = new User();
                        u.setName(eh.getName());
                        u.setLogin(eh.getName());
                        identityAssertions.add(new SpecificUser(ip.getConfig().getOid(), u.getLogin()));
                    } else if (EntityType.GROUP.equals(eh.getType())) {
                        Group g = new Group();
                        g.setName(eh.getName());
                        MemberOfGroup ma = new MemberOfGroup(ip.getConfig().getOid(), g.getName());
                        ma.setGroupName(g.getName());
                        identityAssertions.add(ma);
                    }
                }

                JTree tree =
                  (JTree)WindowManager.
                  getInstance().getComponent(PolicyTree.NAME);
                if (tree != null) {
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    CompositeAssertion ca = (CompositeAssertion)targetNode.asAssertion();
                    List kids = new ArrayList();
                    kids.addAll(ca.getChildren());

                    for (Iterator idit = identityAssertions.iterator(); idit.hasNext();) {
                        Assertion ass = (Assertion)idit.next();
                        kids.add(ass);
                        model.
                          insertNodeInto(AssertionTreeNodeFactory.asTreeNode(ass),
                            targetNode, targetNode.getChildCount());
                    }
                    ca.setChildren(kids);
                } else {
                    log.log(Level.WARNING, "Unable to reach the palette tree.");
                }
                dispose();
            }

        });
        panel.add(addSelectedIdentitiesButton);

        // space
        panel.add(Box.createRigidArea(new Dimension(5, 0)));

        // cancel button
        JButton cancelButton = new JButton();
        cancelButton.setText("Cancel");

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });
        panel.add(cancelButton);

        Utilities.
          equalizeButtonSizes(new JButton[]{
              cancelButton, addSelectedIdentitiesButton
          });
        return panel;
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
              ImageIcon icon = PolicyAddIdentitiesDialog.this.getIcon(type);
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

    /**
     * the <b>no provider</b> anonymous class, used for combo selection tghat
     * indicates no provder is selected
     */
    private static final IdentityProvider
      NO_PROVIDER = new IdentityProvider() {
          public void initialize(IdentityProviderConfig config) {
          }

          public IdentityProviderConfig getConfig() {
              return config;
          }

          public UserManager getUserManager() {
              return null;
          }

          public GroupManager getGroupManager() {
              return null;
          }
          public boolean authenticate( PrincipalCredentials pc ) {
              return false;
          }

          public boolean isReadOnly() {
              return true;
          }

          IdentityProviderConfig config = new IdentityProviderConfig();

          {
              config.setName("No provider selected ");
              config.setDescription("No provider selected ");
          }

      };
    private JTable identitiesInTable;
    private JTable identitiesOutTable;
    private DefaultTableModel identitiesInTableModel;
    private DefaultTableModel identitiesOutTableModel;

    private JScrollPane identitiesOutScrollPane;

    private JButton buttonAddAll;
    private JPanel identitiesjPanel;
    private JButton buttonRemove;
    private JPanel providerSelectorPanel;
    private JPanel identitiesPanel;
    private JButton buttonRemoveAll;
    private JComboBox providersComboBox;
    private JScrollPane identitiesInScrollPane;
    private JButton buttonAdd;
    private JPanel buttonPanel;
    private JPanel usersLabelPanel;

}
