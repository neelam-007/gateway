package com.l7tech.console.logging;

import com.l7tech.console.panels.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;

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

    /**
     *
     * @return the stream attached ot the console
     */
    public OutputStream getOutputStream() {
        return new BufferedOutputStream(logOutputStream);
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
                /* dispose();
                instance = null;*/
                hide();
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
                // ConsoleDialog.this.dispose();
                hide();
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

    private OutputStream logOutputStream = new OutputStream() {
        /**
         * Writes the specified byte to this output stream. The general
         * contract for <code>write</code> is that one byte is written
         * to the output stream. The byte to be written is the eight
         * low-order bits of the argument <code>b</code>. The 24
         * high-order bits of <code>b</code> are ignored.
         * <p>
         * Subclasses of <code>OutputStream</code> must provide an
         * implementation for this method.
         *
         * @param      b   the <code>byte</code>.
         * @exception  IOException  if an I/O error occurs. In particular,
         *             an <code>IOException</code> may be thrown if the
         *             output stream has been closed.
         */
        public void write(final int b) throws IOException {
            SwingUtilities.
              invokeLater(new Runnable() {
                  public void run() {
                      console.append(Byte.toString((byte)b));
                  }
              });
        }

        /**
         * Writes <code>len</code> bytes from the specified byte array
         * starting at offset <code>off</code> to this output stream.
         *
         * @param      b     the data.
         * @param      off   the start offset in the data.
         * @param      len   the number of bytes to write.
         * @exception  IOException  if an I/O error occurs. In particular,
         *             an <code>IOException</code> is thrown if the output
         *             stream is closed.
         */
        public synchronized void write(final byte b[], int off, int len)
          throws IOException {
            SwingUtilities.
              invokeLater(new Runnable() {
                  public void run() {
                      console.append(new String(b));
                  }
              });
        }
    };

}
