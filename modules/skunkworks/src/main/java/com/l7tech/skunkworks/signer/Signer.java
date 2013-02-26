package com.l7tech.skunkworks.signer;

import com.japisoft.fastparser.node.SimpleNode;
import com.japisoft.xmlpad.LocationEvent;
import com.japisoft.xmlpad.LocationListener;
import com.japisoft.xmlpad.XMLContainer;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.util.XMLContainerFactory;
import com.l7tech.gui.util.GuiCertUtil;
import com.l7tech.gui.util.GuiPasswordCallbackHandler;
import com.l7tech.gui.util.Utilities;
import com.l7tech.message.Message;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.NamespaceContextImpl;
import com.l7tech.util.ResourceUtils;
import com.l7tech.xml.xpath.XpathUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility for signing messages.
 *
 * <p>Can be used to generate messages with multiple signatures, with signing
 * of selected message parts.</p>
 */
public class Signer extends JDialog {
    private JPanel contentPane;
    private JButton buttonClose;
    private JPanel viewerPanel;
    private JButton signButton;
    private JLabel clientCertLabel;
    private JButton selectButton;
    private JButton loadButton;
    private JCheckBox signTimestampCheckBox;
    private JButton expandAllButton;
    private JButton importClipboardButton;
    private JButton exportClipboardButton;
    private JList signList;
    private JButton clearSigningListButton;
    private JCheckBox protectTokensCheckBox;
    private JComboBox keyReferenceType;
    private JCheckBox useEncryptedKeyCheckBox;

    private XMLContainer viewer;
    private Document document;
    private Set<String> elementsToSign = new LinkedHashSet<String>();
    private X509Certificate clientCertificate;
    private PrivateKey clientPrivateKey;

