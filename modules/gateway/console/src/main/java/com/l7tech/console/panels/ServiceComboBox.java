package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ComboBox that allows selection of a published service.
 */
public class ServiceComboBox extends JComboBox {
    private static Logger logger = Logger.getLogger(ServiceComboBox.class.getName());

    /**
     * @return the selected PublishedService instance, freshly loaded from the Gateway if possible, or null if none selected or the selected service ID is unknown to the Gateway.
     * If the user does not have permission to read the selected service, a dummy PublishedService with only default values except its OID is returned.
     */
    public PublishedService getSelectedPublishedService() {
        return getSelectedPublishedService(this);
    }

    /**
     * Get the selected PublishedService, loading it from the Gateway if possible, or null.
     * @param serviceCombo a JComboBox (not necessary a ServiceComboBox) that uses ServiceComboItem instances in its model.
     * @return the PublishedService currently selected in the combo box, or null if none is selected or the selected service ID is unknown to the Gateway.
     * If the user does not have permission to read the selected service, a dummy PublishedService with only default values except its GOID is returned.
     */
    public static PublishedService getSelectedPublishedService(JComboBox serviceCombo) {
        PublishedService svc = null;
        ServiceComboItem item = (ServiceComboItem)serviceCombo.getSelectedItem();
        if (item == null) return null;

        ServiceAdmin sa = Registry.getDefault().getServiceManager();
        try {
            svc = sa.findServiceByID(Goid.toString(item.serviceID));
        } catch (FindException e) {
            logger.severe("Can not find service with id " + item.serviceID);
        } catch (final PermissionDeniedException e) {
            // service exists but user does not have permission to read it
            logger.log(Level.WARNING, "User does not have permission to read selected service. Returning default service with selected goid.");
            svc = new PublishedService();
            svc.setGoid(item.serviceID);
        }
        return svc;
    }

    /**
     * Load available services into the combo box.
     *
     * @param selectService true if a service should be selected once the box is populated.  False to leave the selection unchanged.
     * @param serviceIdToSelect  the ID of the service to select, if selectService is true.
     * @return true if at least one published service was found.
     */
    public boolean populateAndSelect(boolean selectService, Goid serviceIdToSelect) {
        return populateAndSelect(this, selectService, serviceIdToSelect);
    }

    /**
     * Load available services into the specified combo box.
     *
     * @param serviceCombo a JComboBox (not necessarily a ServiceComboBox) to hold the published services.  Required.
     * @param selectService true if a service should be selected once the box is populated.  False to leave the selection unchanged.
     * @param serviceIdToSelect  the ID of the service to select, if selectService is true.
     * @return true if the requested service was selected.
     */
    public static boolean populateAndSelect(JComboBox serviceCombo, boolean selectService, Goid serviceIdToSelect) {
        final boolean selected;

        serviceCombo.setRenderer( TextListCellRenderer.<ServiceComboItem>basicComboBoxRenderer() );

        // populate the service combo
        EntityHeader[] allServices = new EntityHeader[0];
        try {
            ServiceAdmin sa = Registry.getDefault().getServiceManager();
            allServices = sa.findAllPublishedServices();
        } catch (Exception e) {
            logger.log(Level.WARNING, "problem listing services: " + ExceptionUtils.getMessage(e), e);
        }
        if (allServices == null || allServices.length == 0) {
            // Case 1: the queue associated with a published service and the user may be with a role of Manage JMS Queue.
            if (selectService) {
                String message = "Service " + serviceIdToSelect + " is selected, but cannot be displayed.";
                ServiceComboItem item = new ServiceComboItem(message, serviceIdToSelect);
                serviceCombo.addItem(item);
                serviceCombo.setSelectedItem(item);
                selected = true;
            }
            // Case 2: There are no any published services at all.
            else {
                // We just want to show the message "No published services available." in the combo box.
                // So "DEFAULT_GOID" is just a dummy ServiceGOID and it won't be used since the checkbox is set to disabled.
                serviceCombo.addItem(new ServiceComboItem("No published services available.", PublishedService.DEFAULT_GOID));
                selected = false;
            }
        } else {
            ArrayList<ServiceComboItem> comboItems = new ArrayList<ServiceComboItem>(allServices.length);
            Object selectMe = null;
            for (int i = 0; i < allServices.length; i++) {
                EntityHeader aService = allServices[i];
                ServiceHeader svcHeader = (ServiceHeader) aService;
                comboItems.add(new ServiceComboItem(
                    svcHeader.isDisabled()? svcHeader.getDisplayName() + " (This service is currently disabled.)" : svcHeader.getDisplayName(),
                    svcHeader.getGoid()
                ));
                if (selectService && Goid.equals(aService.getGoid(), serviceIdToSelect)) {
                    selectMe = comboItems.get(i);
                }
            }
            Collections.sort(comboItems);
            serviceCombo.setModel(new DefaultComboBoxModel(comboItems.toArray()));
            if (selectMe != null) {
                serviceCombo.setSelectedItem(selectMe);
                selected = true;
            } else if (selectService) {
                // oid not found in available services - may not be readable by the current user
                comboItems.add(0, new ServiceComboItem("Service " + serviceIdToSelect + " is selected, but cannot be displayed.", serviceIdToSelect));
                serviceCombo.setModel(new DefaultComboBoxModel(comboItems.toArray()));
                serviceCombo.setSelectedIndex(0);
                selected = true;
            } else {
                selected = false;
            }
        }

        return selected;
    }
}
