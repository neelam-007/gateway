
package com.l7tech.console.panels;

import com.l7tech.common.gui.util.FontUtil;
import com.l7tech.common.gui.widgets.ContextMenuTextField;
import com.l7tech.common.gui.widgets.WrappingLabel;
import com.l7tech.console.panels.PublishServiceWizard.ServiceAndAssertion;
import com.l7tech.console.tree.wsdl.WsdlTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.RoutingAssertion;
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
        serviceUrljPanel = new JPanel();
        serviceUrljLabel = new JLabel();
        wsdlUrljTextField = new ContextMenuTextField();

        setLayout(new GridBagLayout());

        WrappingLabel splain = new WrappingLabel("Enter the URL of the WSDL that describes " +
                                                 "the web service you wish to publish.");
        add(splain,
            new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
                                   GridBagConstraints.CENTER,
                                   GridBagConstraints.HORIZONTAL,
                                   new Insets(20, 20, 20, 20), 0, 0));

        serviceUrljPanel.setLayout(new GridBagLayout());

        JLabel exampleUrl = new JLabel("Example: http://services.example.com/warehouse/purchase.wsdl");
        FontUtil.resizeFont(exampleUrl, 0.84);
        serviceUrljPanel.add(exampleUrl,
                             new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                                    GridBagConstraints.WEST,
                                                    GridBagConstraints.NONE,
                                                    new Insets(0, 0, 0, 0), 0, 0));

        serviceUrljPanel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 5)));
        serviceUrljLabel.setText("URL:");
        serviceUrljLabel.setBorder(new EmptyBorder(new Insets(1, 1, 1, 5)));
        serviceUrljPanel.add(serviceUrljLabel,
                             new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                                    GridBagConstraints.EAST,
                                                    GridBagConstraints.NONE,
                                                    new Insets(0, 0, 0, 0), 0, 0));

        wsdlUrljTextField.setPreferredSize(new Dimension(150, 20));
        wsdlUrljTextField.getDocument().addDocumentListener(createWsdlUrlDocumentListener());
        serviceUrljPanel.add(wsdlUrljTextField,
                             new GridBagConstraints(1, 1, 1, 1, 100.0, 0.0,
                                                    GridBagConstraints.WEST,
                                                    GridBagConstraints.HORIZONTAL,
                                                    new Insets(0, 0, 0, 0), 0, 0));
        add(serviceUrljPanel,
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
                String urlStr = wsdlUrljTextField.getText();
                URL url;
                try {
                    url = new URL(urlStr);
                } catch (MalformedURLException e) {
                    // snogood
                    return false;
                }

                if (url.getProtocol() == null || url.getProtocol().length() < 1)
                    return false;

                if (url.getHost() == null || url.getHost().length() < 1)
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
            String wsdlXml =
              Registry.getDefault().getServiceManager().resolveWsdlTarget(wsdlUrljTextField.getText());
            wsdl = Wsdl.newInstance(null, new StringReader(wsdlXml));

            service.setName(wsdl.getServiceName());
            service.setWsdlXml(wsdlXml);
            service.setWsdlUrl(wsdlUrljTextField.getText());

            isWsdlDownloaded = true;
            notifyListeners();
        } catch (RemoteException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(null,
              "Unable to resolve the WSDL at location '" + wsdlUrljTextField.getText() + "'\n",
              "Error",
              JOptionPane.ERROR_MESSAGE);
        } catch (WSDLException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(null,
              "Unable to parse the WSDL at location '" + wsdlUrljTextField.getText() + "'\n",
              "Error",
              JOptionPane.ERROR_MESSAGE);
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(null,
              "Illegal URL string '" + wsdlUrljTextField.getText() + "'\n",
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
                sa.setRoutingAssertion(new RoutingAssertion());
                if (port != null) {
                    URL url = wsdl.getUrlFromPort(port);
                    if (url != null)
                        sa.setRoutingAssertion(new RoutingAssertion(url.toString()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            isWsdlDownloaded = false;
        }
    }

    /** @return the wizard step label    */
    public String getStepLabel() {
        return "Protected service";
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

    private JLabel serviceUrljLabel;
    private JPanel serviceUrljPanel;
    private JTextField wsdlUrljTextField;
    private boolean isWsdlUrlSyntaxValid = false;
    private boolean isWsdlDownloaded = false;

    // TODO: remove me
    public void setWsdlUrl(String newUrl) {
        wsdlUrljTextField.setText(newUrl);
    }
}
