package com.l7tech.console.logging;

import com.l7tech.console.panels.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

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

    private JTextArea console;
    private ConsoleDialog() {
        super("Application Console and logs");

        console = new JTextArea();
        console.setEditable(false);
        JScrollPane scroller = new JScrollPane();
        scroller.setPreferredSize(new Dimension(400, 400));
        scroller.getViewport().add(console);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scroller, BorderLayout.CENTER);
        panel.add(createButtonPanel(), BorderLayout.SOUTH);

        getContentPane().add(panel, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
                instance = null;
            }
        });
        pack();
        Utilities.centerOnScreen(this);
    }

    JPanel createButtonPanel() {
        GridLayout layout = new GridLayout(1, 4);
        layout.setHgap(5);

        JPanel panel = new JPanel(layout);

        JButton dismiss = new JButton("Dismiss");
        dismiss.setToolTipText("Dismiss window");
        dismiss.addActionListener(new ActionListener() {
            /**
             * Invoked when an action occurs.
             */
            public void actionPerformed(ActionEvent e) {
                // Get rid of window
                ConsoleDialog.this.dispose();
            }
        });

        JButton clear = new JButton("Clear");
        clear.setToolTipText("Clear console");
        clear.addActionListener(new ActionListener() {
            /**
             * Invoked when an action occurs.
             */
            public void actionPerformed(ActionEvent e) {
            }
        });

        panel.add(new JLabel()); // filler
        panel.add(dismiss);
        panel.add(clear);
        panel.add(new JLabel()); // filler

        return panel;
    }


    /**
     * Methods for managing stack traces that are displayed in console.
     */
    void appendToConsole(Exception e) {
        ByteArrayOutputStream str = new ByteArrayOutputStream();
        PrintStream pr = new PrintStream(str);
        e.printStackTrace(pr);
        pr.flush();
        synchronized (console) {
            console.append(str.toString());
        }
    }

    void clearConsole() {
        synchronized (console) {
            console.setText("");
        }
    }
}
