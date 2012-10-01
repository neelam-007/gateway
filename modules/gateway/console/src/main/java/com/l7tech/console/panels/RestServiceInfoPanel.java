package com.l7tech.console.panels;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.SsmApplication;
import com.l7tech.console.action.ManageHttpConfigurationAction;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.util.ResourceUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * <p>Step two of the {@link PublishRestServiceWizard}.  The content of the page is dynamically choosen depending on the previous screen's selection.</p>
 */
public class RestServiceInfoPanel extends WizardStepPanel {
    private static final Pattern FILE_PATTERN = Pattern.compile("(?i).*(:?wadl|xml)");
    private static final String[] COLUMN_HEADERS = new String[]{"Resource Base URL", "Service Name", "Gateway URI"};
    private JPanel mainPanel;
    private JTextField tfServiceName;
    private JTextField tfBackendUrl;
    private JCheckBox overrideGatewayUrl;
    private JTextField tfGatewayUrl;
    private JTextField tfWadlLocation;
    private JButton btHttpOptions;
    private JButton btFile;
    private JTable serviceDescriptors;
    private JCheckBox useCustomValues;
    private JPanel wadlImportPanel;
    private JPanel manualEntryPanel;
    private JButton btLoadWadl;
    private JLabel gatewayPrefix;

    private DefaultTableModel serviceDescriptorsTableModel;

    private String description;

    public RestServiceInfoPanel(final WizardStepPanel next) {
        super(next);
        initialize();
        intializeManualEntryPanel();
        initializeWadlImportPanel();
    }

