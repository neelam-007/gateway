package com.l7tech.console.panels;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.console.action.BaseAction;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.console.table.HttpHeaderRuleTableHandler;
import com.l7tech.console.table.HttpParamRuleTableHandler;
import com.l7tech.console.table.HttpRuleTableHandler;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.IpListPanel;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.policy.variable.*;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.ValidationUtils;
import com.l7tech.wsdl.Wsdl;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>HttpRoutingAssertionDialog</code> is the protected service
 * policy edit dialog.
 *
 * <p>Related function specifications:
 * <ul>
 *  <li><a href="http://sarek.l7tech.com/mediawiki/index.php?title=XML_Variables">XML Variables</a> (4.3)
 * </ul>
 */
public class HttpRoutingAssertionDialog extends LegacyAssertionPropertyDialog {
    private static class MsgSrcComboBoxItem {
        private final String _variableName;
        private final String _displayName;
        private MsgSrcComboBoxItem(String variableName, String displayName) {
            _variableName = variableName;
            _displayName = displayName;
        }
        public String getVariableName() { return _variableName; }
        @Override
        public String toString() { return _displayName; }
    }

    private static final Logger log = Logger.getLogger(HttpRoutingAssertionDialog.class.getName());
    private HttpRuleTableHandler responseHttpRulesTableHandler;
    private HttpRuleTableHandler requestHttpRulesTableHandler;
    private HttpRuleTableHandler requestParamsRulesTableHandler;

    private final EventListenerList listenerList = new EventListenerList();
    private final HttpRoutingAssertion assertion;
    private final boolean bra; // True if this is actually a BridgeRoutingAssertion; some fields are non-functional
    private final HttpRoutingHttpAuthPanel httpAuthPanel;
    private final HttpRoutingSamlAuthPanel samlAuthPanel;
    private final HttpRoutingWindowsIntegratedAuthPanel windowsAuthPanel;

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
    private JRadioButton authWindowsIntegratedRadio;
    private JPanel authDetailsPanel;

    private JRadioButton wssIgnoreRadio;
    private JRadioButton wssCleanupRadio;
    private JRadioButton wssRemoveRadio;
    private JRadioButton wssPromoteRadio;
    private JComboBox wssPromoteActorCombo;

    private JButton okButton;
    private JButton cancelButton;
    private JSpinner connectTimeoutSpinner;
    private JCheckBox connectTimeoutDefaultCheckBox;
    private JSpinner readTimeoutSpinner;
    private JCheckBox readTimeoutDefaultCheckBox;
    private JSpinner maxRetriesSpinner;
    private JCheckBox maxRetriesDefaultCheckBox;
    private JRadioButton resHeadersAll;
    private JRadioButton resHeadersCustomize;
    private JTable resHeadersTable;
    private JButton resHeadersAdd;
    private JButton resHeadersDelete;
    private JComboBox reqMsgSrcComboBox;
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
    private JCheckBox followRedirectCheck;
    private JButton editReqHrButton;
    private JButton editReqPmButton;
    private JButton editResHrButton;
    private JRadioButton failOnErrorRadio;
    private JCheckBox passThroughCheckBox;
    private JRadioButton neverFailRadio;
    private JRadioButton resMsgDestDefaultRadioButton;
    private JRadioButton resMsgDestVariableRadioButton;
    private JTextField resMsgDestVariableTextField;
    private JLabel resMsgDestVariableStatusLabel;
    private JCheckBox gzipCheckBox;
    private JComboBox requestMethodComboBox;
    private JRadioButton automaticRequestMethodRadioButton;
    private JRadioButton overrideRequestMethodRadioButton;
    private JRadioButton rbProxyNone;
    private JRadioButton rbProxySpecified;
    private JTextField proxyHostField;
    private JTextField proxyPortField;
    private JTextField proxyUsernameField;
    private JPasswordField proxyPasswordField;
    private JCheckBox useKeepalivesCheckBox;

    private final AbstractButton[] secHdrButtons = { wssIgnoreRadio, wssCleanupRadio, wssRemoveRadio, wssPromoteRadio };

    private final BaseAction okButtonAction;
    private boolean confirmed = false;

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.HttpRoutingAssertionDialog");

    private final Policy policy;
    private final Wsdl wsdl;
    private Assertion assertionToUseInSearchForPredecessorVariables;
    private InputValidator inputValidator;

