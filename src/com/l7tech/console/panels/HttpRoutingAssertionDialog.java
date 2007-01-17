package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.widgets.IpListPanel;
import com.l7tech.common.gui.widgets.UrlPanel;
import com.l7tech.common.security.rbac.AttemptedUpdate;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.console.action.SecureAction;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.table.HttpRuleTableHandler;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.BridgeRoutingAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>HttpRoutingAssertionDialog</code> is the protected service
 * policy edit dialog.
 */
public class HttpRoutingAssertionDialog extends JDialog {
    private static final Logger log = Logger.getLogger(HttpRoutingAssertionDialog.class.getName());
    private HttpRuleTableHandler responseHttpRulesTableHandler;
    private HttpRuleTableHandler requestHttpRulesTableHandler;
    private HttpRuleTableHandler requestParamsRulesTableHandler;

    private final EventListenerList listenerList = new EventListenerList();
    private final PublishedService service;
    private final HttpRoutingAssertion assertion;
    private final HttpRoutingHttpAuthPanel httpAuthPanel;
    private final HttpRoutingSamlAuthPanel samlAuthPanel;

    private JPanel mainPanel;

    private UrlPanel urlPanel;
    private JButton defaultUrlButton;
    private IpListPanel ipListPanel;
    //private JCheckBox cookiePropagationCheckBox;

    private JRadioButton authNoneRadio;
    private JRadioButton authPasswordRadio;
    private JRadioButton authPassthroughRadio;
    private JRadioButton authSamlRadio;
    private JRadioButton authTaiRadio;
    private JPanel authDetailsPanel;

    private JRadioButton wssRemoveRadio;
    private JRadioButton wssLeaveRadio;
    private JRadioButton wssPromoteRadio;
    private JComboBox wssPromoteActorCombo;

    private JButton okButton;
    private JButton cancelButton;
    private JSpinner connectTimeoutSpinner;
    private JCheckBox connectTimeoutDefaultCheckBox;
    private JSpinner readTimeoutSpinner;
    private JCheckBox readTimeoutDefaultCheckBox;
    private JTabbedPane tabbedPane1;
    private JRadioButton resHeadersAll;
    private JRadioButton resHeadersCustomize;
    private JTable resHeadersTable;
    private JButton resHeadersAdd;
    private JButton resHeadersDelete;
    private JRadioButton reqHeadersAll;
    private JRadioButton reqHeadersCustomize;
    private JTable reqHeadersTable;
    private JRadioButton reqParamsAll;
    private JRadioButton reqParamsCustomize;
    private JTable reqParamsTable;
    private JButton reqHeadersAdd;
    private JButton reqHeadersRemove;
    private JButton reqParamsAdd;
    private JButton reqParamsRemove;

    private final SecureAction okButtonAction;

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.HttpRoutingAssertionDialog");

    /**
     * Creates new form ServicePanel
     */
    public HttpRoutingAssertionDialog(Frame owner, HttpRoutingAssertion assertion, PublishedService service) {
        super(owner, true);
        setTitle(resources.getString("dialog.title"));
        this.assertion = assertion;
        this.service = service;
        this.httpAuthPanel = new HttpRoutingHttpAuthPanel(assertion);
        this.samlAuthPanel = new HttpRoutingSamlAuthPanel(assertion);

        okButtonAction = new SecureAction(new AttemptedUpdate(EntityType.SERVICE, service)) {
            public String getName() {
                return resources.getString("okButton.text");
            }

            protected String iconResource() {
                return null;
            }

            protected void performAction() {
                ok();
            }
        };

        initComponents();
        initFormData();
    }

    /**
     * add the PolicyListener
     * 
     * @param listener the PolicyListener
     */
    public void addPolicyListener(PolicyListener listener) {
        listenerList.add(PolicyListener.class, listener);
    }

    /**
     * remove the the PolicyListener
     * 
     * @param listener the PolicyListener
     */
    public void removePolicyListener(PolicyListener listener) {
        listenerList.remove(PolicyListener.class, listener);
    }

