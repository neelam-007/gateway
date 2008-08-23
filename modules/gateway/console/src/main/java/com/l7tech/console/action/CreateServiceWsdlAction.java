package com.l7tech.console.action;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import static com.l7tech.gateway.common.security.rbac.EntityType.SERVICE;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.util.WsdlComposer;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.event.WizardListener;
import com.l7tech.console.panels.*;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.WsdlUtils;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import org.w3c.dom.Document;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLWriter;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.ConnectException;

/**
 * The <code>PublishServiceAction</code> action invokes the pubish
 * service wizard.
 */
public class CreateServiceWsdlAction extends SecureAction {
    static final Logger log = Logger.getLogger(CreateServiceWsdlAction.class.getName());
    private Document originalWsdl;
    private Set<WsdlComposer.WsdlHolder> importedWsdls;

    public CreateServiceWsdlAction() {
        super(new AttemptedCreate(SERVICE), UI_WSDL_CREATE_WIZARD);
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Create WSDL";
    }

    /**
     * @return the aciton description
     */
    @Override
    public String getDescription() {
        return "Create WSDL for a new Web service";
    }

    /**
     * specify the resource name for this action
     */
    @Override
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
    @Override
    protected void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    WizardStepPanel defPanel =
                      new WSDLCompositionPanel(new WsdlDefinitionPanel(new WsdlMessagesPanel(new WsdlPortTypePanel(new WsdlPortTypeBindingPanel(new WsdlServicePanel(null))))));
                    WsdlCreateOverviewPanel overviewPanel = new WsdlCreateOverviewPanel(defPanel);
                    Frame parent = TopComponents.getInstance().getTopParent();
                    Wizard wizard;
                    if (originalWsdl != null)
                        wizard = new WsdlCreateWizard(parent, overviewPanel, originalWsdl, importedWsdls);
                    else
                        wizard = new WsdlCreateWizard(parent, overviewPanel);
                    wizard.addWizardListener(wizardListener);
                    Utilities.setEscKeyStrokeDisposes(wizard);
                    wizard.pack();
                    Utilities.centerOnScreen(wizard);
                    DialogDisplayer.display(wizard);
                } catch (WsdlUtils.WSDLFactoryNotTrustedException wfnte) {
                    TopComponents.getInstance().showNoPrivilegesErrorMessage();
                } catch (WSDLException we) {
                    throw new RuntimeException(we);
                }
            }
        });
    }

    private PublishedService existingService;
    private Functions.UnaryVoid<Document> editCallback;
    private WizardListener wizardListener = new WizardAdapter() {
        /**
         * Invoked when the wizard has finished.
         *
         * @param we the event describing the wizard finish
         */
        @Override
        public void wizardFinished(WizardEvent we) {
            PublishedService service;
            boolean tryToPublish = false;
            boolean isEdit = false;
            if (existingService == null) {
                service = new PublishedService();
            } else {
                service = existingService;
                isEdit = true;
            }
            Collection<ServiceDocument> sourceDocs = new ArrayList<ServiceDocument>();
            try {
                Wizard w = (Wizard)we.getSource();
                WsdlComposer composer = (WsdlComposer) w.getWizardInput();
                Definition def = composer.buildOutputWsdl();

                WSDLFactory fac = WsdlUtils.getWSDLFactory();
                ExtensionRegistry reg =  Wsdl.disableSchemaExtensions(fac.newPopulatedExtensionRegistry());
                WSDLWriter wsdlWriter = fac.newWSDLWriter();
                def.setExtensionRegistry(reg);
                StringWriter sw = new StringWriter();
                wsdlWriter.writeWSDL(def, sw);
                Wsdl ws = new Wsdl(def);

                //if this is an "edit" then we are only interested in the WSDL and don't need to save the service.
                if (isEdit) {
                    if (editCallback != null)
                        editCallback.call( XmlUtil.stringToDocument(sw.toString()));
                    return;
                }

                // OK to update service here (not an edit)
                service.setWsdlXml(sw.toString());
                service.setName(ws.getServiceName());
                service.setDisabled(true);
                final String serviceAddress = getServiceAddress(def);
                RoutingAssertion ra;
                if (serviceAddress != null) {
                    ra = new HttpRoutingAssertion(serviceAddress);
                } else {
                    ra = new HttpRoutingAssertion();
                }

                // assign empty policy
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                final List children = Arrays.asList(ra);
                WspWriter.writePolicy(new AllAssertion(children), bo);

                service.getPolicy().setXml(bo.toString());
                service.getPolicy().setSoap(service.isSoap());

                Set<WsdlComposer.WsdlHolder> sourceWsdls = composer.getSourceWsdls(false);
                for (WsdlComposer.WsdlHolder sourceWsdl : sourceWsdls) {
                    ServiceDocument sd = new ServiceDocument();
                    sd.setUri(sourceWsdl.getWsdlLocation());
                    sd.setType(WsdlCreateWizard.IMPORT_SERVICE_DOCUMENT_TYPE);
                    sd.setContentType("text/xml");
                    sd.setServiceId(service.getOid());
                    StringWriter writer = new StringWriter();
                    wsdlWriter.writeWSDL(sourceWsdl.wsdl.getDefinition(), writer);
                    sd.setContents(writer.toString());
                    sourceDocs.add(sd);
                }

                tryToPublish = true;
            } catch (WsdlUtils.WSDLFactoryNotTrustedException wfnte) {
                    TopComponents.getInstance().showNoPrivilegesErrorMessage();
            } catch (Exception e) {
                Frame w = TopComponents.getInstance().getTopParent();
                log.log(Level.WARNING, "error saving service", e);
                DialogDisplayer.showMessageDialog(w,
                  "Unable to save the service '" + service.getName() + "'\n",
                  "Error",
                  JOptionPane.ERROR_MESSAGE, null);
            }

            while (tryToPublish) {
                tryToPublish = false;
                try {
                    long oid;
                    if (sourceDocs == null)
                        oid = Registry.getDefault().getServiceManager().savePublishedService(service);
                    else
                        oid = Registry.getDefault().getServiceManager().savePublishedServiceWithDocuments(service, sourceDocs);

                    Registry.getDefault().getSecurityProvider().refreshPermissionCache();
                    service.setOid(oid);
                    serviceAdded( new ServiceHeader(service) );
                } catch (Exception e) {
                    Frame w = TopComponents.getInstance().getTopParent();
                    if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
                        String msg = "This Web service cannot be saved as is because its resolution\n" +
                                     "parameters are already used by an existing published service.\n\nWould " +
                                     "you like to publish this service using a different routing URI?";
                        int answer = JOptionPane.showConfirmDialog(null, msg, "Service Resolution Conflict", JOptionPane.YES_NO_OPTION);
                        if (answer == JOptionPane.YES_OPTION) {
                            // get new routing URI
                            SoapServiceRoutingURIEditor dlg =
                                    new SoapServiceRoutingURIEditor(TopComponents.getInstance().getTopParent(), service);
                            dlg.pack();
                            Utilities.centerOnScreen(dlg);
                            dlg.setVisible(true);
                            if (dlg.wasSubjectAffected()) {
                                tryToPublish = true;
                            } else {
                                logger.info("Service publication aborted.");
                            }
                        } else {
                            logger.info("Service publication aborted.");
                        }
                    } else if (ExceptionUtils.causedBy(e, ConnectException.class)) {
                        log.log(Level.WARNING, "the connection to the SecureSpan Gateway is lost.", e);
                        ErrorManager.getDefault().notify(Level.WARNING, e, "");
                    }
                    else {
                        log.log(Level.WARNING, "error saving service", e);
                        DialogDisplayer.showMessageDialog(w, null, "Unable to save the service '" + service.getName() + "'\n", null);
                    }
                }
            }
        }

        /**
         * Fired when an new service is added.
         */
        public void serviceAdded(final EntityHeader eh) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree)TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
                    if (tree != null) {
                        AbstractTreeNode root = TopComponents.getInstance().getServicesFolderNode();
                        TreeNode[] nodes = root.getPath();
                        TreePath nPath = new TreePath(nodes);
                        if (tree.hasBeenExpanded(nPath)) {
                            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                            AbstractTreeNode sn = TreeNodeFactory.asTreeNode(eh, null);
                            model.insertNodeInto(sn, root, root.getInsertPosition(sn, RootNode.getComparator()));
                            RootNode rootNode = (RootNode) model.getRoot();
                            rootNode.addEntity(eh.getOid(), sn);
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

    /**
     * Set information for the Original WSDL
     *
     * <p>If the given original wsdl uses imports then it MUST have been parsed
     * in a way that will have set the documents base URI. If this is not done
     * then any relative imports will fail.</p>
     *
     * @param origService The original published service
     * @param editCallback Callback to use when editing
     * @param origWsdl The original WSDL document (see note)
     * @param importedWsdls The original source WSDLs (not imports)
     */
    public void setOriginalInformation(PublishedService origService,
                                       Functions.UnaryVoid<Document> editCallback,
                                       Document origWsdl,
                                       Set<WsdlComposer.WsdlHolder> importedWsdls) {
        this.existingService = origService;
        this.editCallback = editCallback;
        this.originalWsdl = origWsdl;
        this.importedWsdls = importedWsdls;
    }
}
