package com.l7tech.console.panels;

import com.l7tech.adminws.logging.Log;
import com.l7tech.common.util.Locator;
import com.l7tech.logging.LogMessage;
import com.ibm.xml.policy.xacl.builtIn.provisional_action.log;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Vector;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.rmi.RemoteException;

/**
 * Created by IntelliJ IDEA.
 * User: fpang
 * Date: Sep 12, 2003
 * Time: 4:12:07 PM
 * To change this template use Options | File Templates.
 */
public class LogPanel {

    private JPanel selectPane = null;
    private JPanel msgPane = null;
    private JPanel filter = new JPanel();
    private JCheckBox severe = new JCheckBox();
    private JCheckBox warning = new JCheckBox();
    private JCheckBox info = new JCheckBox();
    private JCheckBox finest = new JCheckBox();
    private JPanel control = new JPanel();
    private JCheckBox details = new JCheckBox();
    private JButton Refresh = new JButton();
    private JSplitPane jSplitPane1 = new JSplitPane();
    private JScrollPane jScrollPane1 = new JScrollPane();
    private JTable msgTable;
    private JScrollPane jScrollPane2 = new JScrollPane();
    private JTextArea msgDetails = new JTextArea();

    public static LogPanel          logPane = null;
    private JPanel jLogPane = new JPanel();
    private AbstractTableModel logTableModel = null;

    public static LogPanel   instance() {
        if(logPane == null) {
           logPane = new LogPanel ();
        }

        return logPane;
    }

    public JPanel getPane()
    {
         return jLogPane;
    }


