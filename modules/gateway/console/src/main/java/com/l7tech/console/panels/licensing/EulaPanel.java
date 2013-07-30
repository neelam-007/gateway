package com.l7tech.console.panels.licensing;

import com.l7tech.gui.widgets.WrappingLabel;

import javax.swing.*;
import java.awt.*;

/**
 * @author Jamie Williams - wilja33 - jamie.williams2@ca.com
 */
public class EulaPanel extends JPanel {
    private static final int SCROLL_INCREMENT_VERTICAL = 10;
    private static final int SCROLL_INCREMENT_HORIZONTAL = 8;

    private JPanel rootPanel;

    private final String text;

    public EulaPanel(String text) {
        this.text = text.trim();

        init();
    }

    private void init() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(rootPanel);

        WrappingLabel licenseText = new WrappingLabel(text);
        licenseText.setContextMenuAutoSelectAll(false);
        licenseText.setContextMenuEnabled(true);

        JScrollPane eulaScrollPane = new JScrollPane(licenseText,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        eulaScrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_INCREMENT_VERTICAL);
        eulaScrollPane.getHorizontalScrollBar().setUnitIncrement(SCROLL_INCREMENT_HORIZONTAL);
        eulaScrollPane.setPreferredSize(new Dimension(590, 500));

        rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.Y_AXIS));
        rootPanel.add(eulaScrollPane);
    }
}
