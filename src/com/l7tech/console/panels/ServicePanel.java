
package com.l7tech.console.panels;

import com.l7tech.common.gui.util.FontUtil;
import com.l7tech.common.gui.util.SwingWorker;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.widgets.ContextMenuTextField;
import com.l7tech.common.gui.widgets.WrappingLabel;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.console.panels.PublishServiceWizard.ServiceAndAssertion;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.MainWindow;
import com.l7tech.console.event.WsdlListener;
import com.l7tech.console.event.WsdlEvent;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.service.PublishedService;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.wsdl.Port;
import javax.wsdl.WSDLException;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.StringReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.2
 */
public class ServicePanel extends WizardStepPanel {
    private static final Logger logger = Logger.getLogger(ServicePanel.class.getName());

    // local service copy
    private PublishedService service = new PublishedService();
    private Wsdl wsdl;
    private JButton wsdlUrlBrowseButton;

    /** Creates new form ServicePanel */
    public ServicePanel() {
        super(null);
        initComponents();
    }

    public String getDescription() {
        return "Specify the URL that resolves either the WSDL of the service to publish or " +
               "a WSIL document that contains a link to that WSDL.";
    }

    /** This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents() {
        serviceUrlPanel = new JPanel();
        serviceUrlLabel = new JLabel();
        wsdlUrlTextField = new ContextMenuTextField();
        wsdlUrlBrowseButton = new JButton("Search");

        setLayout(new GridBagLayout());

        WrappingLabel splain = new WrappingLabel("Enter the URL of the WSDL or the WSIL.");
        add(splain,
            new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
                                   GridBagConstraints.CENTER,
                                   GridBagConstraints.HORIZONTAL,
                                   new Insets(20, 20, 20, 20), 0, 0));

        serviceUrlPanel.setLayout(new GridBagLayout());

        JLabel exampleUrl = new JLabel("Example: http://services.example.com/warehouse/purchase.wsdl");
        FontUtil.resizeFont(exampleUrl, 0.84);
        serviceUrlPanel.add(exampleUrl,
                             new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                                    GridBagConstraints.WEST,
                                                    GridBagConstraints.NONE,
                                                    new Insets(0, 0, 0, 0), 0, 0));

        serviceUrlPanel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 5)));
        serviceUrlLabel.setText("URL:");
        serviceUrlLabel.setBorder(new EmptyBorder(new Insets(1, 1, 1, 5)));
        serviceUrlPanel.add(serviceUrlLabel,
                             new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                                    GridBagConstraints.EAST,
                                                    GridBagConstraints.NONE,
                                                    new Insets(0, 0, 0, 0), 0, 0));

        wsdlUrlTextField.setPreferredSize(new Dimension(150, 20));
        wsdlUrlTextField.getDocument().addDocumentListener(createWsdlUrlDocumentListener());
        serviceUrlPanel.add(wsdlUrlTextField,
                             new GridBagConstraints(1, 1, 1, 1, 100.0, 0.0,
                                                    GridBagConstraints.WEST,
                                                    GridBagConstraints.HORIZONTAL,
                                                    new Insets(0, 0, 0, 0), 0, 0));

        wsdlUrlBrowseButton.setPreferredSize(new Dimension(30, 20));
        serviceUrlPanel.add(wsdlUrlBrowseButton, new GridBagConstraints(2, 1, 1, 1, 100.0, 0.0,
                                                            GridBagConstraints.WEST,
                                                            GridBagConstraints.HORIZONTAL,
                                                            new Insets(0, 5, 0, 0), 0, 0));
        
        wsdlUrlBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final MainWindow mainWindow = TopComponents.getInstance().getMainWindow();

                try {
                    // open UDDI browser
                    SearchWsdlDialog swd = new SearchWsdlDialog(ServicePanel.this.getOwner());
                    swd.addWsdlListener(new WsdlListener() {

                        public void wsdlSelected(WsdlEvent event) {
                            String wsdlURL = event.getWsdlInfo().getWsdlUrl();

                            // update the wsdlUrlTestField
                            if(wsdlURL != null) wsdlUrlTextField.setText(wsdlURL);
                        }
                    });
                    swd.setSize(700, 500);
                    swd.setModal(true);
                    swd.show();
                } catch (RemoteException ex) {
                    JOptionPane.showMessageDialog(mainWindow,
                            ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                } catch (FindException ex) {
                    JOptionPane.showMessageDialog(mainWindow,
                            ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        add(serviceUrlPanel,
            new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0,
                                   GridBagConstraints.CENTER,
                                   GridBagConstraints.HORIZONTAL,
                                   new Insets(0, 0, 0, 0), 0, 0));

        // space eats remainder
        add(new JLabel(""),
            new GridBagConstraints(0, 99, GridBagConstraints.REMAINDER, GridBagConstraints.REMAINDER, 100.0, 1.8,
                                   GridBagConstraints.CENTER,
                                   GridBagConstraints.BOTH,
                                   new Insets(0, 0, 0, 0), 0, 0));
    }

    private DocumentListener createWsdlUrlDocumentListener() {
        return new DocumentListener() {
            private boolean isUrlOk() {
                String urlStr = wsdlUrlTextField.getText();
                URL url;
                try {
                    url = new URL(urlStr);
                } catch (MalformedURLException e) {
                    // snogood
                    return false;
                }

                if (url.getProtocol() == null || url.getProtocol().length() < 1)
                    return false;

                return true;
            }

            private void checkUrl() {
                isWsdlUrlSyntaxValid = isUrlOk();
                ServicePanel.this.notifyListeners();
            }

            public void insertUpdate(DocumentEvent e) { checkUrl(); }
            public void removeUpdate(DocumentEvent e) { checkUrl(); }
            public void changedUpdate(DocumentEvent e) { checkUrl(); }
        };
    }

    public boolean canAdvance() {
        return isWsdlUrlSyntaxValid;
    }

    public boolean canFinish() {
        return isWsdlUrlSyntaxValid;
    }

    /**
     * Attempt to resolve the WSDL.  Returns true if we have a valid one, or false otherwise.
     * Will pester the user with a dialog box if the WSDL could not be fetched.
     * @return true iff. the WSDL URL in the URL: text field was downloaded successfully.
     */
    public boolean onNextButton() {
        isWsdlDownloaded = false;
        notifyListeners();

        try {
            final String wsdlUrl = wsdlUrlTextField.getText();
            Dialog rootDialog = (Dialog) SwingUtilities.getWindowAncestor(this);
            final CancelableOperationDialog dlg = new CancelableOperationDialog(rootDialog,
                                                                                "Resolving target",
                                                                                "Please wait, resolving target...");
            SwingWorker worker = new SwingWorker() {
                public Object construct() {
                    try {
                        return Registry.getDefault().getServiceManager().resolveWsdlTarget(wsdlUrl);
                    } catch (RemoteException e) {
                        return e;
                    } catch (MalformedURLException e) {
                        return e;
                    } catch (IOException e) {
                        return e;
                    }
                }

                public void finished() {
                    dlg.hide();
                }
            };
            worker.start();
            dlg.show();
            worker.interrupt();
            Object result = worker.get();
            if (result == null)
                // canceled
                return false;
            if (!(result instanceof String)) {
                final String msg = "Unable to parse the WSDL at location '" + wsdlUrlTextField.getText() + "'\n";
                if (result instanceof Throwable) {
                    logger.log(Level.WARNING, msg, (Throwable)result);
                }
                final MainWindow mainWindow = TopComponents.getInstance().getMainWindow();
                JOptionPane.showMessageDialog(mainWindow,
                                              msg,
                                              "Error",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            // this can be either a WSDL or a WSIL document
            String xmlResult = (String) result;

            Document resolvedDoc = XmlUtil.stringToDocument(xmlResult);

            // is this a WSIL?
            Element root = resolvedDoc.getDocumentElement();
            if (root.getLocalName().equals("inspection") &&

                root.getNamespaceURI().equals("http://schemas.xmlsoap.org/ws/2001/10/inspection/")) {
                // parse wsil and choose the wsdl url
                WSILSelectorPanel chooser = new WSILSelectorPanel(owner, resolvedDoc);
                chooser.pack();
                Utilities.centerOnScreen(chooser);
                chooser.show();
                if (!chooser.wasCancelled() && chooser.selectedWSDLURL() != null) {
                    wsdlUrlTextField.setText(chooser.selectedWSDLURL());
                    return onNextButton();
                }
            } else {
                wsdl = Wsdl.newInstance(Wsdl.extractBaseURI(wsdlUrlTextField.getText()), new StringReader(xmlResult));

                final String serviceName = wsdl.getServiceName();
                // if service name not obtained service name is WSDL URL
                if (serviceName == null || "".equals(serviceName)) {
                    service.setName(wsdlUrlTextField.getText());
                } else {
                    service.setName(serviceName);
                }
                service.setWsdlXml(xmlResult);
                service.setWsdlUrl(wsdlUrlTextField.getText());

                isWsdlDownloaded = true;
                notifyListeners();
            }
        } catch (WSDLException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(null,
              "Unable to parse the WSDL at location '" + wsdlUrlTextField.getText() + "'\n",
              "Error",
              JOptionPane.ERROR_MESSAGE);
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(null,
              "Illegal URL string '" + wsdlUrlTextField.getText() + "'\n",
              "Error",
              JOptionPane.ERROR_MESSAGE);
        } catch (IOException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(null,
                                          "Unable to parse the WSDL at location '" + wsdlUrlTextField.getText() +
                                          "'\n",
                                          "Error",
                                          JOptionPane.ERROR_MESSAGE);
        } catch (SAXException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(null,
                                          "Unable to parse the WSDL at location '" + wsdlUrlTextField.getText() +
                                          "'\n",
                                          "Error",
                                          JOptionPane.ERROR_MESSAGE);
        }

        return isWsdlDownloaded;
    }

    public void storeSettings(Object settings) throws IllegalStateException {
        if (!(settings instanceof ServiceAndAssertion)) {
            throw new IllegalArgumentException();
        }
        try {
            PublishServiceWizard.ServiceAndAssertion
              sa = (PublishServiceWizard.ServiceAndAssertion)settings;
            PublishedService publishedService = sa.getService();

            publishedService.setName(service.getName());
            publishedService.setWsdlXml(service.getWsdlXml());
            publishedService.setWsdlUrl(service.getWsdlUrl());

            if (sa.getRoutingAssertion() == null) {
                Port port = wsdl.getSoapPort();
                sa.setRoutingAssertion(new HttpRoutingAssertion());
                if (port != null) {
                    String uri = wsdl.getUriFromPort(port);
                    if (uri != null) {
                        if(uri.startsWith("http") || uri.startsWith("HTTP")) {
                             sa.setRoutingAssertion(new HttpRoutingAssertion(uri));
                        } else {
                             sa.setRoutingAssertion(new HttpRoutingAssertion(Wsdl.extractBaseURI(publishedService.getWsdlUrl()) + uri));
                        }
                    }
                }
                if (sa.getAssertion() != null && sa.getAssertion().getChildren().isEmpty()) {
                    sa.getAssertion().addChild(sa.getRoutingAssertion());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            isWsdlDownloaded = false;
        }
    }

    /** @return the wizard step label    */
    public String getStepLabel() {
        return "Web Service URL";
    }

    /** todo: we'll need a tree renderer like this when we start doing operation-specific policies.
    private static final
    TreeCellRenderer wsdlTreeRenderer = new DefaultTreeCellRenderer() {*/
        /**
         * Sets the value of the current tree cell to <code>value</code>.
         *
         * //@return	the <code>Component</code> that the renderer uses to draw the value
         */
        /*public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean selected, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            this.setBackgroundNonSelectionColor(tree.getBackground());

            WsdlTreeNode node = (WsdlTreeNode)value;
            setText(node.toString());
            Image image = expanded ? node.getOpenedIcon() : node.getIcon();
            Icon icon = null;
            if (image != null) {
                icon = new ImageIcon(image);
            }
            setIcon(icon);
            return this;
        }
    };*/

    private JLabel serviceUrlLabel;
    private JPanel serviceUrlPanel;
    private JTextField wsdlUrlTextField;
    private boolean isWsdlUrlSyntaxValid = false;
    private boolean isWsdlDownloaded = false;
}
