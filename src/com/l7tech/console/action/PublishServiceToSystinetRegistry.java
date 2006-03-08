package com.l7tech.console.action;

import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.Registry;
import com.l7tech.service.PublishedService;
import com.l7tech.objectmodel.FindException;

import java.util.logging.Level;
import java.rmi.RemoteException;

/**
 * Action corresponding to publishing a service wsdl and associated policy on a systinet registry.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Mar 8, 2006<br/>
 */
public class PublishServiceToSystinetRegistry extends NodeAction {
    public PublishServiceToSystinetRegistry(ServiceNode node) {
        super(node);
    }

    public String getName() {
        return "Publish to Systinet Registry";
    }

    public String getDescription() {
        return "Publish the service's WSDL and associated Policy to a Systinet Registry";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Edit16.gif";
    }

    protected void performAction() {
        final ServiceNode serviceNode = ((ServiceNode)node);
        PublishedService svc;
        try {
            svc = serviceNode.getPublishedService();
        } catch (FindException e) {
            logger.log(Level.WARNING, "Cannot get service", e);
            throw new RuntimeException(e);
        } catch (RemoteException e) {
            logger.log(Level.WARNING, "Cannot get service", e);
            throw new RuntimeException(e);
        }
        // todo, capture the publication details through some sort of wizard
        try {
            String res = Registry.getDefault().getServiceManager().publishToSystinetRegistry(""+svc.getOid());
            // todo, not this:
            logger.warning(res);
        } catch (RemoteException e) {
            logger.log(Level.WARNING, "Error publishing service on Systinet registry", e);
            // todo nicer error message here
            throw new RuntimeException(e);
        } catch (FindException e) {
            logger.log(Level.WARNING, "Error publishing service on Systinet registry", e);
            // todo nicer error message here
            throw new RuntimeException(e);
        }
    }
}
