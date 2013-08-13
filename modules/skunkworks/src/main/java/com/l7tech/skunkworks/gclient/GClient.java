package com.l7tech.skunkworks.gclient;

import com.l7tech.common.http.*;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.http.prov.apache.components.HttpComponentsClient;
import com.l7tech.common.io.*;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gui.util.GuiCertUtil;
import com.l7tech.gui.util.GuiPasswordCallbackHandler;
import com.l7tech.gui.util.Utilities;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.proxy.NullRequestInterceptor;
import com.l7tech.proxy.RequestInterceptor;
import com.l7tech.proxy.datamodel.*;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.ClientPolicyFactory;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientDecorator;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.util.*;
import com.l7tech.wsdl.ResourceTrackingWSDLLocator;
import com.l7tech.wsdl.SerializableWSDLLocator;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.wsdl.PrettyGoodWSDLLocator;
import com.l7tech.wsdl.WsdlEntityResolver;
import com.l7tech.xml.soap.SoapMessageGenerator;
import com.l7tech.xml.soap.SoapUtil;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.net.ssl.*;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.wsdl.BindingOperation;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.UnknownExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.extensions.soap12.SOAP12Address;
import javax.wsdl.extensions.soap12.SOAP12Operation;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import java.awt.*;
import java.awt.event.*;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Generic Client for SOAP.
 *
 * Builds sample messages based on discovered service information (from WSDL)
 *
 * @noinspection BoundFieldAssignment,JavaDoc
 */
public class GClient {
    protected static final Logger logger = Logger.getLogger(GClient.class.getName());

    //- PUBLIC

    public GClient() {
        frame = new JFrame("GClient v0.9.6");
        frame.setContentPane(mainPanel);
        defaultTextAreaBg = responseTextArea.getBackground();

        // controls
        buildControls();

        // listeners
        buildListeners(frame, true);

        // do menus
        buildMenus(frame);

    }

    public static void main(String[] args) {
        new GClient().show();
    }

    public void show() {
        // display
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        Utilities.centerOnScreen(frame);
        frame.setVisible(true);
    }

    //- PRIVATE

    private final Functions.UnaryThrows<InputSource, URL, IOException> permissiveUrlGetter = new Functions.UnaryThrows<InputSource, URL, IOException>() {
        @Override
        public InputSource call(URL url) throws IOException {
            URLConnection conn = url.openConnection();
            if (conn instanceof HttpsURLConnection) {
                HttpsURLConnection httpsURLConnection = (HttpsURLConnection)conn;
                try {
                    httpsURLConnection.setSSLSocketFactory(getSSLSocketFactory());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                httpsURLConnection.setHostnameVerifier(new PermissiveHostnameVerifier());
            }
            return new InputSource(conn.getInputStream());
        }
    };


    //
    private static final String[] CONTENT_TYPES = new String[]{"text/xml", "application/soap+xml", "text/plain", "application/fastinfoset", "application/soap+fastinfoset", "application/octet-stream", "application/x-www-form-urlencoded"};
    private static final Color ERROR_COLOR = new Color(255, 192, 192);

    // form members
    private JPanel mainPanel;
    private JButton sendButton;
    private JTextField soapActionTextField;
    private JTextField urlTextField;
    private JComboBox serviceComboBox;
    private JComboBox portComboBox;
    private JComboBox operationComboBox;
    private JTextArea requestTextArea;
    private JTextArea responseTextArea;
    private JLabel statusLabel;
    private JLabel lengthLabel;
    private JLabel ctypeLabel;
    private JTextField loginField;
    private JPasswordField passwordField;
    private JTextField ntlmDomainField;
    private JTextField ntlmHostField;
    private JButton certButton;
    private JLabel clientCertLabel;
    private JCheckBox reformatRequestMessageCheckbox;
    private JCheckBox reformatResponseMessageCheckBox;
    private JCheckBox validateBeforeSendCheckBox;
    private JComboBox contentTypeComboBox;
    private JSpinner threadSpinner;
    private JSpinner requestSpinner;
    private JTextField cookiesTextField;
    private JCheckBox soap11Checkbox;
    private JCheckBox soap12Checkbox;
    private JButton decorationButton;
    private JButton stripDecorationButton;
    private JButton serverCertButton;
    private JLabel serverCertLabel;
    @SuppressWarnings({ "UnusedDeclaration" })
    private JRadioButton textMessageRadioButton;
    private JRadioButton bytesMessageRadioButton;
    private JCheckBox gzipCheckBox;
    private JTextField httpHeaderValueTextField;
    private JTextField httpHeaderNameTextField;


    //
    private final Color defaultTextAreaBg;
    private final Color errorColor = ERROR_COLOR;
    private SerializableWSDLLocator wsdlLocator;
    private Wsdl wsdl;
    private Service service;
    private Port port;
    private BindingOperation operation;
    private SoapMessageGenerator.Message[] requestMessages;
    private String rawResponse;
    private String formattedResponse;
    private SSLSocketFactory sslSocketFactory;
    private X509Certificate clientCertificate;
    private PrivateKey clientPrivateKey;
    private String policyXml;
    private JFrame frame;
    private X509Certificate serverCertificate;


    private boolean formatRequest() {
        return reformatRequestMessageCheckbox.isSelected();
    }

    private boolean formatResponse() {
        return reformatResponseMessageCheckBox.isSelected();
    }

    private boolean validateBeforeSend() {
        return validateBeforeSendCheckBox.isSelected();
    }

    private void buildListeners(final JFrame frame, final boolean addAll ) {
        serviceComboBox.addItemListener(new ItemListener(){
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateServiceSelection();
            }
        });
        portComboBox.addItemListener(new ItemListener(){
            @Override
            public void itemStateChanged(ItemEvent e) {
                updatePortSelection();
            }
        });
        operationComboBox.addItemListener(new ItemListener(){
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateOperationSelection();
            }
        });

