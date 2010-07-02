package com.l7tech.external.assertions.rawtcp.console;

import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.ImageCache;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Raw UDP pluggable tasks menu action.
 */
public class RawUdpStatusAction extends AbstractAction {
    public RawUdpStatusAction() {
        super("Raw UDP Status", ImageCache.getInstance().getIconAsIcon("com/l7tech/console/resources/Bean16.gif"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), "Raw UDP Status", "Raw UDP protocol: l7.raw.udp\nStatus: ready\n", null);
    }
}
