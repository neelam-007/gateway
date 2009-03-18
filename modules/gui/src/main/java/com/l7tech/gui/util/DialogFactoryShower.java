package com.l7tech.gui.util;

import com.l7tech.util.Functions;

import javax.swing.*;

/** A convenient implementation of {@link com.l7tech.gui.util.DialogShower}. */
public class DialogFactoryShower implements DialogShower {
    private final Functions.Nullary<JDialog> dialogFactory;
    private JDialog dialog;

    public DialogFactoryShower(JDialog dialog) {
        if (dialog == null) throw new NullPointerException("dialog");
        this.dialogFactory = null;
        this.dialog = dialog;
    }

    public DialogFactoryShower(Functions.Nullary<JDialog> dialogFactory) {
        if (dialogFactory == null) throw new NullPointerException("dialogFactory");
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