    private void initializeWadlImportPanel() {
        serviceDescriptorsTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(final int row, final int column) {
                if(column == 1) return true;
                return column == 2 && useCustomValues.isSelected();
            }
        };
        serviceDescriptors.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        serviceDescriptors.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        serviceDescriptorsTableModel.setColumnIdentifiers(COLUMN_HEADERS);
        serviceDescriptors.setModel(serviceDescriptorsTableModel);
        btHttpOptions.setAction(new ManageHttpConfigurationAction(this));
        btHttpOptions.setText("HTTP Options");
        btHttpOptions.setIcon(null);
        btLoadWadl.setEnabled(false);
        btLoadWadl.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                loadWadl();
            }
        });
        btFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectFile();
            }
        });

        tfWadlLocation.getDocument().addDocumentListener(new DocumentListener() {
            private void enableLoadButton(){
                final String location = tfWadlLocation.getText().trim();
                boolean enabled = false;
                if(!location.isEmpty()){
                    if(isUrl(location)){
                        enabled = true;
                    } else {
                        File f = new File(location);
                        enabled = f.isFile() && f.exists();
                    }
                }
                btLoadWadl.setEnabled(enabled);
            }

            @Override
            public void insertUpdate(final DocumentEvent e) { enableLoadButton(); }

            @Override
            public void removeUpdate(final DocumentEvent e) { enableLoadButton(); }

            @Override
            public void changedUpdate(final DocumentEvent e) { enableLoadButton(); }
        });
    }

    private void intializeManualEntryPanel() {
        overrideGatewayUrl.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                tfGatewayUrl.setEditable(overrideGatewayUrl.isSelected());
            }
        });
        tfBackendUrl.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) { mirrorValue(); }

            @Override
            public void removeUpdate(final DocumentEvent e) { mirrorValue(); }

            @Override
            public void changedUpdate(final DocumentEvent e) { mirrorValue(); }

            private void mirrorValue(){
                if(!tfGatewayUrl.isEditable()){
                    try {
                        URL url = new URL(tfBackendUrl.getText());
                        if(!url.getHost().isEmpty()){
                            String path = url.getPath();
                            tfGatewayUrl.setText(url.getPath().substring(path.isEmpty() ? 0 : 1));
                        }
                    }
                    catch (Exception e){
                        //ignore
                    }
                }
            }
        });
    }

    private void initialize(){
        setLayout(new BorderLayout());
        add(mainPanel);
        gatewayPrefix.setText("http(s)://" + TopComponents.getInstance().ssgURL().getHost() + ":[port]/");
    }

    @Override
    public void readSettings(final Object settings) throws IllegalArgumentException {
        if(settings instanceof PublishRestServiceWizard.RestServiceConfig){
            PublishRestServiceWizard.RestServiceConfig config = (PublishRestServiceWizard.RestServiceConfig)settings;
            boolean manualEntry = config.isManualEntry();
            manualEntryPanel.setVisible(manualEntry);
            wadlImportPanel.setVisible(!manualEntry);
            if(manualEntry){
                description = "Specify a Service Name, Resource Base URL and optionally overriding the default Gateway URI.  The Gateway URI will mimic the path from the Resource Base URL unless overridden.<br/>" +
                        "<br/>* denotes required fields.<br/>" +
                        "<br/>Example:<br/>" +
                        "Service Name: Twitter Search API<br/>" +
                        "Resource Base URL: http://search.twitter.com<br/>" +
                        "Gateway URI: http(s)://" + TopComponents.getInstance().ssgURL().getHost() + ":[port]/ssg/twitter/";
            }
            else {
                description = "Specify the location to a WADL file and click Load to import the REST service endpoint(s).  The Gateway URI must be unique will mimic the Resource Base URL.<br/>" +
                        "<br/>* denotes required fields.<br/>" +
                        "<br/>Example:<br/>" +
                        "Location: c:\\services\\wadl\\echo_service.wadl (Windows)<br/>" +
                        "Location: \\wadl\\echo_service.wadl (Unix/Linux)<br/>" +
                        "Location: http://www.api.layer7.com/services/echo_service.wadl<br/>";
            }
        }
    }

    @Override
    public void storeSettings(final Object settings) throws IllegalArgumentException {
        if(settings instanceof PublishRestServiceWizard.RestServiceConfig){
            PublishRestServiceWizard.RestServiceConfig config = (PublishRestServiceWizard.RestServiceConfig)settings;
            java.util.List<PublishRestServiceWizard.RestServiceInfo> services = new java.util.ArrayList<PublishRestServiceWizard.RestServiceInfo>();
            if(config.isManualEntry()){
               String gatewayUrl = tfGatewayUrl.getText().trim();
               services.add(PublishRestServiceWizard.RestServiceInfo.build(tfServiceName.getText().trim(), tfBackendUrl.getText().trim(), gatewayUrl));
            }
            else {
                for(int i = 0; i < serviceDescriptorsTableModel.getRowCount(); i++){
                    String backendUrl = (String) serviceDescriptorsTableModel.getValueAt(i, 0);
                    String serviceName = (String) serviceDescriptorsTableModel.getValueAt(i, 1);
                    String gatewayUrl = (String) serviceDescriptorsTableModel.getValueAt(i, 2);
                    services.add(PublishRestServiceWizard.RestServiceInfo.build(
                            serviceName.trim(),
                            backendUrl.trim(),
                            gatewayUrl.trim()
                    ));
                }
            }
            config.setServices(services);
        }
    }

    @Override
    public String getStepLabel() {
        return "REST Proxy Configuration";
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean onNextButton() {
        boolean ret = false;
        if(manualEntryPanel.isVisible()){
            ret = handleManualEntry();
        }
        else if(wadlImportPanel.isVisible()){
            ret = handleWadlImport();
        }
        return ret;
    }

    private boolean handleManualEntry(){
        if(tfServiceName.getText().trim().isEmpty()){
            DialogDisplayer.display(new JOptionPane("Service Name Missing"), getOwner().getContentPane(), "Service Name Missing",  null);
            return false;
        }
        if(tfServiceName.getText().trim().length() > 255){
            DialogDisplayer.display(new JOptionPane("Service Name exceed maximum allowable number of characters (255)"), getOwner().getContentPane(), "Invalid Service Name",  null);
            return false;
        }
        if(tfBackendUrl.getText().trim().isEmpty()){
            DialogDisplayer.display(new JOptionPane("Resource Base URL Missing"), getOwner().getContentPane(), "Backend URL Missing",  null);
            return false;
        }

        boolean invalidUrl = false;
        try {
            URL url = new URL(tfBackendUrl.getText().trim());
            if (url.getHost().isEmpty()) {
                invalidUrl = true;
            }
        } catch (Exception e) {
            invalidUrl = true;
        }
        if (invalidUrl) {
            DialogDisplayer.display(new JOptionPane("Invalid Backend URL"), getOwner().getContentPane(), "Invalid Backend URL", null);
            return false;
        }
        if(overrideGatewayUrl.isSelected() && tfGatewayUrl.getText().trim().isEmpty()){
            DialogDisplayer.display(new JOptionPane("Gateway URI can not be empty."), getOwner().getContentPane(), "Invalid Gateway URI", null);
            return false;
        }
        return true;
    }

    private boolean handleWadlImport(){
        if(serviceDescriptorsTableModel.getRowCount() < 1){
            DialogDisplayer.display(new JOptionPane("Please add a REST service or import from a WADL."), getOwner().getContentPane(), "No Service", null);
            return false;
        }
        //check for duplicate Gateway URL, we can't have duplicate
        Set<String> duplicate = new HashSet<String>();
        for(int i = 0; i < serviceDescriptorsTableModel.getRowCount(); i++){
            String value = (String)serviceDescriptorsTableModel.getValueAt(i, 2);
            String serviceName = (String) serviceDescriptorsTableModel.getValueAt(i, 1);
            if(serviceName.trim().length() > 255){
                DialogDisplayer.display(new JOptionPane("Service Name exceed maximum allowable number of characters (255)"), getOwner().getContentPane(), "Invalid Service Name",  null);
                return false;
            }
            if(useCustomValues.isSelected() && (value == null || value.trim().isEmpty())){
                DialogDisplayer.display(new JOptionPane("Gateway URI can not be empty."), getOwner().getContentPane(), "Invalid Gateway URI", null);
                return false;
            }
            if(duplicate.contains(value)){
                DialogDisplayer.display(new JOptionPane("Duplicate Gateway URL '" + value + "' found.  Please ensure all Gateway URLs are unique."), getOwner().getContentPane(), "Duplicate Gateway URL", null);
                return false;
            }
            duplicate.add(value);
        }
        return true;
    }

    private void selectFile() {
        SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            @Override
            public void useFileChooser(JFileChooser fc) {
                doSelectFile(fc);
            }
        });
    }

    private void doSelectFile(JFileChooser fc) {
        fc.setDialogTitle("Select WADL or XML.");
        fc.setDialogType(JFileChooser.OPEN_DIALOG);
        FileFilter fileFilter = new FileFilter() {
            @Override
            public boolean accept(File f) {
                return  f.isDirectory() || FILE_PATTERN.matcher(f.getName()).matches();
            }
            @Override
            public String getDescription() {
                return "(*.wadl/*.xml) REST Service description files.";
            }
        };
        fc.addChoosableFileFilter(fileFilter);
        fc.setMultiSelectionEnabled(false);
        final int r = fc.showDialog(TopComponents.getInstance().getTopParent(), "Open");
        if(r == JFileChooser.APPROVE_OPTION) {
            final File file = fc.getSelectedFile();
            if(file!=null) {
                tfWadlLocation.setText(file.getAbsolutePath());
            }
        }
    }

    private boolean isUrl(final String location){
        boolean isUrl = false;
        try{
            URL url = new URL(location);
            isUrl = (url.getProtocol().equals("http") || url.getProtocol().equals("https")) && !url.getHost().isEmpty();
        }
        catch(Exception e){
            //ignore
        }
        return isUrl;
    }

    private void loadWadl() {
        final String location = tfWadlLocation.getText().trim();
        InputStream is = null;
        try {
            if (isUrl(location)) {
                ServiceAdmin manager = Registry.getDefault().getServiceManager();
                if (manager == null){
                    DialogDisplayer.display(new JOptionPane("Error loading WADL from a remote location."), getOwner().getContentPane(), "Error Loading", null);
                    return;
                }
                String doc = manager.resolveWsdlTarget(location);
                is = new ByteArrayInputStream(doc.getBytes("UTF-8"));
            } else {
                File f = new File(location);
                is = new FileInputStream(f);
            }
            final Document wadl = XmlUtil.parse(is);
            final Node application = wadl.getFirstChild();
            if(application == null || !application.getLocalName().equals("application")){
                DialogDisplayer.display(new JOptionPane("Specified file is not a WADL document."), getOwner().getContentPane(), "Invalid WADL", null);
                return;
            }
            for(int i = serviceDescriptorsTableModel.getRowCount(); i > 0; i--){
                serviceDescriptorsTableModel.removeRow(0);
            }
            for(Node n = application.getFirstChild(); n != null; n = n.getNextSibling()){
                if(n.getNodeType() == Node.ELEMENT_NODE && n.getLocalName().equals("resources") && n.hasAttributes()){
                    NamedNodeMap attributes = n.getAttributes();
                    Node base = attributes.getNamedItem("base");
                    if(base != null){
                        String baseUrl = base.getNodeValue();
                        try{
                            URL url = new URL(baseUrl);
                            String path = url.getPath();
                            if(!path.isEmpty()){
                                path = path.substring(1);
                            }
                            serviceDescriptorsTableModel.addRow(new String[]{baseUrl, baseUrl, path});
                        }
                        catch(Exception e){

                        }
                    }
                }
            }
        } catch (Exception e) {
            DialogDisplayer.display(new JOptionPane("Unable to import WADL at specified location: " + location), getOwner().getContentPane(), "Error Importing WADL", null);
        } finally {
            ResourceUtils.closeQuietly(is);
        }
    }
}
