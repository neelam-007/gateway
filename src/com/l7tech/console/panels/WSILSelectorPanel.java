package com.l7tech.console.panels;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableModel;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TooManyChildElementsException;
import com.l7tech.console.action.Actions;

/**
 * A panel used to ask the SSM administrator to select a WSDL target among the multiple WSDL targets
 * contained in a WSIL document.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Jan 3, 2005<br/>
 */
public class WSILSelectorPanel extends JDialog {
    private Document wsilXml;
    private boolean cancelled = false;
    private String selectedURL;
    private JPanel mainPanel;
    private JTable wsilTable;
    private JButton helpbutton;
    private JButton cancelbutton;
    private JButton okbutton;

    public WSILSelectorPanel(Dialog parent, Document wsil) {
        super(parent, true);
        wsilXml = wsil;
        initialize();
    }

    public boolean wasCancelled() {
        return cancelled;
    }

    public String selectedWSDLURL() {
        return selectedURL;
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle("Select WSDL target from WSIL list");
        setTableModel();
        setListeners();
        okbutton.setEnabled(false);
    }

    private void setTableModel() {
        final ArrayList serviceNames = new ArrayList();
        final ArrayList wsdlURLs = new ArrayList();
        // build list from wsil doc's contents
        NodeList servicenodes = wsilXml.getElementsByTagNameNS("http://schemas.xmlsoap.org/ws/2001/10/inspection/",
                                                               "service");
        for (int i = 0; i < servicenodes.getLength(); i++) {
            Element servicenode = (Element)servicenodes.item(i);
            String name = null;
            String wsdlUrl = null;
            try {
                Element abstractEl = XmlUtil.findOnlyOneChildElementByName(servicenode,
                                                                           "http://schemas.xmlsoap.org/ws/2001/10/inspection/",
                                                                           "abstract");
                if (abstractEl != null) {
                    name = XmlUtil.getTextValue(abstractEl);
                }
                Element descEl = XmlUtil.findOnlyOneChildElementByName(servicenode,
                                                                       "http://schemas.xmlsoap.org/ws/2001/10/inspection/",
                                                                       "description");
                if (descEl != null) {
                    wsdlUrl = descEl.getAttribute("location");
                }
            } catch (TooManyChildElementsException e) {
                throw new RuntimeException("Unsupported WSIL syntax", e);
            }
            if (wsdlUrl != null && wsdlUrl.length() > 1) {
                wsdlURLs.add(wsdlUrl);
                if (name != null && name.length() > 1) {
                    serviceNames.add(name);
                } else {
                    serviceNames.add("??");
                }
            }
        }
        TableModel model = new AbstractTableModel() {
            public int getColumnCount() {
                return 2;
            }
            public int getRowCount() {
                return serviceNames.size();
            }
            public Object getValueAt(int rowIndex, int columnIndex) {
                switch (columnIndex) {
                    case 0: return serviceNames.get(rowIndex);
                    case 1: return wsdlURLs.get(rowIndex);
                }
                return "";
            }
            public String getColumnName(int columnIndex) {
                switch (columnIndex) {
                    case 0: return "Service Name";
                    case 1: return "WSDL URL";
                }
                return "";
            }
        };
        wsilTable.setModel(model);
    }

    private void setListeners() {

        okbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        helpbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                help();
            }
        });

        cancelbutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        wsilTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                // get new selection
                int selectedRow = wsilTable.getSelectedRow();
                // decide whether or not the remove button should be enabled
                if (selectedRow < 0) {
                    okbutton.setEnabled(false);
                } else {
                    okbutton.setEnabled(true);
                    selectedURL = (String)wsilTable.getValueAt(selectedRow, 1);
                }
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
        wsilTable.addKeyListener(defBehaviorKeyListener);
        okbutton.addKeyListener(defBehaviorKeyListener);
        helpbutton.addKeyListener(defBehaviorKeyListener);
        cancelbutton.addKeyListener(defBehaviorKeyListener);
    }

    private void ok() {
        WSILSelectorPanel.this.dispose();
    }

    private void cancel() {
        cancelled = true;
        WSILSelectorPanel.this.dispose();
    }

    private void help() {
        Actions.invokeHelp(WSILSelectorPanel.this);
    }
}
