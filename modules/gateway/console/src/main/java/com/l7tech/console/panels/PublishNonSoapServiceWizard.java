/*
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.console.action.Actions;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.util.ConsoleLicenseManager;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.EnumSet;

/**
 * Wizard that guides the administrator through the publication of a non-soap service.
 */
public class PublishNonSoapServiceWizard extends Wizard {
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
        setTitle("Publish XML Application Wizard");

        getButtonHelp().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(PublishNonSoapServiceWizard.this);
            }
        });

    }

    protected void finish(ActionEvent evt) {
        PublishedService service = new PublishedService();
        ArrayList allAssertions = new ArrayList();
        try {
            // get the assertions from the all assertion
            if (panel2 != null)
                panel2.readSettings(allAssertions);
            AllAssertion policy = new AllAssertion(allAssertions);
            if (panel1.getDownstreamURL() != null)
                policy.addChild(new HttpRoutingAssertion(panel1.getDownstreamURL()));
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            WspWriter.writePolicy(policy, bo);
            service.setFolder(TopComponents.getInstance().getRootNode().getFolder());
            service.setPolicy(new Policy(PolicyType.PRIVATE_SERVICE, null, bo.toString(), false));
            service.setSoap(false);
            service.setWssProcessingEnabled(false);
            // xml application are not like soap. by default, not just post is allowed
            service.setHttpMethods(EnumSet.of(HttpMethod.POST, HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE));
            service.setName(panel1.getPublishedServiceName());
            service.setRoutingUri(panel1.getRoutingURI());

            long oid = Registry.getDefault().getServiceManager().savePublishedService(service);
            Registry.getDefault().getSecurityProvider().refreshPermissionCache();

            service.setOid(oid);
            PublishNonSoapServiceWizard.this.notify(new ServiceHeader(service));
        } catch (Exception e) {
            if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
                JOptionPane.showMessageDialog(this,
                  "Unable to save the service '" + service.getName() + "'\n" +
                  "because an existing service is already using the URI " + service.getRoutingUri(),
                  "Service already exists",
                  JOptionPane.ERROR_MESSAGE);
            } else {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                  "Unable to save the service '" + service.getName() + "'\n",
                  "Error",
                  JOptionPane.ERROR_MESSAGE);
            }
            return;
        }
        super.finish(evt);
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

    private IdentityProviderWizardPanel panel2; // may be null if no authentication enabled by current license
    private NonSoapServicePanel panel1;
}
