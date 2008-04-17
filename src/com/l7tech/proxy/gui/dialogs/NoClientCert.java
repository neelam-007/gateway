/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Mar 2, 2005<br/>
 */
package com.l7tech.proxy.gui.dialogs;

import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    private JButton closeButton;
    private JLabel lbl2;
    public static final int CLOSED = 0;
    public static final int REQUESTED_IMPORT = 1;
    public static final int REQUESTED_CSR = 2;
    private int exitCondition = CLOSED;

    public NoClientCert(Dialog owner, String ssgName, boolean allowCsr) {
        super(owner, true);
        initialize(ssgName, allowCsr);
    }

    public NoClientCert(Frame owner, String ssgName, boolean allowCsr) {
        super(owner, true);
        initialize(ssgName, allowCsr);
    }

    /**
     * @return CLOSED, REQUESTED_IMPORT, or REQUESTED_CSR
     */
    public int getExitCondition() {
        return exitCondition;
    }

    private void initialize(String ssgName, boolean allowCsr) {
        setContentPane(mainPanel);
        setTitle("Client Certificate Not Found");
        lbl1.setText("There is no client certificate for use with");
        lbl2.setText(ssgName + '.');
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
        if (allowCsr) {
            csrButton.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    exitCondition = REQUESTED_CSR;
                    NoClientCert.this.dispose();
                }
            });
        } else {
            csrButton.setEnabled(false);
            csrButton.setVisible(false);
        }

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

        Utilities.equalizeButtonSizes(new AbstractButton[] {csrButton, importButton});
    }

    public static void main(String[] args) {
        NoClientCert dlg =  new NoClientCert((Frame)null, "blah.acme.com", true);
        dlg.pack();
        dlg.setVisible(true);
        System.exit(0);
    }
}
