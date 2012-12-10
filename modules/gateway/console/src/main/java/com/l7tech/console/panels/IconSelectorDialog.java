package com.l7tech.console.panels;

import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.widgets.ValidatedPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;

/**
 * A dialog which allows the user to select an icon from an image gallery. Images in the gallery are scaled to a uniform size.
 */
public class IconSelectorDialog extends ValidatedPanel<String> {
    private static final Border LINE_BORDER = BorderFactory.createLineBorder(Color.BLACK, 3);
    private static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder();
    private static final int WIDTH = 16;
    private static final int HEIGHT = 16;
    private static final int COLS = 15;
    private static final int PADDING = 3;
    private static final String ICON = "icon";
    private ImageIcon selected;
    private JLabel selectedLabel;

    /**
     * @param selected the ImageIcon to select by default.
     */
    public IconSelectorDialog(@NotNull final ImageIcon selected) {
        super(ICON);
        this.selected = new ImageIcon(selected.getImage().getScaledInstance(WIDTH, HEIGHT, Image.SCALE_SMOOTH));
        this.selected.setDescription(selected.getDescription());
        init();
    }

    /**
     * @return the selected icon's filename.
     */
    @Override
    protected String getModel() {
        return selected.getDescription();
    }

    @Override
    protected void initComponents() {
        // # of rows is dynamic
        setLayout(new GridLayout(0, COLS, PADDING, PADDING));
        final Collection<ImageIcon> icons = ImageCache.getInstance().getIcons(IconSelectorDialog.class.getClassLoader());
        for (final ImageIcon icon : icons) {
            final Image scaled = icon.getImage().getScaledInstance(WIDTH, HEIGHT, Image.SCALE_SMOOTH);
            final ImageIcon scaledIcon = new ImageIcon(scaled);
            scaledIcon.setDescription(icon.getDescription());
            final JLabel iconLabel = new JLabel(scaledIcon);
            if (selected.getDescription().equals(icon.getDescription())) {
                selectedLabel = iconLabel;
                iconLabel.setBorder(LINE_BORDER);
            } else {
                iconLabel.setBorder(EMPTY_BORDER);
            }
            iconLabel.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                }

                @Override
                public void mousePressed(final MouseEvent e) {
                }

                @Override
                public void mouseReleased(final MouseEvent e) {
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
                public void mouseEntered(final MouseEvent e) {
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                }
            });
            add(iconLabel);
        }
    }

    @Override
    public void focusFirstComponent() {
    }

    @Override
    protected void doUpdateModel() {
        // no update needed as the model is just the icon file name
    }
}
