package com.l7tech.console.logging;

import com.l7tech.console.panels.Utilities;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.BadLocationException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements a frame that displays the applicaiton
 * log.
 * todo: change the functionlaity is so it maps over the log output file.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */

public class ConsoleDialog extends JFrame {

    private static ConsoleDialog instance = null;

    public static synchronized ConsoleDialog getInstance() {
        if (instance != null) return instance;
        instance = new ConsoleDialog();
        return instance;
    }


    private JTextArea logTextArea;
    private ConsoleDialog() {
        super("Application Console and logs");

        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        JScrollPane scroller = new JScrollPane();
        scroller.setPreferredSize(new Dimension(400, 400));
        scroller.getViewport().add(logTextArea);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scroller, BorderLayout.CENTER);
        panel.add(createTopBarPanel(), BorderLayout.NORTH);
        panel.add(createButtonPanel(), BorderLayout.SOUTH);

        getContentPane().add(panel, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                /* dispose();
                instance = null;*/
                hide();
            }
        });
        pack();
        Utilities.centerOnScreen(this);
    }

    JPanel createTopBarPanel() {
        BorderLayout layout = new BorderLayout();

        JPanel panel = new JPanel(layout);

        JSlider slider = new JSlider(0, 160);
        slider.setMajorTickSpacing(40);
        Dictionary table = new Hashtable();
        table.put(new Integer(0), new JLabel("finest"));
        table.put(new Integer(40), new JLabel("info"));
        table.put(new Integer(80), new JLabel("warning"));
        table.put(new Integer(120), new JLabel("severe"));
        table.put(new Integer(160), new JLabel("off"));
        slider.setPaintLabels(true);
        slider.setLabelTable(table);
        slider.setSnapToTicks(true);
        slider.addChangeListener( new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider)e.getSource();
                    if (!source.getValueIsAdjusting()) {
                        int value = source.getValue();
                        switch (value) {
                            case   0:
                                adjustHandlerLevel(Level.FINEST);
                                break;
                            case  40:
                                adjustHandlerLevel(Level.INFO);
                                break;
                            case  80:
                                adjustHandlerLevel(Level.WARNING);
                                break;
                            case 120:
                                adjustHandlerLevel(Level.SEVERE);
                                break;
                            case 160:
                                adjustHandlerLevel(Level.OFF);
                                break;
                            default:
                                System.err.println("Unhandled value "+value);
                        }
                    }
            }
        });

        panel.add(slider, BorderLayout.NORTH);
        return panel;
    }

    JPanel createButtonPanel() {
        GridLayout layout = new GridLayout(1, 3);
        layout.setHgap(5);

        JPanel panel = new JPanel(layout);

        JButton dismiss = new JButton("Dismiss");
        dismiss.setToolTipText("Dismiss window");
        dismiss.addActionListener(new ActionListener() {
            /**
             * Invoked when an action occurs.
             */
            public void actionPerformed(ActionEvent e) {
                // ConsoleDialog.this.dispose();
                hide();
            }
        });

        JButton clear = new JButton("Clear");
        clear.setToolTipText("Clear logTextArea");
        clear.addActionListener(new ActionListener() {
            /**
             * Invoked when an action occurs.
             */
            public void actionPerformed(ActionEvent e) {
                logTextArea.setText("");
            }
        });

        panel.add(new JLabel()); // filler
        panel.add(dismiss);
        panel.add(clear);
        panel.add(new JLabel()); // filler

        return panel;
    }

    /**
     * sink the message to the console text area
     * @param msg
     */
    final synchronized void sink(final String msg) {
        SwingUtilities.
          invokeLater(new Runnable() {
              public void run() {
                  Document doc = logTextArea.getDocument();
                  try {
                      if (doc.getLength() >= MAX_LOG_SIZE) {
                          //todo: extend the Document as the default
                          // one is super slow for what we want
                        doc.remove(0, MAX_LOG_SIZE/2);
                      }
                      doc.insertString(doc.getLength(), msg, null);
                  } catch (BadLocationException e) {
                      e.printStackTrace(System.err);
                  }
              }
          });
    }

    private void adjustHandlerLevel(Level l) {
        Handler[] handlers = Logger.global.getHandlers();
        System.err.println("Adjusting level "+l);
        for (int i =0; i< handlers.length; i++) {
            if (handlers[i] instanceof DefaultHandler) {
                handlers[i].setLevel(l);
            }
        }
    }

    private static final int MAX_LOG_SIZE = 1024*1024;
}
