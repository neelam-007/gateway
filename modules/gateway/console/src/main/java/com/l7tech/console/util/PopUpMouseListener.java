package com.l7tech.console.util;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 *  The <code>PopUpMouseListener</code> is a specialized mouse
 * event listener, tha handles the 'mouse right click'
 * <p>
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version  1.0
 */
public abstract class PopUpMouseListener extends MouseAdapter {
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            popUpMenuHandler(e);
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            popUpMenuHandler(e);
        }
    }

    protected abstract void popUpMenuHandler(MouseEvent mouseEvent);
}
