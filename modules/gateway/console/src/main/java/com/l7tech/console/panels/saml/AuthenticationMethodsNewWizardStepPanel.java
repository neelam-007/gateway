package com.l7tech.console.panels.saml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Collections;
import java.util.Collection;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.Set;
import java.util.HashSet;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.gui.util.ImageCache;

/**
 * The <code>WizardStepPanel</code> that allows selection of SAML
 * authentication methods.
 */
public class AuthenticationMethodsNewWizardStepPanel extends WizardStepPanel {
    public static final String RESOURCE_PATH = "com/l7tech/console/resources";

    private JPanel mainPanel;
    private JButton buttonSelectAll;
    private JButton buttonSelectNone;
    private JButton buttonAdd;
    private JButton buttonRemove;
    private JList listAvailable;
    private JList listSelected;
    private JLabel titleLabel;

    private final HashMap authenticationsMap;
    private final HashMap enabledAuthenticationsMap;
    private final boolean showTitleLabel;
    private final SortedListModel selectedList;
    private final SortedListModel unselectedList;

    /**
     * Creates new form ListBasedAuthenticationMethodsWizardStepPanel.
     */
    public AuthenticationMethodsNewWizardStepPanel(WizardStepPanel next, boolean showTitleLabel) {
        super(next);
        this.authenticationsMap = new HashMap();
        this.enabledAuthenticationsMap = new HashMap();
        this.showTitleLabel = showTitleLabel;
        this.selectedList = new SortedListModel();
        this.unselectedList = new SortedListModel();
        initialize();
    }

