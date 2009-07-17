package com.l7tech.console.panels;

import com.l7tech.util.ValidationUtils;
import com.l7tech.gui.util.DialogDisplayer;

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


    public NamespacePrefixQueryForm(Dialog owner, String title) {
        super(owner, true);
        initialize(title);
    }

    public NamespacePrefixQueryForm(Dialog owner) {
        super(owner, true);
        initialize(null);
    }

    private void initialize(String title) {
        setContentPane(mainPanel);
        setTitle(title == null?"Add XML Namespace and Prefix":title);
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

    public void setInitialPrefix(String text) {
        this.prefixtxt.setText(text);
    }

    public void setInitialNsUri(String uri) {
        this.uritxt.setText(uri);
    }

    private void ok() {
        if (! ValidationUtils.isProbablyValidXmlNamespacePrefix(prefixtxt.getText())) {
            DialogDisplayer.showMessageDialog(this, "'" + prefixtxt.getText() + "' is an invalid namespace prefix.  Please correct it and try again.",
                "Invalid Prefix", JOptionPane.ERROR_MESSAGE, null);
            return;
        }
        nsuri = uritxt.getText();
        prefix = prefixtxt.getText();
        NamespacePrefixQueryForm.this.dispose();
    }

    private void cancel() {
        cancelled = true;
        NamespacePrefixQueryForm.this.dispose();
    }
}
