package com.l7tech.gui.util;

import javax.swing.*;

/**
 * User: mike
*/
public interface SheetHolder extends RootPaneContainer {
    /**
     * Show the specified sheet on this SheetHolder.
     * <p/>
     * An implementation is provided: your implementing frame, dialog or applet just invoke
     * {@link DialogDisplayer#showSheet} on itself and the sheet.
     *
     * @param sheet  the sheet to show.  May not be null.
     */
    void showSheet(JInternalFrame sheet);
}
