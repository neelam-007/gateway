package com.l7tech.console.panels;

import com.l7tech.gui.util.DialogFactoryShower;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;

/**
 * Simple "Please Wait.." dialog.
 * @author mike
 * @version 1.0
 */
public class CancelableOperationDialog extends JDialog {
    private final JLabel messageLabel = new JLabel();
    private final JProgressBar progressBar;
    private boolean wasCancel = false;
    private boolean needsInit = true;
    private boolean needsPack = true;

    public static CancelableOperationDialog newCancelableOperationDialog(Component component, String title, String message) {
        Window window = SwingUtilities.getWindowAncestor(component);
        return new CancelableOperationDialog(window, title, message, null);
    }

    public CancelableOperationDialog(Window owner, String title, String message, JProgressBar progressBar) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.messageLabel.setText(message);
        this.progressBar = progressBar;
        if ( owner==null ) {
            ImageIcon imageIcon = new ImageIcon( ImageCache.getInstance().getIcon("com/l7tech/console/resources/CA_Logo_Black_16x16.png"));
            setIconImage(imageIcon.getImage());
        }
    }

    public CancelableOperationDialog(Window parent, String title, String message) {
        this( parent, title, message, null );
    }

    private void doInit() {
        setResizable(false);

        Container p = getContentPane();
        p.setLayout(new GridBagLayout());
        p.add(messageLabel,
              new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                     GridBagConstraints.CENTER,
                                     GridBagConstraints.NONE,
                                     new Insets(15, 25, 15, 25), 0, 0));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                wasCancel = true;
                CancelableOperationDialog.this.dispose();
            }
        });
        p.add(cancelButton,
              new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                                     GridBagConstraints.EAST,
                                     GridBagConstraints.NONE,
                                     new Insets(0, 0, 5, 5), 0, 0));
        if (this.progressBar != null) {
            p.add(progressBar,
                  new GridBagConstraints(1, 2, GridBagConstraints.REMAINDER, 1, 1000.0, 0.0,
                                         GridBagConstraints.CENTER,
                                         GridBagConstraints.HORIZONTAL,
                                         new Insets(5, 15, 5, 15), 0, 0));
        }
    }

    private void doPack() {
        pack();
        Utilities.centerOnParentWindow(this);
    }

    private void maybeInit() {
        if (needsInit) {
            doInit();
            needsInit = false;
        }
    }

    private void maybePack() {
        if (needsPack) {
            doPack();
            needsPack = false;
        }
    }

    private void requestPack() {
        if (isVisible())
            pack();
        else
            needsPack=true;
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            maybeInit();
            maybePack();
        }
        super.setVisible(b);
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
        requestPack(); // Repack after changing label (Bug #3686)
        Utilities.centerOnParentWindow(this);
    }

    public String getMessage() {
        return messageLabel.getText();
    }

    public boolean wasCancelled() {
        return wasCancel;
    }

    /**
     * Synchronously run the specified callable in a background thread, putting up a modal Cancel... dialog and returning
     * control to the user if the thread runs for longer than msBeforeDlg milliseconds.
     * <p/>
     * If the user cancels the dialog this will interrupt the background thread.  The callable may throw
     * InterruptedException to signal that this has occurred.
     * <p/>
     * This method must be called on the Swing event queue thread.
     * <p/>
     * This method makes use of {@link Utilities#doWithDelayedCancelDialog(java.util.concurrent.Callable, javax.swing.JDialog, long)}.
     *
     * @param callable      some work that may safely be done in a new thread.  Required.
     * @param parent        the parent window for the dialog. Optional.
     * @param dialogTitle   the title to display for the cancel dialog, if one is put up.  Required
     * @param dialogMessage the message to display inside the cancel dialog, if one is put up.  Required
     * @param msBeforeDlg  number of milliseconds to wait (blocking the event queue) before
     *                                            putting up the cancel dialog.  If less than one, defaults to 500ms.
     * @return the result of the callable.  May be null if the callable may return null.
     * @throws InterruptedException if the task was canceled by the user, or the Swing thread was interrupted
     * @throws java.lang.reflect.InvocationTargetException if the callable terminated with any exception other than InterruptedException
     */
    public static <T> T doWithDelayedCancelDialog(final Callable<T> callable,
                                                  final Window parent,
                                                  final String dialogTitle,
                                                  final String dialogMessage,
                                                  final long msBeforeDlg)
            throws InterruptedException, InvocationTargetException
    {
        final DialogFactoryShower factory = new DialogFactoryShower(new Functions.Nullary<JDialog>() {
            @Override
            public JDialog call() {
                final JProgressBar progressBar = new JProgressBar();
                progressBar.setIndeterminate(true);
                final CancelableOperationDialog cancelDialog =
                        new CancelableOperationDialog(parent, dialogTitle, dialogMessage, progressBar);
                cancelDialog.setModalityType(ModalityType.APPLICATION_MODAL);
                return cancelDialog;
            }
        });
        return Utilities.doWithDelayedCancelDialog(callable, factory, msBeforeDlg);
    }
}
