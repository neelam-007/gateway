package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

/**
 * A dialog that lets a user specify a namespace URI and an associated prefix.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 16, 2004<br/>
 * $Id$
 */
public class NamespacePrefixQueryForm extends JDialog {
    private JPanel mainPanel;
    private JTextField uritxt;
    private JTextField prefixtxt;
    private JButton cancelbutton;
    private JButton okbutton;
    boolean cancelled = false;

    public String nsuri;
    public String prefix;

    public NamespacePrefixQueryForm(Dialog owner) {
        super(owner, true);
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle("XML Namespace");
        okbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        cancelbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        // implement default behavior for esc and enter keys
        KeyListener defBehaviorKeyListener = new KeyListener() {
            public void keyPressed(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancel();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    ok();
                }
            }
            public void keyTyped(KeyEvent e) {}
        };
        uritxt.addKeyListener(defBehaviorKeyListener);
        prefixtxt.addKeyListener(defBehaviorKeyListener);
        cancelbutton.addKeyListener(defBehaviorKeyListener);
        okbutton.addKeyListener(defBehaviorKeyListener);
    }

    private void ok() {
        nsuri = uritxt.getText();
        prefix = prefixtxt.getText();
        NamespacePrefixQueryForm.this.dispose();
    }

    private void cancel() {
        cancelled = true;
        NamespacePrefixQueryForm.this.dispose();
    }
}
