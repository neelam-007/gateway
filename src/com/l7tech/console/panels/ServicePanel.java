
package com.l7tech.console.panels;

import com.l7tech.common.gui.util.FontUtil;
import com.l7tech.common.gui.util.SwingWorker;
import com.l7tech.common.gui.widgets.ContextMenuTextField;
import com.l7tech.common.gui.widgets.WrappingLabel;
import com.l7tech.console.panels.PublishServiceWizard.ServiceAndAssertion;
import com.l7tech.console.tree.wsdl.WsdlTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.service.PublishedService;
import com.l7tech.common.xml.Wsdl;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.wsdl.Port;
import javax.wsdl.WSDLException;
import java.awt.*;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.2
 */
public class ServicePanel extends WizardStepPanel {
    // local service copy
    private PublishedService service = new PublishedService();
    private Wsdl wsdl;

    /** Creates new form ServicePanel */
    public ServicePanel() {
        super(null);
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents() {
        serviceUrlPanel = new JPanel();
        serviceUrlLabel = new JLabel();
        wsdlUrlTextField = new ContextMenuTextField();

        setLayout(new GridBagLayout());

        WrappingLabel splain = new WrappingLabel("Enter the URL of the WSDL that describes " +
                                                 "the Web service you wish to publish.");
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
                                                                                "Resolving WSDL",
                                                                                "Please wait, resolving WSDL...");
            SwingWorker worker = new SwingWorker() {
                public Object construct() {
                    try {
                        return Registry.getDefault().getServiceManager().resolveWsdlTarget(wsdlUrl);
                    } catch (RemoteException e) {
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
                if (result instanceof Throwable)
                    ((Throwable) result).printStackTrace();
                JOptionPane.showMessageDialog(null,
                  "Unable to parse the WSDL at location '" + wsdlUrlTextField.getText() + "'\n",
                  "Error",
                  JOptionPane.ERROR_MESSAGE);
                return false;
            }
            String wsdlXml = (String) result;
            wsdl = Wsdl.newInstance(null, new StringReader(wsdlXml));

            final String serviceName = wsdl.getServiceName();
            // if service name not obtained service name is WSDL URL
            if (serviceName == null || "".equals(serviceName)) {
                service.setName(wsdlUrlTextField.getText());
            } else {
                service.setName(serviceName);
            }
            service.setWsdlXml(wsdlXml);
            service.setWsdlUrl(wsdlUrlTextField.getText());

            isWsdlDownloaded = true;
            notifyListeners();
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
        }

        return isWsdlDownloaded;
    }

    public void readSettings(Object settings) throws IllegalStateException {
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
                    URL url = wsdl.getUrlFromPort(port);
                    if (url != null)
                        sa.setRoutingAssertion(new HttpRoutingAssertion(url.toString()));
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

    /** todo: we'll need a tree renderer like this when we start doing operation-specific policies. */
    private static final
    TreeCellRenderer wsdlTreeRenderer = new DefaultTreeCellRenderer() {
        /**
         * Sets the value of the current tree cell to <code>value</code>.
         *
         * @return	the <code>Component</code> that the renderer uses to draw the value
         */
        public Component getTreeCellRendererComponent(JTree tree, Object value,
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
    };

    private JLabel serviceUrlLabel;
    private JPanel serviceUrlPanel;
    private JTextField wsdlUrlTextField;
    private boolean isWsdlUrlSyntaxValid = false;
    private boolean isWsdlDownloaded = false;

    // TODO: remove me
    public void setWsdlUrl(String newUrl) {
        wsdlUrlTextField.setText(newUrl);
    }
}
