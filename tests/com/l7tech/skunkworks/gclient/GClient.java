package com.l7tech.skunkworks.gclient;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.GuiCertUtil;
import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.CertUtils;
import com.l7tech.console.util.SoapMessageGenerator;
import com.l7tech.common.xml.Wsdl;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.wsdl.BindingOperation;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.xml.soap.SOAPException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.KeyManager;
import javax.net.ssl.X509KeyManager;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.PasswordCallback;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.URL;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.Socket;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.security.cert.X509Certificate;
import java.security.PrivateKey;
import java.security.Principal;

/**
 * Generic Client for SOAP.
 *
 * Builds sample messages based on discovered service information (from WSDL)
 *
 * @author $Author$
 * @version $Revision$
 */
public class GClient {

    //- PUBLIC

    public GClient() {
        final JFrame frame = new JFrame("GClient v0.3");
        frame.setContentPane(mainPanel);
        defaultTextAreaBg = responseTextArea.getBackground();

        // controls
        buildControls();

        // listeners
        buildListeners(frame);

        // do menus
        buildMenus(frame);

        // display
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        Utilities.centerOnScreen(frame);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        new GClient();
    }

    //- PRIVATE

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


    //
    private final Color defaultTextAreaBg;
    private final Color errorColor = ERROR_COLOR;
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

    private boolean formatRequest() {
        return reformatRequestMessageCheckbox.isSelected();
    }

    private boolean formatResponse() {
        return reformatResponseMessageCheckBox.isSelected();
    }

    private boolean validateBeforeSend() {
        return validateBeforeSendCheckBox.isSelected();
    }

