package com.l7tech.skunkworks.signer;

import com.l7tech.console.xmlviewer.Viewer;
import com.l7tech.console.xmlviewer.XmlElementNode;
import com.l7tech.console.xmlviewer.ExchangerElement;
import com.l7tech.console.xmlviewer.XmlTree;
import com.l7tech.gui.util.GuiCertUtil;
import com.l7tech.gui.util.GuiPasswordCallbackHandler;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.DomUtils;
import com.l7tech.util.NamespaceContextImpl;
import com.l7tech.util.ResourceUtils;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.message.Message;

import javax.swing.*;
import javax.swing.tree.TreePath;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.security.cert.X509Certificate;
import java.security.PrivateKey;
import java.util.*;
import java.io.*;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

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

    private Viewer viewer;
    private Set<String> elementsToSign = new LinkedHashSet<String>();
    private X509Certificate clientCertificate;
    private PrivateKey clientPrivateKey;

    public Signer() throws Exception {
        setTitle("WS-Security Signing Utility 0.2");
        setContentPane(contentPane);
        setModal(true);

        buttonClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onClose();
            }
        });

        // call onClose() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });

        // call onClose() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onClose();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

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
                viewer.expandAll();
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
                            Document document = XmlUtil.parse((String)obj);
                            viewer.setContent( XmlUtil.nodeToString(document) );                            
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
                StringSelection data = new StringSelection(viewer.getContent());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(data, data);                
            }
        });

        viewer = Viewer.createMessageViewer("<empty/>");
        viewer.addDocumentTreeSelectionListener( new TreeSelectionListener(){
            @Override
            public void valueChanged(final TreeSelectionEvent e) {
                TreePath path = ((XmlTree)e.getSource()).getSelectionPath();
                if (path != null) {
                    XmlElementNode node = (XmlElementNode)path.getLastPathComponent();
                    ExchangerElement element = node.getElement();

                    if (element != null) {
                        elementsToSign.add( element.getPath() );
                        DefaultListModel model = new DefaultListModel();
                        for ( String elementPath : elementsToSign ) {
                            model.addElement( elementPath );
                        }
                        signList.setModel( model );
                    }
                }
            }
        } );
        viewerPanel.setLayout(new BorderLayout());
        viewerPanel.add( viewer );
    }

    private void onLoad() {
        JFileChooser fileChooser = new JFileChooser();
        int returnVal = fileChooser.showOpenDialog(this);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            File selected = fileChooser.getSelectedFile();

            FileInputStream fis = null;
            try {
                fis = new FileInputStream(selected);
                Document document = XmlUtil.parse(fis);
                viewer.setContent( XmlUtil.nodeToString(document) );
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
        GuiCertUtil.ImportedData data = GuiCertUtil.importCertificate(this, true, new GuiPasswordCallbackHandler());
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

    private void onClose() {
        dispose();
    }

    private void onClearSigningList() {
        elementsToSign.clear();
        signList.setModel( new DefaultListModel() );
    }

    private void onSign() {
        String content = viewer.getContent();

        try {
            final Document document = XmlUtil.parse(content);
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

            signDocument( document, signTimestampCheckBox.isSelected(), protectTokensCheckBox.isSelected(), elements, clientCertificate, clientPrivateKey );

            viewer.setContent( XmlUtil.nodeToString(document) );
        } catch (Exception e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    private void signDocument( final Document document,
                               final boolean signTimeStamp,
                               final boolean protectTokens,
                               final Set<Element> elements,
                               final X509Certificate clientCertificate,
                               final PrivateKey clientPrivateKey) throws Exception {
        DecorationRequirements decReq = new DecorationRequirements();
        decReq.setTimestampTimeoutMillis(3600000);
        decReq.setIncludeTimestamp(signTimeStamp);
//        if ( !signTimeStamp )
//            decReq.setSignTimestamp(DecorationRequirements.SigningRequirement.FORBIDDEN);
//        else
//            decReq.setSignTimestamp(DecorationRequirements.SigningRequirement.REQUIRED);
        decReq.getElementsToSign().addAll(elements);
        decReq.setProtectTokens(protectTokens);
        decReq.setSenderMessageSigningCertificate(clientCertificate);
        decReq.setSenderMessageSigningPrivateKey(clientPrivateKey);
        decReq.setSecurityHeaderReusable(true);
        decReq.setSecurityHeaderActor(null);
        new WssDecoratorImpl().decorateMessage( new Message(document), decReq );
    }


    public static void main(String[] args) throws Exception {
        Signer dialog = new Signer();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