    public Signer() throws Exception {
        setTitle("WS-Security Signing Utility 0.3");
        setContentPane(contentPane);
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        Utilities.setEscKeyStrokeDisposes(this);

        keyReferenceType.setModel(new DefaultComboBoxModel( KeyInfoInclusionType.values() ));
        keyReferenceType.setSelectedItem( KeyInfoInclusionType.CERT );

        buttonClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        selectButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                onLoadCert();
            }
        });

        loadButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                onLoad();
            }
        });

        expandAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                /* Not supported */
            }
        });

        signButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSign();
            }
        });

        clearSigningListButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onClearSigningList();
            }
        });

        importClipboardButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Transferable trans = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
                    if ( trans != null ) {
                        Object obj = trans.getTransferData(DataFlavor.selectBestTextFlavor(trans.getTransferDataFlavors()));
                        if ( obj instanceof String ) {
                            document = XmlUtil.parse((String)obj);
                            viewer.getAccessibility().setText(XmlUtil.nodeToString(document));
                            onClearSigningList();
                        }
                    }
                } catch (Exception ex) {
                    throw ExceptionUtils.wrap(ex);
                }
            }
        });

        exportClipboardButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ( document != null ) {
                    try {
                        StringSelection data = new StringSelection(XmlUtil.nodeToString(document));
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(data, data);
                    } catch (IOException ex) {
                        throw ExceptionUtils.wrap(ex);
                    }
                }
            }
        });

        viewer = XMLContainerFactory.createXmlContainer(true);
        viewer.getAccessibility().setText("<empty/>");
        viewer.setEnabledTreeLocation(true);
        viewer.getUIAccessibility().setPopupAvailable(false);
        viewer.getUIAccessibility().setTreePopupAvailable(false);
        viewer.setEditableDocumentMode(false);
        viewer.setEditable(false);
        viewer.addLocationListener(new LocationListener() {
            @Override
            public void locationChanged(LocationEvent locationEvent) {
                SimpleNode node = locationEvent.getDocumentLocation();
                if (node == null || SimpleNode.TAG_NODE != node.getType())
                    return;
                final StringBuilder builder = new StringBuilder();
                buildPath(builder, new HashMap<String, String>(), node);
                String loc = builder.toString();

                elementsToSign.add(loc);
                DefaultListModel model = new DefaultListModel();
                for (String elementPath : elementsToSign) {
                    model.addElement(elementPath);
                }
                signList.setModel(model);
            }
        });
        viewerPanel.setLayout(new BorderLayout());
        viewerPanel.add( viewer.getView() );

        Utilities.setDoubleClickAction( signList, new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                doEditSignListXpath();
            }
        }, signList, null);
    }

    private void doEditSignListXpath() {
        String xpath = (String) signList.getSelectedValue();
        String updatedXpath = (String) JOptionPane.showInputDialog( this, "Element XPath", "Edit XPath", JOptionPane.QUESTION_MESSAGE, null, null, xpath );
        if ( updatedXpath != null ) {
            DefaultListModel model = new DefaultListModel();
            Set<String> updatedElementsToSign = new LinkedHashSet<String>();

            for ( String signXpath : elementsToSign ) {
                if ( signXpath.equals(xpath) ) {
                    signXpath = updatedXpath;
                }
                updatedElementsToSign.add( signXpath );
            }
                        
            for ( String elementPath : updatedElementsToSign ) {
                model.addElement( elementPath );
            }

            signList.setModel( model );
            elementsToSign = updatedElementsToSign;
        }

    }

    private void onLoad() {
        JFileChooser fileChooser = new JFileChooser();
        int returnVal = fileChooser.showOpenDialog(this);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            File selected = fileChooser.getSelectedFile();

            FileInputStream fis = null;
            try {
                fis = new FileInputStream(selected);
                document = XmlUtil.parse(fis);
                viewer.getAccessibility().setText(XmlUtil.nodeToString(document));
                onClearSigningList();
            }
            catch(Exception e) {
                throw ExceptionUtils.wrap(e);
            }
            finally {
                ResourceUtils.closeQuietly(fis);
            }
        }
    }

    private void onLoadCert() {
        GuiCertUtil.ImportedData data = GuiCertUtil.importCertificate(this, false, new GuiPasswordCallbackHandler());
        if (data != null) {
            clientCertificate = data.getCertificate();
            clientPrivateKey = data.getPrivateKey();
            clientCertLabel.setText(CertUtils.extractFirstCommonNameFromCertificate(clientCertificate));
        } else {
            clientCertificate = null;
            clientPrivateKey = null;
            clientCertLabel.setText("");
        }
    }

    private void onClearSigningList() {
        elementsToSign.clear();
        signList.setModel( new DefaultListModel() );
    }

    private void onSign() {
        if ( document != null ) {
            try {
                final Set<Element> elements = new LinkedHashSet<Element>();
                final XPathFactory xpf = XPathFactory.newInstance();
                final Map<String,String> docns = DomUtils.findAllNamespaces(document.getDocumentElement());

                for ( String xpathExpression : elementsToSign ) {
                    XPath xpath = xpf.newXPath();
                    xpath.setNamespaceContext( new NamespaceContextImpl(docns) );
                    NodeList nodeList = (NodeList) xpath.evaluate( xpathExpression, document, XPathConstants.NODESET );
                    for ( int n=0; n<nodeList.getLength(); n++ ) {
                        Node node = nodeList.item(n);
                        if ( node.getNodeType() == Node.ELEMENT_NODE ) {
                            elements.add( (Element) node );
                        }
                    }
                }

                onClearSigningList();

                signDocument(
                        document,
                        signTimestampCheckBox.isSelected(),
                        protectTokensCheckBox.isSelected(),
                        useEncryptedKeyCheckBox.isSelected(),
                        elements,
                        clientCertificate,
                        clientPrivateKey,
                        (KeyInfoInclusionType) keyReferenceType.getSelectedObjects()[0] );

                viewer.getAccessibility().setText(XmlUtil.nodeToString(document));
            } catch (Exception e) {
                throw ExceptionUtils.wrap(e);
            }
        }
    }

    private void signDocument( final Document document,
                               final boolean signTimeStamp,
                               final boolean protectTokens,
                               final boolean useEncryptedKey,
                               final Set<Element> elements,
                               final X509Certificate clientCertificate,
                               final PrivateKey clientPrivateKey,
                               final KeyInfoInclusionType keyInfoInclusionType ) throws Exception {
        DecorationRequirements decReq = new DecorationRequirements();
        decReq.setTimestampTimeoutMillis(3600000);
        decReq.setKeyInfoInclusionType(keyInfoInclusionType);
        decReq.setIncludeTimestamp(signTimeStamp);
        decReq.getElementsToSign().addAll(elements);
        decReq.setProtectTokens(protectTokens);
        if ( useEncryptedKey ) {
            decReq.setEncryptedKey(new byte[32]); // nice and secure ..
            decReq.setRecipientCertificate(clientCertificate);
        } else {
            decReq.setSenderMessageSigningCertificate(clientCertificate);
            decReq.setSenderMessageSigningPrivateKey(clientPrivateKey);
        }
        decReq.setSecurityHeaderReusable(true);
        decReq.setSecurityHeaderActor(null);
        new WssDecoratorImpl().decorateMessage( new Message(document), decReq );
    }

    private void buildPath( final StringBuilder builder,
                            final Map<String, String> namespaces,
                            final SimpleNode element ) {

        if ( element.getSimpleParent() != null ) {
            buildPath( builder, namespaces, element.getSimpleParent() );
        }

        builder.append( '/' );

        String prefix = element.getNameSpacePrefix();
        String namespace = element.getNameSpaceURI();

        String nodeName = element.getNodeContent();
        String nodeQname = element.getQualifiedContent();

        if ( namespace == null ) {
            builder.append( nodeName );
        } else if ( prefix == null || !namespace.equals(namespaces.get(prefix)) ) {
            if ( namespaces.containsValue( namespace )) {
                builder.append( findPrefix(namespaces, namespace) );
                builder.append( ':' );
                builder.append( nodeName );
            } else {
                builder.append( "*[local-name()='" );
                builder.append( nodeName ); // name cannot contain "'"
                builder.append( "' and namespace-uri()=" );
                builder.append( XpathUtil.literalExpression(namespace) );
                builder.append( ']' );
            }
        } else {
            builder.append( nodeQname );
        }
    }


    private String findPrefix( final Map<String,String> namespaces, final String namespace ) {
        String prefix = null;

        for ( final Map.Entry<String,String> entry : namespaces.entrySet() ) {
            if ( namespace.equals(entry.getValue()) ) {
                prefix = entry.getKey();
                break;
            }
        }

        return prefix;
    }

    public static void main(String[] args) throws Exception {
        Signer dialog = new Signer();
        dialog.pack();
        Utilities.centerOnScreen(dialog);
        dialog.setVisible(true);
        System.exit(0);
    }
}
