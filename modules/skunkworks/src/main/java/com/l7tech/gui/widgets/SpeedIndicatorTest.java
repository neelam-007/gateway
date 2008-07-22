/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.gui.widgets;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.SpeedIndicator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Test driver for {@link com.l7tech.gui.widgets.SpeedIndicator} widget.
 */
public class SpeedIndicatorTest {
    public static void main(String[] args) {
        JDialog d = new JDialog();
        d.setTitle("SpeedIndicator test");
        d.setModal(true);
        JPanel p = new JPanel(new FlowLayout());
        d.setContentPane(p);

        final SpeedIndicator si = new SpeedIndicator();
        si.setPreferredSize(new Dimension(16, 16));
        si.setMaximumSize(new Dimension(16, 16));
        p.add(si, BorderLayout.CENTER);

        final int[] curSpeed = new int[] { 1 };
        si.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (++curSpeed[0] > 5) curSpeed[0] = 0;
                si.setSpeed(curSpeed[0]);
                System.out.println("Speed = " + curSpeed[0]);
            }
        });

        Utilities.centerOnScreen(d);
        d.pack();
        d.setVisible(true);
        System.exit(0);
    }
}
