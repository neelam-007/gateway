package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.security.rbac.AttemptedCreate;
import static com.l7tech.common.security.rbac.EntityType.SERVICE;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.event.WizardListener;
import com.l7tech.console.panels.*;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServicesTree;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.WsdlUtils;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceAdmin;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLWriter;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.*;

/**
 * The <code>PublishServiceAction</code> action invokes the pubish
 * service wizard.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class CreateServiceWsdlAction extends SecureAction {
    static final Logger log = Logger.getLogger(CreateServiceWsdlAction.class.getName());

    public CreateServiceWsdlAction() {
        super(new AttemptedCreate(SERVICE), LIC_AUTH_ASSERTIONS);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Create WSDL";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Create WSDL for a new Web service";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        // todo: find better icon
        return "com/l7tech/console/resources/CreateWSDL16x16.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    WsdlDefinitionPanel defPanel =
                      new WsdlDefinitionPanel(new WsdlMessagesPanel(new WsdlPortTypePanel(new WsdlPortTypeBindingPanel(new WsdlServicePanel(null)))));
                    WsdlCreateOverviewPanel p = new WsdlCreateOverviewPanel(defPanel);
                    Frame f = TopComponents.getInstance().getTopParent();
                    Wizard w = new WsdlCreateWizard(f, p);

                    w.addWizardListener(wizardListener);
                    Utilities.setEscKeyStrokeDisposes(w);
                    w.pack();
                    w.setSize(850, 500);
                    Utilities.centerOnScreen(w);
                    DialogDisplayer.display(w);
                } catch (WsdlUtils.WSDLFactoryNotTrustedException wfnte) {
                    TopComponents.getInstance().showNoPrivilegesErrorMessage();
                } catch (WSDLException we) {
                    throw new RuntimeException(we);
                }
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
                Definition def = (Definition)w.getWizardInput();

                service.setDisabled(true);
                WSDLFactory fac = WsdlUtils.getWSDLFactory();
                WSDLWriter wsdlWriter = fac.newWSDLWriter();
                StringWriter sw = new StringWriter();
                wsdlWriter.writeWSDL(def, sw);
                Wsdl ws = new Wsdl(def);
                service.setName(ws.getServiceName());
                service.setWsdlXml(sw.toString());
                final String serviceAddress = getServiceAddress(def);
                service.setWsdlUrl(serviceAddress);
                RoutingAssertion ra;
                if (serviceAddress != null) {
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
                Registry.getDefault().getSecurityProvider().refreshPermissionCache();
                EntityHeader header = new EntityHeader();
                header.setType(EntityType.SERVICE);
                header.setName(service.getName());
                header.setOid(oid);
                serviceAdded(header);
            } catch (WsdlUtils.WSDLFactoryNotTrustedException wfnte) {
                TopComponents.getInstance().showNoPrivilegesErrorMessage();    
            } catch (Exception e) {
                Frame w = TopComponents.getInstance().getTopParent();
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
         * Fired when an new service is added.
         */
        public void serviceAdded(final EntityHeader eh) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesTree.NAME);
                    if (tree != null) {
                        AbstractTreeNode root = (AbstractTreeNode)tree.getModel().getRoot();
                        TreeNode[] nodes = root.getPath();
                        TreePath nPath = new TreePath(nodes);
                        if (tree.hasBeenExpanded(nPath)) {
                            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                            AbstractTreeNode sn = TreeNodeFactory.asTreeNode(eh);
                            model.insertNodeInto(sn, root, root.getInsertPosition(sn));
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
        for (Object o : extensibilityElements) {
            if (o instanceof SOAPAddress) {
                return ((SOAPAddress) o).getLocationURI();
            }
        }
        throw new IllegalArgumentException("missing SOAP address port definition");
    }
}
