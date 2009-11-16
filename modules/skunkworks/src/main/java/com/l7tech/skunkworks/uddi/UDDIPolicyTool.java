package com.l7tech.skunkworks.uddi;

import com.l7tech.common.uddi.guddiv3.*;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.util.Functions;
import com.l7tech.util.TimeUnit;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.xml.ws.Holder;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeFactory;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * UDDI Tool for WS-Policies in UDDI. 
 *
 * <p>Use to find and delete polcies and policy references in a UDDI.</p>
 *
 * <p>Note that this will ONLY work correctly for policies and references
 * created by the SecureSpan Manager.</p>
 *
 * <p>WARNING: If used incorrectly it is possible to cause registry corruption.
 * If the UDDI registry has tools to manage policies then that should be used
 * for deletion (however this tool is probably slightly better than Systinets
 * admin tool since it will delete references to a policy before deleting the
 * policy).</p>
 *
 * @author Steve Jones
 */
public class UDDIPolicyTool extends UDDISupport {
    private static final Logger logger = Logger.getLogger(UDDIPolicyTool.class.getName());

    private static final String POLICY_TYPE_KEY_VALUE = "policy";
    private static final String TMODEL_KEY_WSDL_TYPES = "uddi:uddi.org:wsdl:types";
    private static final String WSDL_TYPES_SERVICE = "service";
    private static final String FINDQUALIFIER_APPROXIMATE = "approximateMatch";
    private static final String FINDQUALIFIER_CASEINSENSITIVE = "caseInsensitiveMatch";

    // WS-Policy Attachment 1.2+
    private static final String tModelKeyPolicyType = "uddi:schemas.xmlsoap.org:policytypes:2003_03";
    private static final String tModelKeyLocalPolicyReference = "uddi:schemas.xmlsoap.org:localpolicyreference:2003_03";
    private static final String tModelKeyRemotePolicyReference = "uddi:schemas.xmlsoap.org:remotepolicyreference:2003_03";
    // WS-Policy Attachment 1.5
//    private static final String tModelKeyPolicyType = "uddi:w3.org:ws-policy:v1.5:attachment:policytypes";
//    private static final String tModelKeyLocalPolicyReference = "uddi:w3.org:ws-policy:v1.5:attachment:localpolicyreference";
//    private static final String tModelKeyRemotePolicyReference = "uddi:w3.org:ws-policy:v1.5:attachment:remotepolicyreference";
    
    private JTextField urlInquiryTextField;
    private JTextField urlPublishTextField;
    private JTextField urlSubscriptionTextField;
    private JTextField urlSecurityTextField;
    private JTextField searchPolicyNameTextField;
    private JButton searchPolicyButton;
    private JList policySearchList;
    private JTextField credsUsernameTextField;
    private JPasswordField credsPasswordField;
    private JPanel mainPanel;
    private JList serviceForPolicyList;
    private JButton searchPolicyServiceButton;
    private JButton policyDeleteButton;
    private JTextField searchTModelTextField;
    private JButton searchTModelButton;
    private JTable tModelResultsTable;
    private JButton fetchButton;
    private JTextField searchBusinessTextField;
    private JButton searchBusinessButton;
    private JTable businessResultsTable;
    private JButton businessFetchButton;
    private JButton businessInfoButton;
    private JTextField searchServiceTextField;
    private JButton searchServiceButton;
    private JTable serviceResultsTable;
    private JButton serviceFetchButton;
    private JButton serviceInfoButton;
    private JButton serviceFetchBindingButton;
    private JButton searchSubscriptionsButton;
    private JList subscriptionSearchList;
    private JButton deleteSubscriptionButton;
    private JButton addSubscriptionButton;
    private JButton subscriptionResultsButton;
    private JTextField subscriptionBindingKeyTextField;
    private JButton deleteTModelButton;
    private JButton deleteTModelReferencesButton;

    private JFrame window;

    public UDDIPolicyTool() {
        super(logger);
        JFrame window = new JFrame("UDDI Tool v0.6");

        window.setContentPane(mainPanel);
        window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        policySearchList.setCellRenderer(new TextListCellRenderer<Object>(new TextListCellProvider(true), new TextListCellProvider(false), false));
        serviceForPolicyList.setCellRenderer(new TextListCellRenderer<Object>(new TextListCellProvider(true), new TextListCellProvider(false), false));
        initListeners();
        initData();
        window.pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        window.setLocation((int)(screenSize.getWidth()-window.getWidth())/2, (int)(screenSize.getHeight()-window.getHeight())/2);

        this.window = window;
    }