    /**
     * notfy the listeners
     *
     * @param a the assertion
     */
    void fireEventAssertionChanged(final Assertion a) {
        SwingUtilities.invokeLater(
          new Runnable() {
              public void run() {
                  if (a == null) return;
                  if (a.getParent() == null || a.getParent().getChildren() == null) return;
                  int[] indices = new int[a.getParent().getChildren().indexOf(a)];
                  PolicyEvent event = new
                    PolicyEvent(this, new AssertionPath(a.getPath()), indices, new Assertion[]{a});
                  EventListener[] listeners = listenerList.getListeners(PolicyListener.class);
                  for (EventListener listener : listeners) {
                      ((PolicyListener) listener).assertionsChanged(event);
                  }
              }
          });
    }


    /**
     * This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents() {
        add(mainPanel);

        connectTimeoutSpinner.setModel(new SpinnerNumberModel(1,1,86400,1));  // 1 day in seconds
        readTimeoutSpinner.setModel(new SpinnerNumberModel(1,1,86400,1));
        ActionListener enableSpinners = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                connectTimeoutSpinner.setEnabled(!connectTimeoutDefaultCheckBox.isSelected());
                readTimeoutSpinner.setEnabled(!readTimeoutDefaultCheckBox.isSelected());
            }
        };
        connectTimeoutDefaultCheckBox.addActionListener(enableSpinners);
        readTimeoutDefaultCheckBox.addActionListener(enableSpinners);

        ButtonGroup methodGroup = new ButtonGroup();
        methodGroup.add(this.authNoneRadio);
        methodGroup.add(this.authPasswordRadio);
        methodGroup.add(this.authPassthroughRadio);
        methodGroup.add(this.authSamlRadio);
        methodGroup.add(this.authTaiRadio);

        final ChangeListener radioChangeListener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateAuthMethod();
            }
        };

        authDetailsPanel.setMinimumSize(new Dimension(Math.max(httpAuthPanel.getMinimumSize().width, samlAuthPanel.getMinimumSize().width), -1));
        authDetailsPanel.setLayout(new GridBagLayout());
        authDetailsPanel.add(httpAuthPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        authDetailsPanel.add(samlAuthPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        authNoneRadio.addChangeListener(radioChangeListener);
        authPasswordRadio.addChangeListener(radioChangeListener);
        authPassthroughRadio.addChangeListener(radioChangeListener);
        authSamlRadio.addChangeListener(radioChangeListener);
        authTaiRadio.addChangeListener(radioChangeListener);

        if (service != null && !service.isSoap()) {
            authSamlRadio.setEnabled(false);
            wssPromoteRadio.setEnabled(false);
            wssRemoveRadio.setEnabled(false);
            wssLeaveRadio.setEnabled(false);
            wssPromoteActorCombo.setEnabled(false);
        }

        ButtonGroup wssButtons = new ButtonGroup();
        wssButtons.add(wssRemoveRadio);
        wssButtons.add(wssLeaveRadio);
        wssButtons.add(wssPromoteRadio);
        ActionListener disenableCombo = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (wssPromoteRadio.isSelected()) {
                    wssPromoteActorCombo.setEnabled(true);
                } else {
                    wssPromoteActorCombo.setEnabled(false);
                }
            }
        };
        wssRemoveRadio.addActionListener(disenableCombo);
        wssLeaveRadio.addActionListener(disenableCombo);
        wssPromoteRadio.addActionListener(disenableCombo);

        defaultUrlButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (service != null) {
                    try {
                        Wsdl wsdl = service.parsedWsdl();
                        String serviceURI;
                        if (wsdl != null) {
                            serviceURI = wsdl.getServiceURI();
                            urlPanel.setText(serviceURI);
                        } else {
                            log.log(Level.INFO, "Can't retrieve WSDL from the published service");
                        }
                    } catch (javax.wsdl.WSDLException we) {
                        log.log(Level.INFO, "HttpRoutingAssertionDialog", we);
                    }
                } else {
                    log.log(Level.INFO, "Can't find the service");
                }
            }
        });

        initializeHttpRulesTabs();

        okButton.setAction(okButtonAction);

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });
        getRootPane().setDefaultButton(okButton);
        Utilities.setEscKeyStrokeDisposes(this);
    }

    private void initializeHttpRulesTabs() {

        // init req rules stuff
        ActionListener tablestate = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (reqHeadersCustomize.isSelected()) {
                    reqHeadersTable.setEnabled(true);
                    reqHeadersAdd.setEnabled(true);
                    reqHeadersRemove.setEnabled(true);
                } else {
                    reqHeadersTable.setEnabled(false);
                    reqHeadersAdd.setEnabled(false);
                    reqHeadersRemove.setEnabled(false);
                }
            }
        };
        reqHeadersAll.addActionListener(tablestate);
        reqHeadersCustomize.addActionListener(tablestate);
        requestHttpRulesTableHandler = new HttpRuleTableHandler("Header", reqHeadersTable,
                                                                 reqHeadersAdd, reqHeadersRemove,
                                                                 assertion.getRequestHeaderRules());
        if (assertion.getRequestHeaderRules().isForwardAll()) {
            reqHeadersAll.setSelected(true);
            reqHeadersAdd.setEnabled(false);
            reqHeadersRemove.setEnabled(false);
        } else {
            reqHeadersCustomize.setSelected(true);
        }

        if (assertion instanceof BridgeRoutingAssertion) {
            reqParamsAll.setEnabled(false);
            reqParamsCustomize.setEnabled(false);
            reqParamsTable.setEnabled(false);
            reqParamsAdd.setEnabled(false);
            reqParamsRemove.setEnabled(false);
        } else {
            tablestate = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (reqParamsCustomize.isSelected()) {
                        reqParamsTable.setEnabled(true);
                        reqParamsAdd.setEnabled(true);
                        reqParamsRemove.setEnabled(true);
                    } else {
                        reqParamsTable.setEnabled(false);
                        reqParamsAdd.setEnabled(false);
                        reqParamsRemove.setEnabled(false);
                    }
                }
            };
            reqParamsAll.addActionListener(tablestate);
            reqParamsCustomize.addActionListener(tablestate);
            requestParamsRulesTableHandler = new HttpRuleTableHandler("Parameter", reqParamsTable,
                                                                     reqParamsAdd, reqParamsRemove,
                                                                     assertion.getRequestParamRules());
            if (assertion.getRequestParamRules().isForwardAll()) {
                reqParamsAll.setSelected(true);
                reqParamsAdd.setEnabled(false);
                reqParamsRemove.setEnabled(false);
            } else {
                reqParamsCustomize.setSelected(true);
            }
        }

        // init the response stuff
        tablestate = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (resHeadersCustomize.isSelected()) {
                    resHeadersTable.setEnabled(true);
                    resHeadersAdd.setEnabled(true);
                    resHeadersDelete.setEnabled(true);
                } else {
                    resHeadersTable.setEnabled(false);
                    resHeadersAdd.setEnabled(false);
                    resHeadersDelete.setEnabled(false);
                }
            }
        };
        resHeadersAll.addActionListener(tablestate);
        resHeadersCustomize.addActionListener(tablestate);
        responseHttpRulesTableHandler = new HttpRuleTableHandler("Header", resHeadersTable,
                                                                 resHeadersAdd, resHeadersDelete,
                                                                 assertion.getResponseHeaderRules());
        if (assertion.getResponseHeaderRules().isForwardAll()) {
            resHeadersAll.setSelected(true);
            resHeadersAdd.setEnabled(false);
            resHeadersDelete.setEnabled(false);
        } else {
            resHeadersCustomize.setSelected(true);
        }
    }

    private void ok() {
        // check url before accepting
        String url = urlPanel.getText();
        boolean bad = false;
        if (url == null || url.length() < 1) {
            url = "<empty>";
            bad = true;
        }
        try {
            new URL(url);
        } catch (MalformedURLException e1) {
            bad = true;
            // check if the url has context variables
            if (url.indexOf("${") > -1) {
                // a url may appear to be malformed simply because it relies
                // on the runtime resolution of a context variable
                bad = false;
            }
        }
        if (bad) {
            JOptionPane.showMessageDialog(okButton, MessageFormat.format(resources.getString("invalidUrlMessage"), url));
        } else {
            if (authPasswordRadio.isSelected())
                httpAuthPanel.updateModel();
            else {
                assertion.setLogin(null);
                assertion.setPassword(null);
                assertion.setRealm(null);
                assertion.setNtlmHost(null);
            }

            if (authSamlRadio.isSelected()) {
                samlAuthPanel.updateModel();
            } else {
                assertion.setAttachSamlSenderVouches(false);
            }

            assertion.setProtectedServiceUrl(url);
            assertion.setAttachSamlSenderVouches(authSamlRadio.isSelected());
            assertion.setTaiCredentialChaining(authTaiRadio.isSelected());
            assertion.setPassthroughHttpAuthentication(authPassthroughRadio.isSelected());

            if (ipListPanel.isAddressesEnabled()) {
                assertion.setCustomIpAddresses(ipListPanel.getAddresses());
                assertion.setFailoverStrategyName(ipListPanel.getFailoverStrategyName());
            } else {
                assertion.setCustomIpAddresses(null);
                assertion.setFailoverStrategyName(ipListPanel.getFailoverStrategyName());
            }

            if (connectTimeoutDefaultCheckBox.isSelected())
                assertion.setConnectionTimeout(null);
            else
                assertion.setConnectionTimeout((Integer)connectTimeoutSpinner.getValue()*1000);
            if (readTimeoutDefaultCheckBox.isSelected())
                assertion.setTimeout(null);
            else
                assertion.setTimeout((Integer)readTimeoutSpinner.getValue()*1000);

            if (wssPromoteRadio.isSelected()) {
                String currentVal = (String)wssPromoteActorCombo.getSelectedItem();
                if (currentVal != null && currentVal.length() > 0) {
                    assertion.setXmlSecurityActorToPromote(currentVal);
                    assertion.setCurrentSecurityHeaderHandling(RoutingAssertion.PROMOTE_OTHER_SECURITY_HEADER);
                } else {
                    JOptionPane.showMessageDialog(okButton, resources.getString("actorRequiredMessage"));
                    return;
                }
            } else if (wssRemoveRadio.isSelected()) {
                assertion.setCurrentSecurityHeaderHandling(RoutingAssertion.REMOVE_CURRENT_SECURITY_HEADER);
                assertion.setXmlSecurityActorToPromote(null);
            } else if (wssLeaveRadio.isSelected()) {
                assertion.setCurrentSecurityHeaderHandling(RoutingAssertion.LEAVE_CURRENT_SECURITY_HEADER_AS_IS);
                assertion.setXmlSecurityActorToPromote(null);
            }

            // todo, address the fact that this is no longer controlled by this checkbox
            // assertion.setCopyCookies(cookiePropagationCheckBox.isSelected());

            assertion.getResponseHeaderRules().setRules(responseHttpRulesTableHandler.getData());
            assertion.getResponseHeaderRules().setForwardAll(resHeadersAll.isSelected());

            assertion.getRequestHeaderRules().setRules(requestHttpRulesTableHandler.getData());
            assertion.getRequestHeaderRules().setForwardAll(reqHeadersAll.isSelected());

            if (requestParamsRulesTableHandler != null) { // this will be null in case of BRA
                assertion.getRequestParamRules().setRules(requestParamsRulesTableHandler.getData());
                assertion.getRequestParamRules().setForwardAll(reqParamsAll.isSelected());
            }

            fireEventAssertionChanged(assertion);

            this.dispose();
        }
    }

    private void updateAuthMethod() {
        final boolean password = authPasswordRadio.isSelected();
        final boolean saml = authSamlRadio.isSelected();

        httpAuthPanel.setVisible(password);
        samlAuthPanel.setVisible(saml);
        authDetailsPanel.revalidate();
    }


    private void initFormData() {
        urlPanel.setText(assertion.getProtectedServiceUrl());
        JRadioButton which = authNoneRadio;
        if (assertion.isTaiCredentialChaining()) which = authTaiRadio;
        if (assertion.isPassthroughHttpAuthentication()) which = authPassthroughRadio;
        if (assertion.getLogin() != null) which = authPasswordRadio;
        if (assertion.isAttachSamlSenderVouches()) which = authSamlRadio;
        which.setSelected(true);

        authSamlRadio.setSelected(assertion.isAttachSamlSenderVouches());
        ipListPanel.setAddressesEnabled(assertion.getCustomIpAddresses() != null);
        ipListPanel.setAddresses(assertion.getCustomIpAddresses());
        ipListPanel.setFailoverStrategyName(assertion.getFailoverStrategyName());

        Integer connectTimeout = assertion.getConnectionTimeout();
        if (connectTimeout == null) {
            connectTimeoutSpinner.setValue(30);
            connectTimeoutDefaultCheckBox.setSelected(true);
        } else {
            connectTimeoutSpinner.setValue(connectTimeout / 1000);
            connectTimeoutDefaultCheckBox.setSelected(false);
        }
        connectTimeoutSpinner.setEnabled(!connectTimeoutDefaultCheckBox.isSelected());
        Integer readTimeout = assertion.getTimeout();
        if (readTimeout == null) {
            readTimeoutSpinner.setValue(60);
            readTimeoutDefaultCheckBox.setSelected(true);
        } else {
            readTimeoutSpinner.setValue(readTimeout / 1000);
            readTimeoutDefaultCheckBox.setSelected(false);
        }
        readTimeoutSpinner.setEnabled(!readTimeoutDefaultCheckBox.isSelected());

        // read actor promotion information
        java.util.List<String> existingActors = listExistingXmlSecurityRecipientContextFromPolicy();
        for (String existingActor : existingActors) {
            ((DefaultComboBoxModel) wssPromoteActorCombo.getModel()).addElement(existingActor);
        }

        if (assertion.getCurrentSecurityHeaderHandling() == RoutingAssertion.REMOVE_CURRENT_SECURITY_HEADER) {
            wssPromoteRadio.setSelected(false);
            wssPromoteActorCombo.setEnabled(false);
            wssRemoveRadio.setSelected(true);
            wssLeaveRadio.setSelected(false);
        } else if (assertion.getCurrentSecurityHeaderHandling() == RoutingAssertion.LEAVE_CURRENT_SECURITY_HEADER_AS_IS) {
            wssPromoteRadio.setSelected(false);
            wssPromoteActorCombo.setEnabled(false);
            wssRemoveRadio.setSelected(false);
            wssLeaveRadio.setSelected(true);
        } else {
            wssPromoteRadio.setSelected(true);
            wssRemoveRadio.setSelected(false);
            wssLeaveRadio.setSelected(false);
            wssPromoteActorCombo.setEnabled(true);
            wssPromoteActorCombo.getModel().setSelectedItem(assertion.getXmlSecurityActorToPromote());
        }

        // address the fact that this is no longer controlled by this assertion
        // cookiePropagationCheckBox.setSelected(assertion.isCopyCookies());
    }

    /**
     * @return a list of string objects; one for each different actor referenced from this policy
     */
    private java.util.List<String> listExistingXmlSecurityRecipientContextFromPolicy() {
        ArrayList<String> output = new ArrayList<String>();
        // get to root of policy
        Assertion root = assertion;
        while (root.getParent() != null) {
            root = root.getParent();
        }
        populateXmlSecurityRecipientContext(root, output);

        return output;
    }

    private void populateXmlSecurityRecipientContext(Assertion toInspect, java.util.List<String> receptacle) {
        if (toInspect instanceof CompositeAssertion) {
            CompositeAssertion ca = (CompositeAssertion)toInspect;
            for (Iterator i = ca.children(); i.hasNext();) {
                Assertion a = (Assertion)i.next();
                populateXmlSecurityRecipientContext(a, receptacle);
            }
        } else if (toInspect instanceof SecurityHeaderAddressable) {
            SecurityHeaderAddressable xsecass = (SecurityHeaderAddressable)toInspect;
            if (!xsecass.getRecipientContext().localRecipient()) {
                String existingactor = xsecass.getRecipientContext().getActor();
                if (!receptacle.contains(existingactor)) {
                    receptacle.add(existingactor);
                }
            }
        }
    }
}
