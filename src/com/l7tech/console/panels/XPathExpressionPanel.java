package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Map;

/**
 * A dialog for editing an XPath expression for non-soap applications
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 18, 2004<br/>
 * $Id$
 */
public class XPathExpressionPanel extends JDialog {
    private JButton cancelButton;
    private JButton okButton;
    private JButton namespaceButton;
    private JPanel mainPanel;
    private JTextField xpathField;
    private boolean canceled = false;
    private Map initialNamespaces;
    private String newxpathvalue;

    public XPathExpressionPanel(Frame parent, String title, String initialXpath, Map initialNamespaces) {
        super(parent, true);
        this.initialNamespaces = initialNamespaces;
        initialize(title, initialXpath);
    }

    public boolean wasCanceled() {
        return canceled;
    }

    public String newXpathValue() {
        return newxpathvalue;
    }

    public Map newXpathNamespaceMap() {
        return initialNamespaces;
    }

    private void initialize(String title, String initialXpath) {
        setContentPane(mainPanel);
        setTitle(title);
        setButtonActions();
        xpathField.setText(initialXpath);
    }

    private void setButtonActions() {
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        namespaceButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                NamespaceMapEditor nseditor = new NamespaceMapEditor(XPathExpressionPanel.this,
                                                                     initialNamespaces,
                                                                     null);
                nseditor.pack();
                nseditor.show();
                Map newMap = nseditor.newNSMap();
                if (newMap != null) {
                    initialNamespaces = newMap;
                }
            }
        });
    }

    private void cancel() {
        canceled = true;
        XPathExpressionPanel.this.dispose();
    }

    private void ok() {
        // todo, validate xpath value?
        newxpathvalue = xpathField.getText();
        XPathExpressionPanel.this.dispose();
    }
}