    private LogPanel()
    {
           jLogPane.setLayout(new BoxLayout(jLogPane, BoxLayout.Y_AXIS));

           jLogPane.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0)));
           jLogPane.setMinimumSize(new Dimension(100,150));

           selectPane = new JPanel();
           selectPane.setLayout(new FlowLayout(FlowLayout.LEFT));

            filter.setLayout(new BorderLayout());
                  JSlider slider = new JSlider(0, 160);
                  slider.setMajorTickSpacing(40);

                  Dictionary table = new Hashtable();
                  JLabel aLabel = new JLabel("finest");

                  aLabel.setFont(new java.awt.Font("Dialog", 0, 11));
                  table.put(new Integer(0), aLabel);

                  aLabel = new JLabel("info");
                  aLabel.setFont(new java.awt.Font("Dialog", 0, 11));
                  table.put(new Integer(40), aLabel);

                  aLabel = new JLabel("warning");
                  aLabel.setFont(new java.awt.Font("Dialog", 0, 11));
                  table.put(new Integer(80), aLabel);

                  aLabel = new JLabel("severe");
                  aLabel.setFont(new java.awt.Font("Dialog", 0, 11));
                  table.put(new Integer(120), aLabel);

                  aLabel = new JLabel("off");
                  aLabel.setFont(new java.awt.Font("Dialog", 0, 11));
                  table.put(new Integer(160), aLabel);

                  slider.setPaintLabels(true);
                  slider.setLabelTable(table);
                  slider.setSnapToTicks(true);
                  slider.addChangeListener(new ChangeListener() {
                      public void stateChanged(ChangeEvent e) {
                          JSlider source = (JSlider)e.getSource();
                          if (!source.getValueIsAdjusting()) {
                              int value = source.getValue();
                              switch (value) {
                                  case 0:
                                      //adjustHandlerLevel(Level.FINEST);
                                      break;
                                  case 40:
                                      //adjustHandlerLevel(Level.INFO);
                                      break;
                                  case 80:
                                      //adjustHandlerLevel(Level.WARNING);
                                      break;
                                  case 160:
                                      //adjustHandlerLevel(Level.SEVERE);
                                      break;
                                  case 240:
                                      //adjustHandlerLevel(Level.OFF);
                                      break;
                                  default:
                                      System.err.println("Unhandled value " + value);
                              }
                          }
                      }
                  });

         filter.add(slider);
         control.setLayout(new FlowLayout());
        details.setFont(new java.awt.Font("Dialog", 0, 11));
        details.setText("Details");
        details.setSelected(true);
        details.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                detailsActionPerformed(evt);
            }
        });

        control.add(details);
        Refresh.setFont(new java.awt.Font("Dialog", 0, 11));
        Refresh.setText("Refresh");
        Refresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RefreshActionPerformed(evt);
            }
        });

        control.add(Refresh);
        selectPane.add(filter);
        selectPane.add(control);

        msgTable = new JTable(getLogTableModel(), getLogColumnModel());

        msgTable.setShowHorizontalLines(false);
        msgTable.setShowVerticalLines(false);
        msgTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        jScrollPane1.setViewportView(msgTable);

        jSplitPane1.setLeftComponent(jScrollPane1);

        msgDetails.setEditable(false);
        jScrollPane2.setViewportView(msgDetails);
        msgDetails.setMinimumSize(new Dimension(0,0));

        jSplitPane1.setRightComponent(jScrollPane2);
        jSplitPane1.setResizeWeight(0.5);
        jScrollPane2.setMinimumSize(new Dimension(0,0));

        msgPane = new JPanel();
        msgPane.setLayout(new BorderLayout());

        msgPane.add(jSplitPane1, BorderLayout.CENTER);
        jLogPane.add(msgPane);
        jLogPane.add(selectPane);

        // the logPane is not shown default
        jLogPane.setVisible(false);

        msgTable.getSelectionModel().
           addListSelectionListener(new ListSelectionListener() {
              /**
               * Called whenever the value of the selection changes.
               * @param e the event that characterizes the change.
               */
              public void valueChanged(ListSelectionEvent e) {
                  int row = msgTable.getSelectedRow();
                  String msg;

                  if (row == -1) return;

                 //msg = "The selected row is: " + row + "\n";
                  msg = "";

                  if(msgTable.getModel().getValueAt(row, 0) != null)
                      msg = msg + "Time    : " + msgTable.getModel().getValueAt(row, 0).toString() + "\n";
                  if(msgTable.getModel().getValueAt(row, 1) != null)
                      msg = msg + "Severity: " + msgTable.getModel().getValueAt(row, 1).toString() + "\n";
                  if( msgTable.getModel().getValueAt(row, 2) != null)
                      msg = msg + "Message : " + msgTable.getModel().getValueAt(row, 2).toString() + "\n";
                  if( msgTable.getModel().getValueAt(row, 3) != null)
                      msg = msg + "Class   : " + msgTable.getModel().getValueAt(row, 3).toString() + "\n";
                  if( msgTable.getModel().getValueAt(row, 4) != null)
                      msg = msg + "Method  :" + msgTable.getModel().getValueAt(row, 4).toString() + "\n";
                  msgDetails.setText(msg);

              }
          });
    }


    private DefaultTableColumnModel getLogColumnModel() {
       DefaultTableColumnModel columnModel = new DefaultTableColumnModel();

       columnModel.addColumn(new TableColumn(0, 60));
       columnModel.addColumn(new TableColumn(1, 15));
       columnModel.addColumn(new TableColumn(2, 200));
       columnModel.addColumn(new TableColumn(3, 100));
       columnModel.addColumn(new TableColumn(4, 30));
       columnModel.getColumn(0).setHeaderValue(logTableModel.getColumnName(0));
       columnModel.getColumn(1).setHeaderValue(logTableModel.getColumnName(1));
       columnModel.getColumn(2).setHeaderValue(logTableModel.getColumnName(2));
       columnModel.getColumn(3).setHeaderValue(logTableModel.getColumnName(3));
       columnModel.getColumn(4).setHeaderValue(logTableModel.getColumnName(4));

        return columnModel;
}

    /**
       * create the table model with log fields
       *
       * @return the <code>AbstractTableModel</code> for the
       * log pane
       */
      private AbstractTableModel getLogTableModel() {
          if (logTableModel != null) {
              return logTableModel;
          }


       String[] cols = {"Time", "Severity", "Message", "Class", "Method"};
       String[][] rows = new String [][] {};

        logTableModel = new DefaultTableModel(rows, cols);

          return logTableModel;
      }

    public void getLogs() {

        Log log = (Log) Locator.getDefault().lookup(Log.class);
        if (log == null) throw new IllegalStateException("cannot obtain log remote reference");

        try {
            String[] rawLogs = log.getSystemLog(0, 500);

            while(msgTable.getRowCount() > 0)
           {
                 System.out.println("Number of row left is: " + msgTable.getRowCount() + "\n");
                ((DefaultTableModel)(msgTable.getModel())).removeRow(0);
           }
 //           ((DefaultTableModel)msgTable.getModel()).rowsRemoved(new TableModelEvent(msgTable.getModel()));
            for (int i = 0; i < rawLogs.length; i++) {
                 //msgTable.setValueAt(rawLogs[i], i, 0);
                Vector newRow = new Vector();

                LogMessage logMsg = new LogMessage(rawLogs[i]);
                newRow.add(logMsg.getTime());
                newRow.add(logMsg.getSeverity());
                newRow.add(logMsg.getMessageDetail());
                newRow.add(logMsg.getMessageClass());
                newRow.add(logMsg.getMessageMethod());
                ((DefaultTableModel)msgTable.getModel()).addRow(newRow);
                System.out.println("adding a new row (" + i + ") .....\n");
                System.out.println(rawLogs[i]);
            }

            ((AbstractTableModel)(msgTable.getModel())).fireTableDataChanged();

        }
        catch (RemoteException e) {
            System.err.println("Unable to retrieve logs from server");
        }
    }

    private void RefreshActionPerformed(java.awt.event.ActionEvent evt) {
        getLogs();
    }

    private void detailsActionPerformed(java.awt.event.ActionEvent evt) {
        if(details.isSelected())
        {
            jSplitPane1.setDividerLocation(0.7);
            msgDetails.setVisible(true);
         }
        else
        {
            hideMsgDetails();
        }
    }

    private void finestActionPerformed(java.awt.event.ActionEvent evt) {
        // Add your handling code here:
    }

    private void infoActionPerformed(java.awt.event.ActionEvent evt) {
        // Add your handling code here:
    }

    private void warningActionPerformed(java.awt.event.ActionEvent evt) {
        // Add your handling code here:
    }

    private void severeActionPerformed(java.awt.event.ActionEvent evt) {
        // Add your handling code here:
    }

    private void hideMsgDetails() {

         jSplitPane1.setDividerLocation(jSplitPane1.getMaximumDividerLocation());
         msgDetails.setVisible(false);
     }

 /*
     private Action getLogRefreshAction() {
        if (logRefreshAction != null) return logRefreshAction;
        String atext = resapplication.getString("Refresh_MenuItem_text");
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/Refresh16.gif"));
        logRefreshAction =
          new AbstractAction(atext, icon) {

              public void actionPerformed(ActionEvent event) {
                  // todo: retrieve the logs from ssg
              }
          };
        logRefreshAction.setEnabled(false);
        logRefreshAction.putValue(Action.SHORT_DESCRIPTION, atext);
        return logRefreshAction;
    }

*/
}
