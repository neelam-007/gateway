/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gui.widgets;

import javax.accessibility.Accessible;
import javax.swing.*;
import javax.swing.plaf.basic.BasicComboPopup;
import java.awt.*;

/**
 * Provides wordaround to a <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4618607">bug</a>
 * in {@link JComboBox} (as of 1.6.0_03): the drop-down list
 * is always the same width as the combo box, even if wide items will be truncated.
 * <code>BetterComboBox</code> has a drop-down list at least as wide as the
 * combo box, but will widen to accomodate the widest item when necessary.
 *
 * @author rmak
 * @since SecureSpan 5.0
 */
public class BetterComboBox<E> extends JComboBox<E> {

    @Override
    public void firePopupMenuWillBecomeVisible() {
        resizePopup();
        super.firePopupMenuWillBecomeVisible();
    }

    private void resizePopup() {
        final Accessible assessible = getUI().getAccessibleChild(this, 0);
        if (assessible == null || !(assessible instanceof BasicComboPopup)) return;
        final BasicComboPopup popup = (BasicComboPopup)assessible;

        final Component comp = popup.getComponent(0);
        if (comp == null || !(comp instanceof JScrollPane)) return;
        final JScrollPane scrollPane = (JScrollPane)comp;

        int fittedWidth = 0;
        final FontMetrics fm = getFontMetrics(getFont());
        for (int i = 0; i < getItemCount(); ++i) {
            String str = getItemAt(i).toString();
            if (fittedWidth < fm.stringWidth(str))
                fittedWidth = fm.stringWidth(str);
        }
        fittedWidth += 10; // estimated left and right margin around text; is there a way to tell the exact value?
        if (getItemCount() > getMaximumRowCount()) {
            fittedWidth += scrollPane.getVerticalScrollBar().getPreferredSize().width;
        }
        if (fittedWidth < getWidth()) {
            fittedWidth = getWidth();
        }

        scrollPane.setMaximumSize(new Dimension(fittedWidth, scrollPane.getMaximumSize().height));
        scrollPane.setPreferredSize(new Dimension(fittedWidth, scrollPane.getPreferredSize().height));
        scrollPane.setMinimumSize(new Dimension(fittedWidth, scrollPane.getMinimumSize().height));
    }
}
