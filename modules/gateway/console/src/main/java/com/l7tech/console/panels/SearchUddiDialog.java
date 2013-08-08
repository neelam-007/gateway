package com.l7tech.console.panels;

import com.l7tech.gui.util.*;
import com.l7tech.objectmodel.Goid;
import com.l7tech.uddi.UDDINamedEntity;
import com.l7tech.uddi.WsdlPortInfo;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import com.l7tech.console.table.WsdlTable;
import com.l7tech.console.table.WsdlTableSorter;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SsmPreferences;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.AsyncAdminMethods;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.reflect.InvocationTargetException;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 *
 * Search dialog to find either WSDL's or BusinessEntities from UDDI.
 * <p> @author fpang </p>
 * @author darmstrong
 */
public class SearchUddiDialog extends JDialog {

    private JScrollPane searchResultsScrollPane;
    private JPanel mainPanel;
    private JButton searchButton;
    private JComboBox serviceNameFilterOptionComboBox;
    private JTextField serviceNameSearchPattern;
    private JButton selectButton;
    private JButton cancelButton;
    private WsdlTable wsdlTable = null;
    private JCheckBox caseSensitiveCheckBox;
    private JLabel retrievedRows;
    private JComboBox uddiRegistryComboBox;
    private JLabel nameOfSearchItem;
    private JLabel searchTitleLabel;
    private JCheckBox showSelectWsdlPortDialogCheckBox;
    private JCheckBox retrieveWSDLURLCheckBox;
    private JCheckBox allowWildcardsCheckBox;

    private final SsmPreferences preferences = TopComponents.getInstance().getPreferences();

