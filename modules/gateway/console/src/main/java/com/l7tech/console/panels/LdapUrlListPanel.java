package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.Goid;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A GUI control that keeps track of a list of LDAP URLs, whether to use an SSL client cert, and which private key to use if so.
 */
public class LdapUrlListPanel extends JPanel {
    public static final String PROP_DATA = "data";

    private JPanel mainPanel;
    private JButton moveUpButton;
    private JButton moveDownButton;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private JList ldapUrlList;
    private JCheckBox clientAuthenticationCheckbox;
    private PrivateKeysComboBox privateKeyComboBox;

    private int state = 0;

    public LdapUrlListPanel() {
        initGui();
    }

    private void initGui() {
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        privateKeyComboBox.selectDefaultSsl();
        privateKeyComboBox.setRenderer( TextListCellRenderer.<Object>basicComboBoxRenderer() );
        privateKeyComboBox.setMinimumSize(new Dimension(100, privateKeyComboBox.getPreferredSize().height));
        privateKeyComboBox.setPreferredSize(new Dimension(100, privateKeyComboBox.getPreferredSize().height));

        DefaultComboBoxModel urlListModel = new DefaultComboBoxModel();
        // To update buttons if there are any changes in the Ldap Host List,
        // add a ListDataListener for the list.
        urlListModel.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                firePropertyChange();
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                firePropertyChange();
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                firePropertyChange();
            }
        });
        ldapUrlList.setModel(urlListModel);

        Utilities.equalizeButtonSizes(new JButton[]{moveUpButton, moveDownButton});

        moveUpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int currentPos = ldapUrlList.getSelectedIndex();
                if (currentPos >= 0) {
                    // make sure not already in last position
                    Object selected = ldapUrlList.getSelectedValue();
                    if (currentPos > 0) {
                        DefaultComboBoxModel model = (DefaultComboBoxModel)ldapUrlList.getModel();
                        model.removeElementAt(currentPos);
                        model.insertElementAt(selected, currentPos-1);
                        firePropertyChange();
                    }
                }
            }
        });

        moveDownButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int currentPos = ldapUrlList.getSelectedIndex();
                if (currentPos >= 0) {
                    // make sure not already in last position
                    DefaultComboBoxModel model = (DefaultComboBoxModel)ldapUrlList.getModel();
                    if (model.getSize() > (currentPos+1)) {
                        Object selected = ldapUrlList.getSelectedValue();
                        model.removeElementAt(currentPos);
                        model.insertElementAt(selected, currentPos+1);
                        firePropertyChange();
                    }
                }
            }
        });

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String newUrl = (String)JOptionPane.showInputDialog(addButton,
                                                            "Enter the LDAP URL:",
                                                            "Add LDAP Host URL  ",
                                                            JOptionPane.PLAIN_MESSAGE,
                                                            null, null,
                                                            "ldap://host:port");
                DefaultComboBoxModel model = (DefaultComboBoxModel)ldapUrlList.getModel();
                if (newUrl != null && newUrl.trim().length()>0) {
                    if (model.getIndexOf(newUrl) < 0) {
                        model.insertElementAt(newUrl, model.getSize());
                    }
                    if(model.getSize() > 0) {
                        firePropertyChange();
                    }
                }
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selected = ldapUrlList.getSelectedIndex();
                if (selected < 0) return;
                DefaultComboBoxModel model = (DefaultComboBoxModel)ldapUrlList.getModel();
                String currentUrl = (String)model.getElementAt(selected);
                String newUrl = (String)JOptionPane.showInputDialog(editButton, "Change the LDAP URL:", "Edit LDAP Host URL",
                                                                    JOptionPane.PLAIN_MESSAGE, null, null, currentUrl);
                if (newUrl != null && newUrl.trim().length()>0) {
                    // Check if the modified url exists in the list.
                    if (model.getIndexOf(newUrl) < 0) {
                        model.removeElementAt(selected);
                        model.insertElementAt(newUrl, selected);
                        firePropertyChange();
                    }
                }
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selected = ldapUrlList.getSelectedIndex();
                if (selected > -1) {
                    ((DefaultComboBoxModel)ldapUrlList.getModel()).removeElementAt(selected);
                    if(ldapUrlList.getModel().getSize() == 0) {
                        firePropertyChange();
                    }
                }
            }
        });

        Utilities.equalizeButtonSizes(addButton, editButton, removeButton);

        clientAuthenticationCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                privateKeyComboBox.setEnabled(clientAuthenticationCheckbox.isSelected());
            }
        });

        privateKeyComboBox.setEnabled(clientAuthenticationCheckbox.isSelected());
    }

    private void firePropertyChange() {
        this.firePropertyChange(PROP_DATA, state, ++state);
    }

    public boolean isUrlListEmpty() {
        return ldapUrlList.getModel().getSize() == 0;
    }

    public String[] getUrlList() {
        DefaultComboBoxModel model = (DefaultComboBoxModel)ldapUrlList.getModel();
        String[] newlist = new String[model.getSize()];
        for (int i = 0; i < newlist.length; i++) {
            newlist[i] = (String)model.getElementAt(i);
        }
        return newlist;
    }

    public void setUrlList(String[] ldapUrls) {
        ((DefaultComboBoxModel)ldapUrlList.getModel()).removeAllElements();
        if (ldapUrls != null) {
            for ( String ldapUrl : ldapUrls ) {
                ( (DefaultComboBoxModel) ldapUrlList.getModel() ).addElement( ldapUrl );
            }
        }
    }

    public boolean isClientAuthEnabled() {
        return clientAuthenticationCheckbox.isSelected();
    }

    public void setClientAuthEnabled(boolean clientAuth) {
        clientAuthenticationCheckbox.setSelected(clientAuth);
        privateKeyComboBox.setEnabled(clientAuth);
    }

    public Goid getSelectedKeystoreId() {
        return privateKeyComboBox.getSelectedKeystoreId();
    }

    public String getSelectedKeyAlias() {
        return privateKeyComboBox.getSelectedKeyAlias();
    }

    public void selectPrivateKey(Goid keystoreId, String keyAlias) {
        if ( keystoreId != null && keyAlias != null ) {
            privateKeyComboBox.select(keystoreId, keyAlias);
        }
        if (privateKeyComboBox.getSelectedItem() == null) {
            //the selected key doesnt exists
            JOptionPane.showMessageDialog(this,
                    "Keystore alias not found, will use default SSL key.",
                    "Keystore alias not found",
                    JOptionPane.WARNING_MESSAGE);
            privateKeyComboBox.selectDefaultSsl();
        }
    }

    private void createUIComponents() {
        privateKeyComboBox = new PrivateKeysComboBox(false, true, false);
    }
}
