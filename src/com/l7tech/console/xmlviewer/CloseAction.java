/*
 * $Id$
 *
 * The contents of this file are subject to the Mozilla Public License 
 * Version 1.1 (the "License"); you may not use this file except in 
 * compliance with the License. You may obtain a copy of the License at 
 * http://www.mozilla.org/MPL/ 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License 
 * for the specific language governing rights and limitations under the License.
 *
 * The Original Code is eXchaNGeR browser code. (org.xngr.browser.*)
 *
 * The Initial Developer of the Original Code is Cladonia Ltd.. Portions created 
 * by the Initial Developer are Copyright (C) 2002 the Initial Developer. 
 * All Rights Reserved. 
 *
 * Contributor(s): Edwin Dankert <edankert@cladonia.com>
 */
package com.l7tech.console.xmlviewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * An action that can be used to close the Xml Viewer.
 *
 * @author Edwin Dankert <edankert@cladonia.com>
 * @version	$Revision$, $Date$
 */
public class CloseAction extends AbstractAction {
    private static final boolean DEBUG = false;

    private ViewerFrame viewer = null;

    /**
     * The constructor for the action which allows for closing
     * the Xml Viewer.
     *
     * @param viewer the XML Viewer
     */
    public CloseAction(ViewerFrame viewer) {
        super("Close");

        if (DEBUG) System.out.println("CloseAction( " + viewer + ")");

        putValue(MNEMONIC_KEY, new Integer('C'));
//		putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( KeyEvent.VK_F, InputEvent.CTRL_MASK, false));

        this.viewer = viewer;
    }

    /**
     * The implementation of the close action, called
     * after a user action.
     *
     * @param event the action event.
     */
    public void actionPerformed(ActionEvent event) {
        if (DEBUG) System.out.println("CloseAction.actionPerformed( " + event + ")");

        viewer.close();
    }
}
