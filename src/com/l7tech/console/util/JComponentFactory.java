package com.l7tech.console.util;

import javax.swing.*;

/**
 * UI factory implementations create Swing JComponent for a given
 * object.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public interface JComponentFactory {
    /**
     * Produce the <code>JComponent</code> for the given object.
     * @param o the object that the corresponding component
     * @return the component for the given object or <b>null</b> if
     *         th component cannot be
     */
    JComponent getJComponent(Object o);
}
