package com.l7tech.console.panels;

import com.l7tech.gateway.common.transport.InterfaceTag;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.gui.widgets.ValidatedPanel;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 *
 */
public class InterfaceTagsDialog extends OkCancelDialog<InterfaceTagsPanel> {
    public InterfaceTagsDialog(Frame owner, String title, boolean modal, Set<InterfaceTag> model) {
        super(owner, title, modal, makePanel(model));
    }

    public InterfaceTagsDialog(Frame owner, String title, boolean modal, Set<InterfaceTag> model, boolean readOnly) {
        super(owner, title, modal, makePanel(model), readOnly);
    }

    public InterfaceTagsDialog(Dialog owner, String title, boolean modal, Set<InterfaceTag> model) {
        super(owner, title, modal, makePanel(model));
    }

    public InterfaceTagsDialog(Dialog owner, String title, boolean modal, Set<InterfaceTag> model, boolean readOnly) {
        super(owner, title, modal, makePanel(model), readOnly);
    }

    private static ValidatedPanel makePanel(Set<InterfaceTag> model) {
        return new InterfaceTagsPanel(model);
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        JFrame main = new JFrame("test");
        main.setSize(500, 200);
        Utilities.centerOnScreen(main);
        main.setVisible(true);

        Set<InterfaceTag> testSet = InterfaceTag.parseMultiple("external(202.231.15/24);internal(10.23.15,10.77.92.128/30);loopback(127.0.0.1)");
        final InterfaceTagsDialog dlg = new InterfaceTagsDialog(main, "Interface Tags", true, testSet, false);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
        System.exit(0);
    }
}
