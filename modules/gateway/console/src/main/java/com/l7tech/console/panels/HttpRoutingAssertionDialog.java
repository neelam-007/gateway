package com.l7tech.console.panels;

import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.console.action.BaseAction;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.panels.resources.HttpRoutingOauthPanel;
import com.l7tech.console.panels.resources.HttpRoutingParamsDialog;
import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.console.table.HttpHeaderRuleTableHandler;
import com.l7tech.console.table.HttpRuleTableHandler;
import com.l7tech.console.util.CipherSuiteGuiUtil;
import com.l7tech.console.util.Registry;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.IpListPanel;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.*;
import com.l7tech.wsdl.Wsdl;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
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
    private static class ComboBoxItem {
        private final Object _value;
        private final String _displayName;
        private ComboBoxItem(Object value, String displayName) {
            _value = value;
            _displayName = displayName;
        }
        public Object getValue() { return _value; }
        @Override
        public String toString() { return _displayName; }
    }

    private static final Logger log = Logger.getLogger(HttpRoutingAssertionDialog.class.getName());
    private static final String ANY_TLS_VERSION = "any";
    private HttpRuleTableHandler responseHttpRulesTableHandler;
    private HttpRuleTableHandler requestHttpRulesTableHandler;

    private final EventListenerList listenerList = new EventListenerList();
    private final HttpRoutingAssertion assertion;
    private final HttpRoutingHttpAuthPanel httpAuthPanel;
    private final HttpRoutingSamlAuthPanel samlAuthPanel;
    private final HttpRoutingWindowsIntegratedAuthPanel windowsAuthPanel;
    private final HttpRoutingOauthPanel oauthPanel;

    private final HttpRoutingParamsDialog formParamsDialog;

    private JPanel mainPanel;

    private JButton defaultUrlButton;
    private IpListPanel ipListPanel;
    //private JCheckBox cookiePropagationCheckBox;

    private JRadioButton authNoneRadio;
    private JRadioButton authPasswordRadio;
    private JRadioButton authPassthroughRadio;
    private JRadioButton authSamlRadio;
    private JRadioButton authTaiRadio;
    private JRadioButton authWindowsIntegratedRadio;
    private JRadioButton authOauthRadioButton;
    private JPanel authDetailsPanel;

    private JRadioButton wssIgnoreRadio;
    private JRadioButton wssCleanupRadio;
    private JRadioButton wssRemoveRadio;
    private JRadioButton wssPromoteRadio;
    private JComboBox wssPromoteActorCombo;

    private JButton okButton;
    private JButton cancelButton;
    private JCheckBox connectTimeoutDefaultCheckBox;
    private JCheckBox readTimeoutDefaultCheckBox;
    private JTextField connectTimeoutTextField;
    private JTextField readTimeoutTextField;
    private JSpinner maxRetriesSpinner;
    private JCheckBox maxRetriesDefaultCheckBox;
    private JTable resHeadersTable;
    private JButton resHeadersAdd;
    private JButton resHeadersDelete;
    private JComboBox reqMsgSrcComboBox;
    private JTable reqHeadersTable;
    private JButton reqHeadersAdd;
    private JButton reqHeadersRemove;
    private JCheckBox followRedirectCheck;
    private JButton editReqHrButton;
    private JButton editResHrButton;
    private JRadioButton failOnErrorRadio;
    private JCheckBox passThroughCheckBox;
    private JRadioButton neverFailRadio;
    private JCheckBox gzipCheckBox;
    private JComboBox<Object> requestMethodComboBox;
    private JRadioButton rbProxyNone;
    private JRadioButton rbProxySpecified;
    private JTextField proxyHostField;
    private JTextField proxyPortField;
    private JTextField proxyUsernameField;
    private JPasswordField proxyPasswordField;
    private JCheckBox showProxyPasswordCheckBox;
    private JCheckBox useKeepalivesCheckBox;
    private JButton cipherSuitesButton;
    private JButton trustedServerCertsButton;
    private JComboBox tlsVersionComboBox;
    private ByteLimitPanel byteLimitPanel;
    private JCheckBox forceIncludeRequestBodyCheckBox;
    private JComboBox httpVersionComboBox;
    private JComboBox<Object> responseDestComboBox;
    private JTextField urlField;
    private JButton customizeFormPostParamsButton;
    private JCheckBox requestHeadersCustomCheckBox;
    private JCheckBox responseHeadersCustomCheckBox;
    private JLabel urlErrorLabel;
    private JLabel methodStatusLabel;
    private JLabel destinationStatusLabel;
    private JLabel sourceStatusLabel;
    private JScrollPane urlErrorScrollPane;

    private final AbstractButton[] secHdrButtons = { wssIgnoreRadio, wssCleanupRadio, wssRemoveRadio, wssPromoteRadio };

    private final BaseAction okButtonAction;
    private boolean confirmed = false;

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.HttpRoutingAssertionDialog");

    private final Policy policy;
    private final Wsdl wsdl;

    private InputValidator inputValidator;
    private TargetVariablePanel variableValidator = new TargetVariablePanel();
    private UrlPanel urlValidator = new UrlPanel( "dummy", "", false );
    private String tlsCipherSuites;
    private Set<EntityHeader> tlsTrustedCerts;
    private final Object REQ_METHOD_AUTO = "<Automatic>";
    private final Object RESP_DEFAULT = "<Default Response>";
    private String responseUniqueContextVariableName = "httpResponse1";
    private Object RESP_UNIQUE_CONTEXT_VAR = responseUniqueContextVariableName;

    /**
     * Creates new form ServicePanel
     */
    public HttpRoutingAssertionDialog(Frame owner, HttpRoutingAssertion assertion, Policy policy, Wsdl wsdl, boolean readOnly) {
        super(owner, assertion, true);
        inputValidator = new InputValidator(this, assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME).toString());
        this.assertion = assertion;
        this.policy = policy;
        this.wsdl = wsdl;
        this.httpAuthPanel = new HttpRoutingHttpAuthPanel(assertion);
        this.samlAuthPanel = new HttpRoutingSamlAuthPanel(assertion, inputValidator);
        this.windowsAuthPanel = new HttpRoutingWindowsIntegratedAuthPanel(assertion);
        this.oauthPanel = new HttpRoutingOauthPanel( assertion );

        this.formParamsDialog = new HttpRoutingParamsDialog( this, assertion );

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

        ipListPanel.alsoEnableDiffURLS();

        ipListPanel.registerStateCallback(new IpListPanel.StateCallback() {
            @Override
            public void stateChanged(int newState) {
                if (newState == IpListPanel.CUSTOM_URLS) {
                    urlField.setEnabled( false );
                } else {
                    urlField.setEnabled( true );
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
        listenerList.add( PolicyListener.class, listener );
    }

    /**
     * remove the the PolicyListener
     * 
     * @param listener the PolicyListener
     */
    public void removePolicyListener(PolicyListener listener) {
        listenerList.remove( PolicyListener.class, listener );
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

        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if(connectTimeoutDefaultCheckBox.isSelected()) return null;

                if(ValidationUtils.isValidInteger(connectTimeoutTextField.getText(), false, 1,86400000)) return null;  // one day in ms

                if(Syntax.getReferencedNames(connectTimeoutTextField.getText()).length > 0)
                    return null;
                else
                     return MessageFormat.format("The {0} field must contain a context variable and/or an integer between {1} and {2}","Connection timeout",1,86400000);
            }
        });

        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if(readTimeoutDefaultCheckBox.isSelected()) return null;

                if(ValidationUtils.isValidInteger(readTimeoutTextField.getText(), false, 1,86400000)) return null;  // one day in ms

                if(Syntax.getReferencedNames(readTimeoutTextField.getText()).length > 0)
                    return null;
                else
                     return MessageFormat.format("The {0} field must contain a context variable and/or an integer between {1} and {2}","Read timeout",1,86400000);
            }
        });

        maxRetriesSpinner.setModel(new SpinnerNumberModel(3,0,100,1));
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(maxRetriesSpinner, "Maximum retries"));

        inputValidator.constrainTextFieldToNumberRange("proxy port", proxyPortField, -1, 65535);
        inputValidator.constrainTextFieldToBeNonEmpty("proxy host", proxyHostField, null);

        ActionListener enableSpinners = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectTimeoutTextField.setEnabled(!connectTimeoutDefaultCheckBox.isSelected());
                readTimeoutTextField.setEnabled(!readTimeoutDefaultCheckBox.isSelected());
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
        methodGroup.add( this.authOauthRadioButton );

        final ChangeListener radioChangeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateAuthMethod();
            }
        };

        authDetailsPanel.setMinimumSize( new Dimension( Math.max( httpAuthPanel.getMinimumSize().width, samlAuthPanel.getMinimumSize().width ), -1 ) );
        authDetailsPanel.setLayout( new GridBagLayout() );
        authDetailsPanel.add(httpAuthPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        authDetailsPanel.add(samlAuthPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        authDetailsPanel.add(windowsAuthPanel, new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        authDetailsPanel.add(oauthPanel, new GridBagConstraints(0, 3, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        authNoneRadio.addChangeListener(radioChangeListener);
        authPasswordRadio.addChangeListener(radioChangeListener);
        authPassthroughRadio.addChangeListener(radioChangeListener);
        authSamlRadio.addChangeListener(radioChangeListener);
        authTaiRadio.addChangeListener(radioChangeListener);
        authWindowsIntegratedRadio.addChangeListener(radioChangeListener);
        authOauthRadioButton.addChangeListener( radioChangeListener );

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
                    urlField.setText(serviceURI);
                    urlField.setCaretPosition( 0 );
                    validateUrl( true );
                } else {
                    log.log(Level.INFO, "Can't retrieve WSDL from the published service");
                }
            }
        });
        urlField.getDocument().addDocumentListener( new RunOnChangeListener( new Runnable() {
            @Override
            public void run() {
                validateUrl( false );
            }
        } ) );
        urlField.addFocusListener( new FocusListener() {
            @Override
            public void focusGained( FocusEvent e ) {}

            @Override
            public void focusLost( FocusEvent e ) {
                validateUrl( true );
            }
        } );

        urlErrorScrollPane.setBorder(null);
        urlErrorScrollPane.getViewport().setBackground( mainPanel.getBackground() );
        FontMetrics fm = urlErrorLabel.getFontMetrics( urlErrorLabel.getFont() );
        urlErrorScrollPane.setMinimumSize( new Dimension( -1, fm.getHeight() ) );

        populateReqMsgSrcComboBox();
        populateHttpVersionComboBox();

        byteLimitPanel.setAllowContextVars(true);
        inputValidator.addRule( new InputValidator.ComponentValidationRule( byteLimitPanel ) {
            @Override
            public String getValidationError() {
                return byteLimitPanel.validateFields();
            }
        } );

        final String resMsgDest = assertion.getResponseMsgDest();
        variableValidator.setAssertion( assertion, getPreviousAssertion() );
        responseUniqueContextVariableName = findUniqueResponseContextVariableName();
        RESP_UNIQUE_CONTEXT_VAR = responseUniqueContextVariableName;
        if ( resMsgDest == null ) {
            responseDestComboBox.setModel( new DefaultComboBoxModel<>( new Object[] { RESP_DEFAULT, RESP_UNIQUE_CONTEXT_VAR } ) );
            responseDestComboBox.setSelectedIndex( 0 );
        } else {
            responseDestComboBox.setModel( new DefaultComboBoxModel<>( new Object[] { RESP_DEFAULT, resMsgDest } ) );
            responseDestComboBox.setSelectedIndex( 1 );
        }
        responseDestComboBox.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e ) {
                validateResponseDestination();
            }
        } );
        inputValidator.addRule( new InputValidator.ComponentValidationRule( responseDestComboBox ) {
            @Override
            public String getValidationError() {
                return validateResponseDestination();
            }
        } );
        inputValidator.addRule( new InputValidator.ComponentValidationRule( requestMethodComboBox ) {
            @Override
            public String getValidationError() {
                return validateRequestMethod();
            }
        } );
        requestMethodComboBox.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e ) {
                validateRequestMethod();
            }
        } );
        inputValidator.addRule( new InputValidator.ComponentValidationRule( oauthPanel ) {
            @Override
            public String getValidationError() {
                if ( !authOauthRadioButton.isSelected() ) {
                    return null;
                }

                String var = oauthPanel.getTokenVariable();
                if ( var.trim().length() < 1 ) {
                    return "OAuth Token Variable is required";
                }

                String err = validateVariableName( var, true, false );

                if ( err != null ) {
                    return "OAuth Token Variable: " + err;
                }

                return null;
            }
        });
        reqMsgSrcComboBox.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e ) {
                validateRequestSource();
            }
        } );
        requestMethodComboBox.addFocusListener( new FocusAdapter() {
            @Override
            public void focusGained( FocusEvent e ) {
                requestMethodComboBox.getEditor().selectAll();
            }
        } );

        responseDestComboBox.addFocusListener( new FocusAdapter() {
            @Override
            public void focusGained( FocusEvent e ) {
                responseDestComboBox.getEditor().selectAll();
            }
        } );

        byteLimitPanel.setValue( assertion.getResponseSize(), Registry.getDefault().getPolicyAdmin().getXmlMaxBytes() );

        tlsVersionComboBox.setModel( new DefaultComboBoxModel( new String[] {ANY_TLS_VERSION, "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2" } ) );
        tlsVersionComboBox.setRenderer( new TextListCellRenderer<Object>( new Functions.Unary<String,Object>(){
            @Override
            public String call( final Object protocol ) {
                return resources.getString("tls-protocol." + protocol) ;
            }
        }, null, true ) );
        Utilities.enableGrayOnDisabled(tlsVersionComboBox);
        inputValidator.addRule(new InputValidator.ComponentValidationRule(tlsVersionComboBox) {
            @Override
            public String getValidationError() {
                final Object version = tlsVersionComboBox.getSelectedItem();
                return version == null || ANY_TLS_VERSION.equals(version) || CipherSuiteGuiUtil.isSupportedTlsVersion(version)
                        ? null
                        : "The selected TLS version is not available with the Gateway's current security provider configuration.";
            }
        });

        customizeFormPostParamsButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e ) {
                formParamsDialog.pack();
                Utilities.centerOnParentWindow( formParamsDialog );
                formParamsDialog.setVisible( true );
            }
        } );

        cipherSuitesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CipherSuiteDialog.show(HttpRoutingAssertionDialog.this, "Enabled Cipher Suites", null, readOnly, tlsCipherSuites, new Functions.UnaryVoid<String>() {
                    @Override
                    public void call(String s) {
                        tlsCipherSuites = s;
                    }
                });

            }
        });
        trustedServerCertsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TrustedCertsDialog.show(HttpRoutingAssertionDialog.this, "Trusted TLS Server Certificates", null, readOnly, tlsTrustedCerts, new Functions.UnaryVoid<List<EntityHeader>>() {
                    @Override
                    public void call(List<EntityHeader> entityHeaders) {
                        tlsTrustedCerts = entityHeaders == null ? null : new LinkedHashSet<EntityHeader>(entityHeaders);
                    }
                });
            }
        });
        Utilities.enableGrayOnDisabled(cipherSuitesButton, trustedServerCertsButton);

        initializeHttpRulesTabs();
        initializeProxyTab();

        inputValidator.attachToButton(okButton, okButtonAction);
        okButton.setEnabled( !readOnly );

        cancelButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e ) {
                dispose();
            }
        } );

        Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });
        getRootPane().setDefaultButton(okButton);
    }

    private String validateRequestMethod() {
        final String err;
        Icon icon = null;
        String label = "";

        Object methObj = requestMethodComboBox.getSelectedItem();
        if ( methObj == null ) {
            err = "must be specified";
            icon = variableValidator.getWarningIcon();
        } else if ( methObj instanceof HttpMethod ) {
            err = null;
        } else if ( methObj == REQ_METHOD_AUTO ) {
            err = null;
        } else if ( !( methObj instanceof String ) ) {
            // Can't happen
            err = "must be a valid option or a variable name";
        } else {
            String methVar = methObj.toString();
            if ( methVar.trim().length() < 1 ) {
                err = "may not be empty";
                icon = variableValidator.getWarningIcon();
            } else {
                // See if any context variables are present; if so, validate them
                String[] vars = Syntax.getReferencedNames( methVar, false );
                if ( vars.length < 1 ) {
                    err = null;
                    label = "(Custom; no context variables)";
                } else {
                    String varErr = null;
                    for ( String var : vars ) {
                        varErr = validateVariableName( var, true, false );
                        if ( varErr != null )
                            break;
                    }

                    err = varErr;
                    if ( err == null ) {
                        if ( vars.length == 1 ) {
                            label = "(Existing context variable)";
                        } else {
                            label = "(Existing context variables)";
                        }
                    } else {
                        icon = variableValidator.getStatusLabelIcon();
                    }
                }
            }
        }

        if ( err != null ) {
            label = err;
        }
        methodStatusLabel.setIcon( icon );
        methodStatusLabel.setText( label );
        return err == null ? null : "Request Method: " + err;
    }

    private String validateResponseDestination() {
        final String err;
        Icon icon = null;
        String label = "";

        Object respObj = responseDestComboBox.getSelectedItem();

        if ( respObj == null ) {
            err = "must be specified";
            icon = variableValidator.getWarningIcon();
        } else if ( respObj == RESP_DEFAULT ) {
            // Ok
            err = null;
        } else if ( respObj == RESP_UNIQUE_CONTEXT_VAR ) {
            // Ok
            err = null;
            label = "(New context variable)";
        } else if ( !(respObj instanceof String) ) {
            // Can't happen
            err = "must be a valid option or a variable name";
        } else {
            String respVar = (String) respObj;
            if ( respVar.trim().length() < 1 ) {
                err = "may not be empty";
                icon = variableValidator.getWarningIcon();
            } else {
                err = validateVariableName( respVar, false, true );
                if ( err == null ) {
                    TargetVariablePanel.Status status = variableValidator.getVariableStatus();
                    if ( status == null || !status.isOk() ) {
                        // Can't happen
                        label = variableValidator.getStatusLabelText();
                    } else if ( status.isVariableAlreadyExists() ) {
                        label = "(Existing variable; will overwrite)";
                    } else {
                        label = "(New context variable)";
                    }
                } else {
                    icon = variableValidator.getStatusLabelIcon();
                }
            }
        }

        if ( err != null ) {
            label = err;
        }
        destinationStatusLabel.setIcon( icon );
        destinationStatusLabel.setText( label );
        return err == null ? null : "Response Destination: " + err;
    }

    private void validateRequestSource() {
        final String label;

        ComboBoxItem item = (ComboBoxItem) reqMsgSrcComboBox.getSelectedItem();
        if ( item == null ) {
            // Can't happen
            label = "must be specified";
        } else if ( item.getValue() == null ) {
            // Ok, it's default request
            label = "";
        } else if ( item.getValue() instanceof String ) {
            label = "(Existing context variable)";
        } else {
            // Can't happen
            label = "must be specified or a string";
        }

        sourceStatusLabel.setText( label );
    }

    private String findUniqueResponseContextVariableName() {
        String uniqueVar;

        Set<String> setVars = new TreeSet<String>( String.CASE_INSENSITIVE_ORDER );
        setVars.addAll( getVariablesSetByPredecessors().keySet() );

        int count = 0;
        do {
            count++;
            // TODO strengthen the uniqueness of this -- make it include the current policy name, or part of policy Goid or something,
            // to avoid policy includes all colliding on httpResponse1 (since includes can't tell what vars are used by the policies that include them)
            uniqueVar = "httpResponse" + count;
        } while ( setVars.contains( uniqueVar ) );

        return uniqueVar;
    }

    private void initializeProxyTab() {
        Utilities.enableGrayOnDisabled(proxyHostField);
        Utilities.enableGrayOnDisabled( proxyPortField );
        Utilities.enableGrayOnDisabled(proxyUsernameField);
        Utilities.enableGrayOnDisabled(proxyPasswordField);
        Utilities.configureShowPasswordButton(showProxyPasswordCheckBox, proxyPasswordField);

        ActionListener enabler = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableOrDisableProxyFields();
            }
        };
        rbProxyNone.addActionListener( enabler );
        rbProxySpecified.addActionListener( enabler );
        enableOrDisableProxyFields();
    }

    private void enableOrDisableProxyFields() {
        final boolean proxy = rbProxySpecified.isSelected();
        proxyHostField.setEnabled(proxy);
        proxyPortField.setEnabled(proxy);
        proxyUsernameField.setEnabled(proxy);
        proxyPasswordField.setEnabled(proxy);
    }

    private Map<String, VariableMetadata> getVariablesSetByPredecessors() {
        return SsmPolicyVariableUtils.getVariablesSetByPredecessors(assertion);
    }

    /**
     * Populates request message source combo box, and sets selection according to assertion.
     */
    private void populateReqMsgSrcComboBox() {
        reqMsgSrcComboBox.removeAllItems();
        reqMsgSrcComboBox.addItem(new ComboBoxItem(null, resources.getString("request.msgSrc.default.text")));
        final Map<String, VariableMetadata> predecessorVariables = getVariablesSetByPredecessors();
        final SortedSet<String> predecessorVariableNames = new TreeSet<String>(predecessorVariables.keySet());
        for (String variableName: predecessorVariableNames) {
            if (predecessorVariables.get(variableName).getType() == DataType.MESSAGE) {
                final ComboBoxItem item = new ComboBoxItem( variableName, variableName );
                reqMsgSrcComboBox.addItem(item);
                if (variableName.equals(assertion.getRequestMsgSrc())) {
                    reqMsgSrcComboBox.setSelectedItem(item);
                }
            }
        }
        validateRequestSource();
    }

    private void populateHttpVersionComboBox() {
        httpVersionComboBox.removeAllItems();
        ComboBoxItem item = new ComboBoxItem(null, resources.getString("request.httpVersion.default.text"));
        httpVersionComboBox.addItem(item);
        if (assertion.getHttpVersion() == null) {
            httpVersionComboBox.setSelectedItem(item);
        }
        GenericHttpRequestParams.HttpVersion versions[] = GenericHttpRequestParams.HttpVersion.values();
        for (int i = 0; i < versions.length; i++) {
            item = new ComboBoxItem(versions[i], versions[i].getValue());
            httpVersionComboBox.addItem(item);
            if (assertion.getHttpVersion() != null && assertion.getHttpVersion() == versions[i] ) {
                httpVersionComboBox.setSelectedItem(item);
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
                if (requestHeadersCustomCheckBox.isSelected()) {
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
        requestHeadersCustomCheckBox.addActionListener( tablestate );

        if (assertion.getRequestHeaderRules().isForwardAll()) {
            requestHeadersCustomCheckBox.setSelected( false );
            reqHeadersAdd.setEnabled(false);
            reqHeadersRemove.setEnabled(false);
            editReqHrButton.setEnabled(false);
            reqHeadersTable.setEnabled(false);
            requestHttpRulesTableHandler.setEditable(false);
        } else {
            requestHeadersCustomCheckBox.setSelected( true );
            requestHttpRulesTableHandler.updateeditState();
        }

        // init the response stuff
        responseHttpRulesTableHandler = new HttpHeaderRuleTableHandler(resHeadersTable, resHeadersAdd, resHeadersDelete,
                                                                       editResHrButton, assertion.getResponseHeaderRules());
        tablestate = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ( responseHeadersCustomCheckBox.isSelected() ) {
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
        responseHeadersCustomCheckBox.addActionListener( tablestate );

        if (assertion.getResponseHeaderRules().isForwardAll()) {
            responseHeadersCustomCheckBox.setSelected( false );
            resHeadersAdd.setEnabled(false);
            resHeadersDelete.setEnabled(false);
            editResHrButton.setEnabled(false);
            resHeadersTable.setEnabled(false);
            responseHttpRulesTableHandler.setEditable(false);
        } else {
            responseHeadersCustomCheckBox.setSelected( true );
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
        String url = urlField.getText();
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

        // Check response message destination - not needed, OK button diabled

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

        if ( authOauthRadioButton.isSelected() ) {
            oauthPanel.updateModel();
        } else {
            assertion.setAuthOauthTokenVar( null );
            assertion.setAuthOauthVersion( null );
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
            assertion.setConnectionTimeout((String)null);
        else
            assertion.setConnectionTimeout(connectTimeoutTextField.getText());
        if (readTimeoutDefaultCheckBox.isSelected())
            assertion.setTimeout((String)null);
        else
            assertion.setTimeout(readTimeoutTextField.getText());
        if (maxRetriesDefaultCheckBox.isSelected())
            assertion.setMaxRetries(-1);
        else
            assertion.setMaxRetries((Integer)maxRetriesSpinner.getValue());

        RoutingDialogUtils.configSecurityHeaderHandling(assertion, -1, secHdrButtons);
        if (RoutingAssertion.PROMOTE_OTHER_SECURITY_HEADER == assertion.getCurrentSecurityHeaderHandling())
            assertion.setXmlSecurityActorToPromote(wssPromoteActorCombo.getSelectedItem().toString());

        assertion.setRequestMsgSrc((String)((ComboBoxItem) reqMsgSrcComboBox.getSelectedItem()).getValue());
        assertion.setHttpVersion((GenericHttpRequestParams.HttpVersion) ((ComboBoxItem) httpVersionComboBox.getSelectedItem()).getValue());

        assertion.setResponseSize(byteLimitPanel.getValue());

        assertion.getResponseHeaderRules().setRules(responseHttpRulesTableHandler.getData());
        assertion.getResponseHeaderRules().setForwardAll( !responseHeadersCustomCheckBox.isSelected() );

        assertion.getRequestHeaderRules().setRules(requestHttpRulesTableHandler.getData());
        assertion.getRequestHeaderRules().setForwardAll( !requestHeadersCustomCheckBox.isSelected() );

        formParamsDialog.updateAssertion();

        assertion.setFollowRedirects(followRedirectCheck.isSelected());
        assertion.setFailOnErrorStatus(failOnErrorRadio.isSelected());
        assertion.setPassThroughSoapFaults(passThroughCheckBox.isSelected());

        assertion.setGzipEncodeDownstream(gzipCheckBox.isSelected());

        Object responseDestObj = responseDestComboBox.getSelectedItem();
        if ( RESP_DEFAULT == responseDestObj ) {
            assertion.setResponseMsgDest( null );
        } else if ( RESP_UNIQUE_CONTEXT_VAR == responseDestObj ) {
            assertion.setResponseMsgDest( responseUniqueContextVariableName );
        } else if ( responseDestObj != null ) {
            assertion.setResponseMsgDest( Syntax.stripSyntax( responseDestObj.toString() ) );
        } else {
            assertion.setResponseMsgDest( null );
        }

        Object methodObj = requestMethodComboBox.getSelectedItem();
        if ( REQ_METHOD_AUTO == methodObj ) {
            assertion.setHttpMethod( null );
            assertion.setHttpMethodAsString( null );
        } else if ( methodObj instanceof HttpMethod ) {
            HttpMethod method = (HttpMethod) methodObj;
            assertion.setHttpMethod( method );
            assertion.setHttpMethodAsString( null );
        } else if ( methodObj instanceof String ) {
            assertion.setHttpMethod( HttpMethod.OTHER );
            assertion.setHttpMethodAsString( (String)methodObj );
        } else {
            assertion.setHttpMethod( null );
            assertion.setHttpMethodAsString( null );
        }

        assertion.setUseKeepAlives(useKeepalivesCheckBox.isSelected());

        assertion.setForceIncludeRequestBody(forceIncludeRequestBodyCheckBox.isSelected());

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

        String tlsVersion = (String) tlsVersionComboBox.getSelectedItem();
        assertion.setTlsVersion(tlsVersion == null || ANY_TLS_VERSION.equals(tlsVersion) ? null : tlsVersion);
        assertion.setTlsCipherSuites(tlsCipherSuites);
        if (tlsTrustedCerts == null) {
            assertion.setTlsTrustedCertGoids((Goid[]) null);
            assertion.setTlsTrustedCertNames(null);
        } else {
            EntityHeader[] certs = tlsTrustedCerts.toArray(new EntityHeader[tlsTrustedCerts.size()]);
            Goid[] oids = new Goid[certs.length];
            String[] names = new String[certs.length];
            for (int i = 0; i < certs.length; i++) {
                EntityHeader cert = certs[i];
                oids[i] = cert.getGoid();
                names[i] = cert.getName();
            }
            assertion.setTlsTrustedCertGoids(oids);
            assertion.setTlsTrustedCertNames(names);
        }

        confirmed = true;
        fireEventAssertionChanged(assertion);

        this.dispose();
    }

    private void updateAuthMethod() {
        final boolean password = authPasswordRadio.isSelected();
        final boolean saml = authSamlRadio.isSelected();
        final boolean win = authWindowsIntegratedRadio.isSelected();
        final boolean oauth = authOauthRadioButton.isSelected();

        httpAuthPanel.setVisible(password);
        samlAuthPanel.setVisible(saml);
        samlAuthPanel.setEnabled(saml);
        windowsAuthPanel.setVisible(win);
        oauthPanel.setVisible(oauth);
        authDetailsPanel.revalidate();
    }


    private void initFormData() {
        urlField.setText(assertion.getProtectedServiceUrl());
        urlField.setCaretPosition( 0 );
        JRadioButton which = authNoneRadio;
        if (assertion.isTaiCredentialChaining()) which = authTaiRadio;
        if (assertion.isPassthroughHttpAuthentication()) which = authPassthroughRadio;
        if (assertion.getLogin() != null || assertion.getPassword() != null || assertion.getNtlmHost() != null || assertion.getRealm() != null) which = authPasswordRadio;
        if (assertion.isAttachSamlSenderVouches()) which = authSamlRadio;
        if (assertion.isKrbDelegatedAuthentication() || assertion.isKrbUseGatewayKeytab() || assertion.getKrbConfiguredAccount() != null) which = authWindowsIntegratedRadio;
        if ( assertion.getAuthOauthTokenVar() != null && assertion.getAuthOauthTokenVar().trim().length() > 0 ) {
            which = authOauthRadioButton;
        }
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

        if (assertion.getConnectionTimeout() == null) {
            connectTimeoutTextField.setText("30000");
            connectTimeoutDefaultCheckBox.setSelected(true);
        } else {
            connectTimeoutTextField.setText(assertion.getConnectionTimeout());
            connectTimeoutDefaultCheckBox.setSelected(false);
        }
        connectTimeoutTextField.setEnabled(!connectTimeoutDefaultCheckBox.isSelected());
        if (assertion.getTimeout() == null) {
            readTimeoutTextField.setText("60000");
            readTimeoutDefaultCheckBox.setSelected(true);
        } else {
            readTimeoutTextField.setText(assertion.getTimeout());
            readTimeoutDefaultCheckBox.setSelected(false);
        }
        readTimeoutTextField.setEnabled(!readTimeoutDefaultCheckBox.isSelected());
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

        // Populate HTTP method combo box
        Set<HttpMethod> methods = EnumSet.allOf(HttpMethod.class);
        methods.removeAll(Arrays.asList(HttpMethod.OTHER));
        Collection<Object> methodsList = new ArrayList<>();
        methodsList.add( REQ_METHOD_AUTO );
        methodsList.addAll( methods );

        String methodString = assertion.getHttpMethodAsString();
        if ( methodString != null ) {
            methodsList.add( methodString );
        }
        requestMethodComboBox.setModel( new DefaultComboBoxModel<>( methodsList.toArray() ) );

        // Set selection of HTTP method combo box
        HttpMethod method = assertion.getHttpMethod();
        if ( method == null ) {
            requestMethodComboBox.setSelectedItem( REQ_METHOD_AUTO );
        } else if ( HttpMethod.OTHER.equals( method ) ) {
            requestMethodComboBox.setSelectedItem( methodString );
        } else {
            requestMethodComboBox.setSelectedItem( method );
        }

        useKeepalivesCheckBox.setSelected(assertion.isUseKeepAlives());

        forceIncludeRequestBodyCheckBox.setSelected(assertion.isForceIncludeRequestBody());

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

        String tlsVersion = assertion.getTlsVersion();
        tlsVersionComboBox.setSelectedItem(null == tlsVersion ? ANY_TLS_VERSION : tlsVersion);
        tlsVersionComboBox.setEnabled( true );

        tlsCipherSuites = assertion.getTlsCipherSuites();
        cipherSuitesButton.setEnabled( true );

        if (assertion.getTlsTrustedCertGoids() == null) {
            tlsTrustedCerts = null;
        } else {
            tlsTrustedCerts = new LinkedHashSet<EntityHeader>();
            Goid[] oids = assertion.getTlsTrustedCertGoids();
            for (int i = 0; i < oids.length; i++) {
                Goid oid = oids[i];
                String name = assertion.certName(i);
                if (name == null || name.trim().length() < 1) {
                    // Look up name (Bug #12127)
                    name = lookUpTrustedCertName(oid);
                    if (name == null)
                        name = "<Missing Trusted Certificate OID #" + oid + ">";
                }
                tlsTrustedCerts.add(new EntityHeader(oid, EntityType.TRUSTED_CERT, name, "Trusted SSL/TLS server cert"));
            }
        }
        trustedServerCertsButton.setEnabled( true );

        enableOrDisableProxyFields();
        validateUrl( true );
        validateRequestMethod();
        validateResponseDestination();
    }

    private String lookUpTrustedCertName(Goid oid) {
        try {
            TrustedCert cert = Registry.getDefault().getTrustedCertManager().findCertByPrimaryKey(oid);
            return cert == null ? null : cert.getName();
        } catch (FindException e) {
            log.log(Level.INFO, "Unable to retrieve trusted certificate GOID " + oid + ": " + ExceptionUtils.getMessage(e), e);
            return null;
        }
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

    private void validateUrl(boolean full) {
        String url = urlField.getText();
        String message = urlValidator.getSyntaxError( url );
        if ( full && message == null ) {
            (new BackgroundUrlValidator( url )).execute();
        }
        if ( message == null ) {
            message = " ";
        }
        urlErrorLabel.setText( message );
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

    /**
     * Validate a variable name using the variable validator, which must already have been initialized
     * with a policy position.
     *
     * @param var name of variable, possibly including dollar-brace and close-brace.
     * @param willBeRead true if variable will be read from at runtime.
     * @param willBeWritten true if variable will be written to or created at runtime.
     * @return a validation error message, or null if everything looks good.
     */
    String validateVariableName( String var, boolean willBeRead, boolean willBeWritten ) {
        variableValidator.setValueWillBeRead( willBeRead );
        variableValidator.setValueWillBeWritten( willBeWritten );
        variableValidator.setAcceptEmpty( false );
        variableValidator.setAlwaysPermitSyntax( false );

        variableValidator.setVariable( Syntax.stripSyntax( var ) );
        return variableValidator.getErrorMessage();
    }

    private class BackgroundUrlValidator extends SwingWorker<String, Object> {
        final String url;

        private BackgroundUrlValidator( String url ) {
            this.url = url;
        }

        @Override
        protected String doInBackground() throws Exception {
            return urlValidator.getSemanticError( url );
        }

        @Override
        protected void done() {
            try {
                String error = get();
                if ( error != null )
                    urlErrorLabel.setText( error );
            } catch ( Exception e ) {
                log.log( Level.INFO, "Unable to validate URL: " + ExceptionUtils.getMessage( e ), e );
            }
        }
    }
}