        if ( addAll ) {
            certButton.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    loadCert(frame);
                }
            });
            serverCertButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    loadServerCert(frame);
                }
            });
            sendButton.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    sendMessage();
                }
            });
            decorationButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    decorateMessage();
                }
            });
            stripDecorationButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stripDecorations();
                }
            });
        }
        requestTextArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateRequest();
            }
        });
        reformatRequestMessageCheckbox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateRequest();
            }
         });
        reformatResponseMessageCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateResponse();
            }
        });
    }

    private void stripDecorations() {
        try {
            Document got = XmlUtil.stringToDocument(requestTextArea.getText());

            Element sec = SoapUtil.getSecurityElement(got, "secure_span");
            if (sec == null) return;
            sec.getParentNode().removeChild(sec);

            requestTextArea.setText(XmlUtil.nodeToString(got));
            clearThrowable();

        } catch (SAXException e1) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e1), e1);
            displayThrowable(e1);
        } catch (InvalidDocumentFormatException e1) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e1), e1);
            displayThrowable(e1);
        } catch (IOException e1) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e1), e1);
            displayThrowable(e1);
        }
    }

    private void buildControls() {
        contentTypeComboBox.setModel(new DefaultComboBoxModel(CONTENT_TYPES));
        threadSpinner.setValue(1);
        requestSpinner.setValue(1);
    }

    private void buildMenus(final JFrame frame) {
        JMenuBar mb = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(new AbstractAction("Open Location ..."){
            @Override
            public void actionPerformed(ActionEvent e) {
                openLocation(frame);
            }
        });
        fileMenu.add(new AbstractAction("Save Location ..."){
            @Override
            public void actionPerformed(ActionEvent e) {
                saveLocation(frame);
            }
        });
        fileMenu.add(new AbstractAction("Open File ..."){
            @Override
            public void actionPerformed(ActionEvent e) {
                openFile(frame);
            }
        });
        fileMenu.add(new AbstractAction("Open Configuration...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                openConfiguration(frame);
            }
        });
        fileMenu.add(new AbstractAction("Save Configuration...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveConfiguration(frame);
            }
        });
        fileMenu.add(new AbstractAction("Load Request Message ...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadRequestMessage(frame);
            }
        });
        fileMenu.addSeparator();
        fileMenu.add(new AbstractAction("Exit"){
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
            }
        });
        mb.add(fileMenu);
        frame.setJMenuBar(mb);
    }

    private void setWsdl(Wsdl wsdl, SerializableWSDLLocator wsdlLocator) {
        this.wsdl = wsdl;
        this.wsdlLocator = wsdlLocator;
        this.service = null;
        this.port = null;
        this.operation = null;
        serviceComboBox.removeAllItems();
        portComboBox.removeAllItems();
        operationComboBox.removeAllItems();

        SoapMessageGenerator generator = new SoapMessageGenerator();
        try {
            requestMessages = generator.generateRequests(wsdl);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        updateView();
    }

    private void updateView() {
        Collection services = wsdl.getServices();
        for (Object service1 : services) {
            Service service = (Service)service1;
            serviceComboBox.addItem(service.getQName().getLocalPart());
        }

        if(services.size()==1) {
            serviceComboBox.setSelectedIndex( 0 );
            updateServiceSelection();
        }
    }

    private void updateServiceSelection() {
        if(wsdl!=null) {
            Collection services = wsdl.getServices();
            String selected = (String) serviceComboBox.getSelectedItem();

            if(selected!=null) {
                for (Object service1 : services) {
                    Service service = (Service)service1;
                    String serviceName = service.getQName().getLocalPart();

                    if (serviceName.equals(selected)) {
                        this.service = service;

                        portComboBox.removeAllItems();
                        Set entries = service.getPorts().keySet();
                        for (Object entry : entries) {
                            String portName = (String)entry;
                            portComboBox.addItem(portName);
                        }
                    }
                }
            }
        }
    }

    private void updatePortSelection() {
        if(service!=null) {
            String selectedPort = (String) portComboBox.getSelectedItem();

            if(selectedPort!=null) {
                Port port = service.getPort(selectedPort);

                if(port!=null && port.getBinding()!=null && port.getBinding().getBindingOperations()!=null) {
                    this.port = port;

                    operationComboBox.removeAllItems();
                    java.util.List ops = port.getBinding().getBindingOperations();
                    for (Object op : ops) {
                        BindingOperation bo = (BindingOperation)op;
                        operationComboBox.addItem(bo.getName());
                    }
                }
            }
        }
    }

    private void updateOperationSelection() {
        if(port!=null) {
            String selectedOperation = (String) operationComboBox.getSelectedItem();

            if(selectedOperation!=null) {
                java.util.List ops = port.getBinding().getBindingOperations();
                for (Object op : ops) {
                    BindingOperation bo = (BindingOperation)op;
                    if (selectedOperation.equals(bo.getName())) {
                        operation = bo;
                    }
                }
            }

            updateRequestMessage();
        }
    }

    private void decorateMessage() {
        GClientDecorationDialog dlg = new GClientDecorationDialog(frame);
        dlg.setModal(true);
        dlg.setTitle("Decoration policy");
        if (policyXml != null)
            dlg.setPolicyXml(policyXml);
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);

        if (dlg.isConfirmed()) {
            this.reformatRequestMessageCheckbox.setSelected(false);
            this.validateBeforeSendCheckBox.setSelected(false);

            this.policyXml = dlg.getPolicyXml();

            try {
                final String requestString = requestTextArea.getText();
                if (requestString.trim().length() < 1)
                    throw new IllegalArgumentException("Request xml is empty");

                Managers.getAssertionRegistry();
                Assertion assertion = WspReader.getDefault().parsePermissively(policyXml, WspReader.INCLUDE_DISABLED);
                logger.info("Decorating with policy: \n" + assertion.toString());
                ClientAssertion clientAssertion = ClientPolicyFactory.getInstance().makeClientPolicy(assertion);

                Ssg ssg = new Ssg(new Random().nextLong());
                ssg.getRuntime().setSsgKeyStoreManager(new MySsgKeyStoreManager());
                ssg.getRuntime().setCredentialManager(new CredentialManagerImpl() {
                    @Override
                    public PasswordAuthentication getCredentials(Ssg ssg) {
                        return new PasswordAuthentication(loginField.getText(), passwordField.getPassword());
                    }
                });

                Message request = new Message();
                request.initialize(XmlUtil.stringToDocument(requestString.trim()));

                Message response = new Message();
                RequestInterceptor nri = NullRequestInterceptor.INSTANCE;
                PolicyAttachmentKey pak = new PolicyAttachmentKey("/", "\"\"", "/");
                URL origurl = new URL("http://localhost:7700/");
                PolicyApplicationContext context = new PolicyApplicationContext(ssg, request, response, nri, pak, origurl);
                AssertionStatus result = clientAssertion.decorateRequest(context);
                if (result != null) {
                    //noinspection unchecked
                    Collection<ClientDecorator> deferred = context.getPendingDecorations().values();
                    for (ClientDecorator clientDecorator : deferred) {
                        result = clientDecorator.decorateRequest(context);
                        if (!AssertionStatus.NONE.equals(result))
                            break;
                    }
                }
                if (!AssertionStatus.NONE.equals(result))
                    throw new RuntimeException("Decoration failed: " + result.toString());

                Document document = request.getXmlKnob().getDocumentWritable();
                final Message mess = new Message(document);
                for (DecorationRequirements wssReq : context.getAllDecorationRequirements())
                    new WssDecoratorImpl().decorateMessage(mess, wssReq);

                requestTextArea.setText(XmlUtil.nodeToString(document));
                clearThrowable();

            } catch (Exception e) {
                logger.log(Level.WARNING, "Decoration failed", e);
                displayThrowable(e);
            }
        }
    }

    /** @noinspection ForLoopReplaceableByForEach*/
    private void updateRequestMessage() {
        if(operation!=null && requestMessages!=null) {
            for(int m=0; m<requestMessages.length; m++) {
                SoapMessageGenerator.Message message = requestMessages[m];

                java.util.List elements = port.getExtensibilityElements();
                for (Iterator ite = elements.iterator(); ite.hasNext();) {
                    ExtensibilityElement ee = (ExtensibilityElement) ite.next();
                    if (ee instanceof SOAPAddress) {
                        SOAPAddress sa = (SOAPAddress)ee;
                        urlTextField.setText(sa.getLocationURI());
                    }
                    else if (ee instanceof SOAP12Address) {
                        SOAP12Address sa = (SOAP12Address)ee;
                        urlTextField.setText(sa.getLocationURI());
                    }
                    else if (ee.getElementType().equals(new QName("http://schemas.xmlsoap.org/wsdl/soap12/","address"))
                            && ee instanceof UnknownExtensibilityElement) {
                        UnknownExtensibilityElement uee = (UnknownExtensibilityElement) ee;
                        urlTextField.setText(uee.getElement().getAttribute("location"));
                    }
                }

                elements = operation.getExtensibilityElements();
                for (Iterator ite = elements.iterator(); ite.hasNext();) {
                    ExtensibilityElement ee = (ExtensibilityElement) ite.next();
                    if (ee instanceof SOAPOperation) {
                        SOAPOperation sop = (SOAPOperation)ee;
                        soapActionTextField.setText(sop.getSoapActionURI());
                    }
                    else if (ee instanceof SOAP12Operation) {
                        SOAP12Operation sop = (SOAP12Operation)ee;
                        soapActionTextField.setText(sop.getSoapActionURI());
                    }
                    else if (ee.getElementType().equals(new QName("http://schemas.xmlsoap.org/wsdl/soap12/","operation"))
                            && ee instanceof UnknownExtensibilityElement) {
                        UnknownExtensibilityElement uee = (UnknownExtensibilityElement) ee;
                        soapActionTextField.setText(uee.getElement().getAttribute("soapAction"));
                    }
                }

                if(message.getBinding().equals(port.getBinding().getQName().getLocalPart())
                && message.getOperation().equals(operation.getName())) {
                    try {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        message.getSOAPMessage().writeTo(bos);
                        String messageText = bos.toString();
                        if (soap12Checkbox.isSelected()) {
                            messageText = messageText.replaceAll("http://schemas.xmlsoap.org/soap/envelope/", "http://www.w3.org/2003/05/soap-envelope");
                        }
                        Document messageDoc = XmlUtil.stringToDocument(messageText);
                        requestTextArea.setText(XmlUtil.nodeToFormattedString(messageDoc));
                    }
                    catch(IOException ioe) {
                        ioe.printStackTrace();
                    }
                    catch(SAXException se) {
                        se.printStackTrace();
                    }
                    catch(SOAPException se) {
                        se.printStackTrace();
                    }
                }
            }
        }
    }

    private void updateRequest() {
        if(formatRequest()) {
            try {
                Document messageDoc = XmlUtil.stringToDocument(requestTextArea.getText());
                requestTextArea.setText(XmlUtil.nodeToFormattedString(messageDoc));
                clearThrowable();
            }
            catch(Exception e) {
                e.printStackTrace();
                displayThrowable(e);
            }
        }
    }

    private void updateResponse() {
        clearThrowable();
        if(formatResponse()) {
            responseTextArea.setText(formattedResponse);
        }
        else {
            responseTextArea.setText(rawResponse);
        }
        responseTextArea.getCaret().setDot(0);
    }

    private void displayThrowable(Throwable throwable) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        throwable.printStackTrace(new PrintStream(baos));
        responseTextArea.setText(baos.toString());
        responseTextArea.setBackground(errorColor);
        responseTextArea.getCaret().setDot(0);
    }

    private void clearThrowable() {
        if(responseTextArea.getBackground().equals(errorColor)) {
            responseTextArea.setText("");
            responseTextArea.setBackground(defaultTextAreaBg);
        }
    }

    private void openLocation(Component parent) {
        GClientLocationDialog locationChooser = new GClientLocationDialog((Frame)SwingUtilities.windowForComponent(parent));
        locationChooser.pack();
        Utilities.centerOnScreen(locationChooser);
        if (locationChooser.getOwner() == null)
            Utilities.setAlwaysOnTop(locationChooser, true);
        locationChooser.setVisible(true);
        if(locationChooser.wasOk()) {
            String uri = locationChooser.getOpenLocation();

            try {
                final URL baseUrl = new URL(uri);
                final ResourceTrackingWSDLLocator locator = new ResourceTrackingWSDLLocator( new PrettyGoodWSDLLocator(baseUrl, permissiveUrlGetter), true, false, false );
                setWsdl(Wsdl.newInstance(locator), Wsdl.getWSDLLocator( uri, locator.getResourceMap(), logger ) );
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void saveLocation(Component parent) {
        GClientLocationDialog locationChooser = new GClientLocationDialog((Frame)SwingUtilities.windowForComponent(parent));
        locationChooser.pack();
        Utilities.centerOnScreen(locationChooser);

        if (locationChooser.getOwner() == null)
            Utilities.setAlwaysOnTop(locationChooser, true);
        locationChooser.setVisible(true);

        if(locationChooser.wasOk()) {
            String baseUri = locationChooser.getOpenLocation();

            try {
                DocumentReferenceProcessor processor = new DocumentReferenceProcessor();
                EntityResolver entityResolver = new WsdlEntityResolver(true);
                Map<String,String> urisToResources =
                        processor.processDocument( baseUri, new GClientResourceResolver(entityResolver) );

                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
                int returnVal = fileChooser.showSaveDialog(frame);
                if(returnVal == JFileChooser.APPROVE_OPTION) {
                    File selected = fileChooser.getSelectedFile();

                    for ( final String uri : urisToResources.keySet() ) {
                        System.out.println( filename(uri, ".wsdl") + " :- " + uri );
                        Document document = XmlUtil.parse( new InputSource( new StringReader(urisToResources.get( uri )) ){{setSystemId(uri);}}, false );
                        processor.processDocumentReferences( document, new DocumentReferenceProcessor.ReferenceCustomizer(){
                            @Override
                            public String customize( final Document document,
                                                     final Node node,
                                                     final String documentUrl,
                                                     final DocumentReferenceProcessor.ReferenceInfo referenceInfo ) {
                                if ( referenceInfo.getReferenceUrl()==null ) {
                                    return null; // e.g. namespace only XML Schema reference
                                }
                                return filename( referenceInfo.getReferenceUrl(), ".wsdl" );
                            }
                        } );
                        File file = new File( selected, filename(uri, ".wsdl") );
                        FileOutputStream out = null;
                        try {
                            out = new FileOutputStream(file);
                            XmlUtil.nodeToOutputStream( document, out );
                        } finally {
                            ResourceUtils.closeQuietly( out );
                        }
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String filename( final String uri, final String extension ) {
        String filename = uri;

        int queryIndex = filename.indexOf( '?' );
        if ( queryIndex > 0 ) {
            final String query = filename.substring( queryIndex+1 );
            filename = filename.substring( 0, queryIndex );
            if ( query.matches( ".*=[a-zA-Z0-9_\\-\\.]+" ) ) {
                filename += query.substring( query.lastIndexOf( '=' )+1 );
            }
        }

        int dirIndex = filename.lastIndexOf( '/' );
        if ( dirIndex > 0 ) {
            filename = filename.substring( dirIndex+1 );
        }

        if ( filename.indexOf('.') < 0 ) {
            filename = filename + extension;    
        }

        return filename;
    }

    private void saveConfiguration(JFrame frame) {
        JFileChooser fileChooser = new JFileChooser();

        FileFilter filter = new FileFilter(){
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".gclient");
            }

            @Override
            public String getDescription() {
                return "GClient configuration files (*.gclient)";
            }
        };
        fileChooser.setFileFilter(filter);
        int returnVal = fileChooser.showSaveDialog(frame);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            File selected = fileChooser.getSelectedFile();

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(selected);
                XMLEncoder enc = new XMLEncoder(fos);
                enc.writeObject(GClient.this);
                enc.close();
            } catch (FileNotFoundException e) {
                displayThrowable(e);
            } finally {
                ResourceUtils.closeQuietly(fos);
            }
        }
    }

    private void openConfiguration(JFrame frame) {
        JFileChooser fileChooser = new JFileChooser();

        FileFilter filter = new FileFilter(){
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".gclient");
            }

            @Override
            public String getDescription() {
                return "GClient configuration files (*.gclient)";
            }
        };
        fileChooser.setFileFilter(filter);
        int returnVal = fileChooser.showOpenDialog(frame);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            File selected = fileChooser.getSelectedFile();

            FileInputStream fis = null;
            try {
                fis = new FileInputStream(selected);
                XMLDecoder dec = new XMLDecoder(fis);
                Object obj = dec.readObject();
                if (obj instanceof GClient) {
                    GClient gClient = (GClient)obj;
                    gClient.buildListeners( gClient.frame, false );
                    gClient.show();
                    GClient.this.frame.dispose();
                }
            }
            catch(Exception e) {
                displayThrowable(e);
            }
            finally {
                ResourceUtils.closeQuietly(fis);
            }
        }
    }

    private void loadRequestMessage(JFrame frame) {
        JFileChooser fileChooser = new JFileChooser();

        int returnVal = fileChooser.showOpenDialog(frame);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            File selected = fileChooser.getSelectedFile();

            FileInputStream fis = null;
            try {
                fis = new FileInputStream(selected);
                requestTextArea.setText(new String( IOUtils.slurpStream(fis, 5000000)));
                requestTextArea.setCaretPosition(0);
            }
            catch(Exception e) {
                displayThrowable(e);
            }
            finally {
                ResourceUtils.closeQuietly(fis);
            }
        }

    }

    private void openFile(Component parent) {
        JFileChooser fileChooser = new JFileChooser();

        FileFilter filter = new FileFilter(){
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".wsdl");
            }

            @Override
            public String getDescription() {
                return "WSDL files (*.wsdl)";
            }
        };
        fileChooser.setFileFilter(filter);
        int returnVal = fileChooser.showOpenDialog(parent);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            File selected = fileChooser.getSelectedFile();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(selected);
                final InputSource source = new InputSource();
                source.setSystemId(selected.toURI().toString());
                source.setByteStream(fis);
                final ResourceTrackingWSDLLocator locator = new ResourceTrackingWSDLLocator( Wsdl.getWSDLLocator( source, true ), true, false, false );
                setWsdl(Wsdl.newInstance(locator), Wsdl.getWSDLLocator( selected.toURI().toString(), locator.getResourceMap(), logger ) );
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            finally {
                ResourceUtils.closeQuietly(fis);
            }
        }
    }

    private SSLSocketFactory getSSLSocketFactory() throws Exception {
        if (sslSocketFactory == null) {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyManager[] keyManagers = null;
            if (clientPrivateKey != null) {
                keyManagers = new KeyManager[] {
                    getKeyManager(clientPrivateKey, clientCertificate)
                };
            }
            sslContext.init(keyManagers, new X509TrustManager[] { new PermissiveX509TrustManager() }, null);
            sslSocketFactory = sslContext.getSocketFactory();
        }
        return sslSocketFactory;
    }

    private KeyManager getKeyManager(final PrivateKey privateKey, final X509Certificate certificate) {
        return new X509KeyManager() {
            @Override
            public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
                return "alias";
            }

            @Override
            public String chooseServerAlias(String string, Principal[] principals, Socket socket) {
                throw new RuntimeException("This key manager is for clients only.");
            }

            @Override
            public X509Certificate[] getCertificateChain(String string) {
                return new X509Certificate[]{certificate};
            }

            @Override
            public String[] getClientAliases(String string, Principal[] principals) {
                return new String[]{"alias"};
            }

            @Override
            public PrivateKey getPrivateKey(String string) {
                return privateKey;
            }

            @Override
            public String[] getServerAliases(String string, Principal[] principals) {
                throw new RuntimeException("This key manager is for clients only.");
            }
        };
    }

    /**
     * Load a client certificate
     */
    private void loadCert(final JFrame frame) {
        GuiCertUtil.ImportedData data = GuiCertUtil.importCertificate(frame, true, new GuiPasswordCallbackHandler());
        if (data != null) {
            clientCertificate = data.getCertificate();
            clientPrivateKey = data.getPrivateKey();
            sslSocketFactory = null;
            clientCertLabel.setText(CertUtils.extractFirstCommonNameFromCertificate(clientCertificate));
        }
        else {
            clientCertificate = null;
            clientPrivateKey = null;
            sslSocketFactory = null;
            clientCertLabel.setText("");
        }
    }

    private void loadServerCert(JFrame frame) {
        GuiCertUtil.ImportedData data = GuiCertUtil.importCertificate(frame, false, new GuiPasswordCallbackHandler());
        if (data != null) {
            serverCertificate = data.getCertificate();
            serverCertLabel.setText( CertUtils.extractFirstCommonNameFromCertificate(serverCertificate));
        } else {
            serverCertificate = null;
            serverCertLabel.setText("");
        }
    }

    private byte[] compress( final byte[] data ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gout = new GZIPOutputStream(baos);
        gout.write(data);
        gout.close();
        return baos.toByteArray();
    }

    /**
     * Send the message text to the selected uri (if it parses)
     */
    private void sendMessage() {
        // clear existing output
        formattedResponse = "";
        rawResponse = "";
        updateResponse();

        final String soapAction = soapActionTextField.getText();
        final String targetUrl = urlTextField.getText();
        final String message = requestTextArea.getText();
        final String contentType = (String) contentTypeComboBox.getSelectedItem();
        final String cookies = cookiesTextField.getText();
        final boolean gzip = gzipCheckBox.isSelected();

        try {
            boolean isFastInfoset = contentType.indexOf("fastinfoset") > 0;
            byte[] requestBytes;
            if(validateBeforeSend() || isFastInfoset) {
                Document requestDocument = XmlUtil.stringToDocument(message);

                if (!isFastInfoset) {
                    requestBytes = XmlUtil.nodeToFormattedString(requestDocument).getBytes(Charsets.UTF8);
                }
                else {
                    com.sun.xml.fastinfoset.dom.DOMDocumentSerializer ser =
                            new com.sun.xml.fastinfoset.dom.DOMDocumentSerializer();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
                    ser.setOutputStream(baos);
                    ser.serialize(requestDocument);
                    requestBytes = baos.toByteArray();
                }
            }
            else {
                requestBytes = message.getBytes(Charsets.UTF8);
            }

            if ( gzip ) {
                requestBytes = compress( requestBytes );
            }

            int threads = (Integer)threadSpinner.getValue();
            final int requests = (Integer)requestSpinner.getValue();

            boolean isJms = targetUrl.startsWith("jms:/");
            boolean isFtp = targetUrl.startsWith("ftp:/");

            if(threads==1) {
                if (isJms && requests > 1) throw new Exception("JMS not supported for multiple requests.");
                if (isFtp && requests > 1) throw new Exception("FTP not supported for multiple requests.");
                final long startTime = System.currentTimeMillis();
                for(int i=0; i<requests; i++) {
                    String[] responseData;
                    if ( isJms ) {
                        responseData = doJmsMessage(targetUrl, requestBytes);
                    } else if ( isFtp ) {
                        responseData = doFtpMessage(targetUrl, requestBytes);
                    } else {
                        responseData = doMessage(soapAction, targetUrl, cookies, requestBytes, gzip);
                    }
                    if(responseData!=null) {
                        statusLabel.setText(responseData[0]);
                        lengthLabel.setText(responseData[1]);
                        ctypeLabel.setText(responseData[2]);
                        if (responseData[4]!=null && responseData[4].length()>0)
                            cookiesTextField.setText(responseData[4]);
                        rawResponse = responseData[3];
                        try {
                            formattedResponse = XmlUtil.nodeToFormattedString(XmlUtil.stringToDocument(responseData[3]));
                        } catch (SAXException e) {
                            // Oh well, set as just text
                            formattedResponse = rawResponse;
                        }
                        updateResponse();
                    }
                    else {
                        // reset
                        //noinspection unchecked
                        clientLocal.set(null);
                    }
                }
                System.out.println("Processing requests took "+(System.currentTimeMillis()-startTime)+"ms.");
            }
            else {
                if (isJms) throw new Exception("JMS not supported for multiple threads.");
                if (isFtp) throw new Exception("FTP not supported for multiple threads.");
                final ThreadGroup requestGroup = new ThreadGroup("GClientRequests");
                final Thread[] requestThreads = new Thread[threads];
                final byte[] requestData = requestBytes;
                final long startTime = System.currentTimeMillis();
                for(int t=0; t<threads; t++) {
                    requestThreads[t] = new Thread(requestGroup, new Runnable(){
                        @Override
                        public void run() {
                            for(int i=0; i<requests; i++) {
                                String[] responseData = doMessage(soapAction, targetUrl, null, requestData, gzip);
                                if(responseData==null) {
                                    System.out.println("Request thread exiting after error!");
                                    //noinspection unchecked
                                    clientLocal.set(null);
                                    break;
                                }
                            }
                        }
                    });
                    requestThreads[t].start();
                }
                while(requestGroup.activeCount()>0) {
                    int liveCount = 0;
                    for (Thread thread : requestThreads) {
                        if (thread.isAlive()) liveCount++;
                    }
                    if(liveCount==0) break;
                    Thread.sleep(50);
                }
                System.out.println("Processing requests took "+(System.currentTimeMillis()-startTime)+"ms.");
            }
        }
        catch(Exception e) {
            e.printStackTrace();
            displayThrowable(e);
        }
    }

    private String[] doJmsMessage(String targetUrl, byte[] requestBytes) {
        try {
            PasswordAuthentication credentials = null;
            if (loginField.getText() != null && loginField.getText().length() > 0) {
                credentials = new PasswordAuthentication(loginField.getText(), passwordField.getPassword());
            }

            boolean isBytes = bytesMessageRadioButton.isSelected();
            JmsClient client = new JmsClient(new URI(targetUrl), credentials, isBytes);
            String response = client.getResponse(new String(requestBytes), true);
            if (response != null) {
                return new String[]{"0",
                                    Integer.toString(response.length()),
                                    "",
                                    response,
                                    ""};
            }
        }
        catch(Exception e) {
            e.printStackTrace();
            displayThrowable(e);
        }

        return null;
    }

    private String[] doFtpMessage(String targetUrl, byte[] requestBytes) {
        OutputStream os = null;
        try {
            String ftpUrl = targetUrl;
            PasswordAuthentication credentials = null;
            if (loginField.getText() != null && loginField.getText().length() > 0) {
                credentials = new PasswordAuthentication(loginField.getText(), passwordField.getPassword());
            }

            boolean isBytes = bytesMessageRadioButton.isSelected();

            if ( credentials != null ) {
                ftpUrl = ftpUrl.replaceAll( "ftp://", "ftp://" + credentials.getUserName() + ":" + new String(credentials.getPassword()) + "@" );
            }

            URL url = new URL(ftpUrl);
            URLConnection urlc = url.openConnection();
            urlc.setConnectTimeout( 30000 );
            urlc.setReadTimeout( 60000 );
            urlc.setRequestProperty( "type", isBytes ? "i" : "a" );
            os = urlc.getOutputStream();
            os.write( requestBytes );
            os.flush();
        }
        catch(Exception e) {
            e.printStackTrace();
            displayThrowable(e);
        }
        finally {
            ResourceUtils.closeQuietly( os );
        }

        return null;
    }

    /**
     * Send the message text to the selected uri (if it parses)
     * /
    GenericHttpClient client = null;
    {
        SimpleHttpConnectionManager mhcm = new SimpleHttpConnectionManager();
        //mhcm.setMaxConnectionsPerHost(100);
        //mhcm.setMaxTotalConnections(100);
        client = new CommonsHttpClient(mhcm);
    }*/
    ThreadLocal clientLocal = new ThreadLocal();
    private String[] doMessage(String soapAction, String targetUrl, String cookies, final byte[] requestBytes, final boolean gzip) {

        GenericHttpClient client = (GenericHttpClient) clientLocal.get();
        if(client==null) {
            client = new HttpComponentsClient(new BasicClientConnectionManager(), 30000, 30000);
            //noinspection unchecked
            clientLocal.set(client);
        }
        GenericHttpRequest request = null;
        GenericHttpResponse response = null;
        InputStream responseIn = null;
        try {
            GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(targetUrl));
            String login = loginField.getText();
            char[] password = passwordField.getPassword();
            String ntlmDomain = ntlmDomainField.getText();
            String ntlmHost = ntlmHostField.getText();
            if (login != null && login.length() > 0 && password != null) {
                if (ntlmDomain != null && ntlmDomain.trim().length() > 0) {
                    String host = ntlmHost;
                    if (host == null) host = InetAddress.getLocalHost().getHostName();
                    params.setNtlmAuthentication(new NtlmAuthentication(login, password, ntlmDomain, host));
                } else {
                    params.setPasswordAuthentication(new PasswordAuthentication(login, password));
                }
            }
            String contentType = (String) contentTypeComboBox.getSelectedItem();
            if(contentType.length() > 0) {
                if (soap12Checkbox.isSelected()) {
                    contentType += "; action=" + soapAction;
                }
                if(contentType.indexOf("charset") < 0 && contentType.indexOf("fastinfoset") < 0) {
                    contentType += "; charset=\"UTF-8\"";
                }
                params.setContentType(ContentTypeHeader.parseValue(contentType));
            }
            //noinspection unchecked
            boolean setContentLength = true;
            Collection<GenericHttpHeader> headers = new ArrayList();
            if (cookies != null && cookies.trim().length() > 0) {
                headers.add(new GenericHttpHeader("Cookie", cookies));
            }
            if (soap11Checkbox.isSelected()) {
                headers.add(new GenericHttpHeader(SoapUtil.SOAPACTION, soapAction));
            }
            if (gzip) {
                headers.add(new GenericHttpHeader("Content-Encoding", "gzip"));
            }
            if (!httpHeaderNameTextField.getText().trim().isEmpty()) {
                headers.add(new GenericHttpHeader(httpHeaderNameTextField.getText().trim(), httpHeaderValueTextField.getText()));
                if ( "Transfer-Encoding".equalsIgnoreCase(httpHeaderNameTextField.getText().trim()) &&
                     "chunked".equalsIgnoreCase(httpHeaderValueTextField.getText())) {
                    setContentLength = false;
                }
                if ( "Host".equalsIgnoreCase(httpHeaderNameTextField.getText().trim()) ) {
                    params.setVirtualHost(httpHeaderValueTextField.getText());
                }
            }
            if (!headers.isEmpty())
                params.setExtraHeaders(headers.toArray(new GenericHttpHeader[headers.size()]));

            if(params.getTargetUrl().getProtocol().equals("https")) {
                params.setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String s, SSLSession sslSession) {
                        return true;
                    }
                });
                params.setSslSocketFactory(getSSLSocketFactory());
            }

            if ( setContentLength ) {
                params.setContentLength((long)requestBytes.length);
            }

            request = client.createRequest(HttpMethod.POST, params);
            if (request instanceof RerunnableHttpRequest) {
                RerunnableHttpRequest reRequest = (RerunnableHttpRequest) request;
                reRequest.setInputStreamFactory(new RerunnableHttpRequest.InputStreamFactory(){
                    @Override
                    public InputStream getInputStream() {
                        return new ByteArrayInputStream(requestBytes);
                    }
                });
            } else {
                request.setInputStream(new ByteArrayInputStream(requestBytes));
            }
            response = request.getResponse();
            statusLabel.setText(Integer.toString(response.getStatus()));
            final Long clen = response.getContentLength();
            lengthLabel.setText(clen == null ? "(null)" : clen.toString());
            ContentTypeHeader type = response.getContentType();
            if (type == null) type = ContentTypeHeader.TEXT_DEFAULT;
            responseIn = response.getInputStream();
            String responseText = "gzip".equals(response.getHeaders().getFirstValue("Content-Encoding")) ?
                    new String(IOUtils.slurpStream(new GZIPInputStream(responseIn)), type.getEncoding()) :
                    new String(IOUtils.slurpStream(responseIn), type.getEncoding());

            return new String[]{response==null ? "" : Integer.toString(response.getStatus()),
                                response==null || response.getContentLength()==null ? "" : response.getContentLength().toString(),
                                String.valueOf(type == null ? null : type.getFullValue()),
                                responseText,
                                getCookies(params.getTargetUrl(), response.getHeaders())};
        }
        catch(Exception e) {
            e.printStackTrace();
            displayThrowable(e);
        }
        finally {
            ResourceUtils.closeQuietly(responseIn);
            if(response!=null) //noinspection EmptyCatchBlock
                try {response.close(); }catch(Exception e){}
            if(request!=null) //noinspection EmptyCatchBlock
                try {request.close(); }catch(Exception e){}
        }

        return null;
    }

    /**
     * Get the cookies name/value pairs as a string
     */
    private String getCookies(final URL url, final HttpHeaders headers) {
        StringBuffer cookieBuffer = new StringBuffer();

        Collection cookies = headers.getValues("Set-Cookie");
        for (Object cooky : cookies) {
            String cookieStatement = (String)cooky;
            try {
                HttpCookie cookie = new HttpCookie(url, cookieStatement);
                cookieBuffer.append(cookie.getCookieName());
                cookieBuffer.append('=');
                cookieBuffer.append(cookie.getCookieValue());
                cookieBuffer.append(';');
            }
            catch (HttpCookie.IllegalFormatException ife) {
                ife.printStackTrace();
            }
        }

        return cookieBuffer.toString();
    }

    private class MySsgKeyStoreManager extends SsgKeyStoreManager {
        @Override
        public boolean isClientCertUnlocked() throws KeyStoreCorruptException {
            return true;
        }

        @Override
        public void deleteClientCert() throws IOException, KeyStoreException, KeyStoreCorruptException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean deleteStores() {
            return false;
        }

        @Override
        public void saveSsgCertificate(X509Certificate cert) throws KeyStoreException, IOException, KeyStoreCorruptException, CertificateException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveClientCertificate(PrivateKey privateKey, X509Certificate cert, char[] privateKeyPassword) throws KeyStoreException, IOException, KeyStoreCorruptException, CertificateException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void obtainClientCertificate(PasswordAuthentication credentials) throws BadCredentialsException, GeneralSecurityException, KeyStoreCorruptException, CertificateAlreadyIssuedException, IOException, ServerFeatureUnavailableException {
            throw new UnsupportedOperationException("A client certificate must be configured to use this decoration policy.");
        }

        @Override
        public String lookupClientCertUsername() {
            return null;
        }

        @Override
        protected X509Certificate getServerCert() throws KeyStoreCorruptException {
            return serverCertificate;
        }

        @Override
        protected X509Certificate getClientCert() throws KeyStoreCorruptException {
            return clientCertificate;
        }

        @Override
        public PrivateKey getClientCertPrivateKey(PasswordAuthentication passwordAuthentication) throws NoSuchAlgorithmException, BadCredentialsException, OperationCanceledException, KeyStoreCorruptException, HttpChallengeRequiredException {
            return clientPrivateKey;
        }

        @Override
        protected boolean isClientCertAvailabile() throws KeyStoreCorruptException {
            return true;
        }

        @Override
        protected KeyStore getKeyStore(char[] password) throws KeyStoreCorruptException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected KeyStore getTrustStore() throws KeyStoreCorruptException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void importServerCertificate(File file) throws IOException, CertificateException, KeyStoreCorruptException, KeyStoreException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void importClientCertificate(File certFile, char[] pass, CertUtils.AliasPicker aliasPicker, char[] ssgPassword) throws IOException, GeneralSecurityException, KeyStoreCorruptException, AliasNotFoundException {
            throw new UnsupportedOperationException();
        }
    }

    // Properties that should be saved with a saved configuration

    public String getFormattedResponse() {
        return formattedResponse;
    }

    public void setFormattedResponse(String formattedResponse) {
        this.formattedResponse = formattedResponse;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public String getServerCertificatePem() throws IOException, CertificateEncodingException {
        return serverCertificate == null ? null : CertUtils.encodeAsPEM(serverCertificate);
    }

    public void setServerCertificatePem(String b64) throws IOException, CertificateException {
        this.serverCertificate = b64 == null ? null : CertUtils.decodeFromPEM(b64);
    }

    public String getClientCertificatePem() throws IOException, CertificateEncodingException {
        return clientCertificate == null ? null : CertUtils.encodeAsPEM(clientCertificate);
    }

    public void setClientCertificatePem(String b64) throws IOException, CertificateException {
        this.clientCertificate = b64 == null ? null : CertUtils.decodeFromPEM(b64);
    }

    /** Similar to RSAPrivateKeySpec, but can be saved by java.beans.XMLEncoder */
    public static class SaveableRsaPrivateKey {
        protected BigInteger modulus;
        protected BigInteger privateExponent;

        public SaveableRsaPrivateKey() {
        }

        public SaveableRsaPrivateKey(BigInteger modulus, BigInteger privateExponent) {
            this.modulus = modulus;
            this.privateExponent = privateExponent;
        }

        public String getModulus() {
            return modulus == null ? null : modulus.toString();
        }

        public void setModulus(String modulus) {
            this.modulus = modulus == null ? null : new BigInteger(modulus);
        }

        public String getPrivateExponent() {
            return privateExponent == null ? null : privateExponent.toString();
        }

        public void setPrivateExponent(String privateExponent) {
            this.privateExponent = privateExponent == null ? null : new BigInteger(privateExponent);
        }

        public RSAPrivateKeySpec asRSAPrivateKeySpec() {
            return new RSAPrivateKeySpec(modulus, privateExponent);
        }
    }

    // TODO do we really want to be saving private keys these like this, even in a test tool?  Decisions, decisions
    public SaveableRsaPrivateKey getClientPrivateKey() {
        if (clientPrivateKey == null)
            return null;
        if (clientPrivateKey instanceof RSAPrivateKey) {
            RSAPrivateKey rpk = (RSAPrivateKey)clientPrivateKey;
            return new SaveableRsaPrivateKey(rpk.getModulus(), rpk.getPrivateExponent());
        }
        logger.info("Unable to save non-exportable or non-RSA private key: " + clientPrivateKey.getClass().getName());
        return null;
    }

    public void setClientPrivateKey(SaveableRsaPrivateKey spec) throws InvalidKeySpecException, NoSuchAlgorithmException {
        this.clientPrivateKey = spec == null ? null : KeyFactory.getInstance("RSA").generatePrivate(spec.asRSAPrivateKeySpec());
    }

    public String getPolicyXml() {
        return policyXml;
    }

    public void setPolicyXml(String policyXml) {
        this.policyXml = policyXml;
    }

    public Port getPort() {
        return port;
    }

    public void setPort(Port port) {
        this.port = port;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public JCheckBox getReformatRequestMessageCheckbox() {
        return reformatRequestMessageCheckbox;
    }

    public void setReformatRequestMessageCheckbox(JCheckBox reformatRequestMessageCheckbox) {
        this.reformatRequestMessageCheckbox = reformatRequestMessageCheckbox;
    }

    public JCheckBox getReformatResponseMessageCheckBox() {
        return reformatResponseMessageCheckBox;
    }

    public void setReformatResponseMessageCheckBox(JCheckBox reformatResponseMessageCheckBox) {
        this.reformatResponseMessageCheckBox = reformatResponseMessageCheckBox;
    }

    public JCheckBox getValidateBeforeSendCheckBox() {
        return validateBeforeSendCheckBox;
    }

    public void setValidateBeforeSendCheckBox(JCheckBox validateBeforeSendCheckBox) {
        this.validateBeforeSendCheckBox = validateBeforeSendCheckBox;
    }

    public JCheckBox getSoap11Checkbox() {
        return soap11Checkbox;
    }

    public void setSoap11Checkbox(JCheckBox soap11Checkbox) {
        this.soap11Checkbox = soap11Checkbox;
    }

    public JCheckBox getSoap12Checkbox() {
        return soap12Checkbox;
    }

    public void setSoap12Checkbox(JCheckBox soap12Checkbox) {
        this.soap12Checkbox = soap12Checkbox;
    }

    public JComboBox getContentTypeComboBox() {
        return contentTypeComboBox;
    }

    public void setContentTypeComboBox(JComboBox contentTypeComboBox) {
        this.contentTypeComboBox = contentTypeComboBox;
    }

    public JLabel getServerCertLabel() {
        return serverCertLabel;
    }

    public void setServerCertLabel(JLabel serverCertLabel) {
        this.serverCertLabel = serverCertLabel;
    }

    public JLabel getClientCertLabel() {
        return clientCertLabel;
    }

    public void setClientCertLabel(JLabel clientCertLabel) {
        this.clientCertLabel = clientCertLabel;
    }

    public JTextField getNtlmHostField() {
        return ntlmHostField;
    }

    public void setNtlmHostField(JTextField ntlmHostField) {
        this.ntlmHostField = ntlmHostField;
    }

    public JTextField getNtlmDomainField() {
        return ntlmDomainField;
    }

    public void setNtlmDomainField(JTextField ntlmDomainField) {
        this.ntlmDomainField = ntlmDomainField;
    }

    public JPasswordField getPasswordField() {
        return passwordField;
    }

    public void setPasswordField(JPasswordField passwordField) {
        this.passwordField = passwordField;
    }

    public JTextField getLoginField() {
        return loginField;
    }

    public void setLoginField(JTextField loginField) {
        this.loginField = loginField;
    }

    public JTextArea getRequestTextArea() {
        return requestTextArea;
    }

    public void setRequestTextArea(JTextArea requestTextArea) {
        this.requestTextArea = requestTextArea;
    }

    public JTextArea getResponseTextArea() {
        return responseTextArea;
    }

    public void setResponseTextArea(JTextArea responseTextArea) {
        this.responseTextArea = responseTextArea;
    }

    public JTextField getUrlTextField() {
        return urlTextField;
    }

    public void setUrlTextField(JTextField urlTextField) {
        this.urlTextField = urlTextField;
    }

    public JTextField getSoapActionTextField() {
        return soapActionTextField;
    }

    public void setSoapActionTextField(JTextField soapActionTextField) {
        this.soapActionTextField = soapActionTextField;
    }

    public JSpinner getThreadSpinner() {
        return threadSpinner;
    }

    public void setThreadSpinner(JSpinner threadSpinner) {
        this.threadSpinner = threadSpinner;
    }

    public JSpinner getRequestSpinner() {
        return requestSpinner;
    }

    public void setRequestSpinner(JSpinner requestSpinner) {
        this.requestSpinner = requestSpinner;
    }

    public JTextField getCookiesTextField() {
        return cookiesTextField;
    }

    public void setCookiesTextField(JTextField cookiesTextField) {
        this.cookiesTextField = cookiesTextField;
    }

    public String getSerializedWsdlLocator() {
        String value = null;
        if ( wsdlLocator != null ) {
            ByteArrayOutputStream baos = null;
            ObjectOutputStream out = null;
            try {
                baos = new ByteArrayOutputStream();
                out = new ObjectOutputStream( baos );
                out.writeObject( wsdlLocator );
                value = HexUtils.encodeBase64( baos.toByteArray() );
            } catch ( Exception e ) {
                e.printStackTrace();
            } finally {
                ResourceUtils.closeQuietly( out );
                ResourceUtils.closeQuietly( baos );
            }
        }
        return value;
    }

    public void setSerializedWsdlLocator( final String wsdlStr ) {
        if ( wsdlStr != null && wsdlStr.length() > 0 ) {
            ObjectInputStream in = null;
            try {
                in = new ObjectInputStream( new ByteArrayInputStream( HexUtils.decodeBase64(wsdlStr) ) );
                final SerializableWSDLLocator wsdlLocator = (SerializableWSDLLocator) in.readObject();
                final Wsdl wsdl = Wsdl.newInstance( wsdlLocator );
                SwingUtilities.invokeLater( new Runnable(){
                    @Override
                    public void run() {
                        final String action = soapActionTextField.getText(); // save and restore so setting WSDL doesn't overwrite
                        final String url = urlTextField.getText();
                        final String message = requestTextArea.getText();
                        setWsdl( wsdl, wsdlLocator );
                        soapActionTextField.setText( action );
                        soapActionTextField.setCaretPosition( 0 );
                        urlTextField.setText( url );
                        urlTextField.setCaretPosition( 0 );
                        requestTextArea.setText( message );
                        requestTextArea.setCaretPosition( 0 );
                    }
                } );
            } catch ( Exception e ) {
                e.printStackTrace();
            } finally {
                ResourceUtils.closeQuietly( in );
            }
        }
    }

    private final class GClientResourceResolver implements DocumentReferenceProcessor.ResourceResolver {
        private final EntityResolver entityResolver;

        private GClientResourceResolver( final EntityResolver entityResolver ) {
            this.entityResolver = entityResolver;
        }

        @Override
        public String resolve( final String importLocation ) throws IOException {
            String resource = null;

            final InputSource source = permissiveUrlGetter.call(new URL(importLocation));
            final String resolvedResource = source!=null && source.getByteStream()!=null ?
                    new String(IOUtils.slurpStream(source.getByteStream())): // encoding issues here ...
                    null;
            if ( resolvedResource != null ) {
                resource = ResourceTrackingWSDLLocator.processResource(importLocation, resolvedResource, entityResolver, false, true);
            }

            if ( resource == null) {
                throw new IOException("Resource not found '"+importLocation+"'.");
            }

            return resource;
        }
    }

}