    /**
     * Creates new form ServicePanel
     */
    public HttpRoutingAssertionDialog(Frame owner, HttpRoutingAssertion assertion, Policy policy, Wsdl wsdl, boolean readOnly) {
        super(owner, assertion, true);
        inputValidator = new InputValidator(this, assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME).toString());
        this.assertion = assertion;
        this.bra = this.assertion instanceof BridgeRoutingAssertion;
        this.policy = policy;
        this.wsdl = wsdl;
        this.httpAuthPanel = new HttpRoutingHttpAuthPanel(assertion);
        this.samlAuthPanel = new HttpRoutingSamlAuthPanel(assertion, inputValidator);
        this.windowsAuthPanel = new HttpRoutingWindowsIntegratedAuthPanel(assertion);

        okButtonAction = new BaseAction() {
            @Override
            public String getName() {
                return resources.getString("okButton.text");
            }

            @Override
            protected String iconResource() {
                return null;
            }

            @Override
            protected void performAction() {
                ok();
            }
        };

        if (!bra)
            ipListPanel.alsoEnableDiffURLS();

        urlPanel.setShowManageHttpOptions( false );
        ipListPanel.registerStateCallback(new IpListPanel.StateCallback() {
            @Override
            public void stateChanged(int newState) {
                if (newState == IpListPanel.CUSTOM_URLS) {
                    urlPanel.setEnabled(false);
                } else {
                    urlPanel.setEnabled(true);
                }
            }
        });

        // Set a validator to check if a URL contains context variable
        ipListPanel.setContextVariableValidator(new Functions.Unary<Boolean, String>() {
            @Override
            public Boolean call(String s) {
                String[] res = Syntax.getReferencedNames(s,false);
                return res != null && res.length > 0;
            }
        });

        initComponents(readOnly);
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
     * Workaround for use by {@link BridgeRoutingAssertionPropertiesDialog}.
     *
     * @param a     the surrogate assertion
     * @since SecureSpan 4.3; because the new request message source needs predecessor context variables
     */
    public void setAssertionToUseInSearchForPredecessorVariables(Assertion a) {
        assertionToUseInSearchForPredecessorVariables = a;
        populateReqMsgSrcComboBox();
    }

