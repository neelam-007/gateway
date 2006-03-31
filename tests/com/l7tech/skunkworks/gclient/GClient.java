package com.l7tech.skunkworks.gclient;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.SoapMessageGenerator;
import com.l7tech.common.xml.Wsdl;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.*;
import java.net.URL;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * Generic Client for SOAP.
 *
 * Builds sample messages based on discovered service information (from WSDL)
 *
 * @author $Author$
 * @version $Revision$
 */
public class GClient {
    private Color defaultTextAreaBg;

    //- PUBLIC

    public GClient() {
        JFrame frame = new JFrame("GClient v0.1");
        frame.setContentPane(mainPanel);
        defaultTextAreaBg = responseTextArea.getBackground();

        // listeners
        buildListeners();

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

    private Wsdl wsdl;
    private Service service;
    private Port port;
    private BindingOperation operation;
    private SoapMessageGenerator.Message[] requestMessages;

    private void buildListeners() {
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
        sendButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
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

    private void openLocation(Component parent) {
        GClientLocationDialog locationChooser = new GClientLocationDialog((Frame)SwingUtilities.windowForComponent(parent));
        locationChooser.pack();
        Utilities.centerOnScreen(locationChooser);
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

    /**
     * Send the message text to the selected uri (if it parses)
     */
    private void sendMessage() {
        String soapAction = soapActionTextField.getText();
        String targetUrl = urlTextField.getText();
        String message = requestTextArea.getText();

        GenericHttpClient client = null;
        GenericHttpRequest request = null;
        GenericHttpResponse response = null;
        InputStream responseIn = null;
        try {
            Document requestDocument = XmlUtil.stringToDocument(message);
            byte[] requestBytes = XmlUtil.nodeToFormattedString(requestDocument).getBytes("UTF-8");

            client = new CommonsHttpClient(new MultiThreadedHttpConnectionManager());
            GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(targetUrl));
            String login = loginField.getText();
            char[] password = passwordField.getPassword();
            String ntlmDomain = ntlmDomainField.getText();
            String ntlmHost = ntlmHostField.getText();
            if (login != null && password != null) {
                if (ntlmDomain != null) {
                    String host = ntlmHost;
                    if (host == null) host = InetAddress.getLocalHost().getHostName();
                    params.setNtlmAuthentication(new NtlmAuthentication(login, password, ntlmDomain, host));
                } else {
                    params.setPasswordAuthentication(new PasswordAuthentication(login, password));
                }
            }
            params.setContentType(ContentTypeHeader.XML_DEFAULT);
            //params.setContentType("application/soap+xml");
            params.setExtraHeaders(new HttpHeader[]{new GenericHttpHeader(SoapUtil.SOAPACTION, soapAction)});
            request = client.createRequest(GenericHttpClient.POST, params);
            request.setInputStream(new ByteArrayInputStream(requestBytes));
            if (params.getNtlmAuthentication() == null && params.getPasswordAuthentication() == null) {
                params.setContentLength(new Long(requestBytes.length));
            }
            response = request.getResponse();
            statusLabel.setText(Integer.toString(response.getStatus()));
            lengthLabel.setText(response.getContentLength().toString());
            ContentTypeHeader type = response.getContentType();
            ctypeLabel.setText(String.valueOf(type == null ? null : type.getFullValue()));
            if (type == null) type = ContentTypeHeader.TEXT_DEFAULT;
            responseIn = response.getInputStream();
            String responseText = new String(HexUtils.slurpStream(responseIn), type.getEncoding());
            responseTextArea.setText(responseText);
            try {
                responseTextArea.setText(XmlUtil.nodeToFormattedString(XmlUtil.stringToDocument(responseText)));
            } catch (SAXException e) {
                // Oh well, leave as just text
            }
            responseTextArea.setBackground(defaultTextAreaBg);
        }
        catch(Exception e) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(baos));
            e.printStackTrace();
            responseTextArea.setText(baos.toString());
            responseTextArea.setBackground(new Color(255, 192, 192));
        }
        finally {
            ResourceUtils.closeQuietly(responseIn);
            if(response!=null) try {response.close(); }catch(Exception e){}
            if(request!=null) try {request.close(); }catch(Exception e){}
        }
    }
}
