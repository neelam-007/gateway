package com.l7tech.console.logging;

import com.l7tech.console.panels.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * This class implements a frame that displays the applicaiton
 * log.
 *
 * @author
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
                  logTextArea.append(msg);
              }
          });
    }
}