    /**
     * Creates new form ListBasedAuthenticationMethodsWizardStepPanel. Full constructor, that specifies
     * the owner dialog.
     */
    public AuthenticationMethodsNewWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, JDialog owner) {
        super(next);
        this.authenticationsMap = new HashMap();
        this.enabledAuthenticationsMap = new HashMap();
        this.showTitleLabel = showTitleLabel;
        this.selectedList = new SortedListModel();
        this.unselectedList = new SortedListModel();
        setOwner(owner);
        initialize();
    }

    /**
     * Creates new form WizardPanel with default optins
     */
    public AuthenticationMethodsNewWizardStepPanel(WizardStepPanel next) {
        this(next, true);
    }

    /**
     * Provides the wizard with the current data--either
     * the default data or already-modified settings.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    public void readSettings(Object settings) throws IllegalArgumentException {
        RequireWssSaml assertion = (RequireWssSaml)settings;
        SamlAuthenticationStatement statement = assertion.getAuthenticationStatement();
        setSkipped(statement == null);
        if (statement == null) {
            return;
        }

        enableForVersion(assertion.getVersion()==null ? 1 : assertion.getVersion().intValue());

        unselectedList.removeAll(authenticationsMap.values());
        unselectedList.addAll(enabledAuthenticationsMap.values());
        selectedList.removeAll(authenticationsMap.values());

        String[] methods = statement.getAuthenticationMethods();
        for (int i = 0; i < methods.length; i++) {
            String method = methods[i];
            String methodText = (String)authenticationsMap.get(method);
            if (methodText == null) {
                throw new IllegalArgumentException("No corresponding widget for "+method);
            }
            if (enabledAuthenticationsMap.keySet().contains(method))
                selectedList.addAll(Collections.singleton(methodText));
            unselectedList.removeAll(Collections.singleton(methodText));
        }
    }

    /**
     * Provides the wizard panel with the opportunity to update the
     * settings with its current customized state.
     * Rather than updating its settings with every change in the GUI,
     * it should collect them, and then only save them when requested to
     * by this method.
     * <p/>
     * This is a noop version that subclasses implement.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    public void storeSettings(Object settings) throws IllegalArgumentException {
        RequireWssSaml assertion = (RequireWssSaml)settings;
        SamlAuthenticationStatement statement = assertion.getAuthenticationStatement();
        if (statement == null) {
            throw new IllegalArgumentException();
        }
        Map authMap = new HashMap(authenticationsMap);
        authMap.values().retainAll(selectedList.getItems());
        statement.setAuthenticationMethods((String[])authMap.keySet().toArray(new String[] {}));
    }

    private void initialize() {
        setLayout(new BorderLayout());

        // main panel
        add(mainPanel, BorderLayout.CENTER);

        // hide label (used when displayed as a tab pane)
        if (showTitleLabel) {
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        } else {
            titleLabel.getParent().remove(titleLabel);
        }

        // initialize authentication map
        authenticationsMap.put(SamlConstants.PASSWORD_AUTHENTICATION, "Password");
        authenticationsMap.put(SamlConstants.KERBEROS_AUTHENTICATION, "Kerberos");
        authenticationsMap.put(SamlConstants.SRP_AUTHENTICATION, "Secure Remote Password (SRP)");
        authenticationsMap.put(SamlConstants.HARDWARE_TOKEN_AUTHENTICATION, "Hardware Token");
        authenticationsMap.put(SamlConstants.SSL_TLS_CERTIFICATE_AUTHENTICATION, "SSL/TLS Client Certificate Authentication");
        authenticationsMap.put(SamlConstants.X509_PKI_AUTHENTICATION, "X.509 Public Key");
        authenticationsMap.put(SamlConstants.PGP_AUTHENTICATION, "PGP Public Key");
        authenticationsMap.put(SamlConstants.SPKI_AUTHENTICATION, "SPKI Public Key");
        authenticationsMap.put(SamlConstants.XKMS_AUTHENTICATION, "XKMS Public Key");
        authenticationsMap.put(SamlConstants.XML_DSIG_AUTHENTICATION, "XML Digital Signature");
        authenticationsMap.put(SamlConstants.UNSPECIFIED_AUTHENTICATION, "Unspecified");
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_TELEPHONY_AUTH, "Authenticated Telephony");
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_IP, "Internet Protocol");
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_IPPASSWORD, "Internet Protocol Password");
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_MOBILE_1FACTOR_CONTRACT, "Mobile One Factor Contract");
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_MOBILE_1FACTOR_UNREG, "Mobile One Factor Unregistered");
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_MOBILE_2FACTOR_CONTRACT, "Mobile Two Factor Contract");
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_MOBILE_2FACTOR_UNREG, "Mobile Two Factor Unregistered");
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_TELEPHONY_NOMAD, "Nomad Telephony");
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_PASSWORD_PROTECTED, "Password Protected Transport");
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_TELEPHONY_PERSONALIZED, "Personalized Telephony");
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_SESSION, "Previous Session");
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_SMARTCARD, "Smartcard");
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_SMARTCARD_PKI, "Smartcard PKI");
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_SOFTWARE_PKI, "Software PKI");
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_TELEPHONY, "Telephony");
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_TIME_SYNC_TOKEN, "Time Synchronization Token");

        // set intially available methods
        enabledAuthenticationsMap.putAll(authenticationsMap);

        // initialize list models
        unselectedList.addAll(authenticationsMap.values());
        listSelected.setModel(selectedList);
        listAvailable.setModel(unselectedList);

        // handle add/remove on double click
        JListDoubleClickMoveListener listener = new JListDoubleClickMoveListener(listSelected, listAvailable);
        listSelected.addMouseListener(listener);
        listAvailable.addMouseListener(listener);

        // button arrow icons
        buttonAdd.setIcon(new ImageIcon(ImageCache.getInstance().getIcon(RESOURCE_PATH + "/Add16.gif")));
        buttonRemove.setIcon(new ImageIcon(ImageCache.getInstance().getIcon(RESOURCE_PATH + "/Remove16.gif")));

        // enable add/remove buttons based on list selections
        listSelected.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            public void valueChanged(ListSelectionEvent e) {
                buttonRemove.setEnabled(listSelected.getSelectedValues().length > 0);
            }
        });

        listAvailable.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            public void valueChanged(ListSelectionEvent e) {
                buttonAdd.setEnabled(listAvailable.getSelectedValues().length > 0);
            }
        });

        // button listeners
        buttonSelectAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                unselectedList.removeAll(authenticationsMap.values());
                selectedList.addAll(enabledAuthenticationsMap.values());
                listAvailable.clearSelection();
                listSelected.clearSelection();
            }
        });
        buttonSelectNone.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectedList.removeAll(authenticationsMap.values());
                unselectedList.addAll(enabledAuthenticationsMap.values());
                listAvailable.clearSelection();
                listSelected.clearSelection();
            }
        });
        buttonAdd.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Collection toAdd = Arrays.asList(listAvailable.getSelectedValues());
                selectedList.addAll(toAdd);
                unselectedList.removeAll(toAdd);
                listAvailable.clearSelection();
            }
        });
        buttonRemove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Collection toAdd = Arrays.asList(listSelected.getSelectedValues());
                unselectedList.addAll(toAdd);
                selectedList.removeAll(toAdd);
                listSelected.clearSelection();
            }
        });
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Authentication Methods";
    }

    /**
     * Test whether the step is finished and it is safe to advance to the next one.
     * A single authentication method must be specified.
     *
     * @return true if the panel is valid, false otherwis
     */
    public boolean canAdvance() {
        return selectedList.getSize() != 0;
    }

    public String getDescription() {
        return
          "<html>Specify one or more accepted authentication methods that the SAML statement must assert. " +
            "At least one authentication method must be selected</html>";
    }

    /**
     * Enable only the methods that are applicable for a given saml version(s)
     */
    private void enableForVersion(int samlVersion) {
        // reset
        enabledAuthenticationsMap.clear();
        enabledAuthenticationsMap.putAll(authenticationsMap);

        // prune for version
        if (samlVersion == 1) {
            enabledAuthenticationsMap.keySet().retainAll(Arrays.asList(SamlConstants.ALL_AUTHENTICATIONS));
        }
        else if (samlVersion == 2) {
            Set v1Only = new HashSet(Arrays.asList(SamlConstants.ALL_AUTHENTICATIONS));
            v1Only.removeAll(SamlConstants.AUTH_MAP_SAML_1TO2.keySet());
            enabledAuthenticationsMap.keySet().removeAll(v1Only);
        }
    }

    private class SortedListModel extends AbstractListModel {
        private final Set items;

        public SortedListModel() {
            items = new TreeSet();
        }

        public Object getElementAt(int index) {
            Object item = null;
            int count = 0;
            for (Iterator itemIter=items.iterator(); itemIter.hasNext(); ) {
                Object current = itemIter.next();
                if (count == index) {
                    item = current;
                    break;
                }
                count++;
            }
            return item;
        }

        public int getSize() {
            return items.size();
        }

        public Collection getItems() {
            return Collections.unmodifiableCollection(items);
        }

        public void addAll(Collection toAdd) {
            items.addAll(toAdd);
            fireContentsChanged(this,0,0);
            notifyListeners();
        }

        public void removeAll(Collection toRemove) {
            items.removeAll(toRemove);
            fireContentsChanged(this,0,0);
            notifyListeners();
        }
    };

    private static class JListDoubleClickMoveListener extends MouseAdapter {
        private final JList list1;
        private final JList list2;

        public JListDoubleClickMoveListener(JList list1, JList list2){
            this.list1 = list1;
            this.list2 = list2;
        }

        public void mouseClicked(MouseEvent e){
            Object source = e.getSource();
            if(e.getClickCount() == 2){
                if (source == list1) {
                    int index = list1.locationToIndex(e.getPoint());
                    ListModel dlm = list1.getModel();
                    Object item = dlm.getElementAt(index);;
                    ((SortedListModel)list1.getModel()).removeAll(Collections.singletonList(item));
                    ((SortedListModel)list2.getModel()).addAll(Collections.singletonList(item));
                } else if (source == list2){
                    int index = list2.locationToIndex(e.getPoint());
                    ListModel dlm = list2.getModel();
                    Object item = dlm.getElementAt(index);;
                    ((SortedListModel)list2.getModel()).removeAll(Collections.singletonList(item));
                    ((SortedListModel)list1.getModel()).addAll(Collections.singletonList(item));
                }
            }
        }
    }
}