    /**
     * notfy the listeners
     *
     * @param a the assertion
     */
    void fireEventAssertionChanged(final Assertion a) {
        SwingUtilities.invokeLater(
          new Runnable() {
              @Override
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
    private void initComponents(final boolean readOnly) {
        add(mainPanel);

        connectTimeoutSpinner.setModel(new SpinnerNumberModel(1,1,86400,1));  // 1 day in seconds
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(connectTimeoutSpinner, "Connection timeout"));

        readTimeoutSpinner.setModel(new SpinnerNumberModel(1,1,86400,1));
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(readTimeoutSpinner, "Read timeout"));

        maxRetriesSpinner.setModel(new SpinnerNumberModel(3,0,100,1));
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(maxRetriesSpinner, "Maximum retries"));

        inputValidator.constrainTextFieldToNumberRange("proxy port", proxyPortField, -1, 65535);
        inputValidator.constrainTextFieldToBeNonEmpty("proxy host", proxyHostField, null);

        ActionListener enableSpinners = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectTimeoutSpinner.setEnabled(!connectTimeoutDefaultCheckBox.isSelected());
                readTimeoutSpinner.setEnabled(!readTimeoutDefaultCheckBox.isSelected());
                maxRetriesSpinner.setEnabled(!maxRetriesDefaultCheckBox.isSelected());
            }
        };
        connectTimeoutDefaultCheckBox.addActionListener(enableSpinners);
        readTimeoutDefaultCheckBox.addActionListener(enableSpinners);
        maxRetriesDefaultCheckBox.addActionListener(enableSpinners);

        ActionListener enablePassThroughFaults = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                passThroughCheckBox.setEnabled(failOnErrorRadio.isSelected());
            }
        };
        failOnErrorRadio.addActionListener(enablePassThroughFaults);
        neverFailRadio.addActionListener(enablePassThroughFaults);

        ButtonGroup methodGroup = new ButtonGroup();
        methodGroup.add(this.authNoneRadio);
        methodGroup.add(this.authPasswordRadio);
        methodGroup.add(this.authPassthroughRadio);
        methodGroup.add(this.authSamlRadio);
        methodGroup.add(this.authTaiRadio);
        methodGroup.add(this.authWindowsIntegratedRadio);

        final ChangeListener radioChangeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateAuthMethod();
            }
        };

        authDetailsPanel.setMinimumSize(new Dimension(Math.max(httpAuthPanel.getMinimumSize().width, samlAuthPanel.getMinimumSize().width), -1));
        authDetailsPanel.setLayout(new GridBagLayout());
        authDetailsPanel.add(httpAuthPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        authDetailsPanel.add(samlAuthPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        authDetailsPanel.add(windowsAuthPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        authNoneRadio.addChangeListener(radioChangeListener);
        authPasswordRadio.addChangeListener(radioChangeListener);
        authPassthroughRadio.addChangeListener(radioChangeListener);
        authSamlRadio.addChangeListener(radioChangeListener);
        authTaiRadio.addChangeListener(radioChangeListener);
        authWindowsIntegratedRadio.addChangeListener(radioChangeListener);

        if (!policy.isSoap()) {
            authSamlRadio.setEnabled(false);
        }

        ButtonGroup wssButtons = new ButtonGroup();
        ActionListener disenableCombo = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                wssPromoteActorCombo.setEnabled(wssPromoteRadio.isSelected());
            }
        };
        for (AbstractButton button : secHdrButtons) {
            wssButtons.add(button);
            button.addActionListener(disenableCombo);
        }
        RoutingDialogUtils.tagSecurityHeaderHandlingButtons(secHdrButtons);

        defaultUrlButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String serviceURI;
                if (wsdl != null) {
                    serviceURI = wsdl.getServiceURI();
                    urlPanel.setText(serviceURI);
                } else {
                    log.log(Level.INFO, "Can't retrieve WSDL from the published service");
                }
            }
        });

        populateReqMsgSrcComboBox();

        resMsgDestVariableRadioButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                validateResMsgDest();
            }
        });
        resMsgDestVariableTextField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                validateResMsgDest();
            }
        }));
        validateResMsgDest();
        final String resMsgDest = assertion.getResponseMsgDest();
        if (resMsgDest == null) {
            resMsgDestDefaultRadioButton.doClick();
        } else {
            resMsgDestVariableTextField.setText(resMsgDest);
            resMsgDestVariableRadioButton.doClick();
        }

        Set<HttpMethod> methods = EnumSet.allOf(HttpMethod.class);
        methods.removeAll(Arrays.asList(HttpMethod.OTHER)); // Omit methods not supports by Commons HTTP client
        requestMethodComboBox.setModel(new DefaultComboBoxModel(methods.toArray()));
        Utilities.enableGrayOnDisabled(requestMethodComboBox);
        final ActionListener requestMethodListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateRequestMethodComboBoxEnableState();
            }
        };
        overrideRequestMethodRadioButton.addActionListener(requestMethodListener);
        automaticRequestMethodRadioButton.addActionListener(requestMethodListener);

        initializeHttpRulesTabs();
        initializeProxyTab();

        inputValidator.attachToButton(okButton, okButtonAction);
        okButton.setEnabled( !readOnly );

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });
        getRootPane().setDefaultButton(okButton);               
        Utilities.setEscKeyStrokeDisposes(this);
    }

    private void initializeProxyTab() {
        Utilities.enableGrayOnDisabled(proxyHostField);
        Utilities.enableGrayOnDisabled(proxyPortField);
        Utilities.enableGrayOnDisabled(proxyUsernameField);
        Utilities.enableGrayOnDisabled(proxyPasswordField);

        ActionListener enabler = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableOrDisableProxyFields();
            }
        };
        rbProxyNone.addActionListener(enabler);
        rbProxySpecified.addActionListener(enabler);
        enableOrDisableProxyFields();
    }

    private void enableOrDisableProxyFields() {
        final boolean proxy = rbProxySpecified.isSelected();
        proxyHostField.setEnabled(proxy);
        proxyPortField.setEnabled(proxy);
        proxyUsernameField.setEnabled(proxy);
        proxyPasswordField.setEnabled(proxy);
    }

    private void updateRequestMethodComboBoxEnableState() {
        requestMethodComboBox.setEnabled(overrideRequestMethodRadioButton.isSelected());
    }

    private Map<String, VariableMetadata> getVariablesSetByPredecessors() {
        if (assertionToUseInSearchForPredecessorVariables == null) {
            return SsmPolicyVariableUtils.getVariablesSetByPredecessors(assertion);
        } else {
            return SsmPolicyVariableUtils.getVariablesSetByPredecessors(assertionToUseInSearchForPredecessorVariables);
        }
    }

    /**
     * Populates request message source combo box, and sets selection according to assertion.
     */
    private void populateReqMsgSrcComboBox() {
        reqMsgSrcComboBox.removeAllItems();
        reqMsgSrcComboBox.addItem(new MsgSrcComboBoxItem(null, resources.getString("request.msgSrc.default.text")));
        final MessageFormat displayFormat = new MessageFormat(resources.getString("request.msgSrc.contextVariable.format"));
        final Map<String, VariableMetadata> predecessorVariables = getVariablesSetByPredecessors();
        final SortedSet<String> predecessorVariableNames = new TreeSet<String>(predecessorVariables.keySet());
        for (String variableName: predecessorVariableNames) {
            if (predecessorVariables.get(variableName).getType() == DataType.MESSAGE) {
                final MsgSrcComboBoxItem item = new MsgSrcComboBoxItem(variableName, displayFormat.format(new Object[]{Syntax.SYNTAX_PREFIX, variableName, Syntax.SYNTAX_SUFFIX}));
                reqMsgSrcComboBox.addItem(item);
                if (variableName.equals(assertion.getRequestMsgSrc())) {
                    reqMsgSrcComboBox.setSelectedItem(item);
                }
            }
        }
    }

    private void initializeHttpRulesTabs() {

        // init req rules stuff
        requestHttpRulesTableHandler = new HttpHeaderRuleTableHandler(reqHeadersTable, reqHeadersAdd, reqHeadersRemove, editReqHrButton,
                                                                 assertion.getRequestHeaderRules());
        ActionListener tablestate = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (reqHeadersCustomize.isSelected()) {
                    reqHeadersTable.setEnabled(true);
                    requestHttpRulesTableHandler.setEditable(true);
                    reqHeadersAdd.setEnabled(true);
                    reqHeadersRemove.setEnabled(true);
                    requestHttpRulesTableHandler.updateeditState();
                } else {
                    reqHeadersTable.setEnabled(false);
                    requestHttpRulesTableHandler.setEditable(false);
                    reqHeadersAdd.setEnabled(false);
                    reqHeadersRemove.setEnabled(false);
                    editReqHrButton.setEnabled(false);
                }
            }
        };
        reqHeadersAll.addActionListener(tablestate);
        reqHeadersCustomize.addActionListener(tablestate);

        if (assertion.getRequestHeaderRules().isForwardAll()) {
            reqHeadersAll.setSelected(true);
            reqHeadersAdd.setEnabled(false);
            reqHeadersRemove.setEnabled(false);
            editReqHrButton.setEnabled(false);
            reqHeadersTable.setEnabled(false);
            requestHttpRulesTableHandler.setEditable(false);
        } else {
            reqHeadersCustomize.setSelected(true);
            requestHttpRulesTableHandler.updateeditState();
        }

        if (bra) {
            reqParamsAll.setEnabled(false);
            reqParamsCustomize.setEnabled(false);
            reqParamsTable.setEnabled(false);
            reqParamsAdd.setEnabled(false);
            reqParamsRemove.setEnabled(false);
            editReqPmButton.setEnabled(false);
        } else {
            requestParamsRulesTableHandler = new HttpParamRuleTableHandler(reqParamsTable, reqParamsAdd,
                                                                           reqParamsRemove, editReqPmButton,
                                                                           assertion.getRequestParamRules());
            tablestate = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (reqParamsCustomize.isSelected()) {
                        reqParamsTable.setEnabled(true);
                        requestParamsRulesTableHandler.setEditable(true);
                        reqParamsAdd.setEnabled(true);
                        reqParamsRemove.setEnabled(true);
                        requestParamsRulesTableHandler.updateeditState();
                    } else {
                        reqParamsTable.setEnabled(false);
                        requestParamsRulesTableHandler.setEditable(false);
                        reqParamsAdd.setEnabled(false);
                        reqParamsRemove.setEnabled(false);
                        editReqPmButton.setEnabled(false);
                    }
                }
            };
            reqParamsAll.addActionListener(tablestate);
            reqParamsCustomize.addActionListener(tablestate);

            if (assertion.getRequestParamRules().isForwardAll()) {
                reqParamsAll.setSelected(true);
                reqParamsAdd.setEnabled(false);
                reqParamsRemove.setEnabled(false);
                editReqPmButton.setEnabled(false);
                reqParamsTable.setEnabled(false);
                requestParamsRulesTableHandler.setEditable(false);
            } else {
                reqParamsCustomize.setSelected(true);
                requestParamsRulesTableHandler.updateeditState();
            }
        }

        // init the response stuff
        responseHttpRulesTableHandler = new HttpHeaderRuleTableHandler(resHeadersTable, resHeadersAdd, resHeadersDelete,
                                                                       editResHrButton, assertion.getResponseHeaderRules());
        tablestate = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (resHeadersCustomize.isSelected()) {
                    resHeadersTable.setEnabled(true);
                    responseHttpRulesTableHandler.setEditable(true);
                    resHeadersAdd.setEnabled(true);
                    resHeadersDelete.setEnabled(true);
                    responseHttpRulesTableHandler.updateeditState();
                } else {
                    resHeadersTable.setEnabled(false);
                    responseHttpRulesTableHandler.setEditable(false);
                    resHeadersAdd.setEnabled(false);
                    resHeadersDelete.setEnabled(false);
                    editResHrButton.setEnabled(false);
                }
            }
        };
        resHeadersAll.addActionListener(tablestate);
        resHeadersCustomize.addActionListener(tablestate);

        if (assertion.getResponseHeaderRules().isForwardAll()) {
            resHeadersAll.setSelected(true);
            resHeadersAdd.setEnabled(false);
            resHeadersDelete.setEnabled(false);
            editResHrButton.setEnabled(false);
            resHeadersTable.setEnabled(false);
            responseHttpRulesTableHandler.setEditable(false);
        } else {
            resHeadersCustomize.setSelected(true);
            responseHttpRulesTableHandler.updateeditState();
        }
    }

    private void ok() {
        // Do checks before we start changing the assertion, so Cancel will work as expected
        if (wssPromoteRadio.isSelected() && null == wssPromoteActorCombo.getSelectedItem()) {
            JOptionPane.showMessageDialog(okButton, resources.getString("actorRequiredMessage"));
            return;
        }

        // check url before accepting
        String url = urlPanel.getText();
        boolean bad = false;
        // If the option "Use multiple URLs" is chosen, then we don't care what the main URL.is.
        if (! ipListPanel.isURLsEnabled()) {
            if (url == null || url.length() < 1) {
                url = "<empty>";
                bad = true;
            } else if ( url.indexOf("${") < 0 && !ValidationUtils.isValidUrl( url, false, CollectionUtils.caseInsensitiveSet( "http", "https" ) )) {
                bad = true;
            }
        }
        // If the "Use multiple URLs" option is chosen, but the URL list is empty.  We need to give a warning. 
        else if (ipListPanel.getAddresses().length == 0) {
            DialogDisplayer.showMessageDialog(this,
                resources.getString("warningEmptyMultipleURLsList"),
                resources.getString("validationWarning.title"),
                JOptionPane.WARNING_MESSAGE, null);
            return;
        }
        
        if (bad) {
            JOptionPane.showMessageDialog(okButton, MessageFormat.format(resources.getString("invalidUrlMessage"), url));
            return;
        }

        // Check response message destination.
        if (!validateResMsgDest()) {
            JOptionPane.showMessageDialog(okButton, MessageFormat.format(resources.getString("invalidResMsgDestMessage"), url));
            return;
        }

        // From here down we must succeed

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

        if (authWindowsIntegratedRadio.isSelected()) {
            windowsAuthPanel.updateModel();
        } else {
            // set the assertion as needed
            assertion.setKrbConfiguredAccount(null);
            assertion.setKrbConfiguredPassword(null);
            assertion.setKrbUseGatewayKeytab(false);
            assertion.setKrbDelegatedAuthentication(false);
        }

        assertion.setProtectedServiceUrl(url);
        assertion.setAttachSamlSenderVouches(authSamlRadio.isSelected());
        assertion.setTaiCredentialChaining(authTaiRadio.isSelected());
        assertion.setPassthroughHttpAuthentication(authPassthroughRadio.isSelected());

        if (ipListPanel.isURLsEnabled()) {
            assertion.setCustomURLs(ipListPanel.getAddresses());
            assertion.setCustomIpAddresses(null);
            assertion.setFailoverStrategyName(ipListPanel.getFailoverStrategyName());
        } else if (ipListPanel.isAddressesEnabled()) {
            assertion.setCustomURLs(null);
            assertion.setCustomIpAddresses(ipListPanel.getAddresses());
            assertion.setFailoverStrategyName(ipListPanel.getFailoverStrategyName());
        } else {
            assertion.setCustomURLs(null);
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
        if (maxRetriesDefaultCheckBox.isSelected())
            assertion.setMaxRetries(-1);
        else
            assertion.setMaxRetries((Integer)maxRetriesSpinner.getValue());

        RoutingDialogUtils.configSecurityHeaderHandling(assertion, -1, secHdrButtons);
        if (RoutingAssertion.PROMOTE_OTHER_SECURITY_HEADER == assertion.getCurrentSecurityHeaderHandling())
            assertion.setXmlSecurityActorToPromote(wssPromoteActorCombo.getSelectedItem().toString());

        assertion.setRequestMsgSrc(((MsgSrcComboBoxItem)reqMsgSrcComboBox.getSelectedItem()).getVariableName());

        if (resMsgDestDefaultRadioButton.isSelected()) {
            assertion.setResponseMsgDest(null);
        } else if (resMsgDestVariableRadioButton.isSelected()) {
            assertion.setResponseMsgDest(resMsgDestVariableTextField.getText());
        }

        assertion.getResponseHeaderRules().setRules(responseHttpRulesTableHandler.getData());
        assertion.getResponseHeaderRules().setForwardAll(resHeadersAll.isSelected());

        assertion.getRequestHeaderRules().setRules(requestHttpRulesTableHandler.getData());
        assertion.getRequestHeaderRules().setForwardAll(reqHeadersAll.isSelected());

        if (requestParamsRulesTableHandler != null) { // this will be null in case of BRA
            assertion.getRequestParamRules().setRules(requestParamsRulesTableHandler.getData());
            assertion.getRequestParamRules().setForwardAll(reqParamsAll.isSelected());
        }
        assertion.setFollowRedirects(followRedirectCheck.isSelected());
        assertion.setFailOnErrorStatus(failOnErrorRadio.isSelected());
        assertion.setPassThroughSoapFaults(passThroughCheckBox.isSelected());

        assertion.setGzipEncodeDownstream(gzipCheckBox.isSelected());

        assertion.setHttpMethod(overrideRequestMethodRadioButton.isSelected() ? (HttpMethod)requestMethodComboBox.getSelectedItem() : null);

        assertion.setUseKeepAlives(useKeepalivesCheckBox.isSelected());

        final boolean proxy = rbProxySpecified.isSelected();
        if (proxy) {
            assertion.setProxyHost(proxyHostField.getText());
            assertion.setProxyPort(Integer.parseInt(proxyPortField.getText()));
            assertion.setProxyUsername(proxyUsernameField.getText());
            assertion.setProxyPassword(new String(proxyPasswordField.getPassword()));
        } else {
            assertion.setProxyHost(null);
            assertion.setProxyPort(-1);
            assertion.setProxyUsername(null);
            assertion.setProxyPassword(null);
        }

        confirmed = true;
        fireEventAssertionChanged(assertion);

        this.dispose();
    }

    private void updateAuthMethod() {
        final boolean password = authPasswordRadio.isSelected();
        final boolean saml = authSamlRadio.isSelected();
        final boolean win = authWindowsIntegratedRadio.isSelected();

        httpAuthPanel.setVisible(password);
        samlAuthPanel.setVisible(saml);
        samlAuthPanel.setEnabled(saml);
        windowsAuthPanel.setVisible(win);
        authDetailsPanel.revalidate();
    }


    private void initFormData() {
        urlPanel.setText(assertion.getProtectedServiceUrl());
        JRadioButton which = authNoneRadio;
        if (assertion.isTaiCredentialChaining()) which = authTaiRadio;
        if (assertion.isPassthroughHttpAuthentication()) which = authPassthroughRadio;
        if (assertion.getLogin() != null || assertion.getPassword() != null || assertion.getNtlmHost() != null || assertion.getRealm() != null) which = authPasswordRadio;
        if (assertion.isAttachSamlSenderVouches()) which = authSamlRadio;
        if (assertion.isKrbDelegatedAuthentication() || assertion.isKrbUseGatewayKeytab() || assertion.getKrbConfiguredAccount() != null) which = authWindowsIntegratedRadio;
        which.setSelected(true);

        //we need to apply updateAuthMethod() if none of the JRadioButton from above was selected other than the "None"
        //radio button
        if (which.equals(authNoneRadio)) {
            updateAuthMethod();
        }

        authSamlRadio.setSelected(assertion.isAttachSamlSenderVouches());
        if (assertion.getCustomURLs() != null) {
            ipListPanel.setURLsEnabled(true);
            ipListPanel.setAddresses(assertion.getCustomURLs());
        } else if (assertion.getCustomIpAddresses() != null) {
            ipListPanel.setAddressesEnabled(true);
            ipListPanel.setAddresses(assertion.getCustomIpAddresses());
        } else {
            ipListPanel.setAddressesEnabled(false);
        }
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
        int maxRetries = assertion.getMaxRetries();
        if (maxRetries == -1) {
            maxRetriesSpinner.setValue(3);
            maxRetriesDefaultCheckBox.setSelected(true);
        } else {
            maxRetriesSpinner.setValue(maxRetries);
            maxRetriesDefaultCheckBox.setSelected(false);
        }
        maxRetriesSpinner.setEnabled(!maxRetriesDefaultCheckBox.isSelected());

        // read actor promotion information
        java.util.List<String> existingActors = listExistingXmlSecurityRecipientContextFromPolicy();
        for (String existingActor : existingActors) {
            ((DefaultComboBoxModel) wssPromoteActorCombo.getModel()).addElement(existingActor);
        }

        RoutingDialogUtils.configSecurityHeaderRadioButtons(assertion, -1, wssPromoteActorCombo, secHdrButtons);
        wssPromoteActorCombo.getModel().setSelectedItem(assertion.getXmlSecurityActorToPromote());

        followRedirectCheck.setSelected(assertion.isFollowRedirects());
        if (assertion.isFailOnErrorStatus()) {
            failOnErrorRadio.setSelected(true);
            passThroughCheckBox.setEnabled(true);
        } else {
            neverFailRadio.setSelected(true);
            passThroughCheckBox.setEnabled(false);
        }
        passThroughCheckBox.setSelected(assertion.isPassThroughSoapFaults());

        if (assertion.isGzipEncodeDownstream()) {
            gzipCheckBox.setSelected(true);
        } else {
            gzipCheckBox.setSelected(false);
        }

        HttpMethod method = assertion.getHttpMethod();
        if (method == null || bra) {
            overrideRequestMethodRadioButton.setSelected(false);
            automaticRequestMethodRadioButton.setSelected(true);
            requestMethodComboBox.setSelectedItem(HttpMethod.POST);
        } else {
            automaticRequestMethodRadioButton.setSelected(false);
            overrideRequestMethodRadioButton.setSelected(true);
            requestMethodComboBox.setSelectedItem(method);
        }
        if (bra)
            overrideRequestMethodRadioButton.setEnabled(false);
        updateRequestMethodComboBoxEnableState();

        useKeepalivesCheckBox.setSelected(assertion.isUseKeepAlives());

        String host = assertion.getProxyHost();
        if (host == null || host.trim().length() < 1) {
            rbProxyNone.setSelected(true);
            proxyHostField.setText("");
            proxyPortField.setText("-1");
            proxyUsernameField.setText("");
            proxyPasswordField.setText("");
        } else {
            rbProxySpecified.setSelected(true);
            proxyHostField.setText(host);
            proxyPortField.setText(String.valueOf(assertion.getProxyPort()));
            proxyUsernameField.setText(assertion.getProxyUsername());
            proxyPasswordField.setText(assertion.getProxyPassword());
        }
        if (bra)
            rbProxySpecified.setEnabled(false);
        enableOrDisableProxyFields();
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

    /**
     * Validates the response message destination; with the side effect of setting the status icon and text.
     *
     * @return <code>true</code> if response messge destination is valid, <code>false</code> if invalid
     */
    private boolean validateResMsgDest() {
        boolean ok = RoutingDialogUtils.validateMessageDestinationTextField(
            resMsgDestVariableTextField,
            resMsgDestVariableRadioButton.isSelected(),
            resMsgDestVariableStatusLabel,
            getVariablesSetByPredecessors().keySet()
        );
        refreshDialog();
        return ok;
    }

    /**
     * Resize the dialog due to some components getting extended.
     */
    private void refreshDialog() {
        if (getSize().width < mainPanel.getMinimumSize().width) {
            setSize(mainPanel.getMinimumSize().width, getSize().height);
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
