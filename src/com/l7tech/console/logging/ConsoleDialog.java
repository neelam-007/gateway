package com.l7tech.console.logging;

import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.io.File;

/**
 * This class implements a frame that displays the applicaiton
 * log.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */

public class ConsoleDialog extends JFrame {

    private File logFile;

    private JTextArea logTextArea;

    public ConsoleDialog(File file) {
        super("Manager Log");
        if (file == null) {
            throw new IllegalArgumentException("file == null");
        }
        logFile = file;
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
                dispose();
            }
        });
        pack();
        Utilities.centerOnScreen(this);
    }

    JPanel createTopBarPanel() {
        BorderLayout layout = new BorderLayout();

        JPanel panel = new JPanel(layout);

        JSlider slider = new JSlider(0, 150);
        slider.setMajorTickSpacing(30);
        Dictionary table = new Hashtable();

        Level dl = Level.INFO;
        Handler handler = null;

        if (handler != null) {
            dl = handler.getLevel();
        }

        table.put(new Integer(0), new JLabel("all"));
        table.put(new Integer(30), new JLabel("finest"));
        table.put(new Integer(60), new JLabel("info"));
        table.put(new Integer(90), new JLabel("warning"));
        table.put(new Integer(120), new JLabel("severe"));
        table.put(new Integer(150), new JLabel("off"));

        Enumeration en = table.keys();
        while (en.hasMoreElements()) {
            Object key = en.nextElement();
            JLabel l = (JLabel) table.get(key);
            if (l.getText().equalsIgnoreCase(dl.getName())) {
                slider.setValue(((Integer) key).intValue());
                break;
            }
        }

        slider.setPaintLabels(true);
        slider.setLabelTable(table);
        slider.setSnapToTicks(true);
        slider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                if (!source.getValueIsAdjusting()) {
                    int value = source.getValue();
                    switch (value) {
                        case 0:
                            adjustHandlerLevel(Level.ALL);
                        case 30:
                            adjustHandlerLevel(Level.FINEST);
                            break;
                        case 60:
                            adjustHandlerLevel(Level.INFO);
                            break;
                        case 90:
                            adjustHandlerLevel(Level.WARNING);
                            break;
                        case 120:
                            adjustHandlerLevel(Level.SEVERE);
                            break;
                        case 150:
                            adjustHandlerLevel(Level.OFF);
                            break;
                        default:
                            System.err.println("Unhandled value " + value);
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
                                doc.remove(0, MAX_LOG_SIZE / 2);
                            }
                            doc.insertString(doc.getLength(), msg, null);
                        } catch (BadLocationException e) {
                            e.printStackTrace(System.err);
                        }
                    }
                });
    }

    /**
     * Adjust the default handler log level
     *
     * @param l the new level
     */
    private void adjustHandlerLevel(Level l) {
        Handler handler = null;
        if (handler != null) {
            handler.setLevel(l);
        }
    }

    private static final int MAX_LOG_SIZE = 1024 * 1024;
}
