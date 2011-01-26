package com.l7tech.console.panels;

import com.japisoft.xmlpad.PopupModel;
import com.japisoft.xmlpad.UIAccessibility;
import com.japisoft.xmlpad.XMLContainer;
import com.japisoft.xmlpad.action.ActionModel;
import com.japisoft.xmlpad.editor.XMLEditor;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.console.action.Actions;
import com.l7tech.console.action.CreateServiceWsdlAction;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.WsdlComposer;
import com.l7tech.console.util.WsdlDependenciesResolver;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.gui.FilterDocument;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import com.l7tech.uddi.WsdlPortInfo;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.xml.soap.SoapVersion;
import org.w3c.dom.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SSM Dialog for editing properties of a PublishedService object.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * @author flascell<br/>
 */
public class ServicePropertiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(ServicePropertiesDialog.class.getName());

    private final PublishedService subject;
    private final boolean wasTracingEnabled;
    private XMLEditor editor;
    private Collection<ServiceDocument> newWsdlDocuments;
    private Document newWSDL = null;
    private String newWSDLUrl = null;
    private boolean wasoked = false;
    private static final String STD_PORT = ":8080";
    private static final String STD_PORT_DISPLAYED = ":[port]";
    private JPanel mainPanel;
    private JTabbedPane tabbedPane1;
    private JTextField nameField;
    private JRadioButton noURIRadio;
    private JRadioButton customURIRadio;
    private JTextField uriField;
    private JCheckBox getCheck;
    private JCheckBox putCheck;
    private JCheckBox postCheck;
    private JCheckBox deleteCheck;
    private JCheckBox headCheck;
    private JPanel wsdlPanel;
    private JButton resetWSDLButton;
    private JButton helpButton;
    private JButton okButton;
    private JButton cancelButton;
    private JRadioButton enableRadio;
    private JRadioButton disableRadio;
    private JButton editWSDLButton;
    private JEditorPane routingURL;
    private JCheckBox laxResolutionCheckbox;
    private JTextField oidField;
    private JTextField policyGuidField;
    private JCheckBox enableWSSSecurityProcessingCheckBox;
    private JButton selectButton;
    private JButton clearButton;
    private JCheckBox wsdlUnderUDDIControlCheckBox;
    private JButton serviceUDDISettingsButton;
    private JTextField uddiRegistryTextField;
    private JTextField bsNameTextField;
    private JTextField bsKeyTextField;
    private JTextField wsdlPortTextField;
    private JCheckBox monitoringEnabledCheckBox;
    private JCheckBox monitoringDisableServicecheckBox;
    private JCheckBox monitoringUpdateWsdlCheckBox;
    private JTextField wsdlBindingTextField;
    private JTextField wsdlBindingNamespaceTextField;
    private JCheckBox tracingCheckBox;
    private JRadioButton soapVersionUnspecifiedButton;
    private JRadioButton soapVersion11Button;
    private JRadioButton soapVersion12Button;
    private JButton checkForResolutionConflictsButton;
    private JLabel readOnlyWarningLabel;
    private JLabel resolutionConflictWarningLabel;
    private String ssgURL;
    private final boolean canUpdate;
    private final boolean canTrace;
    private UDDIServiceControl uddiServiceControl;
    private UDDIProxiedServiceInfo uddiProxiedServiceInfo;
    private String originalServiceEndPoint;
    private Long lastModifiedTimeStamp;
    private String accessPointURL;

    public ServicePropertiesDialog(Frame owner, PublishedService svc, boolean hasUpdatePermission, boolean hasTracePermission) {
        super(owner, true);
        subject = svc;
        wasTracingEnabled = subject.isTracingEnabled();

        if (areUnsavedChangesToThisPolicy(svc)) {
            readOnlyWarningLabel.setText("Service has unsaved policy changes");
            this.canUpdate = false;
            this.canTrace = false;
        } else {
            if (hasResolutionConflict(svc, null)) {
                resolutionConflictWarningLabel.setText("Service has resolution conflict");
            }
            this.canUpdate = hasUpdatePermission;
            this.canTrace = hasTracePermission;
        }

        initialize();
        DialogDisplayer.suppressSheetDisplay(this); // incompatible with xmlpad
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle("Published Service Properties");

        nameField.setDocument(new FilterDocument(255, new FilterDocument.Filter() {
                                                             @Override
                                                             public boolean accept(String str) {
                                                                 return str != null;
                                                             }
                                                         }));
        uriField.setDocument(new FilterDocument(128, null));

        // set initial data
        nameField.setText(subject.getName());
        oidField.setText(subject.getId());
        oidField.putClientProperty(Utilities.PROPERTY_CONTEXT_MENU_AUTO_SELECT_ALL, "true");
        Utilities.attachDefaultContextMenu(oidField);
        Policy policy = subject.getPolicy();
        String policyGuid = policy == null ? "" : policy.getGuid();
        policyGuidField.setText(policyGuid);
        policyGuidField.putClientProperty(Utilities.PROPERTY_CONTEXT_MENU_AUTO_SELECT_ALL, "true");
        Utilities.attachDefaultContextMenu(policyGuidField);
        enableWSSSecurityProcessingCheckBox.setSelected(subject.isWssProcessingEnabled());
        tracingCheckBox.setSelected(subject.isTracingEnabled());
        if (subject.isDisabled()) {
            disableRadio.setSelected(true);
        } else {
            enableRadio.setSelected(true);
        }

        soapVersionUnspecifiedButton.setSelected(false);
        soapVersion11Button.setSelected(false);
        soapVersion12Button.setSelected(false);
        SoapVersion soapVersion = subject.getSoapVersion();
        if (SoapVersion.SOAP_1_1.equals(soapVersion)) {
            soapVersion11Button.setSelected(true);
        } else if (SoapVersion.SOAP_1_2.equals(soapVersion)) {
            soapVersion12Button.setSelected(true);
        } else {
            soapVersionUnspecifiedButton.setSelected(true);
        }

        String hostname = TopComponents.getInstance().ssgURL().getHost();
        // todo, we need to be able to query gateway to get port instead of assuming default
        ssgURL = "http://" + hostname + STD_PORT;        

        String existinguri = subject.getRoutingUri();
        if (subject.isInternal()) {
            noURIRadio.setEnabled(false);
            customURIRadio.setSelected(true);
            uriField.setEnabled(true);
            uriField.setText(existinguri);
        } else {
            if (existinguri == null) {
                noURIRadio.setSelected(true);
                customURIRadio.setSelected(false);
                uriField.setEnabled(false);
            } else {
                noURIRadio.setSelected(false);
                customURIRadio.setSelected(true);
                uriField.setText(existinguri);
            }
            ActionListener toggleurifield = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    if (noURIRadio.isSelected()) {
                        uriField.setEnabled(false);
                    } else {
                        uriField.setEnabled(true);
                    }
                }
            };
            noURIRadio.addActionListener(toggleurifield);
            customURIRadio.addActionListener(toggleurifield);
        }
        Set<HttpMethod> methods = subject.getHttpMethodsReadOnly();
        if (methods.contains(HttpMethod.GET)) {
            getCheck.setSelected(true);
        }
        if (methods.contains(HttpMethod.PUT)) {
            putCheck.setSelected(true);
        }
        if (methods.contains(HttpMethod.POST)) {
            postCheck.setSelected(true);
        }
        if (methods.contains(HttpMethod.DELETE)) {
            deleteCheck.setSelected(true);
        }
        if (methods.contains(HttpMethod.HEAD)) {
            headCheck.setSelected(true);
        }

        if (!subject.isSoap()) {
            tabbedPane1.setEnabledAt(2, false);
            tabbedPane1.setEnabledAt(3, false);            
            noURIRadio.setEnabled(false);
            customURIRadio.setSelected(true);
        } else {
            XMLContainer xmlContainer = new XMLContainer(true);
            final UIAccessibility uiAccessibility = xmlContainer.getUIAccessibility();
            editor = uiAccessibility.getEditor();
            setWsdl( editor, null, subject.getWsdlXml(), subject.isInternal() );
            Action reformatAction = ActionModel.getActionByName(ActionModel.FORMAT_ACTION);
            reformatAction.actionPerformed(null);
            uiAccessibility.setTreeAvailable(false);
            uiAccessibility.setTreeToolBarAvailable(false);
            xmlContainer.setEditable(false);
            uiAccessibility.setToolBarAvailable(false);
            xmlContainer.setStatusBarAvailable(false);
            PopupModel popupModel = xmlContainer.getPopupModel();
            // remove the unwanted actions
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.PARSE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.FORMAT_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.LOAD_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.SAVE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.SAVEAS_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.NEW_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.INSERT_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.COMMENT_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_SELECTNODE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_COMMENTNODE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_COPYNODE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_CUTNODE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_EDITNODE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_CLEANHISTORY_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_ADDHISTORY_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_PREVIOUS_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_NEXT_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.UNDO_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.REDO_ACTION));
            boolean lastWasSeparator = true; // remove trailing separator
            for (int i=popupModel.size()-1; i>=0; i--) {
                boolean isSeparator = popupModel.isSeparator(i);
                if (isSeparator && (i==0 || lastWasSeparator)) {
                    popupModel.removeSeparator(i);
                } else {
                    lastWasSeparator = isSeparator;
                }
            }
            wsdlPanel.setLayout(new BorderLayout());
            wsdlPanel.add(xmlContainer.getView(), BorderLayout.CENTER);
            laxResolutionCheckbox.setSelected(subject.isLaxResolution());
        }
        // event handlers
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        helpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                help();
            }
        });

        checkForResolutionConflictsButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                checkResolution();
            }
        } );

        uriField.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {
                //always start with "/" for URI except an empty uri.
                String uri = uriField.getText();
                if (uri != null && !uri.isEmpty() && !uri.startsWith("/")) {
                    uri = "/" + uri.trim();
                    uriField.setText(uri);
                }

                updateURL();
            }

            @Override
            public void keyTyped(KeyEvent e) {}
        });
        uriField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {}
            @Override
            public void focusLost(FocusEvent e) {
                if (customURIRadio.isSelected()) {
                    String url = updateURL();
                    if (url != null && !url.startsWith("/")) url = "/" + url;
                    uriField.setText(url);
                }
            }
        });
        if (!subject.isInternal()) {
            noURIRadio.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    updateURL();
                }
            });
        }
        customURIRadio.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                updateURL();
            }
        });

        resetWSDLButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                resetWSDL();
            }
        });

        editWSDLButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editWsdl();
            }
        });

        //UDDI tab
        serviceUDDISettingsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final ServiceUDDISettingsDialog dlg =
                        new ServiceUDDISettingsDialog(ServicePropertiesDialog.this, subject, false);
                dlg.pack();
                Utilities.centerOnScreen(dlg);
                DialogDisplayer.display(dlg);
            }
        });

        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if(uddiServiceControl == null) return;//this is a programming error

                if(uddiServiceControl.getOid() == UDDIServiceControl.DEFAULT_OID){
                    clearLocalUDDIServiceControl();
                }else{
                    try {
                        final UDDIProxiedServiceInfo serviceInfo =
                                Registry.getDefault().getUDDIRegistryAdmin().findProxiedServiceInfoForPublishedService(uddiServiceControl.getPublishedServiceOid());
                        if(serviceInfo == null || serviceInfo.getPublishType() == UDDIProxiedServiceInfo.PublishType.PROXY){
                            int selection = JOptionPane.showConfirmDialog(ServicePropertiesDialog.this, "The association to the original UDDI BusinessService will be lost.", "Remove BusinessService Association", JOptionPane.WARNING_MESSAGE);
                            if (selection == 0) {
                                Registry.getDefault().getUDDIRegistryAdmin().deleteUDDIServiceControl(uddiServiceControl.getOid());
                                clearLocalUDDIServiceControl();
                            } else {
                                return;
                            }
                        } else {
                            String errorMsg;
                            if(serviceInfo.getPublishType() == UDDIProxiedServiceInfo.PublishType.OVERWRITE){
                                errorMsg = "BusinessService in UDDI has been overwritten. Please remove before deleting";
                            }else if(serviceInfo.getPublishType() == UDDIProxiedServiceInfo.PublishType.ENDPOINT){
                                errorMsg = "BusinessService in UDDI has had a Gateway endpoint added. Please remove before deleting";
                            }else{
                                throw new IllegalStateException("Illegal publish type found");//can only happen if either publish type enum changes or above logic does
                            }
                            showErrorMessage("Cannot delete", errorMsg, null, false);
                            return;
                        }
                    } catch (FindException e1) {
                        showErrorMessage("Cannot find", "Cannot determine if any proxied UDDI info was published to the service from the Gateway: " + ExceptionUtils.getMessage(e1), ExceptionUtils.getDebugException(e1), true);
                        return;
                    } catch (DeleteException e1) {
                        showErrorMessage("Cannot delete", "Cannot remove existing UDDI information from Gateway: " + ExceptionUtils.getMessage(e1), ExceptionUtils.getDebugException(e1), true);
                        return;
                    } catch (UpdateException e1) {
                        showErrorMessage("Cannot delete", "Cannot remove existing UDDI information from Gateway: " + ExceptionUtils.getMessage(e1), ExceptionUtils.getDebugException(e1), true);
                        return;
                    }
                }

                modelToView();
                enableDisableControls();
            }
        });

        selectButton.addActionListener(new ActionListener() {
            //The select button should only ever be enabled when uddiServiceControl == null
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if(uddiServiceControl != null) throw new RuntimeException("uddiServiceControl should be null");
                    
                    SearchUddiDialog swd = new SearchUddiDialog(ServicePropertiesDialog.this, SearchUddiDialog.SEARCH_TYPE.WSDL_SEARCH, true);
                    swd.addSelectionListener(new SearchUddiDialog.ItemSelectedListener() {
                        @Override
                        public void itemSelected(Object item) {
                            if(!(item instanceof WsdlPortInfo)) return;
                            WsdlPortInfo wsdlPortInfo = (WsdlPortInfo) item;
                            wsdlPortInfo.setWasWsdlPortSelected(true);
                            uddiServiceControl = getNewUDDIServiceControl(wsdlPortInfo);
                            lastModifiedTimeStamp = wsdlPortInfo.getLastUddiMonitoredTimeStamp();
                            accessPointURL = wsdlPortInfo.getAccessPointURL();

                            //now update the UI with selections
                            modelToView();
                            enableDisableControls();
                        }
                    });
                    swd.setSize(700, 500);
                    swd.setModal(true);
                    DialogDisplayer.display(swd);
                } catch (FindException e1) {
                    showErrorMessage("Cannot search UDDI", "Unable to show search UDDI dialog", e1, true);
                }
            }
        });

        RunOnChangeListener enableDisableChangeListener = new RunOnChangeListener( new Runnable(){
            @Override
            public void run() {
                enableDisableControls();
            }
        } );

        wsdlUnderUDDIControlCheckBox.addActionListener( enableDisableChangeListener );
        monitoringEnabledCheckBox.addActionListener( enableDisableChangeListener );

        Utilities.setEscAction(this, new AbstractAction() {//todo why is this not consistently working?
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        Utilities.setEnterAction(this, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        try {
            uddiServiceControl = Registry.getDefault().getUDDIRegistryAdmin().getUDDIServiceControl(subject.getOid());
            if(uddiServiceControl != null){
                originalServiceEndPoint = Registry.getDefault().getUDDIRegistryAdmin().getOriginalServiceEndPoint(uddiServiceControl.getOid());
            }
            uddiProxiedServiceInfo = Registry.getDefault().getUDDIRegistryAdmin().findProxiedServiceInfoForPublishedService(subject.getOid());
        } catch (FindException e) {
            uddiServiceControl = null;
        }

        updateURL();

        modelToView();

        enableDisableControls();

        //apply permissions last
        applyPermissions();
    }

    private void clearLocalUDDIServiceControl() {
        uddiServiceControl = null;
        lastModifiedTimeStamp = null;
        accessPointURL = null;
    }

    private void enableDisableControls(){

        final boolean underUDDIControl = uddiServiceControl != null && uddiServiceControl.isUnderUddiControl();
        //ensure appropriate read only status if this service is internal or if the WSDL is under UDDI control
        applyReadOnlySettings(subject.isInternal() || underUDDIControl);

        selectButton.setEnabled(canUpdate);
        tracingCheckBox.setEnabled(canUpdate && canTrace);
        if (canUpdate && !canTrace)
            tracingCheckBox.setToolTipText("Disabled because enabling tracing requires having permission to update all published services");

        if(uddiServiceControl == null){
            clearButton.setEnabled( false );
            selectButton.setEnabled(canUpdate);
            wsdlUnderUDDIControlCheckBox.setEnabled( false );
            monitoringEnabledCheckBox.setEnabled( false );
            monitoringUpdateWsdlCheckBox.setEnabled( false );
            monitoringDisableServicecheckBox.setEnabled( false );
        }else{
            clearButton.setEnabled(canUpdate);
            selectButton.setEnabled(false);            

            final boolean serviceCannotBeUnderUDDIControl = uddiProxiedServiceInfo != null &&
                    uddiProxiedServiceInfo.getPublishType() != UDDIProxiedServiceInfo.PublishType.PROXY;

            if (serviceCannotBeUnderUDDIControl || uddiServiceControl.isHasBeenOverwritten() || uddiServiceControl.isHasHadEndpointRemoved()) {
                wsdlUnderUDDIControlCheckBox.setEnabled(false);
            } else {
                wsdlUnderUDDIControlCheckBox.setEnabled(canUpdate);
            }

            boolean enableMonitoring = canUpdate && uddiServiceControl != null && wsdlUnderUDDIControlCheckBox.isSelected();
            monitoringEnabledCheckBox.setEnabled( enableMonitoring );
            monitoringUpdateWsdlCheckBox.setEnabled( enableMonitoring && monitoringEnabledCheckBox.isSelected() );
            monitoringDisableServicecheckBox.setEnabled( enableMonitoring && monitoringEnabledCheckBox.isSelected() );
        }
    }

    private UDDIServiceControl getNewUDDIServiceControl(WsdlPortInfo wsdlPortInfo) {
        return new UDDIServiceControl(subject.getOid(), wsdlPortInfo.getUddiRegistryOid(),
                wsdlPortInfo.getBusinessEntityKey(), wsdlPortInfo.getBusinessEntityName(), wsdlPortInfo.getBusinessServiceKey(),
                wsdlPortInfo.getBusinessServiceName(), wsdlPortInfo.getWsdlServiceName(), wsdlPortInfo.getWsdlPortName(),
                wsdlPortInfo.getWsdlPortBinding(), wsdlPortInfo.getWsdlPortBindingNamespace(),
                wsdlUnderUDDIControlCheckBox.isSelected() || wsdlPortInfo.isWasWsdlPortSelected());
    }

    /**
     * Model to View - for UDDI tab initially
     */
    private void modelToView(){

        if(uddiServiceControl != null){
            try {
                final UDDIRegistry uddiRegistry = Registry.getDefault().getUDDIRegistryAdmin().findByPrimaryKey(uddiServiceControl.getUddiRegistryOid());
                uddiRegistryTextField.setText(uddiRegistry.getName());
            } catch (FindException e) {
                showErrorMessage("Cannot find UDDI Registry", "UDDI Registry cannot be found", e, true);
                return;
            }

            bsNameTextField.setText(uddiServiceControl.getUddiServiceName());
            bsKeyTextField.setText(uddiServiceControl.getUddiServiceKey());
            wsdlPortTextField.setText(uddiServiceControl.getWsdlPortName());
            wsdlBindingTextField.setText(uddiServiceControl.getWsdlPortBinding());
            wsdlBindingNamespaceTextField.setText((uddiServiceControl.getWsdlPortBindingNamespace() != null) ? uddiServiceControl.getWsdlPortBindingNamespace() : "");

            wsdlUnderUDDIControlCheckBox.setSelected(uddiServiceControl.isUnderUddiControl());
            monitoringEnabledCheckBox.setSelected( uddiServiceControl.isMonitoringEnabled() );
            monitoringUpdateWsdlCheckBox.setSelected( uddiServiceControl.isUpdateWsdlOnChange() );
            monitoringDisableServicecheckBox.setSelected( uddiServiceControl.isDisableServiceOnChange() );

            bsNameTextField.setCaretPosition( 0 );
            bsKeyTextField.setCaretPosition( 0 );
            wsdlPortTextField.setCaretPosition( 0 );
            wsdlBindingTextField.setCaretPosition( 0 );
            wsdlBindingNamespaceTextField.setCaretPosition( 0 );
        }else{
            //make sure all text fields are cleared
            uddiRegistryTextField.setText("");
            bsNameTextField.setText("");
            bsKeyTextField.setText("");
            wsdlPortTextField.setText("");
            wsdlBindingTextField.setText("");
            wsdlBindingNamespaceTextField.setText("");

            wsdlUnderUDDIControlCheckBox.setSelected( false );
            monitoringEnabledCheckBox.setSelected( false );
            monitoringUpdateWsdlCheckBox.setSelected( false );
            monitoringDisableServicecheckBox.setSelected( false );
        }
    }

    private void showErrorMessage(String title, String msg, Throwable e, boolean log) {
        showErrorMessage(title, msg, e, null, log);
    }

    private void showErrorMessage(String title, String msg, Throwable e, Runnable continuation, boolean log) {
        if(log) logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, continuation);
    }

    private static boolean areUnsavedChangesToThisPolicy(PublishedService subject) {
        PolicyEditorPanel pep = TopComponents.getInstance().getPolicyEditorPanel();
        return pep != null && pep.isEditingPublishedService() && subject.getOid() == pep.getPublishedServiceOid() && pep.isUnsavedChanges();
    }

    public static boolean hasResolutionConflict( final PublishedService subject,
                                                 final Collection<ServiceDocument> subjectDocuments ) {
        try {
            return !Registry.getDefault().getServiceManager().generateResolutionReport( subject, subjectDocuments ).isSuccess();
        } catch ( FindException e ) {
            logger.log( Level.WARNING, "Error checking for service resolution conflict" );
        }
        return false;
    }

    /**
     * Set the editor text to the given WSDL optionally making abstract
     *
     * @param editor The editor (not null)
     * @param wsdlDocSource The source in DOM form (if available)
     * @param wsdlSource The source in String form (required if DOM not provided)
     * @param isAbstract True to display an abstract WSDL (remove service information)
     */
    private void setWsdl( final XMLEditor editor,
                          final Document wsdlDocSource,
                          final String wsdlSource,
                          final boolean isAbstract ) {
        try {
            String wsdl;

            if ( isAbstract ) {
                Document wsdlDoc = wsdlDocSource==null ? XmlUtil.stringAsDocument( wsdlSource ) : wsdlDocSource;
                XmlUtil.removeChildElementsByName( wsdlDoc.getDocumentElement(), "http://schemas.xmlsoap.org/wsdl/", "service" );
                wsdl = XmlUtil.nodeToString( wsdlDoc );
            } else {
                wsdl = wsdlSource == null ? XmlUtil.nodeToString( wsdlDocSource ) : wsdlSource;
            }

            editor.setText( wsdl );
            editor.setCaretPosition(0);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Cannot display new WSDL", e);
            editor.setText( "" );
        }
    }

    private void applyPermissions() {
        enableIfHasUpdatePermission(nameField);
        enableIfHasUpdatePermission(okButton);
        enableIfHasUpdatePermission(resetWSDLButton);
        if (! subject.isInternal()) enableIfHasUpdatePermission(noURIRadio);
        enableIfHasUpdatePermission(customURIRadio);
        enableIfHasUpdatePermission(uriField);
        enableIfHasUpdatePermission(editWSDLButton);
        enableIfHasUpdatePermission(getCheck);
        enableIfHasUpdatePermission(putCheck);
        enableIfHasUpdatePermission(postCheck);
        enableIfHasUpdatePermission(deleteCheck);
        enableIfHasUpdatePermission(headCheck);
        enableIfHasUpdatePermission(disableRadio);
        enableIfHasUpdatePermission(enableRadio);
        enableIfHasUpdatePermission(laxResolutionCheckbox);
        enableIfHasUpdatePermission(enableWSSSecurityProcessingCheckBox);
        enableIfHasUpdatePermission(tracingCheckBox);
    }

    private void applyReadOnlySettings(boolean isReadOnly) {
        editWSDLButton.setEnabled(!isReadOnly);
        resetWSDLButton.setEnabled(!isReadOnly);
        resetWSDLButton.setEnabled(!isReadOnly);        
    }

    private void enableIfHasUpdatePermission(final Component component) {
        component.setEnabled(canUpdate && component.isEnabled());
    }

    private void help() {
        Actions.invokeHelp(this);
    }

    private void checkResolution() {
        final PublishedService service = new PublishedService(subject);
        String newURI = null;
        if (customURIRadio.isSelected()) {
            newURI = uriField.getText();
            // remove leading '/'s
            if (newURI != null) {
                newURI = newURI.trim();
                while (newURI.startsWith("//")) {
                    newURI = newURI.substring(1);
                }
                if (!newURI.startsWith("/")) newURI = "/" + newURI;
            }
        }
        if (newURI != null && (newURI.length() < 1 || newURI.equals("/"))) newURI = null;
        service.setRoutingUri(newURI);
        service.setLaxResolution(laxResolutionCheckbox.isSelected());

        if (newWSDLUrl != null) {
            try {
                service.setWsdlUrl(
                        newWSDLUrl.toLowerCase().startsWith("http:") ||
                        newWSDLUrl.toLowerCase().startsWith("https:") ||
                        newWSDLUrl.toLowerCase().startsWith("file:")
                                ? newWSDLUrl : null);
            } catch (MalformedURLException e) {
                logger.log( Level.WARNING, "Invalid WSDL URL:" + ExceptionUtils.getMessage( e ), ExceptionUtils.getDebugException( e ) );
            }
        }

        if (newWSDL != null) {
            try {
                subject.setWsdlXml( XmlUtil.nodeToString(newWSDL));
            } catch (IOException e) {
                logger.log( Level.WARNING, "Invalid WSDL:" + ExceptionUtils.getMessage( e ), ExceptionUtils.getDebugException( e ) );
            }
        }

        try {
            final ServiceAdmin.ResolutionReport report = Registry.getDefault().getServiceManager().generateResolutionReport( service, getServiceDocuments() );
            if ( report.isSuccess() ) {
                DialogDisplayer.showMessageDialog( this, "No Conflicts", "The service resolves successfully.", null );
                // this warning label doesn't show the "live" state but it seems useful
                // to clear it when the conflict is known to be resolved.
                resolutionConflictWarningLabel.setText( "" );
            } else {
                final String message = report.toString();
                final FontMetrics fontMetrics = getFontMetrics(getFont());
                final int width = Utilities.computeStringWidth(fontMetrics, message);
                final int height = Utilities.computeStringHeight(fontMetrics, message);
                final Object object;
                if( width > 600 || height > 100 ){
                    object = Utilities.getTextDisplayComponent( message, 600, 100, -1, -1 );
                } else {
                    object = message;
                }
                final JOptionPane pane = new JOptionPane(object, JOptionPane.WARNING_MESSAGE);
                final JDialog dialog = pane.createDialog(this,  "Service Resolution Conflicts");
                dialog.setMinimumSize( dialog.getContentPane().getMinimumSize() );
                dialog.setPreferredSize( new Dimension( 720, 200 ) );
                dialog.setResizable( true );
                dialog.pack();
                Utilities.centerOnParentWindow( dialog );
                DialogDisplayer.display( dialog );
            }
        } catch ( FindException e ) {
            showErrorMessage( "Error Checking for Resolution Conflicts", ExceptionUtils.getMessage( e ), e, true);
        }
    }

    private void cancel() {
        this.dispose();
    }

    public boolean wasOKed() {
        return wasoked;
    }

    public Collection<ServiceDocument> getServiceDocuments() {
        Collection<ServiceDocument> docs = null;
        if (newWsdlDocuments != null)
            docs = Collections.unmodifiableCollection(newWsdlDocuments);        
        return docs;
    }

    private void ok() {

        // validate the name
        String name = nameField.getText();
        if (name == null || name.length() < 1) {
            JOptionPane.showMessageDialog(this, "The service must be given a name");
            return;
        }
        // validate the uri
        String newURI = null;
        if (customURIRadio.isSelected()) {
            newURI = uriField.getText();
            // remove leading '/'s
            if (newURI != null) {
                newURI = newURI.trim();
                while (newURI.startsWith("//")) {
                    newURI = newURI.substring(1);
                }
                if (!newURI.startsWith("/")) newURI = "/" + newURI;
                uriField.setText(newURI);
            }
            try {
                new URL(ssgURL + newURI);
            } catch (MalformedURLException e) {
                JOptionPane.showMessageDialog(this, ssgURL + newURI + " is not a valid URL");
                return;
            }
        }

        if (!subject.isSoap() || subject.isInternal()) {
            if (newURI == null || newURI.length() <= 0 || newURI.equals("/")) { // non-soap service cannot have null routing uri
                String serviceType = subject.isInternal() ? "an internal" : "non-soap";
                JOptionPane.showMessageDialog(this, "Cannot set empty URI on " + serviceType + " service");
                return;
            } else if (newURI.startsWith(SecureSpanConstants.SSG_RESERVEDURI_PREFIX)) {
                JOptionPane.showMessageDialog(this, "custom resolution path cannot start with " + SecureSpanConstants.SSG_RESERVEDURI_PREFIX);
                return;
            } else if (uriConflictsWithServiceOIDResolver(newURI)) {
                JOptionPane.showMessageDialog(this, "This custom resolution path conflicts with an internal resolution mechanism.");
                return;
            }
        } else {
            if (newURI != null && newURI.length() > 0 && newURI.startsWith(SecureSpanConstants.SSG_RESERVEDURI_PREFIX)) {
                JOptionPane.showMessageDialog(this, "Custom resolution path cannot start with " + SecureSpanConstants.SSG_RESERVEDURI_PREFIX);
                return;
            }  else if (newURI != null && newURI.length() > 0 && uriConflictsWithServiceOIDResolver(newURI)) {
                JOptionPane.showMessageDialog(this, "This custom resolution path conflicts with an internal resolution mechanism.");
                return;
            }
        }

        if (!getCheck.isSelected() && !putCheck.isSelected() && !postCheck.isSelected() && !deleteCheck.isSelected() && !headCheck.isSelected()) {
            int res = JOptionPane.showConfirmDialog(this, "Because no HTTP methods are selected, this service will " +
                                                          "not be accessible through HTTP. Are you sure you want to " +
                                                          "do this?", "Warning", JOptionPane.YES_NO_OPTION);
            if (res != JOptionPane.YES_OPTION) {
                return;
            }
        }

        // set the new data into the edited subject
        subject.setName(name);
        subject.setDisabled(!enableRadio.isSelected());
        if (newURI != null && (newURI.length() < 1 || newURI.equals("/"))) newURI = null;
        subject.setRoutingUri(newURI);
        EnumSet<HttpMethod> methods = EnumSet.noneOf(HttpMethod.class);
        if (getCheck.isSelected()) {
            methods.add(HttpMethod.GET);
        }
        if (putCheck.isSelected()) {
            methods.add(HttpMethod.PUT);
        }
        if (postCheck.isSelected()) {
            methods.add(HttpMethod.POST);
        }
        if (deleteCheck.isSelected()) {
            methods.add(HttpMethod.DELETE);
        }
        if (headCheck.isSelected()) {
            methods.add(HttpMethod.HEAD);
        }
        subject.setHttpMethods(methods);
        subject.setLaxResolution(laxResolutionCheckbox.isSelected());
        subject.setWssProcessingEnabled(enableWSSSecurityProcessingCheckBox.isSelected());
        subject.setTracingEnabled(tracingCheckBox.isSelected());

        if (newWSDLUrl != null) {
            try {
                subject.setWsdlUrl(
                        newWSDLUrl.toLowerCase().startsWith("http:") ||
                        newWSDLUrl.toLowerCase().startsWith("https:") ||
                        newWSDLUrl.toLowerCase().startsWith("file:")
                                ? newWSDLUrl : null);
            } catch (MalformedURLException e) {
                throw new RuntimeException("Invalid URL", e);
            }
        }

        if (newWSDL != null) {
            try {
                subject.setWsdlXml( XmlUtil.nodeToString(newWSDL));
            } catch (IOException e) {
                throw new RuntimeException("Error Changing WSDL. Consult log for more information.", e);
            }
        }

        if ( wsdlUnderUDDIControlCheckBox.isSelected() && originalServiceEndPoint != null ) {
            subject.setDefaultRoutingUrl(originalServiceEndPoint);
        } else {
            subject.setDefaultRoutingUrl( null );
        }

        if (soapVersion11Button.isSelected()) {
            subject.setSoapVersion(SoapVersion.SOAP_1_1);
        } else if (soapVersion12Button.isSelected()) {
            subject.setSoapVersion(SoapVersion.SOAP_1_2);
        } else {
            subject.setSoapVersion(SoapVersion.UNKNOWN);
        }

        //attempt to save the changes
        try {
            Collection<ServiceDocument> documents = getServiceDocuments();
            if (documents == null)
                Registry.getDefault().getServiceManager().savePublishedService(subject);
            else
                Registry.getDefault().getServiceManager().savePublishedServiceWithDocuments(subject, documents);

            // Update tracing flag if indicated
            if (wasTracingEnabled != subject.isTracingEnabled())
                Registry.getDefault().getServiceManager().setTracingEnabled(subject.getOid(), subject.isTracingEnabled());

            //uddi settings
            if(uddiServiceControl != null){
                boolean enableMonitoring = false;
                boolean updateWsdlOnChange = false;
                boolean disableServiceOnChange = false;
                if ( wsdlUnderUDDIControlCheckBox.isSelected() &&
                     monitoringEnabledCheckBox.isSelected() ) {
                    enableMonitoring = true;
                    updateWsdlOnChange = monitoringUpdateWsdlCheckBox.isSelected();
                    disableServiceOnChange = monitoringDisableServicecheckBox.isSelected();
                }

                if( wsdlUnderUDDIControlCheckBox.isSelected() != uddiServiceControl.isUnderUddiControl() ||
                    enableMonitoring != uddiServiceControl.isMonitoringEnabled() ||
                    updateWsdlOnChange != uddiServiceControl.isUpdateWsdlOnChange() ||
                    disableServiceOnChange != uddiServiceControl.isDisableServiceOnChange() ||
                    uddiServiceControl.getOid() == UDDIServiceControl.DEFAULT_OID ){
                    
                    uddiServiceControl.setUnderUddiControl( wsdlUnderUDDIControlCheckBox.isSelected() );
                    uddiServiceControl.setMonitoringEnabled( enableMonitoring );
                    uddiServiceControl.setUpdateWsdlOnChange( updateWsdlOnChange );
                    uddiServiceControl.setDisableServiceOnChange( disableServiceOnChange );

                    //note accessPointURL and lastModifiedTimeStamp will be null when the UDDIServiceControl already existed
                    //if they are not null they are ignored by the admin method when it's not a save
                    Registry.getDefault().getUDDIRegistryAdmin().saveUDDIServiceControlOnly(uddiServiceControl, accessPointURL, lastModifiedTimeStamp);
                }
            }

            //we are good to close the dialog
            wasoked = true;
            cancel();

        } catch (DuplicateObjectException e) {
            JOptionPane.showMessageDialog(this,
                    "Unable to save the service '" + subject.getName() + "'\n" +
                            "because an existing non-SOAP or lax SOAP service is already using the URI " + subject.getRoutingUri(),
                    "Service already exists",
                    JOptionPane.ERROR_MESSAGE);
        } catch(StaleUpdateException e){
            JOptionPane.showMessageDialog(this,
                    "Unable to save the service '" + subject.getName() + "'\n" +
                            "Service has been updated since this dialog was opened. Please close and try again.",
                    "Service out of date",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            String msg = "Error while changing service properties";
            logger.log(Level.INFO, msg, e);
            String errorMessage = e.getMessage();
            if (errorMessage != null) msg += ":\n" + errorMessage;
            JOptionPane.showMessageDialog(this, msg);
        }
    }

    private boolean uriConflictsWithServiceOIDResolver(String newURI) {
        java.util.List<Pattern> compiled = new ArrayList<Pattern>();

        try {
            for (String s : SecureSpanConstants.RESOLUTION_BY_OID_REGEXES) {
                compiled.add(Pattern.compile(s));
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "A Regular Expression failed to compile. " +
                                      "This resolver is disabled.", e);
            compiled.clear();
        }

        for (Pattern regexPattern : compiled) {
            Matcher matcher = regexPattern.matcher(newURI);
            if (matcher.find() && matcher.groupCount() >= 1) {
                return true;
            }
        }
        return false;
    }

    private String updateURL() {
        String currentValue = null;
        if (customURIRadio.isSelected()) {
            currentValue = uriField.getText();
        }
        String urlvalue;
        if (currentValue != null) {
            currentValue = currentValue.trim();
            String cvWithoutSlashes = currentValue.replace("/", "");
            if (cvWithoutSlashes.length() <= 0) {
                currentValue = null;
            }
        }
        if (currentValue == null || currentValue.length() < 1) {
            urlvalue = ssgURL + "/ssg/soap";
        } else {
            if (currentValue.startsWith("/")) {
                urlvalue = ssgURL + currentValue;
            } else {
                urlvalue = ssgURL + "/" + currentValue;
            }
        }

        String tmp = urlvalue.replace(STD_PORT, STD_PORT_DISPLAYED);
        tmp = tmp.replace("http://", "http(s)://");
        routingURL.setText("<html><a href=\"" + urlvalue + "\">" + tmp + "</a></html>");
        routingURL.setCaretPosition(0);
        return currentValue;
    }

    private void editWsdl() {
        CreateServiceWsdlAction action = new CreateServiceWsdlAction();
        try {
            Collection<ServiceDocument> svcDocuments = newWsdlDocuments;
            if (svcDocuments == null) {
                ServiceAdmin svcAdmin = Registry.getDefault().getServiceManager();
                svcDocuments = svcAdmin.findServiceDocumentsByServiceID(String.valueOf(subject.getOid()));
            }

            Set<WsdlComposer.WsdlHolder> importedWsdls = new HashSet<WsdlComposer.WsdlHolder>();
            for (ServiceDocument svcDocument : svcDocuments) {
                if (svcDocument.getType().equals(WsdlCreateWizard.IMPORT_SERVICE_DOCUMENT_TYPE)) {
                    String contents = svcDocument.getContents();
                    Wsdl wsdl = Wsdl.newInstance(svcDocument.getUri(), new StringReader(contents));
                    WsdlComposer.WsdlHolder holder = new WsdlComposer.WsdlHolder(wsdl, svcDocument.getUri());
                    importedWsdls.add(holder);
                }
            }

            Functions.UnaryVoid<Document> editCallback = new Functions.UnaryVoid<Document>() {
                @Override
                public void call(Document wsdlDocument) {
                    // record info
                    newWSDL = wsdlDocument;

                    // display new xml in xml display
                    setWsdl( editor, newWSDL, null, subject.isInternal() );
                }
            };

            final WsdlDependenciesResolver wsdlDepsResolver = new WsdlDependenciesResolver(
                newWSDL == null? subject.getWsdlUrl() : newWSDL.getBaseURI(),           // WSDL URI
                newWSDL == null? subject.getWsdlXml() : XmlUtil.nodeToString(newWSDL),  // WSDL XML
                svcDocuments);                                                          // Service Documents
            action.setOriginalInformation(subject, editCallback, importedWsdls, wsdlDepsResolver);
            action.actionPerformed(null);
        } catch (Exception e) {
            logger.log(Level.WARNING, "cannot display new wsdl ", e);
        }

    }

    private void resetWSDL() {
        String existingURL = subject.getWsdlUrl();
        if (existingURL == null) existingURL = "";

        final SelectWsdlDialog rwd = new SelectWsdlDialog(this, "Reset WSDL");
        rwd.setWsdlUrl(existingURL);
        rwd.pack();
        Utilities.centerOnScreen(rwd);
        DialogDisplayer.display(rwd, new Runnable() {
            @Override
            public void run() {
                Wsdl wsdl = rwd.getWsdl();
                if (wsdl != null) {
                    newWSDL = rwd.getWsdlDocument();
                    newWSDLUrl = rwd.getWsdlUrl();

                    newWsdlDocuments = new ArrayList<ServiceDocument>();
                    for (int i=1; i<rwd.getWsdlCount(); i++) {
                        ServiceDocument sd = new ServiceDocument();
                        sd.setUri(rwd.getWsdlUri(i));
                        sd.setContentType("text/xml");
                        sd.setType("WSDL-IMPORT");
                        sd.setContents(rwd.getWsdlContent(i));
                        newWsdlDocuments.add(sd);
                    }

                    // display new xml in xml display
                    setWsdl( editor, newWSDL, null, subject.isInternal() );
                }
            }
        });
    }
}
