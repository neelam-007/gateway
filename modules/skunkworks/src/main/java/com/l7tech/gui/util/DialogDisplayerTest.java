package com.l7tech.gui.util;

import com.l7tech.gui.util.SheetHolder;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Standalone test for reproducing Bug #3969.
 */
public class DialogDisplayerTest {
    private static class TopWindow extends JFrame implements SheetHolder {
        public void showSheet(JInternalFrame sheet) {
            DialogDisplayer.showSheet(this, sheet);
        }
    }

    private static final String labelText = "       Blah       ";
    private static final JLabel label = new JLabel(labelText);

    private static class MyDialog extends JDialog {
        public MyDialog(Frame owner) {
            super(owner, "Test Dialog", true);
            Container cp = getContentPane();
            cp.setLayout(new FlowLayout());
            cp.add(label);

            JButton big = new JButton("big");
            cp.add(big);
            big.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    label.setPreferredSize(new Dimension(300, 300));
                    MyDialog.this.firePropertyChange("_Layer7_sheetRepack", null, Boolean.TRUE);
                    MyDialog.this.pack();
                }
            });

            JButton small = new JButton("small");
            cp.add(small);
            small.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    label.setPreferredSize(null);
                    label.setText("foo");
                    label.setText(labelText);
                    MyDialog.this.firePropertyChange("_Layer7_sheetRepack", null, Boolean.TRUE);
                    MyDialog.this.pack();
                }
            });

            JButton close = new JButton("close");
            cp.add(close);
            close.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MyDialog.this.dispose();
                }
            });
        }
    }

    public static void main(String[] args) {
        JFrame top = new TopWindow();
        top.setTitle("DialogDisplayerTest");
        top.setSize(700, 500);
        Utilities.centerOnScreen(top);
        top.setVisible(true);

        int result = JOptionPane.showConfirmDialog(top, "Use sheets?", "DialogDisplayerTest", JOptionPane.YES_NO_OPTION);
        DialogDisplayer.setForceNative(result != JOptionPane.YES_OPTION);

        MyDialog dlg = new MyDialog(top);
        Utilities.centerOnScreen(dlg);
        dlg.pack();
        
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                System.exit(0);
            }
        });
    }
}
