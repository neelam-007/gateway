package com.l7tech.console.panels;

import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.widgets.ValidatedPanel;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;

public class IconSelectorDialog extends ValidatedPanel<ImageIcon> {
    private static final Border LINE_BORDER = BorderFactory.createLineBorder(Color.BLACK, 3);
    private static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder();
    private ImageIcon selected;
    private JLabel selectedLabel;

    public IconSelectorDialog() {
        super("icon");
        init();
    }

    @Override
    protected ImageIcon getModel() {
        return selected;
    }

    @Override
    protected void initComponents() {
        setLayout(new GridLayout(0, 15, 3, 3));
        final Collection<ImageIcon> icons = ImageCache.getInstance().getIcons(IconSelectorDialog.class.getClassLoader());
        int count = 0;
        for (final ImageIcon icon : icons) {
            final Image scaled = icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            final ImageIcon scaledIcon = new ImageIcon(scaled);
            final JLabel iconLabel = new JLabel(scaledIcon);
            if (count == 0) {
                // select the first one
                selected = scaledIcon;
                selectedLabel = iconLabel;
                iconLabel.setBorder(LINE_BORDER);
            } else {
                iconLabel.setBorder(EMPTY_BORDER);
            }
            iconLabel.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                }

                @Override
                public void mousePressed(MouseEvent e) {
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (!selected.equals(scaledIcon)) {
                        selected = scaledIcon;
                        // remove border from previously-selected icon
                        selectedLabel.setBorder(EMPTY_BORDER);
                        selectedLabel = iconLabel;
                        selectedLabel.setBorder(LINE_BORDER);
                    }
                    checkSyntax();
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                }

                @Override
                public void mouseExited(MouseEvent e) {
                }
            });
            add(iconLabel);
            count++;
        }
    }

    @Override
    public void focusFirstComponent() {
    }

    @Override
    protected void doUpdateModel() {
    }
}
