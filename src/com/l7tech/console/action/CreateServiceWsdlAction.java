package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.console.MainWindow;
import com.l7tech.console.event.*;
import com.l7tech.console.panels.*;
import com.l7tech.console.tree.ServicesTree;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceAdmin;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLWriter;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>PublishServiceAction</code> action invokes the pubish
 * service wizard.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class CreateServiceWsdlAction extends BaseAction implements ConnectionListener {
    static final Logger log = Logger.getLogger(CreateServiceWsdlAction.class.getName());

    public CreateServiceWsdlAction() {
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Create Service";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Create the new service definition (WSDL)";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        // todo: find better icon
        return "com/l7tech/console/resources/policy16.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                WsdlDefinitionPanel defPanel =
                  new WsdlDefinitionPanel(
                    new WsdlMessagesPanel(
                      new WsdlPortTypePanel(
                        new WsdlPortTypeBindingPanel(
                          new WsdlServicePanel(null))
                      )
                    ));
                WsdlCreateOverviewPanel p = new WsdlCreateOverviewPanel(defPanel);
                JFrame f = Registry.getDefault().getComponentRegistry().getMainWindow();
                Wizard w = new WsdlCreateWizard(f, p);
                w.addWizardListener(wizardListener);
                w.pack();
                w.setSize(850, 500);
                Utilities.centerOnScreen(w);
                w.setVisible(true);
            }
        });
    }

    private WizardListener wizardListener = new WizardAdapter() {
        /**
         * Invoked when the wizard has finished.
         * 
         * @param we the event describing the wizard finish
         */
        public void wizardFinished(WizardEvent we) {
            PublishedService service = new PublishedService();
            try {
                Wizard w = (Wizard)we.getSource();
                Definition def = (Definition)w.getCollectedInformation();

                service.setDisabled(true);
                WSDLFactory fac = WSDLFactory.newInstance();
                WSDLWriter wsdlWriter = fac.newWSDLWriter();
                StringWriter sw = new StringWriter();
                wsdlWriter.writeWSDL(def, sw);
                Wsdl ws = Wsdl.newInstance(null, new StringReader(sw.toString()));
                service.setName(ws.getServiceName());
                service.setWsdlXml(sw.toString());
                final String serviceAddress = getServiceAddress(def);
                service.setWsdlUrl(serviceAddress);
                RoutingAssertion ra = null;
                if (serviceAddress !=null) {
                   ra = new HttpRoutingAssertion(serviceAddress);
                } else {
                    ra = new HttpRoutingAssertion();
                }

                // assign empty policy
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                final List children = Arrays.asList(new Assertion[]{ra});
                WspWriter.writePolicy(new AllAssertion(children), bo);

                service.setPolicyXml(bo.toString());


                ServiceAdmin serviceManager = Registry.getDefault().getServiceManager();
                long oid = serviceManager.savePublishedService(service);
                EntityHeader header = new EntityHeader();
                header.setType(EntityType.SERVICE);
                header.setName(service.getName());
                header.setOid(oid);
                serviceAdded(header);
            } catch (Exception e) {
                MainWindow w = Registry.getDefault().getComponentRegistry().getMainWindow();
                if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
                    JOptionPane.showMessageDialog(w,
                      "Unable to save the service '" + service.getName() + "'\n" +
                      "because there an existing service already using that namespace URI\n" +
                      "and SOAPAction combination.",
                      "Service already exists",
                      JOptionPane.ERROR_MESSAGE);
                } else {
                    log.log(Level.WARNING, "erro saving service", e);
                    JOptionPane.showMessageDialog(w,
                      "Unable to save the service '" + service.getName() + "'\n",
                      "Error",
                      JOptionPane.ERROR_MESSAGE);

                }
            }
        }

        /**
         * Invoked when the wizard has been cancelled.
         * 
         * @param e the event describinng the wizard cancel
         */
        public void wizardCanceled(WizardEvent e) {
        }

        /**
         * Fired when an new service is added.
         */
        public void serviceAdded(final EntityHeader eh) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesTree.NAME);
                    if (tree != null) {
                        DefaultMutableTreeNode root = (DefaultMutableTreeNode)tree.getModel().getRoot();
                        TreeNode[] nodes = root.getPath();
                        TreePath nPath = new TreePath(nodes);
                        if (tree.hasBeenExpanded(nPath)) {
                            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                            model.insertNodeInto(TreeNodeFactory.asTreeNode(eh), root, root.getChildCount());
                        }
                    } else {
                        log.log(Level.WARNING, "Service tree unreachable.");
                    }
                }
            });
        }

    };

    /**
     * determine the soap address of the first service/port
     * 
     * @param def the WSDL definition model
     * @return the soap address as String
     * @throws IllegalArgumentException if the soap address is not found
     */
    private String getServiceAddress(Definition def)
      throws IllegalArgumentException {
        Map services = def.getServices();
        if (services.isEmpty()) {
            throw new IllegalArgumentException("missing service");
        }
        Service sv = (Service)services.values().iterator().next();
        Map ports = sv.getPorts();
        if (ports.isEmpty()) {
            throw new IllegalArgumentException("missing service port definition");
        }
        Port port = (Port)ports.values().iterator().next();
        java.util.List extensibilityElements = port.getExtensibilityElements();
        for (Iterator iterator = extensibilityElements.iterator(); iterator.hasNext();) {
            Object o = iterator.next();
            if (o instanceof SOAPAddress) {
                return ((SOAPAddress)o).getLocationURI();
            }
        }
        throw new IllegalArgumentException("missing SOAP address port definition");
    }

    public void onConnect(ConnectionEvent e) {
        setEnabled(true);
    }

    public void onDisconnect(ConnectionEvent e) {
        setEnabled(false);
    }
}
