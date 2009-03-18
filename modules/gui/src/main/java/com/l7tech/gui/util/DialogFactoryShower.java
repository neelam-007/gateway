package com.l7tech.gui.util;

import com.l7tech.util.Functions;

import javax.swing.*;

/** A convenient implementation of {@link com.l7tech.gui.util.DialogShower}. */
public class DialogFactoryShower implements DialogShower {
    private JDialog dialog;
    private Functions.Nullary<JDialog> dialogFactory;

    public DialogFactoryShower(JDialog dialog) {
        this.dialog = dialog;
    }

    public DialogFactoryShower(Functions.Nullary<JDialog> dialogFactory) {
        this.dialogFactory = dialogFactory;
    }

    public void showDialog() {
        if (dialog == null) dialog = dialogFactory.call();
        dialog.setVisible(true);
    }

    public void hideDialog() {
        if (dialog != null) dialog.dispose();
    }
}
