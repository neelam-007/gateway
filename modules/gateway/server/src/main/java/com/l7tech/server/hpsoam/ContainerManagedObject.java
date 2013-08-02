package com.l7tech.server.hpsoam;

import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Background;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.hpsoam.metrics.PerformanceMetricsAggregator;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.gateway.common.service.PublishedService;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * The container managed object (see wsmf)
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 27, 2007<br/>
 */
public class ContainerManagedObject {
    private final Logger logger = Logger.getLogger(ContainerManagedObject.class.getName());
    private final ArrayList<ServiceManagedObject> serviceMOs = new ArrayList<ServiceManagedObject>();
    private String version;
    private PerformanceMetricsAggregator pma;
    private final ServiceManager serviceManager;

    public ContainerManagedObject(ServiceManager serviceManager, MessageProcessor messageProcessor) {
        version = Long.toString(System.currentTimeMillis());
        this.serviceManager = serviceManager;
        Collection<PublishedService> allservices;
        try {
            allservices = serviceManager.findAll();
            for (PublishedService service : allservices) {
                serviceMOs.add(new ServiceManagedObject(service));
            }
        } catch (FindException e) {
            logger.warning("Could not search existing services");
        }
        pma = new PerformanceMetricsAggregator(messageProcessor, serviceManager);

        // once in a while, we'll want to update the list of services
        Background.scheduleRepeated(new TimerTask() {
            public void run() {
                maintenance();
            }
        }, 10000, 30000);
    }

    private void maintenance() {
        if (!WSMFService.isEnabled()) {
            return;
        }

        // check for new services and for deleted ones
        Collection<PublishedService> allservices;
        synchronized (serviceMOs) {
            try {
                allservices = serviceManager.findAll();
                for (PublishedService service : allservices) {
                    // add service is not already managed
                    boolean alreadyManaged = false;
                    for (ServiceManagedObject smo : serviceMOs) {
                        if (Goid.equals(smo.getServiceID(), service.getGoid())) {
                            alreadyManaged = true;
                            // If any property of the published service has changed, the service MO needs to be updated.
                            // But comparing each property value is time consuming; so just replaces it without testing.
                            smo.setPublishedService(service);
                            break;
                        }
                    }
                    if (!alreadyManaged) {
                        serviceMOs.add(new ServiceManagedObject(service));
                        logger.info("Adding Managed Object for service " + service.getName());
                    }
                }
                // make sure existin services still exist
                ArrayList<ServiceManagedObject> toBeSacked = new ArrayList<ServiceManagedObject>();
                for (ServiceManagedObject smo : serviceMOs) {
                    boolean stillExists = false;
                    for (PublishedService service : allservices) {
                        if (Goid.equals(smo.getServiceID(), service.getGoid())) {
                            stillExists = true;
                            break;
                        }
                    }
                    if (!stillExists) {
                        toBeSacked.add(smo);
                    }
                }
                for (ServiceManagedObject sackMe : toBeSacked) {
                    serviceMOs.remove(sackMe);
                    logger.info("Removing Managed Object for service " + sackMe.getServiceID());
                }
            } catch (FindException e) {
                logger.warning("Could not search existing services");
            }
        }
    }

    public PerformanceMetricsAggregator getPerformanceMetricsAggregator() {
        return pma;
    }

    public ArrayList<ServiceManagedObject> getServiceMOs() {
        return serviceMOs;
    }

    public String getVersion() {
        return version;
    }

    public String getResourceVersion() {
        return getVersion() + "[resource]";
    }

    public String respondTo(WSMFService.RequestContext context) {
        // pass to relevent serviceMO
        for (ServiceManagedObject serviceMO : serviceMOs) {
            if (Goid.equals(serviceMO.getServiceID(), context.serviceid)) {
                return serviceMO.respondTo(context);
            }
        }
        logger.warning("Could not find service managed object for id " + context.serviceid);
        return null;
    }

    public String handleMOSpecificGET(String fullURL, Goid serviceid, HttpServletRequest req) {
        for (ServiceManagedObject serviceMO : serviceMOs) {
            if (Goid.equals(serviceMO.getServiceID(), serviceid)) {
                return serviceMO.handleMOSpecificGET(fullURL, req);
            }
        }
        logger.warning("Could not find service managed object for id " + serviceid);
        return null;
    }

    public String handlePerformanceWindowRequest(WSMFService.RequestContext context) {
        return pma.handlePerformanceWindowRequest(context);
    }
}
