package com.l7tech.console.panels;

import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.util.Registry;
import com.l7tech.console.action.Actions;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.service.PublishedService;
import com.l7tech.common.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EventListener;
import java.io.ByteArrayOutputStream;

/**
 * Wizard that guides the administrator through the publication of a non-soap service.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 14, 2004<br/>
 * $Id$<br/>
 */
public class PublishNonSoapServiceWizard extends Wizard {
    public static PublishNonSoapServiceWizard getInstance(Frame parent) {
        IdentityProviderWizardPanel panel2 = new IdentityProviderWizardPanel(false);
        NonSoapServicePanel panel1 = new NonSoapServicePanel(panel2);
        PublishNonSoapServiceWizard output = new PublishNonSoapServiceWizard(parent, panel1);
        output.panel2 = panel2;
        output.panel1 = panel1;
        return output;
    }

    public PublishNonSoapServiceWizard(Frame parent, WizardStepPanel panel) {
        super(parent, panel);
        setTitle("Publish XML Application");

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
        panel2.readSettings(allAssertions);
        AllAssertion policy = new AllAssertion(allAssertions);
        policy.addChild(new HttpRoutingAssertion(panel1.getDownstreamURL()));
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        WspWriter.writePolicy(policy, bo);
        service.setPolicyXml(bo.toString());
        service.setSoap(false);
        service.setName(panel1.getPublishedServiceName());
        service.setRoutingUri(panel1.getRoutingURI());
        long oid = Registry.getDefault().getServiceManager().savePublishedService(service);
        EntityHeader header = new EntityHeader();
        header.setType(EntityType.SERVICE);
        header.setName(service.getName());
        header.setOid(oid);
        PublishNonSoapServiceWizard.this.notify(header);
        } catch (Exception e) {
            if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
                JOptionPane.showMessageDialog(null,
                  "Unable to save the service '" + service.getName() + "'\n" +
                  "because an existing service is already using that namespace URI\n" +
                  "and SOAPAction combination.",
                  "Service already exists",
                  JOptionPane.ERROR_MESSAGE);
            } else {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
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
        EventListener[] listeners = listenerList.getListeners(EntityListener.class);
        for (int i = 0; i < listeners.length; i++) {
            ((EntityListener)listeners[i]).entityAdded(event);
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

    private IdentityProviderWizardPanel panel2;
    private NonSoapServicePanel panel1;
}
