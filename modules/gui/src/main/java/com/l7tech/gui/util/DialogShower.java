package com.l7tech.gui.util;

/** Manages lazy dialog for {@link com.l7tech.gui.util.Utilities#doWithDelayedCancelDialog(java.util.concurrent.Callable, DialogShower , long)}. */
public interface DialogShower {
    /** Show a modal cancel dialog.  The dialog must block until canceled or hidden via call to hideDialog. */
    void showDialog();

    /** Hide any currently-showing modal cancel dialog. */
    void hideDialog();
}
