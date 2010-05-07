package com.l7tech.console.panels;

import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Undecorated dialog to show the user an information message which will dispose on the first key stroke or when it loses
 * focus or is clicked.
 * The dialog will self destruct after a default time of 2 seconds, if not set via a constructor
 * Dialog feels light weight and not as intrusive as a normal pop up message.
 * Has the ability to report on whether a specific key stroke causes the dialog to dismiss.
 *
 */
public class InformationDialog extends JDialog {
    private JPanel contentPane;
    private JLabel msgLabel;
    private boolean specialKeyDisposed = false;
    private boolean firstKeyEvent = true;

    public InformationDialog(String msg){
        this(msg, 0, false);
    }

    public InformationDialog(String msg, final int specialKey){
        this(msg, specialKey, false);
    }

    /**
     * Create an undecorated Dialog which will simply show the supplied String msg with a yellow background.
     * Any key stroke or a loss of focus causes this dialog to dispose.
     *
     * @param msg          String text to show
     * @param specialKey   int if the key with this code is used to dispose the dialog, then isSpecialKeyDisposed() will
     *                     return true
     * @param requiresMask boolean, true if the specialKey requires a mask to make it's key code e.g. Shift + F3
     */
    public InformationDialog(final String msg, final int specialKey, final boolean requiresMask) {
        //by setting the parent, the dialog will not be show it's own task bar button
        super(TopComponents.getInstance().getTopParent());
        
        setContentPane(contentPane);
        contentPane.setBackground(new Color(0xFF, 0xFF, 0xe1));
        msgLabel.setText(msg);
        setUndecorated(true);
        final KeyAdapter adapter = new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                processEvent(e);
            }

            @Override
            public void keyPressed(KeyEvent e) {//special keys e.g. F3 will only be reported here
                processEvent(e);
            }

            private void processEvent(KeyEvent e) {
                if (e.getKeyCode() == specialKey && !requiresMask) {
                    specialKeyDisposed = true;
                } else if( e.getKeyCode() == specialKey && !firstKeyEvent){
                    specialKeyDisposed = true;
                }

                if (firstKeyEvent && requiresMask) {
                    firstKeyEvent = false;
                    if(e.getModifiers() == 0){
                        //if the first key was not a modifier (e.g. shift / ctrl) don't need to wait for the second key
                        dispose();
                    }
                } else {
                    dispose();
                }
            }
        };

        this.addKeyListener(adapter);

        this.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                //nothing to do
            }

            @Override
            public void focusLost(FocusEvent e) {
                dispose();
            }
        });

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
            }
        });
    }

    public boolean isSpecialKeyDisposed() {
        return specialKeyDisposed;
    }

    public static void main(String[] args) {
        InformationDialog dialog = new InformationDialog("Test msg", KeyEvent.VK_F3);
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
