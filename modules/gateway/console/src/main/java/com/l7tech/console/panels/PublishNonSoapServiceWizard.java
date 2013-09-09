package com.l7tech.console.panels;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.console.action.Actions;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.logging.PermissionDeniedErrorHandler;
import com.l7tech.console.util.ConsoleLicenseManager;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Option;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wizard that guides the administrator through the publication of a non-soap service.
 */
public class PublishNonSoapServiceWizard extends Wizard {
    private static final Logger logger = Logger.getLogger(PublishNonSoapServiceWizard.class.getName());

    public static PublishNonSoapServiceWizard getInstance(Frame parent) {
        IdentityProviderWizardPanel panel2 = null;
        NonSoapServicePanel panel1 = new NonSoapServicePanel(null);
        if (ConsoleLicenseManager.getInstance().isAuthenticationEnabled()) {
            panel2 = new IdentityProviderWizardPanel(false);
            panel1.setNextPanel(panel2);
        }
        PublishNonSoapServiceWizard output = new PublishNonSoapServiceWizard(parent, panel1);
        output.panel1 = panel1;
        output.panel2 = panel2;
        return output;
    }

    public PublishNonSoapServiceWizard(Frame parent, WizardStepPanel panel) {
        super(parent, panel);
        setTitle("Publish Web API Wizard");

        getButtonHelp().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(PublishNonSoapServiceWizard.this);
            }
        });
    }

    @Override
    protected void finish( final ActionEvent evt ) {
        final PublishedService service = new PublishedService();
        ArrayList<Assertion> allAssertions = new ArrayList<Assertion>();
        try {
            // get the assertions from the all assertion
            if (panel2 != null) {
                panel2.readSettings(allAssertions);
                service.setSecurityZone(panel2.getSelectedSecurityZone());
            }
            AllAssertion policy = new AllAssertion(allAssertions);
            if (panel1.getDownstreamURL() != null)
                policy.addChild(new HttpRoutingAssertion(panel1.getDownstreamURL()));
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            WspWriter.writePolicy(policy, bo);
            service.setFolder(folder.orSome(TopComponents.getInstance().getRootNode().getFolder()));
            final Policy servicePolicy = new Policy(PolicyType.PRIVATE_SERVICE, null, bo.toString(), false);
            // service policy inherits same security zone as the service
            servicePolicy.setSecurityZone(panel2.getSelectedSecurityZone());
            service.setPolicy(servicePolicy);
            service.setSoap(false);
            service.setWssProcessingEnabled(false);
            // xml application are not like soap. by default, not just post is allowed
            service.setHttpMethods(EnumSet.of(HttpMethod.POST, HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE));
            service.setName(panel1.getPublishedServiceName());
            service.setRoutingUri(panel1.getRoutingURI());

            final Runnable saver = new Runnable(){
                @Override
                public void run() {
                    try {
                        Goid goid = Registry.getDefault().getServiceManager().savePublishedService(service);
                        Registry.getDefault().getSecurityProvider().refreshPermissionCache();
                        service.setGoid(goid);
                        Thread.sleep(1000);
                        PublishNonSoapServiceWizard.this.notify(new ServiceHeader(service));
                        PublishNonSoapServiceWizard.super.finish(evt);
                    } catch ( Exception e ) {
                        handlePublishError( service, e );
                    }
                }
            };

            if ( ServicePropertiesDialog.hasResolutionConflict( service, null ) ) {
                final String message =
                      "Resolution parameters conflict for service '" + service.getName() + "'\n" +
                      "because an existing service is already using the URI " + service.getRoutingUri() + "\n\n" +
                      "Do you want to save this service?";
                DialogDisplayer.showConfirmDialog(this, message, "Service Resolution Conflict", JOptionPane.YES_NO_OPTION, new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult( final int option ) {
                        if (option == JOptionPane.YES_OPTION) {
                            saver.run();
                        }
                    }
                });
            } else {
                saver.run();
            }
        } catch (Exception e) {
            handlePublishError( service, e );
        }
    }

    private void handlePublishError( final PublishedService service, final Exception e ) {
        final String message = "Unable to save the service '" + service.getName() + "'\n";
        logger.log( Level.INFO, message, ExceptionUtils.getDebugException(e));
        if (e instanceof PermissionDeniedException) {
            PermissionDeniedErrorHandler.showMessageDialog((PermissionDeniedException) e, logger);
        } else {
            JOptionPane.showMessageDialog(this,
              message,
              "Error",
              JOptionPane.ERROR_MESSAGE);
        }
    }

    private void notify(EntityHeader header) {
        EntityEvent event = new EntityEvent(this, header);
        EntityListener[] listeners = listenerList.getListeners(EntityListener.class);
        for (EntityListener listener : listeners) {
            listener.entityAdded(event);
        }
    }

    /**
     * add the EntityListener
     *
     * @param listener the EntityListener
     */
    public void addEntityListener(EntityListener listener) {
        listenerList.add(EntityListener.class, listener);
    }

    /**
     * remove the the EntityListener
     *
     * @param listener the EntityListener
     */
    public void removeEntityListener(EntityListener listener) {
        listenerList.remove(EntityListener.class, listener);
    }

    /**
     * Set the Folder for the service.
     *
     * @param folder The folder to use (required)
     */
    public void setFolder( @NotNull final Folder folder ) {
        this.folder = Option.some( folder );
    }

    private IdentityProviderWizardPanel panel2; // may be null if no authentication enabled by current license
    private NonSoapServicePanel panel1;
    private Option<Folder> folder = Option.none();
}
