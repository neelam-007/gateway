package com.l7tech.console.util;

import javax.swing.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

/**
 * This util class is used to synchronize two scroll panes in vertical and horizontal directions.
 */
public class ScrollPanesSynchronizer implements AdjustmentListener {
    final JScrollBar horizontalBar1;
    final JScrollBar horizontalBar2;
    final JScrollBar verticalBar1;
    final JScrollBar verticalBar2;

    public ScrollPanesSynchronizer(JScrollPane scrollPane1, JScrollPane scrollPane2) {
        horizontalBar1 = scrollPane1.getHorizontalScrollBar();
        horizontalBar2 = scrollPane2.getHorizontalScrollBar();
        verticalBar1 = scrollPane1.getVerticalScrollBar();
        verticalBar2 = scrollPane2.getVerticalScrollBar();
    }

    public void adjustmentValueChanged(AdjustmentEvent e) {
        final JScrollBar sourceScrollBar = (JScrollBar)e.getSource();
        final int value = sourceScrollBar.getValue();
        final JScrollBar targetScrollBar;

        if (sourceScrollBar == verticalBar1) {
            targetScrollBar = verticalBar2;
        } else if(sourceScrollBar == horizontalBar1) {
            targetScrollBar = horizontalBar2;
        } else if (sourceScrollBar == verticalBar2) {
            targetScrollBar = verticalBar1;
        } else if(sourceScrollBar == horizontalBar2) {
            targetScrollBar = horizontalBar1;
        } else {
            throw new RuntimeException("Invalid source scroll bar");
        }

        targetScrollBar.setValue(value);
    }
}