    private void buildListeners(final JFrame frame) {
        serviceComboBox.addItemListener(new ItemListener(){
            public void itemStateChanged(ItemEvent e) {
                updateServiceSelection();
            }
        });
        portComboBox.addItemListener(new ItemListener(){
            public void itemStateChanged(ItemEvent e) {
                updatePortSelection();
            }
        });
        operationComboBox.addItemListener(new ItemListener(){
            public void itemStateChanged(ItemEvent e) {
                updateOperationSelection();
            }
        });
        certButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                loadCert(frame);
            }
        });
        sendButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        requestTextArea.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                updateRequest();
            }
        });
        reformatRequestMessageCheckbox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateRequest();
            }
         });
        reformatResponseMessageCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateResponse();
            }
        });
    }

    private void buildControls() {
        contentTypeComboBox.setModel(new DefaultComboBoxModel(CONTENT_TYPES));
        threadSpinner.setValue(new Integer(1));
        requestSpinner.setValue(new Integer(1));
    }

    private void buildMenus(final JFrame frame) {
        JMenuBar mb = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(new AbstractAction("Open Location ..."){
            public void actionPerformed(ActionEvent e) {
                openLocation(frame);
            }
        });
        fileMenu.add(new AbstractAction("Open File ..."){
            public void actionPerformed(ActionEvent e) {
                openFile(frame);
            }
        });
        fileMenu.addSeparator();
        fileMenu.add(new AbstractAction("Exit"){
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
            }
        });
        mb.add(fileMenu);
        frame.setJMenuBar(mb);
    }

    private void setWsdl(Wsdl wsdl) {
        this.wsdl = wsdl;
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
        for (Iterator iterator = services.iterator(); iterator.hasNext();) {
            Service service = (Service) iterator.next();
            serviceComboBox.addItem(service.getQName().getLocalPart());
        }

        if(services.size()==1) {
            updateServiceSelection();
        }
    }

    private void updateServiceSelection() {
        if(wsdl!=null) {
            Collection services = wsdl.getServices();
            String selected = (String) serviceComboBox.getSelectedItem();

            if(selected!=null) {
                for (Iterator servIterator = services.iterator(); servIterator.hasNext();) {
                    Service service = (Service) servIterator.next();
                    String serviceName = service.getQName().getLocalPart();

                    if(serviceName.equals(selected)) {
                        this.service = service;

                        portComboBox.removeAllItems();
                        Set entries = service.getPorts().keySet();
                        for (Iterator iterator = entries.iterator(); iterator.hasNext();) {
                            String portName = (String) iterator.next();
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
                    for (Iterator iterator = ops.iterator(); iterator.hasNext();) {
                        BindingOperation bo = (BindingOperation) iterator.next();
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
                for (Iterator iterator = ops.iterator(); iterator.hasNext();) {
                    BindingOperation bo = (BindingOperation) iterator.next();
                    if(selectedOperation.equals(bo.getName())) {
                        operation = bo;
                    }
                }
            }

            updateRequestMessage();
        }
    }

    private void updateRequestMessage() {
        if(operation!=null && requestMessages!=null) {
            for(int m=0; m<requestMessages.length; m++) {
                SoapMessageGenerator.Message message = requestMessages[m];

                java.util.List elements = port.getExtensibilityElements();
                for (Iterator ite = elements.iterator(); ite.hasNext();) {
                    Object o = ite.next();
                    if (o instanceof SOAPAddress) {
                        SOAPAddress sa = (SOAPAddress)o;
                        urlTextField.setText(sa.getLocationURI());
                    }
                }

                elements = operation.getExtensibilityElements();
                for (Iterator ite = elements.iterator(); ite.hasNext();) {
                    Object o = ite.next();
                    if (o instanceof SOAPOperation) {
                        SOAPOperation sop = (SOAPOperation)o;
                        soapActionTextField.setText(sop.getSoapActionURI());
                    }
                }

                if(message.getBinding().equals(port.getBinding().getQName().getLocalPart())
                && message.getOperation().equals(operation.getName())) {
                    try {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        message.getSOAPMessage().writeTo(bos);
                        String messageText = bos.toString();
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
        Utilities.setAlwaysOnTop(locationChooser, true);
        locationChooser.setVisible(true);
        if(locationChooser.wasOk()) {
            String uri = locationChooser.getOpenLocation();

            InputStream is = null;
            try {
                URL url = new URL(uri);
                is = new URL(uri).openStream();
                setWsdl(Wsdl.newInstance(url.toString(), new InputStreamReader(is, "UTF-8")));
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            finally {
                ResourceUtils.closeQuietly(is);
            }
        }
    }

    private void openFile(Component parent) {
        JFileChooser fileChooser = new JFileChooser();

        FileFilter filter = new FileFilter(){
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".wsdl");
            }

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
                setWsdl(Wsdl.newInstance(selected.toURI().toString(), new InputStreamReader(fis, "UTF-8")));
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
            SSLContext sslContext = SSLContext.getInstance("SSL");
            X509TrustManager trustManager = new PermissiveX509TrustManager();
            KeyManager[] keyManagers = null;
            if (clientPrivateKey != null) {
                keyManagers = new KeyManager[] {
                    getKeyManager(clientPrivateKey, clientCertificate)
                };
            }
            sslContext.init(keyManagers, new X509TrustManager[] {trustManager}, null);
            sslSocketFactory = sslContext.getSocketFactory();
        }
        return sslSocketFactory;
    }

    private KeyManager getKeyManager(final PrivateKey privateKey, final X509Certificate certificate) {
        return new X509KeyManager() {
            public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
                return "alias";
            }

            public String chooseServerAlias(String string, Principal[] principals, Socket socket) {
                throw new RuntimeException("This key manager is for clients only.");
            }

            public X509Certificate[] getCertificateChain(String string) {
                return new X509Certificate[]{certificate};
            }

            public String[] getClientAliases(String string, Principal[] principals) {
                return new String[]{"alias"};
            }

            public PrivateKey getPrivateKey(String string) {
                return privateKey;
            }

            public String[] getServerAliases(String string, Principal[] principals) {
                throw new RuntimeException("This key manager is for clients only.");
            }
        };
    }

    /**
     * Load a client certificate
     */
    private void loadCert(final JFrame frame) {
        GuiCertUtil.ImportedData data = GuiCertUtil.importCertificate(frame, true, getCallbackHandler());
        if (data != null) {
            clientCertificate = data.getCertificate();
            clientPrivateKey = data.getPrivateKey();
            sslSocketFactory = null;
            clientCertLabel.setText(CertUtils.extractCommonNameFromClientCertificate(clientCertificate));
        }
        else {
            clientCertificate = null;
            clientPrivateKey = null;
            sslSocketFactory = null;
            clientCertLabel.setText("");
        }
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

        try {
            boolean isFastInfoset = contentType.indexOf("fastinfoset") > 0;
            byte[] requestBytes = null;
            if(validateBeforeSend() || isFastInfoset) {
                Document requestDocument = XmlUtil.stringToDocument(message);

                if (!isFastInfoset) {
                    requestBytes = XmlUtil.nodeToFormattedString(requestDocument).getBytes("UTF-8");
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
                requestBytes = message.getBytes("UTF-8");
            }

            int threads = ((Integer)threadSpinner.getValue()).intValue();
            final int requests = ((Integer)requestSpinner.getValue()).intValue();

            boolean isJms = targetUrl.startsWith("jms:/");

            if(threads==1) {
                if (isJms && requests > 1) throw new Exception("JMS not supported for multiple requests.");
                final long startTime = System.currentTimeMillis();
                for(int i=0; i<requests; i++) {
                    String[] responseData = !isJms ?
                            doMessage(soapAction, targetUrl, cookies, requestBytes) :
                            doJmsMessage(targetUrl, requestBytes);
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
                        clientLocal.set(null);
                    }
                }
                System.out.println("Processing requests took "+(System.currentTimeMillis()-startTime)+"ms.");
            }
            else {
                if (isJms) throw new Exception("JMS not supported for multiple threads.");
                final ThreadGroup requestGroup = new ThreadGroup("GClientRequests");
                final Thread[] requestThreads = new Thread[threads];
                final byte[] requestData = requestBytes;
                final long startTime = System.currentTimeMillis();
                for(int t=0; t<threads; t++) {
                    requestThreads[t] = new Thread(requestGroup, new Runnable(){
                        public void run() {
                            for(int i=0; i<requests; i++) {
                                String[] responseData = doMessage(soapAction, targetUrl, null, requestData);
                                if(responseData==null) {
                                    System.out.println("Request thread exiting after error!");
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
                    for (int t = 0; t < requestThreads.length; t++) {
                        Thread thread = requestThreads[t];
                        if(thread.isAlive()) liveCount++;
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
            JmsClient client = new JmsClient(new URI(targetUrl), credentials);
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
    private String[] doMessage(String soapAction, String targetUrl, String cookies, byte[] requestBytes) {

        GenericHttpClient client = (GenericHttpClient) clientLocal.get();
        if(client==null) {
            client = new CommonsHttpClient(new SimpleHttpConnectionManager(), 30000, 30000);
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
                if(contentType.indexOf("charset") < 0 && contentType.indexOf("fastinfoset") < 0) {
                    contentType += "; charset=\"UTF-8\"";
                }
                params.setContentType(ContentTypeHeader.parseValue(contentType));
            }
            if (cookies != null && cookies.length() > 0) {
                params.setExtraHeaders(new HttpHeader[]{
                        new GenericHttpHeader(SoapUtil.SOAPACTION, soapAction),
                        new GenericHttpHeader("Cookie", cookies),
                });
            } else {
                params.setExtraHeaders(new HttpHeader[]{
                        new GenericHttpHeader(SoapUtil.SOAPACTION, soapAction),
                });
            }
            if(params.getTargetUrl().getProtocol().equals("https")) {
                params.setSslSocketFactory(getSSLSocketFactory());
            }

            request = client.createRequest(GenericHttpClient.POST, params);
            request.setInputStream(new ByteArrayInputStream(requestBytes));
            if (params.getNtlmAuthentication() == null && params.getPasswordAuthentication() == null) {
                params.setContentLength(new Long(requestBytes.length));
            }
            response = request.getResponse();
            statusLabel.setText(Integer.toString(response.getStatus()));
            final Long clen = response.getContentLength();
            lengthLabel.setText(clen == null ? "(null)" : clen.toString());
            ContentTypeHeader type = response.getContentType();
            if (type == null) type = ContentTypeHeader.TEXT_DEFAULT;
            responseIn = response.getInputStream();
            String responseText = new String(HexUtils.slurpStream(responseIn), type.getEncoding());

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
            if(response!=null) try {response.close(); }catch(Exception e){}
            if(request!=null) try {request.close(); }catch(Exception e){}
        }

        return null;
    }

    /**
     * Get the cookies name/value pairs as a string
     */
    private String getCookies(final URL url, final HttpHeaders headers) {
        StringBuffer cookieBuffer = new StringBuffer();

        Collection cookies = headers.getValues("Set-Cookie");
        for (Iterator cookieIter=cookies.iterator(); cookieIter.hasNext();) {
            String cookieStatement = (String) cookieIter.next();
            try {
                HttpCookie cookie = new HttpCookie(url, cookieStatement);
                cookieBuffer.append(cookie.getCookieName());
                cookieBuffer.append('=');
                cookieBuffer.append(cookie.getCookieValue());
                cookieBuffer.append(';');
            }
            catch(HttpCookie.IllegalFormatException ife) {                
                ife.printStackTrace();
            }
        }

        return cookieBuffer.toString();
    }

    /**
     *
     */
    private CallbackHandler getCallbackHandler() {
        return new CallbackHandler() {
            public void handle(Callback[] callbacks) {
                PasswordCallback passwordCallback = null;

                for (int i = 0; i < callbacks.length; i++) {
                    Callback callback = callbacks[i];
                    if (callback instanceof PasswordCallback) {
                        passwordCallback = (PasswordCallback) callback;
                    }
                }

                if (passwordCallback != null) {
                    final JPasswordField pwd = new JPasswordField(22);
                    JOptionPane pane = new JOptionPane(pwd, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
                    JDialog dialog = pane.createDialog(null, "Enter " + passwordCallback.getPrompt());
                    dialog.addWindowFocusListener(new WindowAdapter(){
                        public void windowGainedFocus(WindowEvent e) {
                            pwd.requestFocusInWindow();
                        }
                    });
                    dialog.setVisible(true);
                    dialog.dispose();
                    Object value = pane.getValue();
                    if (value != null && ((Integer)value).intValue() == JOptionPane.OK_OPTION)
                        passwordCallback.setPassword(pwd.getPassword());
                }
            }
        };
    }
}
