package com.l7tech.external.assertions.rawtcp.console;

import com.l7tech.console.util.IconManager;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.ImageCache;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Action showing raw TCP status.
 */
public class RawTcpStatusAction extends AbstractAction {
    public RawTcpStatusAction() {
        super("Raw TCP Status", ImageCache.getInstance().getIconAsIcon("com/l7tech/console/resources/Bean16.gif"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), "Raw TCP Status", "Raw TCP protocol: l7.raw.tcp\nStatus: ready\n", null);
    }
}
