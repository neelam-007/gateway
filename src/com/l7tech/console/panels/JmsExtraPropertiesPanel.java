/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.console.panels;

import javax.swing.*;
import java.util.Properties;

/**
 * @author rmak
 * @since SecureSpan 3.7
 */
public abstract class JmsExtraPropertiesPanel extends JPanel {
    /**
     * Applies given properties to initialize the view.
     */
    public abstract void setProperties(final Properties properties);

    /**
     * Creates properties from the current view. 
     */
    public abstract Properties getProperties();
}