    private void initListeners() {
        searchPolicyButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    findPolicies();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        searchPolicyServiceButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    findPolicyServices();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        policyDeleteButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    int result = JOptionPane.showConfirmDialog(
                            UDDIPolicyTool.this.window,
                            "Really delete policy and any reference(s) to it?",
                            "Confirm Policy Deletion",
                            JOptionPane.OK_CANCEL_OPTION);
                    if ( result == JOptionPane.OK_OPTION ) {
                        deletePolicy();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        searchBusinessButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    findBusinesses();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        businessInfoButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    fetchInfoBusiness();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        businessFetchButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    fetchBusiness();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        searchServiceButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    findServices();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        serviceInfoButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    fetchInfoService();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        serviceFetchButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    fetchService();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        serviceFetchBindingButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    fetchServiceBinding();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        searchTModelButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    findTModels();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        fetchButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    fetchTModel();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        deleteTModelButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    deleteTModel();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        deleteTModelReferencesButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    deleteTModelReferences();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        searchSubscriptionsButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    findSubscriptions();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        addSubscriptionButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    addSubscription();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }  );
        subscriptionResultsButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    subscriptionResults();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }  );
        deleteSubscriptionButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    deleteSubscription();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } );

        DefaultTableModel businessResultsModel = new DefaultTableModel(new String[]{ "Key", "Name", "Owner", "Created", "Modified" },0){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        businessResultsTable.setModel( businessResultsModel );
        businessResultsTable.setRowSorter(new TableRowSorter<DefaultTableModel>(businessResultsModel));

        DefaultTableModel serviceResultsModel = new DefaultTableModel(new String[]{ "Key", "Name", "Owner", "Created", "Modified" },0){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        serviceResultsTable.setModel( serviceResultsModel );
        serviceResultsTable.setRowSorter(new TableRowSorter<DefaultTableModel>(serviceResultsModel));

        DefaultTableModel model = new DefaultTableModel(new String[]{ "Key", "Name", "Desc" },0){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tModelResultsTable.setModel( model );
        tModelResultsTable.setRowSorter(new TableRowSorter<DefaultTableModel>(model));
    }

    private void initData() {
//        urlInquiryTextField.setText("http://centrasite8.l7tech.com:53307/UddiRegistry/inquiry");
//        urlPublishTextField.setText("http://centrasite8.l7tech.com:53307/UddiRegistry/publish");
//        urlSubscriptionTextField.setText("http://centrasite8.l7tech.com:53307/UddiRegistry/publish");
//        urlSecurityTextField.setText("http://centrasite8.l7tech.com:53307/UddiRegistry/publish");
//        credsUsernameTextField.setText("layer7");

//        urlInquiryTextField.setText("http://donalwinxp.l7tech.com:53307/UddiRegistry/inquiry");
//        urlPublishTextField.setText("http://donalwinxp.l7tech.com:53307/UddiRegistry/publish");
//        urlSubscriptionTextField.setText("http://donalwinxp.l7tech.com:53307/UddiRegistry/publish");
//        urlSecurityTextField.setText("http://donalwinxp.l7tech.com:53307/UddiRegistry/publish");
//        credsUsernameTextField.setText("administrator");

//        urlInquiryTextField.setText("http://centrasitegov.l7tech.com:2020/registry/uddi/inquiry");
//        urlPublishTextField.setText("http://centrasitegov.l7tech.com:2020/registry/uddi/publish");
//        urlSecurityTextField.setText("http://centrasitegov.l7tech.com:2020/registry/uddi/security");
//        urlSubscriptionTextField.setText("http://centrasitegov.l7tech.com:2020/registry/uddi/subscription");
//        credsUsernameTextField.setText("administrator@webmethods.com");

//        urlInquiryTextField.setText("http://centrasiteuddi.l7tech.com:53307/UddiRegistry/inquiry");
//        urlPublishTextField.setText("http://centrasiteuddi.l7tech.com:53307/UddiRegistry/publish");
//        urlSecurityTextField.setText("http://centrasiteuddi.l7tech.com:53307/UddiRegistry/publish");
//        urlSubscriptionTextField.setText("http://centrasiteuddi.l7tech.com:53307/UddiRegistry/publish");
//        credsUsernameTextField.setText("administrator");

        urlInquiryTextField.setText("http://systinet.l7tech.com:8080/uddi/inquiry");
        urlPublishTextField.setText("http://systinet.l7tech.com:8080/uddi/publishing");
        urlSecurityTextField.setText("http://systinet.l7tech.com:8080/uddi/security");
        urlSubscriptionTextField.setText("http://systinet.l7tech.com:8080/uddi/subscription");
        credsUsernameTextField.setText("admin");
    }

    @Override
    protected String getUsername() {
        return credsUsernameTextField.getText();
    }

    @Override
    protected String getPassword() {
        return new String(credsPasswordField.getPassword());
    }

    @Override
    protected String getInquiryUrl() {
        return urlInquiryTextField.getText();
    }

    @Override
    protected String getPublishingUrl() {
        return urlPublishTextField.getText();
    }

    @Override
    protected String getSubscriptionUrl() {
        return urlSubscriptionTextField.getText();
    }

    @Override
    protected String getSecurityUrl() {
        return urlSecurityTextField.getText();
    }

    /**
     *
     */
    private void findBusinesses() throws Exception {
        String searchPattern = searchBusinessTextField.getText();
        if (searchPattern.length()==0) {
            searchPattern = "%";
        }

        DefaultTableModel model = (DefaultTableModel) businessResultsTable.getModel();
        model.setRowCount(0);
        try {
            String authToken = authToken();

            // if approximate match is used for the URL then CentraSite will perform a prefix match ...
            FindQualifiers findQualifiers = buildFindQualifiers(false, null);
            Name name = buildName(searchPattern);

            UDDIInquiryPortType inquiryPort = getInquirePort();

            // find policy tmodel(s)
            FindBusiness findBusiness = new FindBusiness();
            findBusiness.setAuthInfo(authToken);
            findBusiness.setFindQualifiers(findQualifiers);
            findBusiness.getName().add(name);
            BusinessList businessList = inquiryPort.findBusiness(findBusiness);

            BusinessInfos businessInfos = businessList.getBusinessInfos();
            if (businessInfos != null) {
                // find ownership
                // find info
                GetOperationalInfo getOperationalInfo = new GetOperationalInfo();
                getOperationalInfo.setAuthInfo(authToken);
                for (BusinessInfo businessInfo : businessInfos.getBusinessInfo()) {
                    getOperationalInfo.getEntityKey().add(businessInfo.getBusinessKey());
                }
                OperationalInfos infos = inquiryPort.getOperationalInfo(getOperationalInfo);
                List<OperationalInfo> operationalInfos = null;
                if (infos != null) {
                    operationalInfos = infos.getOperationalInfo();
                }
                if ( operationalInfos == null ) operationalInfos = Collections.emptyList();


                for (BusinessInfo businessInfo : businessInfos.getBusinessInfo()) {
                    if (businessInfo.getName() != null) {
                        String key = businessInfo.getBusinessKey();
                        String businessname = businessInfo.getName().get(0).getValue();
                        String desc = businessInfo.getDescription().toString();

                        OperationalInfo info = null;
                        for ( OperationalInfo operationalInfo : operationalInfos ) {
                            if ( key.equals(operationalInfo.getEntityKey()) ) {
                                info = operationalInfo;
                                break;
                            }
                        }

                        System.out.println("Key: " + key + " Name: " + businessname + " Desc:" + desc );
                        if ( info != null ) {
                            model.addRow(new String[]{ key, businessname,info.getAuthorizedName(), info.getCreated().toString(), info.getModified().toString() });
                        } else {
                            model.addRow(new String[]{ key, businessname,"", "", "" });
                        }
                    }
                }
            }
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error listing policies: ", drfm);
        }
    }

    /**
     * Fetch / log info for currently selected tmodel
     */
    private void fetchInfoBusiness() throws Exception {
        int row = businessResultsTable.getSelectedRow();
        if ( row >= 0 ) {
            int modelRow = businessResultsTable.convertRowIndexToModel(row);
            String businessKey = (String)((DefaultTableModel)businessResultsTable.getModel()).getValueAt(modelRow, 0);
            System.out.println( "Selected key is: " + businessKey );

            try {
                String authToken = authToken();

                UDDIInquiryPortType inquiryPort = getInquirePort();

                // find info
                GetOperationalInfo getOperationalInfo = new GetOperationalInfo();
                getOperationalInfo.setAuthInfo(authToken);
                getOperationalInfo.getEntityKey().add(businessKey);

                OperationalInfos infos = inquiryPort.getOperationalInfo(getOperationalInfo);
                if (infos != null) {
                    List<OperationalInfo> OperationalInfos = infos.getOperationalInfo();
                    for ( OperationalInfo operationalInfo : OperationalInfos ) {
                        System.out.println( "Owner: " + operationalInfo.getAuthorizedName() );
                        System.out.println( "Key: " + operationalInfo.getEntityKey());
                        System.out.println( "Created: " + operationalInfo.getCreated());
                    }
                }
            } catch (DispositionReportFaultMessage drfm) {
                throw buildFaultException("Error listing policies: ", drfm);
            }
        } else {
            System.out.println( "No selected row." );
        }
    }

    /**
     * Fetch / log info for currently selected tmodel
     */
    private void fetchBusiness() throws Exception {
        int row = businessResultsTable.getSelectedRow();
        if ( row >= 0 ) {
            int modelRow = businessResultsTable.convertRowIndexToModel(row);
            String businessKey = (String)((DefaultTableModel)businessResultsTable.getModel()).getValueAt(modelRow, 0);
            System.out.println( "Selected key is: " + businessKey );

            try {
                String authToken = authToken();

                UDDIInquiryPortType inquiryPort = getInquirePort();

                // find policy tmodel(s)
                GetBusinessDetail getBusinessDetail = new GetBusinessDetail();
                getBusinessDetail.setAuthInfo(authToken);
                getBusinessDetail.getBusinessKey().add(businessKey);

                BusinessDetail detail = inquiryPort.getBusinessDetail(getBusinessDetail);
                if (detail != null) {
                    List<BusinessEntity> businessEntities = detail.getBusinessEntity();
                    for ( BusinessEntity businessEntity : businessEntities ) {
                        System.out.println( "Key: " + businessEntity.getBusinessKey() );

                        System.out.println( "Name: " + businessEntity.getName().get(0).getValue());
                        for ( Description desc : businessEntity.getDescription() ) {
                            System.out.println( "Desc: " + desc.getValue() );
                        }
                        if ( businessEntity.getCategoryBag() != null ) {
                            for( KeyedReference cbagKref : businessEntity.getCategoryBag().getKeyedReference() ) {
                                System.out.println( "CBagKName: " + cbagKref.getKeyName() );
                                System.out.println( "CBagKValu: " + cbagKref.getKeyValue() );
                            }
                        }
                        System.out.println( "IBag: " + businessEntity.getIdentifierBag() );

                        System.out.println( "Sig: " + businessEntity.getSignature() );
                    }
                }
            } catch (DispositionReportFaultMessage drfm) {
                throw buildFaultException("Error listing policies: ", drfm);
            }
        } else {
            System.out.println( "No selected row." );
        }
    }

    /**
     *
     */
    private void findServices() throws Exception {
        String searchPattern = searchServiceTextField.getText();
        if (searchPattern.length()==0) {
            searchPattern = "%";
        }

        DefaultTableModel model = (DefaultTableModel) serviceResultsTable.getModel();
        model.setRowCount(0);
        try {
            String authToken = authToken();

            // if approximate match is used for the URL then CentraSite will perform a prefix match ...
            FindQualifiers findQualifiers = buildFindQualifiers(false, null);
            Name name = buildName(searchPattern);

            UDDIInquiryPortType inquiryPort = getInquirePort();

            // find policy tmodel(s)
            FindService findService = new FindService();
            findService.setAuthInfo(authToken);
            findService.setFindQualifiers(findQualifiers);
            findService.getName().add(name);
            ServiceList serviceList = inquiryPort.findService(findService);

            ServiceInfos serviceInfos = serviceList.getServiceInfos();
            if (serviceInfos != null) {
                // find ownership
                // find info
                GetOperationalInfo getOperationalInfo = new GetOperationalInfo();
                getOperationalInfo.setAuthInfo(authToken);
                for (ServiceInfo serviceInfo : serviceInfos.getServiceInfo()) {
                    getOperationalInfo.getEntityKey().add(serviceInfo.getServiceKey());
                }
                OperationalInfos infos = inquiryPort.getOperationalInfo(getOperationalInfo);
                List<OperationalInfo> operationalInfos = null;
                if (infos != null) {
                    operationalInfos = infos.getOperationalInfo();
                }
                if ( operationalInfos == null ) operationalInfos = Collections.emptyList();


                for (ServiceInfo serviceInfo : serviceInfos.getServiceInfo()) {
                    if (serviceInfo.getName() != null) {
                        String key = serviceInfo.getServiceKey();
                        String servicename = serviceInfo.getName().get(0).getValue();
                        String desc = "";

                        OperationalInfo info = null;
                        for ( OperationalInfo operationalInfo : operationalInfos ) {
                            if ( key.equals(operationalInfo.getEntityKey()) ) {
                                info = operationalInfo;
                                break;
                            }
                        }

                        System.out.println("Key: " + key + " Name: " + servicename + " Desc:" + desc );
                        if ( info != null ) {
                            model.addRow(new String[]{ key, servicename,info.getAuthorizedName(), info.getCreated().toString(), info.getModified().toString() });
                        } else {
                            model.addRow(new String[]{ key, servicename,"", "", "" });
                        }
                    }
                }
            }
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error listing policies: ", drfm);
        }
    }

    /**
     * Fetch / log info for currently selected tmodel
     */
    private void fetchInfoService() throws Exception {
        int row = serviceResultsTable.getSelectedRow();
        if ( row >= 0 ) {
            int modelRow = serviceResultsTable.convertRowIndexToModel(row);
            String serviceKey = (String)((DefaultTableModel)serviceResultsTable.getModel()).getValueAt(modelRow, 0);
            System.out.println( "Selected key is: " + serviceKey );

            try {
                String authToken = authToken();

                UDDIInquiryPortType inquiryPort = getInquirePort();

                // find info
                GetOperationalInfo getOperationalInfo = new GetOperationalInfo();
                getOperationalInfo.setAuthInfo(authToken);
                getOperationalInfo.getEntityKey().add(serviceKey);

                OperationalInfos infos = inquiryPort.getOperationalInfo(getOperationalInfo);
                if (infos != null) {
                    List<OperationalInfo> OperationalInfos = infos.getOperationalInfo();
                    for ( OperationalInfo operationalInfo : OperationalInfos ) {
                        System.out.println( "Owner: " + operationalInfo.getAuthorizedName() );
                        System.out.println( "Key: " + operationalInfo.getEntityKey());
                        System.out.println( "Created: " + operationalInfo.getCreated());
                    }
                }
            } catch (DispositionReportFaultMessage drfm) {
                throw buildFaultException("Error listing policies: ", drfm);
            }
        } else {
            System.out.println( "No selected row." );
        }
    }

    /**
     * Fetch / log info for currently selected tmodel
     */
    private void fetchService() throws Exception {
        int row = serviceResultsTable.getSelectedRow();
        if ( row >= 0 ) {
            int modelRow = serviceResultsTable.convertRowIndexToModel(row);
            String serviceKey = (String)((DefaultTableModel)serviceResultsTable.getModel()).getValueAt(modelRow, 0);
            System.out.println( "Selected key is: " + serviceKey );

            try {
                String authToken = authToken();

                UDDIInquiryPortType inquiryPort = getInquirePort();

                // find service detail
                GetServiceDetail getServiceDetail = new GetServiceDetail();
                getServiceDetail.setAuthInfo(authToken);
                getServiceDetail.getServiceKey().add(serviceKey);

                ServiceDetail detail = inquiryPort.getServiceDetail(getServiceDetail);
                if (detail != null) {
                    List<BusinessService> businessServices = detail.getBusinessService();
                    for ( BusinessService businessService : businessServices ) {
                        System.out.println( "Key: " + businessService.getBusinessKey() );

                        System.out.println( "Name: " + businessService.getName().get(0).getValue());
                        for ( Description desc : businessService.getDescription() ) {
                            System.out.println( "Desc: " + desc.getValue() );
                        }
                        if ( businessService.getCategoryBag() != null ) {
                            for( KeyedReference cbagKref : businessService.getCategoryBag().getKeyedReference() ) {
                                System.out.println( "CBagKName: " + cbagKref.getKeyName() );
                                System.out.println( "CBagKValu: " + cbagKref.getKeyValue() );
                            }
                        }
                        //System.out.println( "IBag: " + businessService.getIdentifierBag() );

                        System.out.println( "Sig: " + businessService.getSignature() );
                    }
                }
            } catch (DispositionReportFaultMessage drfm) {
                throw buildFaultException("Error listing service: ", drfm);
            }
        } else {
            System.out.println( "No selected row." );
        }
    }

    /**
     * Fetch / log info for currently selected tmodel
     */
    private void fetchServiceBinding() throws Exception {
        int row = serviceResultsTable.getSelectedRow();
        if ( row >= 0 ) {
            int modelRow = serviceResultsTable.convertRowIndexToModel(row);
            String serviceKey = (String)((DefaultTableModel)serviceResultsTable.getModel()).getValueAt(modelRow, 0);
            System.out.println( "Selected key is: " + serviceKey );

            try {
                String authToken = authToken();

                UDDIInquiryPortType inquiryPort = getInquirePort();

                // find service detail
                FindBinding findBinding = new FindBinding();
                findBinding.setAuthInfo(authToken);
                findBinding.setServiceKey(serviceKey);

                BindingDetail detail = inquiryPort.findBinding(findBinding);
                if (detail != null) {
                    List<BindingTemplate> templates = detail.getBindingTemplate();
                    for ( BindingTemplate bindingTemplate : templates ) {
                        System.out.println( "Service Key: " + bindingTemplate.getServiceKey() );
                    }
                }
            } catch (DispositionReportFaultMessage drfm) {
                throw buildFaultException("Error listing binding: ", drfm);
            }
        } else {
            System.out.println( "No selected row." );
        }
    }

    /**
     *
     */
    private void findTModels() throws Exception {
        String searchPattern = searchTModelTextField.getText();
        if (searchPattern.length()==0) {
            searchPattern = "%";
        }

        //List<UDDINamedEntity> policies = new ArrayList<UDDINamedEntity>();

        DefaultTableModel model = (DefaultTableModel) tModelResultsTable.getModel();
        model.setRowCount(0);
        try {
            String authToken = authToken();

            // if approximate match is used for the URL then CentraSite will perform a prefix match ...
            FindQualifiers findQualifiers = buildFindQualifiers(false);
            Name name = buildName(searchPattern);

            UDDIInquiryPortType inquiryPort = getInquirePort();

            // find policy tmodel(s)
            FindTModel findTModel = new FindTModel();
            findTModel.setAuthInfo(authToken);
            findTModel.setFindQualifiers(findQualifiers);
            findTModel.setName(name);
            //findTModel.setCategoryBag(cbag);
            TModelList tModelList = inquiryPort.findTModel(findTModel);

            TModelInfos tModelInfos = tModelList.getTModelInfos();
            if (tModelInfos != null) {
                for (TModelInfo tModel : tModelInfos.getTModelInfo()) {
                    if (tModel.getName() != null) {
                        String key = tModel.getTModelKey();
                        String tmodelname = tModel.getName().getValue();
                        String desc = toString(tModel.getDescription());

                        System.out.println("Key: " + key + " Name: " + tmodelname + " Desc:" + desc );
                        model.addRow(new String[]{ key, tmodelname, desc });
                    }
                }
            }
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error listing policies: ", drfm);
        }
    }

    private String toString( final List<Description> descriptions ) {
        String desc = "";

        if ( descriptions != null && !descriptions.isEmpty() ) {
            desc = descriptions.get( 0 ).getValue();           
        }

        return desc;
    }

    /**
     * Fetch / log info for currently selected tmodel
     */
    private void fetchTModel() throws Exception {
        int row = tModelResultsTable.getSelectedRow();
        if ( row >= 0 ) {
            int modelRow = tModelResultsTable.convertRowIndexToModel(row);
            String tModelKey = (String)((DefaultTableModel)tModelResultsTable.getModel()).getValueAt(modelRow, 0);
            System.out.println( "Selected key is: " + tModelKey );

            try {
                String authToken = authToken();

                UDDIInquiryPortType inquiryPort = getInquirePort();

                // find tmodel(s)
                GetTModelDetail getTModelDetail = new GetTModelDetail();
                getTModelDetail.setAuthInfo(authToken);
                getTModelDetail.getTModelKey().add(tModelKey);

                TModelDetail detail = inquiryPort.getTModelDetail(getTModelDetail);
                if (detail != null) {
                    List<TModel> tModels = detail.getTModel();
                    for ( TModel tModel : tModels ) {
                        System.out.println( "Key: " + tModel.getTModelKey() );

                        System.out.println( "Name: " + tModel.getName().getValue());
                        for ( Description desc : tModel.getDescription() ) {
                            System.out.println( "Desc: " + desc.getValue() );
                        }
                        for ( OverviewDoc over : tModel.getOverviewDoc() ) {
                            System.out.println( "ODocType: " + over.getOverviewURL().getUseType() );
                            System.out.println( "ODocURL: " + over.getOverviewURL().getValue() );
                        }
                        for( KeyedReference cbagKref : tModel.getCategoryBag().getKeyedReference() ) {
                            System.out.println( "CBagTMKey: " + cbagKref.getTModelKey() );
                            System.out.println( "CBagKName: " + cbagKref.getKeyName() );
                            System.out.println( "CBagKValu: " + cbagKref.getKeyValue() );
                        }
                        System.out.println( "IBag: " + tModel.getIdentifierBag() );

                        System.out.println( "Sig: " + tModel.getSignature() );
                    }
                }
            } catch (DispositionReportFaultMessage drfm) {
                throw buildFaultException("Error listing policies: ", drfm);
            }
        } else {
            System.out.println( "No selected row." );
        }
    }

    private void deleteTModel() throws Exception {
        int row = tModelResultsTable.getSelectedRow();
        if ( row >= 0 ) {
            int modelRow = tModelResultsTable.convertRowIndexToModel(row);
            String tModelKey = (String)((DefaultTableModel)tModelResultsTable.getModel()).getValueAt(modelRow, 0);
            System.out.println( "Selected key is: " + tModelKey );

            try {
                String authToken = authToken();

                UDDIPublicationPortType publicationPort = getPublishPort();

                // delete tmodel
                DeleteTModel deleteTModel = new DeleteTModel();
                deleteTModel.setAuthInfo(authToken);
                deleteTModel.getTModelKey().add(tModelKey);

                publicationPort.deleteTModel( deleteTModel );
            } catch (DispositionReportFaultMessage drfm) {
                throw buildFaultException("Error deleting TModel: ", drfm);
            }
        } else {
            System.out.println( "No selected row." );
        }
    }

    /**
     * Delete keyed reference from any service that references the TModel 
     */
    private void deleteTModelReferences() throws Exception {
        int row = tModelResultsTable.getSelectedRow();
        if ( row >= 0 ) {
            int modelRow = tModelResultsTable.convertRowIndexToModel(row);
            String tModelKey = (String)((DefaultTableModel)tModelResultsTable.getModel()).getValueAt(modelRow, 0);
            System.out.println( "Selected key is: " + tModelKey );

            try {
                String authToken = authToken();

                UDDIInquiryPortType inquiryPort = getInquirePort();

                FindService findService = new FindService();
                findService.setAuthInfo(authToken);
                findService.setFindQualifiers(buildFindQualifiers(false));

                // category constraints
                CategoryBag cb2 = new CategoryBag();

                // WSDL services only
                KeyedReference keyedReference = new KeyedReference();
                keyedReference.setKeyValue(WSDL_TYPES_SERVICE);
                keyedReference.setTModelKey(TMODEL_KEY_WSDL_TYPES);
                cb2.getKeyedReference().add(keyedReference);

                // tModel reference
                KeyedReference keyedReference2 = new KeyedReference();
                keyedReference2.setKeyValue(tModelKey);
                keyedReference2.setTModelKey(tModelKeyLocalPolicyReference);
                cb2.getKeyedReference().add(keyedReference2);

                findService.setCategoryBag( cb2 );

                ServiceList serviceList = inquiryPort.findService(findService);

                //
                List<UDDINamedEntity> services = new ArrayList<UDDINamedEntity>();
                ServiceInfos serviceInfos = serviceList.getServiceInfos();
                if (serviceInfos != null) {
                    for (ServiceInfo serviceInfo : serviceInfos.getServiceInfo()) {
                        String name = get(serviceInfo.getName(), "service name", false).getValue();
                        services.add(new UDDINamedEntity(serviceInfo.getServiceKey(), name));
                    }
                }

                for ( UDDINamedEntity service : services ) {
                    System.out.println( "Referenced from service : " + service.getName() + " " + service.getKey() );    
                }

                UDDIPublicationPortType publicationPort = getPublishPort();
                for (UDDINamedEntity uddiEntity : services) {
                    String serviceKey = uddiEntity.getKey();

                    GetServiceDetail getServiceDetail = new GetServiceDetail();
                    getServiceDetail.setAuthInfo(authToken);
                    getServiceDetail.getServiceKey().add(serviceKey);
                    ServiceDetail serviceDetail = inquiryPort.getServiceDetail(getServiceDetail);

                    if (serviceDetail.getBusinessService().size() != 1) {
                        String msg = "UDDI registry returned either empty serviceDetail or " +
                                     "more than one business services";
                        throw new Exception(msg);
                    }

                    //get the bag for the service
                    BusinessService toUpdate = get(serviceDetail.getBusinessService(), "service", true);
                    CategoryBag cbag = toUpdate.getCategoryBag();

                    // find references and remove them
                    if ( cbag != null ) {
                        Collection<KeyedReference> updated = new ArrayList<KeyedReference>();
                        if (cbag.getKeyedReference() != null) {
                            for (KeyedReference kref : cbag.getKeyedReference()) {
                                if (kref.getKeyValue().equals(tModelKey)) {
                                    // then we don't wan't this one
                                } else {
                                    updated.add(kref);
                                }
                            }
                        }

                        cbag.getKeyedReference().clear();
                        cbag.getKeyedReference().addAll(updated);

                        // update service in uddi
                        SaveService saveService = new SaveService();
                        saveService.setAuthInfo(authToken);
                        saveService.getBusinessService().add(toUpdate);
                        publicationPort.saveService(saveService);
                    }
                }   
            } catch (DispositionReportFaultMessage drfm) {
                throw buildFaultException("Error removing references to TModel: ", drfm);
            }
        }
    }

    /**
     * Find all the policy tModels (filter by name)
     */
    private void findPolicies() throws Exception {
        String policyPattern = searchPolicyNameTextField.getText();
        if (policyPattern.length()==0) {
            policyPattern = "%";
        }

        List<UDDINamedEntity> policies = new ArrayList<UDDINamedEntity>();

        try {
            String authToken = authToken();

            CategoryBag cbag = new CategoryBag();
            cbag.getKeyedReference().add(buildKeyedReference(tModelKeyPolicyType, null, POLICY_TYPE_KEY_VALUE));

            // if approximate match is used for the URL then CentraSite will perform a prefix match ...
            FindQualifiers findQualifiers = buildFindQualifiers(false);
            Name name = buildName(policyPattern);

            UDDIInquiryPortType inquiryPort = getInquirePort();

            // find policy tmodel(s)
            FindTModel findTModel = new FindTModel();
            findTModel.setAuthInfo(authToken);
            findTModel.setFindQualifiers(findQualifiers);
            findTModel.setName(name);
            findTModel.setCategoryBag(cbag);
            TModelList tModelList = inquiryPort.findTModel(findTModel);

            List<String> policyKeys = new ArrayList<String>();
            TModelInfos tModelInfos = tModelList.getTModelInfos();
            if (tModelInfos != null) {
                for (TModelInfo tModel : tModelInfos.getTModelInfo()) {
                    if (tModel.getName() != null) {
                        String key = tModel.getTModelKey();
                        policyKeys.add(key);
                        String tmodelname = tModel.getName().getValue();
                        policies.add(new UDDINamedEntity(key, tmodelname));
                    }
                }
            }

            // Get policy urls the named info
            if ( !policyKeys.isEmpty() ) {
                GetTModelDetail getTModelDetail = new GetTModelDetail();
                getTModelDetail.setAuthInfo(authToken);
                getTModelDetail.getTModelKey().addAll(policyKeys);
                TModelDetail res = inquiryPort.getTModelDetail(getTModelDetail);
                if (res != null) {
                    for (TModel tModel : res.getTModel()) {
                        CategoryBag categoryBag = tModel.getCategoryBag();
                        if (categoryBag == null)
                            continue;

                        List<KeyedReference> keyedReferences = categoryBag.getKeyedReference();
                        if (keyedReferences == null || keyedReferences.isEmpty())
                            continue;

                        for (KeyedReference keyedReference : keyedReferences) {
                            if (keyedReference.getTModelKey().equals(tModelKeyRemotePolicyReference)) {
                                mergePolicyUrlToInfo(
                                        tModel.getTModelKey(),
                                        keyedReference.getKeyValue(),
                                        policies);
                                break;
                            }
                        }
                    }
                }
            }

            // update view 
            policySearchList.setListData(policies.toArray());
            serviceForPolicyList.setListData(new Object[0]);
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error listing policies: ", drfm);
        }
    }

    /**
     * Find the services associated with the selected policy tModel 
     */
    private void findPolicyServices() throws Exception {
        UDDINamedEntity entity = (UDDINamedEntity) policySearchList.getSelectedValue();
        String policyKey = entity==null ? null : entity.getKey();

        List<UDDINamedEntity> services = new ArrayList<UDDINamedEntity>();
        try {
            String authToken = authToken();

            UDDIInquiryPortType inquiryPort = getInquirePort();

            // find service
            FindService findService = new FindService();
            findService.setAuthInfo(authToken);
            findService.setFindQualifiers(buildFindQualifiers(false));

            // category constraints
            CategoryBag cb2 = new CategoryBag();

            // WSDL services only
            KeyedReference keyedReference = new KeyedReference();
            keyedReference.setKeyValue(WSDL_TYPES_SERVICE);
            keyedReference.setTModelKey(TMODEL_KEY_WSDL_TYPES);
            cb2.getKeyedReference().add(keyedReference);

            // Policy tModel reference
            KeyedReference keyedReference2 = new KeyedReference();
            keyedReference2.setKeyValue(policyKey);
            keyedReference2.setTModelKey(tModelKeyLocalPolicyReference);
            cb2.getKeyedReference().add(keyedReference2);

            findService.setCategoryBag(cb2);

            if (policyKey != null) {
                ServiceList serviceList = inquiryPort.findService(findService);

                //
                ServiceInfos serviceInfos = serviceList.getServiceInfos();
                if (serviceInfos != null) {
                    for (ServiceInfo serviceInfo : serviceInfos.getServiceInfo()) {
                        String name = get(serviceInfo.getName(), "service name", false).getValue();
                        services.add(new UDDINamedEntity(serviceInfo.getServiceKey(), name));
                    }
                }
            }

        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error listing policies: ", drfm);
        } finally {
            // update view
            serviceForPolicyList.setListData(services.toArray());
        }
    }

    /**
     * Find the policy references for services associated with the selected policy tModel
     * then delete the policy tModel.
     */
    private void deletePolicy() throws Exception {
        UDDINamedEntity entity = (UDDINamedEntity) policySearchList.getSelectedValue();
        String policyKey = entity==null ? null : entity.getKey();

        List<UDDINamedEntity> services = new ArrayList<UDDINamedEntity>();
        try {
            String authToken = authToken();

            UDDIInquiryPortType inquiryPort = getInquirePort();

            // find service
            FindService findService = new FindService();
            findService.setAuthInfo(authToken);
            findService.setFindQualifiers(buildFindQualifiers(false));

            // category constraints
            CategoryBag cb2 = new CategoryBag();

            // WSDL services only
            KeyedReference keyedReference = new KeyedReference();
            keyedReference.setKeyValue(WSDL_TYPES_SERVICE);
            keyedReference.setTModelKey(TMODEL_KEY_WSDL_TYPES);
            cb2.getKeyedReference().add(keyedReference);

            // Policy tModel reference
            KeyedReference keyedReference2 = new KeyedReference();
            keyedReference2.setKeyValue(policyKey);
            keyedReference2.setTModelKey(tModelKeyLocalPolicyReference);
            cb2.getKeyedReference().add(keyedReference2);

            findService.setCategoryBag(cb2);

            if (policyKey != null) {
                ServiceList serviceList = inquiryPort.findService(findService);

                //
                ServiceInfos serviceInfos = serviceList.getServiceInfos();
                if (serviceInfos != null) {
                    for (ServiceInfo serviceInfo : serviceInfos.getServiceInfo()) {
                        String name = get(serviceInfo.getName(), "service name", false).getValue();
                        services.add(new UDDINamedEntity(serviceInfo.getServiceKey(), name));
                    }
                }

                List<UDDINamedEntity> oldServicesList = new ArrayList<UDDINamedEntity>();
                for (int i=0; i<serviceForPolicyList.getModel().getSize(); i++) {
                    oldServicesList.add((UDDINamedEntity)serviceForPolicyList.getModel().getElementAt(i));
                }

                if ( !services.equals(oldServicesList) ) {
                    JOptionPane.showMessageDialog(window,
                            "The list of services does not match those found in UDDI.\nPlease search for services and try again.",
                            "Unable to delete policy",
                            JOptionPane.OK_OPTION);
                } else {
                    UDDIPublicationPortType publicationPort = getPublishPort();
                    for (UDDINamedEntity uddiEntity : oldServicesList) {
                        String serviceKey = uddiEntity.getKey();

                        GetServiceDetail getServiceDetail = new GetServiceDetail();
                        getServiceDetail.setAuthInfo(authToken);
                        getServiceDetail.getServiceKey().add(serviceKey);
                        ServiceDetail serviceDetail = inquiryPort.getServiceDetail(getServiceDetail);

                        if (serviceDetail.getBusinessService().size() != 1) {
                            String msg = "UDDI registry returned either empty serviceDetail or " +
                                         "more than one business services";
                            throw new Exception(msg);
                        }

                        //get the bag for the service
                        BusinessService toUpdate = get(serviceDetail.getBusinessService(), "service", true);
                        Collection<CategoryBag> cbags = new ArrayList<CategoryBag>();
                        {
                            CategoryBag cbag = toUpdate.getCategoryBag();
                            if (cbag == null) {
                                cbag = new CategoryBag();
                                toUpdate.setCategoryBag(cbag);
                            }
                            cbags.add(cbag);
                        }

                        // find references and remove them
                        for (CategoryBag cbag : cbags) {
                            Collection<KeyedReference> updated = new ArrayList<KeyedReference>();
                            if (cbag.getKeyedReference() != null) {
                                for (KeyedReference kref : cbag.getKeyedReference()) {
                                    if (kref.getTModelKey().equals(tModelKeyLocalPolicyReference) &&
                                        kref.getKeyValue().equals(policyKey)) {
                                        // then we don't wan't this one
                                    } else {
                                        updated.add(kref);
                                    }
                                }
                            }

                            cbag.getKeyedReference().clear();
                            cbag.getKeyedReference().addAll(updated);
                        }

                        // update service in uddi
                        SaveService saveService = new SaveService();
                        saveService.setAuthInfo(authToken);
                        saveService.getBusinessService().add(toUpdate);
                        publicationPort.saveService(saveService);
                    }

                    // delete policy
                    DeleteTModel deleteTModel = new DeleteTModel();
                    deleteTModel.setAuthInfo(authToken);
                    deleteTModel.getTModelKey().add(policyKey);
                    publicationPort.deleteTModel(deleteTModel);

                    // update view
                    policySearchList.setListData(new Object[0]);
                    serviceForPolicyList.setListData(new Object[0]);
                }
            }
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error listing policies: ", drfm);
        }
    }

    private void findSubscriptions() throws Exception {
        try {
            UDDISubscriptionPortType subscriptionPort = getSubscriptionPort();

            List<String> subs = new ArrayList<String>();
            List<Subscription> subscriptions = subscriptionPort.getSubscriptions( authToken() );
            if ( subscriptions != null ) {
                for ( Subscription subscription : subscriptions ) {
                    subs.add( subscription.getSubscriptionKey() + " " + subscription.getExpiresAfter().toString() );
                }
            }

            subscriptionSearchList.setListData( subs.toArray( new Object[subs.size()] ) );
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error adding subscription: ", drfm);
        }
    }

    private void addSubscription() throws Exception {
        try {
            int row = serviceResultsTable.getSelectedRow();
            if ( row >= 0 ) {
                int modelRow = serviceResultsTable.convertRowIndexToModel(row);
                String serviceKey = (String)((DefaultTableModel)serviceResultsTable.getModel()).getValueAt(modelRow, 0);
                System.out.println( "Selected key is: " + serviceKey );

                DatatypeFactory factory = DatatypeFactory.newInstance();

                UDDISubscriptionPortType subscriptionPort = getSubscriptionPort();

                GetServiceDetail getServiceDetail = new GetServiceDetail();
                getServiceDetail.getServiceKey().add( serviceKey );

                SubscriptionFilter filter = new SubscriptionFilter();
                filter.setGetServiceDetail( getServiceDetail );

                GregorianCalendar expiresGC = new GregorianCalendar();
                expiresGC.setTimeInMillis( System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1) );
                XMLGregorianCalendar expiresAfter = factory.newXMLGregorianCalendar( expiresGC );

                String subscriptionBindingKey = subscriptionBindingKeyTextField.getText();
                if ( subscriptionBindingKey != null ) {
                    subscriptionBindingKey = subscriptionBindingKey.trim();
                    if ( subscriptionBindingKey.isEmpty() ) subscriptionBindingKey = null;
                }

                Subscription subscription = new Subscription();
                subscription.setBrief( true );
                subscription.setExpiresAfter( expiresAfter );
                subscription.setSubscriptionFilter( filter );
                if ( subscriptionBindingKey != null ) {
                    subscription.setBindingKey( subscriptionBindingKey );
                    subscription.setNotificationInterval( factory.newDuration( TimeUnit.MINUTES.toMillis(1)) );
                }

                Holder<List<Subscription>> holder = new Holder<List<Subscription>>( Collections.singletonList( subscription ));

                subscriptionPort.saveSubscription( authToken(), holder );

                for ( Subscription sub : holder.value ) {
                    System.out.println( "Added subscription : " + sub.getSubscriptionKey() );
                }
            } else {
                System.out.println("Select a service before adding subscription.");
            }
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error adding subscription: ", drfm);
        }
    }

    private void subscriptionResults() throws Exception {
        try {
            String selected = (String) subscriptionSearchList.getSelectedValue();
            if ( selected != null ) {
                String subscriptionKey = selected.substring( 0, selected.indexOf(' '));
                UDDISubscriptionPortType subscriptionPort = getSubscriptionPort();

                GregorianCalendar startGC = new GregorianCalendar();
                startGC.setTimeInMillis( System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1) );
                XMLGregorianCalendar start = DatatypeFactory.newInstance().newXMLGregorianCalendar( startGC );

                CoveragePeriod period = new CoveragePeriod();
                period.setStartPoint( start );

                GetSubscriptionResults getSubscriptionResults = new GetSubscriptionResults();
                getSubscriptionResults.setAuthInfo( authToken() );
                getSubscriptionResults.setSubscriptionKey( subscriptionKey );
                getSubscriptionResults.setCoveragePeriod( period );

                SubscriptionResultsList results = subscriptionPort.getSubscriptionResults( getSubscriptionResults );
                if ( results != null ) {
                    List<KeyBag> bags = results.getKeyBag();
                    if ( bags != null ) {
                        for ( KeyBag bag : bags ) {
                            if ( bag.isDeleted() ) {
                                System.out.println( "Deleted " + bag.getServiceKey() );
                            } else {
                                System.out.println( "Updated " + bag.getServiceKey() );
                            }
                        }
                    } else {
                        System.out.println( "Results bag is missing." );
                    }
                } else {
                    System.out.println( "Results missing" );
                }
            }
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error getting subscription results: ", drfm);
        }
    }

    private void deleteSubscription() throws Exception {
        String selected = (String) subscriptionSearchList.getSelectedValue();
        if ( selected != null ) {
            String subscriptionKey = selected.substring( 0, selected.indexOf(' '));
            UDDISubscriptionPortType subscriptionPort = getSubscriptionPort();

            DeleteSubscription deleteSubscription = new DeleteSubscription();
            deleteSubscription.setAuthInfo( authToken() );
            deleteSubscription.getSubscriptionKey().add( subscriptionKey );

            subscriptionPort.deleteSubscription( deleteSubscription );
        }
    }

    private <T> T get(List<T> list, String description, boolean onlyOne) throws Exception {
        if (list == null || list.isEmpty()) {
            throw new Exception("Missing " + description);
        } else if (onlyOne && list.size()!=1) {
            throw new Exception("Duplicate " + description);
        }

        return list.get(0);
    }

    private void mergePolicyUrlToInfo(final String policyKey,
                                      final String policyUrl,
                                      final List<UDDINamedEntity> uddiNamedEntity) {
        for (int i=0; i<uddiNamedEntity.size(); i++) {
            UDDINamedEntity current = uddiNamedEntity.get(i);

            if ( policyKey.equals(current.getKey()) ) {
                UDDINamedEntity merged = new UDDINamedEntity(
                        current.getKey(),
                        current.getName(),
                        policyUrl,
                        current.getWsdlUrl());
                uddiNamedEntity.remove(i);
                uddiNamedEntity.add(i, merged);
                break;
            }
        }
    }

    private FindQualifiers buildFindQualifiers(boolean caseSensitive) {
        return buildFindQualifiers( caseSensitive, null );
    }

    private FindQualifiers buildFindQualifiers(boolean caseSensitive, Boolean owned) {
        FindQualifiers findQualifiers = new FindQualifiers();
        List<String> qualifiers = findQualifiers.getFindQualifier();
        qualifiers.add(FINDQUALIFIER_APPROXIMATE);

        if ( !caseSensitive )
            qualifiers.add(FINDQUALIFIER_CASEINSENSITIVE);

        //TODO Use these systinet specific extensions?
        if ( Boolean.TRUE.equals(owned) )
            qualifiers.add("uddi:systinet.com:findQualifier:myEntities");  // find only owned entities
        else if ( Boolean.FALSE.equals(owned) )
            qualifiers.add("uddi:systinet.com:findQualifier:foreignEntities");  // find only non-owned entities

        return findQualifiers;
    }

    private KeyedReference buildKeyedReference(String key, String name, String value) {
        KeyedReference keyedReference = new KeyedReference();
        keyedReference.setTModelKey(key);
        keyedReference.setKeyName(name);
        keyedReference.setKeyValue(value);
        return keyedReference;
    }

    private Name buildName(String value) {
        Name name = new Name();
        name.setValue(value);
        return name;
    }

    /**
     *
     */
    public static void main(String[] args) {
        new UDDIPolicyTool().window.setVisible(true);
    }

    /**
     * Function for accessing either the display or tooltip text.
     */
    private static final class TextListCellProvider implements Functions.Unary<String, Object> {
        private final boolean showName;

        private TextListCellProvider(final boolean showName) {
            this.showName = showName;
        }

        @Override
        public String call(final Object entityObject) {
            UDDINamedEntity uddiNamedEntity = (UDDINamedEntity) entityObject;
            String text;

            if ( showName ) {
                // generate the entry text
                String tModelName = uddiNamedEntity.getName();
                String tModelKey = uddiNamedEntity.getKey();
                text = tModelName + " [" + tModelKey + "]";
            } else {
                // generate the tooltip text
                text = uddiNamedEntity.getPolicyUrl();
            }

            return text;
        }
    }

    public static class UDDINamedEntity {
        private final String key;
        private final String name;
        private final String wsdlUrl;
        private final String policyUrl;

        public UDDINamedEntity(String key, String name) {
            this(key, name, null, null);
        }

        public UDDINamedEntity(String key, String name, String policyUrl, String wsdlUrl) {
            this.key = key;
            this.name = name;
            this.policyUrl = policyUrl;
            this.wsdlUrl = wsdlUrl;
        }

        public String getKey() {
            return key;
        }

        public String getName() {
            return name;
        }

        public String getPolicyUrl() {
            return policyUrl;
        }

        public String getWsdlUrl() {
            return wsdlUrl;
        }

        @Override
        @SuppressWarnings({"RedundantIfStatement"})
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UDDINamedEntity that = (UDDINamedEntity) o;

            if (key != null ? !key.equals(that.key) : that.key != null) return false;
            if (name != null ? !name.equals(that.name) : that.name != null) return false;
            if (policyUrl != null ? !policyUrl.equals(that.policyUrl) : that.policyUrl != null) return false;
            if (wsdlUrl != null ? !wsdlUrl.equals(that.wsdlUrl) : that.wsdlUrl != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = (key != null ? key.hashCode() : 0);
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (wsdlUrl != null ? wsdlUrl.hashCode() : 0);
            result = 31 * result + (policyUrl != null ? policyUrl.hashCode() : 0);
            return result;
        }
    }
}