    private static final Logger logger = Logger.getLogger(SearchUddiDialog.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.SearchUddiDialog", Locale.getDefault());
    private static final String EQUALS = "Equals";
    private static final String CONTAINS = "Contains";
    private static final String UDDI_REGISTRY = "UDDI.TYPE";
    private static final String WSDL_PORT_DIALOG = "UDDI.WSDL.PORT.DIALOG";
    private static final String RETRIEVE_WSDL = "UDDI.WSDL.RETRIEVE";
    private static final int MAX_SERVICE_NAME_LENGTH = 255;

    private static final String PROP_PREFIX = "com.l7tech.console";
    private static final long DELAY_INITIAL = ConfigFactory.getLongProperty( PROP_PREFIX + ".uddiSearch.serverSideDelay.initial", 100L );
    private static final long DELAY_CAP = ConfigFactory.getLongProperty( PROP_PREFIX + ".uddiSearch.serverSideDelay.maximum", 30000L );
    private static final double DELAY_MULTIPLIER = SyspropUtil.getDouble(PROP_PREFIX + ".uddiSearch.serverSideDelay.multiplier", 1.6);
    
    private Map<String, UDDIRegistry> allRegistries;
    private BusinessEntityTable businessEntityTable;

    public enum SEARCH_TYPE{WSDL_SEARCH, BUSINESS_ENTITY_SEARCH}

    private final SEARCH_TYPE searchType;

    /**
     * When this holds a non null value, the UDDI Registry drop down is constrained to only show a single value
     */
    private final String onlyAvailableRegistry;

    private final boolean requireWsdlPortDialog;

    public SearchUddiDialog(JDialog parent, SEARCH_TYPE searchType, String onlyAvailableRegistry) throws FindException {
        super(parent, resources.getString("window.title"), true);
        this.searchType = searchType;
        if(onlyAvailableRegistry != null && onlyAvailableRegistry.trim().isEmpty())
            throw new IllegalArgumentException("onlyAvailableRegistry cannot be the empty string");
        this.onlyAvailableRegistry = onlyAvailableRegistry;
        requireWsdlPortDialog = false;
        initialize();
    }

    public SearchUddiDialog(JDialog parent, SEARCH_TYPE searchType, boolean requireWsdlPortDialog) throws FindException {
        super(parent, resources.getString("window.title"), true);
        this.searchType = searchType;
        this.requireWsdlPortDialog = requireWsdlPortDialog;
        requireWsdlPortDialog = false;
        onlyAvailableRegistry = null;
        initialize();
    }

    public SearchUddiDialog(JDialog parent, SEARCH_TYPE searchType) throws FindException {
        super(parent, resources.getString("window.title"), true);
        this.searchType = searchType;
        this.onlyAvailableRegistry = null;
        requireWsdlPortDialog = false;
        initialize();
    }

    public SearchUddiDialog(JFrame parent, SEARCH_TYPE searchType) throws FindException {
        super(parent, resources.getString("window.title"), true);
        this.searchType = searchType;
        onlyAvailableRegistry = null;
        requireWsdlPortDialog = false;
        initialize();
    }

    public static interface TableInterface{
        void clearData();
    }

    public static boolean uddiEnabled() {
        try {
            UDDIRegistryAdmin uddiRegistrydmin = Registry.getDefault().getUDDIRegistryAdmin();
            if(!uddiRegistrydmin.findAllUDDIRegistries().isEmpty()) return true;
        }
        catch(Exception e) {
            logger.log(Level.WARNING, "Could not check if UDDI is enabled. '"+e.getMessage()+"'.");
        }

        return false;
    }

    private void initialize() throws FindException {
        if (getOwner() == null)
            Utilities.setAlwaysOnTop(this, true);

        Container p = getContentPane();
        Utilities.setEscKeyStrokeDisposes(this);
        
        p.setLayout(new BorderLayout());
        p.add(mainPanel, BorderLayout.CENTER);

        // The default-service-name-filter option is "Contains", so the valid max length of the search input
        // is MAX_SERVICE_NAME_LENGTH - 2, since there are two '%'s at the begin and the end respectively.
        ((AbstractDocument)serviceNameSearchPattern.getDocument()).setDocumentFilter(new DocumentSizeFilter(MAX_SERVICE_NAME_LENGTH - 2));
        serviceNameFilterOptionComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { CONTAINS, EQUALS }));
        serviceNameFilterOptionComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JComboBox cb = (JComboBox)actionEvent.getSource();
                String filterName = (String)cb.getSelectedItem();
                int validMaxLen = MAX_SERVICE_NAME_LENGTH;
                if (CONTAINS.equals(filterName)) {
                    // since there are two '%' in the search input: the first % locates at the begin and the second % locates the end.
                    validMaxLen = MAX_SERVICE_NAME_LENGTH - 2;
                    // Check if keeping the search string when swapping the two filter options
                    if (serviceNameSearchPattern.getText().length() > validMaxLen) {
                        serviceNameSearchPattern.setText("");
                    }
                }
                ((AbstractDocument)serviceNameSearchPattern.getDocument()).setDocumentFilter(new DocumentSizeFilter(validMaxLen));
            }
        });

        loadUddiRegistries();

        switch(searchType){
            case WSDL_SEARCH:
                if(requireWsdlPortDialog){
                    showSelectWsdlPortDialogCheckBox.setSelected(true);
                    showSelectWsdlPortDialogCheckBox.setEnabled(false);
                }else{
                    final String wsdlDialogChoice = preferences.getString(WSDL_PORT_DIALOG, "");
                    if(wsdlDialogChoice.equals("")){
                        showSelectWsdlPortDialogCheckBox.setSelected(false);
                    }else{
                        showSelectWsdlPortDialogCheckBox.setSelected(Boolean.valueOf(wsdlDialogChoice));
                    }
                }
                final String retrieveWsdlPreference = preferences.getString(RETRIEVE_WSDL, "");
                if(retrieveWsdlPreference.equals("")){
                    retrieveWSDLURLCheckBox.setSelected(true);
                }else{
                    retrieveWSDLURLCheckBox.setSelected(Boolean.valueOf(retrieveWsdlPreference));
                }

                nameOfSearchItem.setText("Service Name:");
                wsdlTable = new WsdlTable(retrieveWSDLURLCheckBox.isSelected());
                searchResultsScrollPane.setViewportView(wsdlTable);
                selectButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {

                        int row = wsdlTable.getSelectedRow();
                        if(row < 0) {
                            dispose();
                            return;
                        }

                        final WsdlPortInfo selectedWsdlInfo = (WsdlPortInfo) wsdlTable.getTableSorter().getData(row);

                        if(showSelectWsdlPortDialogCheckBox.isSelected()){
                            //Show new dialog and get user selection
                            final String regName = (String) uddiRegistryComboBox.getSelectedItem();
                            final UDDIRegistry uddiRegistry = allRegistries.get(regName);
                            final SelectWsdlPortDialog portDialog;

                            final JProgressBar bar = new JProgressBar();
                            bar.setIndeterminate(true);
                            final CancelableOperationDialog cancelDlg =
                                    new CancelableOperationDialog(SearchUddiDialog.this, "Searching UDDI", "Please wait, retrieving BusinessService details from UDDI...", bar);
                            try {
                                final Callable<WsdlPortInfo[]> wsdlInfoCallable = getWsdlPortSearchCallable(uddiRegistry.getGoid(), selectedWsdlInfo.getBusinessServiceKey(), false);
                                final WsdlPortInfo [] wsdlPortsFound = Utilities.doWithDelayedCancelDialog(wsdlInfoCallable, cancelDlg, DELAY_INITIAL);
                                portDialog = new SelectWsdlPortDialog(SearchUddiDialog.this, wsdlPortsFound);

                                final WsdlPortInfo [] wsdlPortInfo = new WsdlPortInfo[1];
                                portDialog.addSelectionListener(new SelectWsdlPortDialog.ItemSelectedListener() {
                                    @Override
                                    public void itemSelected(Object item) {
                                        if(item == null) return;
                                        if(!(item instanceof WsdlPortInfo)) return;
                                        wsdlPortInfo[0] = (WsdlPortInfo) item;
                                    }
                                });
                                portDialog.setSize(700, 500);
                                portDialog.setModal(true);
                                DialogDisplayer.display(portDialog, new Runnable() {
                                    @Override
                                    public void run() {
                                        if(wsdlPortInfo[0] == null) return;
                                        final boolean canDispose = validateWsdlSelection(SearchUddiDialog.this, wsdlPortInfo[0]);
                                        if(canDispose){
                                            wsdlPortInfo[0].setWasWsdlPortSelected(true);
                                            fireItemSelectedEvent(wsdlPortInfo[0]);
                                            dispose();
                                        }
                                    }
                                });
                                
                            } catch (FindException e1) {
                                showErrorMessage("Cannot find wsdl:port information", "Cannot display all applicable wsdl:port for the BusinessService: " + ExceptionUtils.getMessage(e1), e1);
                            } catch (InvocationTargetException e1) {
                                logger.log(Level.WARNING, ExceptionUtils.getMessage(e1), ExceptionUtils.getDebugException(e1));
                                JOptionPane.showMessageDialog(SearchUddiDialog.this, ExceptionUtils.getMessage(e1), "Error Searching UDDI Registry", JOptionPane.ERROR_MESSAGE);
                            } catch (InterruptedException e1) {
                                //Should be handled inside of either callable
                                logger.log(Level.FINE, "Search of UDDI was cancelled");
                            }
                        }else{
                            if(!retrieveWSDLURLCheckBox.isSelected()){
                                //we need to get the WSDL from UDDI
                                final String regName = (String) uddiRegistryComboBox.getSelectedItem();
                                final UDDIRegistry uddiRegistry = allRegistries.get(regName);

                                try {
                                    final JProgressBar bar = new JProgressBar();
                                    bar.setIndeterminate(true);
                                    final CancelableOperationDialog cancelDlg =
                                            new CancelableOperationDialog(SearchUddiDialog.this, "Searching UDDI", "Please wait, Searching UDDI for WSDL URL...", bar);

                                    final Callable<WsdlPortInfo[]> wsdlInfoCallable = getWsdlPortSearchCallable(uddiRegistry.getGoid(), selectedWsdlInfo.getBusinessServiceKey(), true);
                                    final WsdlPortInfo[] allApplicableWsdlInfos = Utilities.doWithDelayedCancelDialog(wsdlInfoCallable, cancelDlg, DELAY_INITIAL);
                                    if (allApplicableWsdlInfos == null || allApplicableWsdlInfos.length == 0) {
                                        throw new FindException("No WSDL URL could be found for BusinessService");
                                    }
                                    final boolean canDispose = validateWsdlSelection(SearchUddiDialog.this, allApplicableWsdlInfos[0]);
                                    if (canDispose) {
                                        allApplicableWsdlInfos[0].setWasWsdlPortSelected(false);
                                        fireItemSelectedEvent(allApplicableWsdlInfos[0]);
                                        dispose();
                                    }

                                } catch (FindException e1) {
                                    showErrorMessage("Error getting WSDL", "Cannot get WSDL from UDDI for BusinessService: " + ExceptionUtils.getMessage(e1), e1);
                                } catch (InvocationTargetException e1) {
                                    logger.log(Level.WARNING, ExceptionUtils.getMessage(e1), ExceptionUtils.getDebugException(e1));
                                    JOptionPane.showMessageDialog(SearchUddiDialog.this, ExceptionUtils.getMessage(e1), "Error Searching UDDI Registry for WSDL URL", JOptionPane.ERROR_MESSAGE);
                                } catch (InterruptedException e1) {
                                    //Should be handled inside of either callable
                                    logger.log(Level.FINE, "Search of UDDI for WSDL URL was cancelled");
                                }
                            }else{
                                //this will show any required warnings to the user
                                final boolean canDispose = validateWsdlSelection(SearchUddiDialog.this, selectedWsdlInfo);
                                if(canDispose){
                                    fireItemSelectedEvent(selectedWsdlInfo);
                                    dispose();
                                }
                            }
                        }
                    }
                });
                Utilities.setDoubleClickAction(wsdlTable, selectButton);
                searchTitleLabel.setText("Search UDDI Registry for WSDLs");
                break;
            case BUSINESS_ENTITY_SEARCH:
                showSelectWsdlPortDialogCheckBox.setEnabled(false);
                showSelectWsdlPortDialogCheckBox.setVisible(false);
                retrieveWSDLURLCheckBox.setEnabled(false);
                retrieveWSDLURLCheckBox.setVisible(false);
                
                nameOfSearchItem.setText("Business Name:");
                businessEntityTable = new BusinessEntityTable();
                searchResultsScrollPane.setViewportView(businessEntityTable);
                selectButton.addActionListener(new ActionListener(){
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        UDDINamedEntity businessEntity = businessEntityTable.getSelectedBusinessEntity();
                        if(businessEntity != null){
                            fireItemSelectedEvent(businessEntity);                                    
                        }
                        dispose();
                    }
                });
                Utilities.setDoubleClickAction(businessEntityTable, selectButton);
                searchTitleLabel.setText("Search UDDI Registry for Business Entities");
                break;
        }

        //BusinessEntityTable
        searchResultsScrollPane.getViewport().setBackground(Color.white);

        cancelButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                SearchUddiDialog.this.dispose();
            }
        });

        searchButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                InputValidator.ValidationRule comboValidator = new InputValidator.ValidationRule() {
                    @Override
                    public String getValidationError() {
                        if(uddiRegistryComboBox.getSelectedIndex() == -1) return "Please select a UDDI Registry";
                        UDDIRegistry uddiRegistry = allRegistries.get(uddiRegistryComboBox.getSelectedItem().toString());
                        if(!uddiRegistry.isEnabled()) return "UDDI Registry is not currently enabled.";
                        return null;
                    }
                };

                InputValidator ruleValidator = new InputValidator(SearchUddiDialog.this, "Search Business Entity");
                ruleValidator.addRule(comboValidator);
                if(!ruleValidator.validateWithDialog()) return;
                
                final String searchString;
                if(serviceNameSearchPattern.getText().length() > 0) {
                    String escapedStr = (!allowWildcardsCheckBox.isSelected())?
                            escapeString(serviceNameSearchPattern.getText()):
                            serviceNameSearchPattern.getText();
                    
                    if((serviceNameFilterOptionComboBox.getSelectedItem()).equals(CONTAINS)) {
                        searchString = "%" + escapedStr + "%";
                    } else {
                        searchString = escapedStr;
                    }
                } else {
                    searchString = "%";
                }

                retrievedRows.setText("Result: 0");
                final String errorMessage;
                switch(searchType){
                    case WSDL_SEARCH:
                        // clear the list
                        ((WsdlTableSorter) wsdlTable.getModel()).clearData();
                        errorMessage = "Error searching UDDI for WSDLs";
                        break;
                    case BUSINESS_ENTITY_SEARCH:
                        errorMessage = "Error seraching UDDI for BusinessEntities";
                        break;
                    default:errorMessage = "Undefined";//can't happen
                }

                final String registryName = (String) uddiRegistryComboBox.getSelectedItem();

                final JProgressBar bar = new JProgressBar();
                bar.setIndeterminate(true);
                final CancelableOperationDialog cancelDlg =
                        new CancelableOperationDialog(SearchUddiDialog.this, "Searching UDDI", "Please wait, Searching UDDI...", bar);

                retrievedRows.setText("Result: 0");
                try {
                    switch(searchType){
                        case WSDL_SEARCH:
                            final Callable<WsdlPortInfo[]> serviceCallable = getServiceSearchCallable(searchString);
                            final WsdlPortInfo [] serviceResult = Utilities.doWithDelayedCancelDialog(serviceCallable, cancelDlg, DELAY_INITIAL);
                            processServiceSearchResults(registryName, serviceResult);
                            break;
                        case BUSINESS_ENTITY_SEARCH:
                            final Callable<UDDINamedEntity[]> businessCallable = getBusinessSearchCallable(searchString);                            
                            final UDDINamedEntity[] businessResult = Utilities.doWithDelayedCancelDialog(businessCallable, cancelDlg, DELAY_INITIAL);
                            processBusinessSearchResults(registryName, businessResult);
                    }
                } catch (InterruptedException e2) {
                    //Should be handled inside of either callable
                    logger.log(Level.FINE, "Search of UDDI was cancelled");
                } catch (InvocationTargetException e2) {
                    String message = ExceptionUtils.getMessage(e2);
                    logger.log(Level.WARNING, message, ExceptionUtils.getDebugException(e2));
                    int messageLengthLimit = 60;
                    Object messageObject;
                    if (message.length() < messageLengthLimit) {
                        messageObject = message;
                    } else {
                        JTextArea text = new JTextArea(message);
                        text.setColumns(Math.min(message.length(), messageLengthLimit));
                        text.setRows(Math.min(4, (message.length() + 1) / messageLengthLimit) + 1);
                        text.setMargin(new Insets(2,5,2,5));
                        text.setLineWrap(true);
                        text.setEditable(false);
                        messageObject = new JScrollPane(text);
                    }

                    JOptionPane pane = new JOptionPane(messageObject, JOptionPane.ERROR_MESSAGE);
                    JDialog dialog = pane.createDialog(SearchUddiDialog.this, "Error Searching UDDI Registry");
                    dialog.setResizable(true);
                    dialog.setVisible(true);
                }
            }
        });

        allowWildcardsCheckBox.setSelected(true);
        pack();
        Utilities.centerOnScreen(this);
    }

    private void processBusinessSearchResults(String registryName, UDDINamedEntity[] businessResult) {
        // store prefs on successful search
        preferences.putProperty(UDDI_REGISTRY, registryName);

        boolean searchTruncated = false;
        List<BusinessEntityTableRow> rows = new ArrayList<BusinessEntityTableRow>();
        for (int i = 0; i < (businessResult).length; i++) {
            final UDDINamedEntity entity = (businessResult)[i];
            if (WsdlPortInfo.MAXED_OUT_UDDI_RESULTS_URL.equals(entity.getName())) {
                // Flag value indicating that search results were truncated
                searchTruncated = true;
            } else {
                // Normal result
                rows.add(new BusinessEntityTableRow(entity));
            }
        }

        // populate the data to the table
        businessEntityTable.setData(rows);
        Utilities.setRowSorter(businessEntityTable, businessEntityTable.getModel());
        String warning = "";
        if (searchTruncated) {
            warning = "  <b>(QUERY TOO BROAD - Only the first " + rows.size() + " results are presented)</b>";
            retrievedRows.setForeground(new Color(255, 64, 64));
        } else {
            retrievedRows.setForeground(new JLabel().getForeground());
        }
        retrievedRows.setText("<HTML>Result: " + rows.size() + warning);
    }

    private void processServiceSearchResults(String registryName, WsdlPortInfo[] serviceResult) {
        // store prefs on successful search
        preferences.putProperty(UDDI_REGISTRY, registryName);
        preferences.putProperty(WSDL_PORT_DIALOG, (showSelectWsdlPortDialogCheckBox.isSelected()) ? "true" : "false");
        preferences.putProperty(RETRIEVE_WSDL, (retrieveWSDLURLCheckBox.isSelected()) ? "true" : "false");

        boolean searchTruncated = false;
        Vector<WsdlPortInfo> urlList = new Vector<WsdlPortInfo>();
        for (int i = 0; i < serviceResult.length; i++) {
            final WsdlPortInfo wi = (serviceResult)[i];
            if (WsdlPortInfo.MAXED_OUT_UDDI_RESULTS_URL.equals(wi.getWsdlUrl())) {//note wi.getWsdlUrl can be null
                // Flag value indicating that search results were truncated
                searchTruncated = true;
            } else {
                // Normal result
                urlList.add(wi);
            }
        }

        // populate the data to the table
        ((WsdlTableSorter) wsdlTable.getModel()).setData(urlList);
        String warning = "";
        if (searchTruncated) {
            warning = "  <b>(QUERY TOO BROAD - Only the first " + urlList.size() + " results are presented)</b>";
            retrievedRows.setForeground(new Color(255, 64, 64));
        } else {
            retrievedRows.setForeground(new JLabel().getForeground());
        }
        retrievedRows.setText("<HTML>Result: " + urlList.size() + warning);
    }

    /**
     * This callable does not return null
     * @param searchString String UDDI search string
     * @return Callable UDDINamedEntity[] which can be run async to search UDDI
     * @return
     */
    private Callable<UDDINamedEntity[]> getBusinessSearchCallable(final String searchString) {
        return new Callable<UDDINamedEntity[]>() {
            @Override
            public UDDINamedEntity[] call() throws Exception {
                final ServiceAdmin serviceAdmin = Registry.getDefault().getServiceManager();
                if (serviceAdmin == null) throw new RuntimeException("Service Admin reference not found");
                final String regName = (String) uddiRegistryComboBox.getSelectedItem();
                final UDDIRegistry uddiRegistry = allRegistries.get(regName);
                final AsyncAdminMethods.JobId<UDDINamedEntity[]> jobId =
                        serviceAdmin.findBusinessesFromUDDIRegistry(uddiRegistry.getGoid(), searchString, caseSensitiveCheckBox.isSelected());

                try {
                    UDDINamedEntity [] result = null;
                    double delay = DELAY_INITIAL;
                    Thread.sleep((long)delay);
                    while (result == null) {
                        String status = serviceAdmin.getJobStatus(jobId);
                        if (status == null)
                            throw new IllegalStateException("Server could not find our uddi serach job ID");
                        if (status.startsWith("i")) {
                            final AsyncAdminMethods.JobResult<UDDINamedEntity[]> jobResult = serviceAdmin.getJobResult(jobId);
                            result = jobResult.result;
                            if (result == null){
                                final String errorMessage = jobResult.throwableMessage;
                                if(errorMessage != null){
                                    throw new RuntimeException(errorMessage);
                                }else{
                                    throw new RuntimeException("Unknown problem searching UDDI. Please check Gateway logs");
                                }
                            }
                        }
                        delay = delay >= DELAY_CAP ? DELAY_CAP : delay * DELAY_MULTIPLIER;
                        Thread.sleep((long)delay);
                    }
                    return result;
                } catch (InterruptedException e) {
                    logger.log(Level.FINE, "UDDI search is canccelled. Attemping to stop search on Gateway");
                    if(jobId != null) Registry.getDefault().getServiceManager().cancelJob(jobId, true);
                    throw e;
                } 
            }
        };
    }

    /**
     * This callable does not return null.
     * @param searchString String UDDI search string
     * @return Callable WsdlPortInfo[] which can be run async to search UDDI
     */
    private Callable<WsdlPortInfo[]> getServiceSearchCallable(final String searchString) {
        return new Callable<WsdlPortInfo[]>() {
            @Override
            public WsdlPortInfo[] call() throws Exception {
                final ServiceAdmin serviceAdmin = Registry.getDefault().getServiceManager();
                if (serviceAdmin == null) throw new RuntimeException("Service Admin reference not found");
                final String regName = (String) uddiRegistryComboBox.getSelectedItem();
                final UDDIRegistry uddiRegistry = allRegistries.get(regName);
                final AsyncAdminMethods.JobId<WsdlPortInfo[]> jobId =
                        serviceAdmin.findWsdlInfosFromUDDIRegistry(
                                uddiRegistry.getGoid(), searchString, caseSensitiveCheckBox.isSelected(), retrieveWSDLURLCheckBox.isSelected());

                return getAsyncResult( serviceAdmin, jobId );
            }
        };
    }

    private WsdlPortInfo[] getAsyncResult( final ServiceAdmin serviceAdmin,
                                           final AsyncAdminMethods.JobId<WsdlPortInfo[]> jobId ) throws AsyncAdminMethods.UnknownJobException, AsyncAdminMethods.JobStillActiveException, InterruptedException {
        try {
            WsdlPortInfo [] result = null;
            double delay = DELAY_INITIAL;
            Thread.sleep((long)delay);
            while (result == null) {
                String status = serviceAdmin.getJobStatus(jobId);
                if (status == null)
                    throw new IllegalStateException("Server could not find our uddi serach job ID");
                if (status.startsWith("i")) {
                    final AsyncAdminMethods.JobResult<WsdlPortInfo[]> jobResult = serviceAdmin.getJobResult(jobId);
                    result = jobResult.result;
                    if (result == null){
                        final String errorMessage = jobResult.throwableMessage;
                        if(errorMessage != null){
                            throw new RuntimeException(errorMessage);
                        }else{
                            throw new RuntimeException("Unknown problem searching UDDI. Please check Gateway logs");
                        }
                    }
                }
                delay = delay >= DELAY_CAP ? DELAY_CAP : delay * DELAY_MULTIPLIER;
                Thread.sleep((long)delay);
            }
            return result;
        } catch (InterruptedException e) {
            logger.log( Level.FINE, "UDDI search is cancelled. Attemping to stop search on Gateway");
            if(jobId != null) Registry.getDefault().getServiceManager().cancelJob(jobId, true);
            throw e;
        }
    }

    /**
     * This callable does not return null.
     * @param registryGoid Goid goid of the uddi registry to search. Do not get this from the UI as it may have changed
     * @param serviceKey String serviceKey of the BusinessService to search for applicable wsdl:ports
     * @param getFirstOnly
     * @return Callable WsdlPortInfo[] which can be run async to search UDDI
     */
    private Callable<WsdlPortInfo[]> getWsdlPortSearchCallable(final Goid registryGoid,
                                                               final String serviceKey,
                                                               final boolean getFirstOnly) {
        return new Callable<WsdlPortInfo[]>() {
            @Override
            public WsdlPortInfo[] call() throws Exception {
                final ServiceAdmin serviceAdmin = Registry.getDefault().getServiceManager();
                if (serviceAdmin == null) throw new RuntimeException("Service Admin reference not found");

                final AsyncAdminMethods.JobId<WsdlPortInfo[]> jobId =
                        serviceAdmin.findWsdlInfosForSingleBusinessService(registryGoid, serviceKey, getFirstOnly);

                return getAsyncResult( serviceAdmin, jobId );
            }
        };
    }

    /**
     *
     * @param parent Component parent. Cannot be null
     * @param wsdlPortInfo WsdlPortInfo user select. Cannot be null. wsdlUrl property must not return null
     * @return true if the WSDL does not route to the Gateway or it may, but the user decided to choose it anyway, false
     * otherwise, in which case the WsdlPortInfo should not be used
     */
    static boolean validateWsdlSelection(final Component parent, final WsdlPortInfo wsdlPortInfo){
        final boolean [] canFireEventAndDispose = new boolean[]{true};
        //Check if the selected WSDL may be from this Gateway
        if(wsdlPortInfo.isGatewayWsdl()){
            DialogDisplayer.showMessageDialog(parent,  "This WSDL is from the Gateway. Cannot route to self",
                    "Cannot select WSDL", JOptionPane.WARNING_MESSAGE, null);
            return false;
        }

        if(wsdlPortInfo.isLikelyGatewayWsdl()){
            DialogDisplayer.showConfirmDialog(parent,
                    "It is likely this WSDL is from the Gateway. Please confirm selection",
                    "Confirm WSDL selection", JOptionPane.YES_NO_OPTION,
                    new DialogDisplayer.OptionListener() {
                @Override
                public void reportResult(int option) {
                    if(option != JOptionPane.YES_OPTION) canFireEventAndDispose[0] = false;
                }
            });
        }

        return canFireEventAndDispose[0];
    }

    private void loadUddiRegistries() {
        try {
            UDDIRegistryAdmin uddiRegistryAdmin = getUDDIRegistryAdmin();

            allRegistries = new HashMap<String, UDDIRegistry>();
            Collection<UDDIRegistry> registries = uddiRegistryAdmin.findAllUDDIRegistries();
            for (UDDIRegistry uddiRegistry : registries){
                if(onlyAvailableRegistry != null){
                    if(uddiRegistry.getName().equals(onlyAvailableRegistry)){
                        allRegistries.put(uddiRegistry.getName(), uddiRegistry);
                        break;
                    }
                }else if (uddiRegistry.isEnabled()) {
                    allRegistries.put(uddiRegistry.getName(), uddiRegistry);
                }
            }

            if(onlyAvailableRegistry != null){
                if(allRegistries.keySet().size() != 1){
                    logger.log(Level.WARNING, "Could not find UDDI Registry '"+onlyAvailableRegistry+"'. May have been deleted");
                    showErrorMessage("Cannot display Dialog", "Required UDDI Registry '"+onlyAvailableRegistry+"' was not found. It may have been deleted", null);
                    return;
                }

                uddiRegistryComboBox.addItem(onlyAvailableRegistry);
                uddiRegistryComboBox.setSelectedItem(onlyAvailableRegistry);
            }else{
                for(String regName: allRegistries.keySet()){
                    uddiRegistryComboBox.addItem(regName);
                }

                // load prefs
                String registryName = preferences.getString(UDDI_REGISTRY, "");
                if(!registryName.equals("")){
                    uddiRegistryComboBox.setSelectedItem(registryName);
                }else{
                    uddiRegistryComboBox.setSelectedIndex(-1);
                }
            }
        } catch (FindException e) {
            showErrorMessage("Loading failed", "Unable to list all UDDI Registry: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void showErrorMessage(String title, String msg, Throwable e) {
        showErrorMessage(title, msg, e, null);
    }

    private void showErrorMessage(String title, String msg, Throwable e, Runnable continuation) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, continuation);
    }
    
    /** @return the UDDIRegistryAdmin interface, or null if not connected or it's unavailable for some other reason */
    private UDDIRegistryAdmin getUDDIRegistryAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent())
            return null;
        return reg.getUDDIRegistryAdmin();
    }

    /**
     * notify the listeners
     *
     * @param  item Object to send to listeners
     */
    private void fireItemSelectedEvent(final Object item) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for(ItemSelectedListener itemSelectedListener: listeners) {
                    itemSelectedListener.itemSelected(item);
                }
            }
        });
    }

    private final List<ItemSelectedListener> listeners = new ArrayList<ItemSelectedListener>();

    public void addSelectionListener(ItemSelectedListener selectedListener){
        listeners.add(selectedListener);        
    }

    public static interface ItemSelectedListener{
        void itemSelected(Object item);
    }

    /**
     * @param string    The string that will be escaped.
     * @return  The new escaped string which will only escape the following characters:
     *
     *<ul>
     * <li>% --> \%</li>
     * <li>_ --> \_</li>
     * <li>\ --> \\</li> 
     * </ul>
     */
    public String escapeString(String string) {
        StringBuffer result = new StringBuffer("");
        for (int i=0; i < string.length(); i++) {
            char character = string.charAt(i);
            if (character == '%') {
                result.append("\\%");
            } else if (character == '_') {
                result.append("\\_");
            } else if (character == '\\') {
                result.append("\\\\");
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }

    private static class BusinessEntityTable extends JTable {
        private final BusinessEntityTableModel model = new BusinessEntityTableModel();

        BusinessEntityTable() {
            setModel(model);
            getTableHeader().setReorderingAllowed(false);
            getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            TableColumnModel cols = getColumnModel();
            int numCols = model.getColumnCount();
            for (int i = 0; i < numCols; ++i) {
                final TableColumn col = cols.getColumn(i);
                col.setMinWidth(model.getColumnMinWidth(i));
                col.setPreferredWidth(model.getColumnPrefWidth(i));
                col.setMaxWidth(model.getColumnMaxWidth(i));
                TableCellRenderer cr = model.getCellRenderer(i, getDefaultRenderer(String.class));
                if (cr != null) col.setCellRenderer(cr);
            }
        }

        public BusinessEntityTableRow getRowAt(int row) {
            return model.getRowAt(row);
        }

        public void setData(java.util.List<BusinessEntityTableRow> rows) {
            model.setData(rows);
        }

        /** @return the current selected SsgConnector, or null */
        public UDDINamedEntity getSelectedBusinessEntity() {
            int selectedRow = getSelectedRow();
            if (selectedRow < 0)
                return null;

            int rowNum = this.getRowSorter().convertRowIndexToModel(selectedRow);
            BusinessEntityTableRow row = getRowAt(rowNum);
            if (row == null)
                return null;
            return row.getNamedEntity();
        }

    }

    private static class BusinessEntityTableModel extends AbstractTableModel {
        private abstract class Col {
            final String name;
            final int minWidth;
            final int prefWidth;
            final int maxWidth;

            protected Col(String name, int minWidth, int prefWidth, int maxWidth) {
                this.name = name;
                this.minWidth = minWidth;
                this.prefWidth = prefWidth;
                this.maxWidth = maxWidth;
            }

            abstract Object getValueForRow(BusinessEntityTableRow row);

            public TableCellRenderer getCellRenderer(final TableCellRenderer current) {
                return new TableCellRenderer() {
                    private Color defFg;
                    private Color defSelFg;

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        if (defFg == null) {
                            TableCellRenderer def1 = new DefaultTableCellRenderer();
                            Component c = def1.getTableCellRendererComponent(table, value, false, false, row, column);
                            defFg = c.getForeground();
                            TableCellRenderer def2 = new DefaultTableCellRenderer();
                            Component csel = def2.getTableCellRendererComponent(table, value, true, false, row, column);
                            defSelFg = csel.getForeground();
                        }

                        Component ret = current.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                        if (isSelected) {
                            ret.setForeground(defSelFg);
                        } else {
                            ret.setForeground(defFg);
                        }

                        return ret;
                    }
                };
            }
        }

        public final Col[] columns = new Col[] {
                new Col("BusinessEntity Name", 60, 90, 999999) {
                    @Override
                    Object getValueForRow(BusinessEntityTableRow row) {
                        return row.getName();
                    }
                },

                new Col("businessKey", 60, 90, 999999) {
                    @Override
                    Object getValueForRow(BusinessEntityTableRow row) {
                        return row.getBusinessKey();
                    }
                },

        };

        private final ArrayList<BusinessEntityTableRow> rows = new ArrayList<BusinessEntityTableRow>();

        private BusinessEntityTableModel() {
        }

        public int getColumnMinWidth(int column) {
            return columns[column].minWidth;
        }

        public int getColumnPrefWidth(int column) {
            return columns[column].prefWidth;
        }

        public int getColumnMaxWidth(int column) {
            return columns[column].maxWidth;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column].name;
        }

        public TableCellRenderer getCellRenderer(int column, final TableCellRenderer current) {
            return columns[column].getCellRenderer(current);
        }

        public void setData(List<BusinessEntityTableRow> rows) {
            this.rows.clear();
            this.rows.addAll(rows);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return columns[columnIndex].getValueForRow(rows.get(rowIndex));
        }

        public BusinessEntityTableRow getRowAt(int rowIndex) {
            return rows.get(rowIndex);
        }
   }

    private static class BusinessEntityTableRow {
        private final UDDINamedEntity namedEntity;

        public BusinessEntityTableRow(UDDINamedEntity namedEntity) {
            this.namedEntity = namedEntity;
        }

        public UDDINamedEntity getNamedEntity() {
            return namedEntity;
        }

        public Object getName() {
            return namedEntity.getName();
        }

        public Object getBusinessKey(){
            return namedEntity.getKey();
        }
    }

}
