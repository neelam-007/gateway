/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Mar 2, 2005<br/>
 */
package com.l7tech.proxy.gui.dialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * A dialog that provides different options for agent users that dont already have
 * client certs.
 *
 * @author flascelles@layer7-tech.com
 */
public class NoClientCert extends JDialog {
    private JPanel mainPanel;
    private JLabel lbl1;
    private JButton csrButton;
    private JButton importButton;
    private JButton helpButton;
    private JButton closeButton;
    private JLabel lbl2;
    public static final int CLOSED = 0;
    public static final int REQUESTED_IMPORT = 1;
    public static final int REQUESTED_CSR = 2;
    private int exitCondition = CLOSED;

    public NoClientCert(Dialog owner, String ssgName) {
        super(owner, true);
        initialize(ssgName);
    }

    public NoClientCert(Frame owner, String ssgName) {
        super(owner, true);
        initialize(ssgName);
    }

    /**
     * @return CLOSED, REQUESTED_IMPORT, or REQUESTED_CSR
     */
    public int getExitCondition() {
        return exitCondition;
    }

    private void initialize(String ssgName) {
        setContentPane(mainPanel);
        setTitle("Client Certificate Not Found");
        lbl1.setText("You do not have a client certificate for use with");
        lbl2.setText("the SecureSpan Gateway " + ssgName + ".");
        closeButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                NoClientCert.this.dispose();
            }
        });
        importButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                exitCondition = REQUESTED_IMPORT;
                NoClientCert.this.dispose();
            }
        });
        csrButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                exitCondition = REQUESTED_CSR;
                NoClientCert.this.dispose();
            }
        });
        helpButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                // todo, plug this help thing in
            }
        });

        AbstractAction closeaction = new AbstractAction() {
                                        public void actionPerformed(ActionEvent e) {
                                            NoClientCert.this.dispose();
                                        }
                                     };

        JLayeredPane layeredPane = getLayeredPane();
        final KeyStroke escKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        final KeyStroke enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        layeredPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escKeyStroke, "close-it");
        layeredPane.getInputMap(JComponent.WHEN_FOCUSED).put(escKeyStroke, "close-it");
        layeredPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(escKeyStroke, "close-it");
        layeredPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(enterKeyStroke, "close-it");
        layeredPane.getInputMap(JComponent.WHEN_FOCUSED).put(enterKeyStroke, "close-it");
        layeredPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(enterKeyStroke, "close-it");
        layeredPane.getActionMap().put("close-it", closeaction);
    }

    public static void main(String[] args) {
        NoClientCert dlg =  new NoClientCert((Frame)null, "blah.acme.com");
        dlg.pack();
        dlg.show();
        System.exit(0);
    }
}
