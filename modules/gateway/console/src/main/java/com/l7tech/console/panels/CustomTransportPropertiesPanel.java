package com.l7tech.console.panels;

import javax.swing.*;
import java.util.Map;

/**
 * Superclass for custom GUI panel for modular SsgConnector-based transport protocols.
 */
public abstract class CustomTransportPropertiesPanel extends JPanel {
    /**
     * Configure the GUI controls with information from the specified SsgConnector instance.
     * <P/>
     * The custom GUI should assume that it will only be visible when the custom transport
     * protocol is selected.
     * 
     * @param advancedProperties the connector the provides the information to set.
     */
    public abstract void setData(Map<String, String> advancedProperties);

    /**
     * @return the new value for SsgConnector advanced properties, as currently configured by this GUI.
     *         <p/>
     *         The returned data will be used to update any properties.  Properties not present in the return map
     *         will be left unchanged.  To force a property to be removed completely, return a value of null for it. 
     */
    public abstract Map<String, String> getData();

    /**
     * @return an array of SsgConnector advanced property value names that should be hidden in the Advanced tab
     *         when this custom panel is in use.  The intent is that you would hide properties that are
     *         configured by a custom properties panel, to avoid confusing users.
     *         May be empty, but must never be null.
     */
    public abstract String[] getAdvancedPropertyNamesUsedByGui();
}
