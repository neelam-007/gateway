package com.l7tech.console.panels;

import com.l7tech.console.MainWindow;
import com.l7tech.console.SsmApplication;
import com.l7tech.console.util.EncapsulatedAssertionConsoleUtil;
import com.l7tech.console.util.IconManager;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.l7tech.console.util.EncapsulatedAssertionConsoleUtil.IconType;

/**
 * A dialog which allows the user to select an icon from an image gallery or browse for an image file.
 */
public class IconSelectorPanel extends ValidatedPanel<Pair<EncapsulatedAssertionConsoleUtil.IconType, String>> {
    private static final int ICON_MAX_SIZE = SyspropUtil.getInteger("com.l7tech.icon.resource.max.size", 16);
    private static final int ICON_MIN_SIZE = SyspropUtil.getInteger("com.l7tech.icon.resource.min.size", 16);
    private static final Border LINE_BORDER = BorderFactory.createLineBorder(Color.BLACK, 3);
    private static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder(3, 3, 3, 3);
    private static final int CUSTOM_ICON_MAX_BYTES = SyspropUtil.getInteger("com.l7tech.icon.custom.max.bytes", 32768);
    private static final int CUSTOM_ICON_MAX_PIXELS = SyspropUtil.getInteger("com.l7tech.icon.custom.max.pixels", 32);
    private static final int COLS = 15;
    private static final int PADDING = 3;
    private static final String ICON = "icon";
    private ImageIcon defaultSelect;
    private JLabel selectedLabel;
    private JPanel galleryPanel;
    private JPanel contentPanel;
    private JButton browseButton;
    private JTextField fileTextField;
    private Pair<IconType, String> model;

    /**
     * @param defaultSelect the ImageIcon to select by default (can be null).
     */
    public IconSelectorPanel(@Nullable final ImageIcon defaultSelect) {
        super(ICON);
        if (defaultSelect != null) {
            this.defaultSelect = defaultSelect;
            this.defaultSelect.setDescription(defaultSelect.getDescription());
            model = new Pair<IconType, String>(IconType.INTERNAL_RESOURCE, defaultSelect.getDescription());
        } else {
            model = new Pair<IconType, String>(null, null);
        }
        init();
    }

    /**
     * Returns a Pair where left = IconType and right is a file path (for IconType.CUSTOM_IMAGE) or a filename (IconType.INTERNAL_RESOURCE).
     *
     * @return a Pair representing the selected image.
     */
    @Override
    protected Pair<IconType, String> getModel() {
        return model;
    }

    @Override
    protected String getSyntaxError(final Pair<IconType, String> model) {
        String error = null;
        if (model == null || model.left == null || model.right == null) {
            error = "No valid selection";
        }
        if (IconType.CUSTOM_IMAGE.equals(model.left)) {
            final File file = new File(model.right);
            if (!file.exists() || !file.isFile()) {
                error = "Invalid file";
            }
        }
        return error;
    }

    @Override
    protected void initComponents() {
        // # of rows is dynamic
        galleryPanel.setLayout(new GridLayout(0, COLS, PADDING, PADDING));
        createIconGallery();
        browseButton.addActionListener(new FileButtonActionListener());
        fileTextField.addKeyListener(new FileTextFieldKeyListener());
        setLayout(new BorderLayout());
        add(contentPanel, BorderLayout.CENTER);
    }

    @Override
    public void focusFirstComponent() {
    }

    @Override
    protected void doUpdateModel() {
        if (!fileTextField.getText().isEmpty()) {
            final File file = new File(fileTextField.getText());
            if (file.length() > CUSTOM_ICON_MAX_BYTES) {
                throw new IllegalArgumentException("Maximum file size is " + CUSTOM_ICON_MAX_BYTES + " bytes.");
            }
            if (fileContainsOversizedIcon(file)) {
                throw new IllegalArgumentException("Icon may not be larger than " + ICON_MAX_SIZE + " pixels wide or tall.");
            }
        }
    }

    private boolean fileContainsOversizedIcon(File file) {
        try {
            BufferedImage image = ImageCache.getInstance().createUncachedBufferedImage(IOUtils.slurpFile(file), Transparency.OPAQUE);
            return image.getHeight() > CUSTOM_ICON_MAX_PIXELS || image.getWidth() > CUSTOM_ICON_MAX_PIXELS;
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read icon file: " + ExceptionUtils.getMessage(e));
        }
    }

    private void createIconGallery() {
        final java.util.List<String> imageNames = IconManager.getInstance().getImageNames();
        Collections.sort(imageNames);
        for (final String imageName : imageNames) {
            final ImageIcon icon = ImageCache.getInstance().getIconAsIcon(MainWindow.RESOURCE_PATH + "/" + imageName);
            if (icon != null &&
                    icon.getIconHeight() <= ICON_MAX_SIZE && icon.getIconHeight() >= ICON_MIN_SIZE &&
                    icon.getIconWidth() <= ICON_MAX_SIZE && icon.getIconWidth() >= ICON_MIN_SIZE) {
                icon.setDescription(imageName);
                final JLabel iconLabel = new JLabel(icon);
                if (defaultSelect != null && defaultSelect.getDescription().equals(icon.getDescription())) {
                    selectedLabel = iconLabel;
                    iconLabel.setBorder(LINE_BORDER);
                } else {
                    iconLabel.setBorder(EMPTY_BORDER);
                }
                iconLabel.setToolTipText(icon.getDescription());
                iconLabel.addMouseListener(new IconLabelMouseListener(iconLabel, icon.getDescription()));
                galleryPanel.add(iconLabel);
            }
        }
    }

    private void removeBorder() {
        if (selectedLabel != null) {
            selectedLabel.setBorder(EMPTY_BORDER);
            selectedLabel = null;
        }
    }

    /**
     * Handles icon gallery mouse actions.
     */
    private class IconLabelMouseListener implements MouseListener {
        private JLabel iconLabel;
        private String filename;

        private IconLabelMouseListener(@NotNull final JLabel iconLabel, @NotNull final String filename) {
            this.iconLabel = iconLabel;
            this.filename = filename;
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
        }

        @Override
        public void mousePressed(final MouseEvent e) {
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            removeBorder();
            selectedLabel = iconLabel;
            selectedLabel.setBorder(LINE_BORDER);
            fileTextField.setText(StringUtils.EMPTY);
            model = new Pair<IconType, String>(IconType.INTERNAL_RESOURCE, filename);
            checkSyntax();
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }
    }

    /**
     * Handles file selection via Browse button.
     */
    private class FileButtonActionListener implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent e) {
            SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
                @Override
                public void useFileChooser(JFileChooser fc) {
                    fc.setFileFilter(FileUtils.getImageFileFilter());
                    if (JFileChooser.APPROVE_OPTION == (fc.showOpenDialog(TopComponents.getInstance().getTopParent()))) {
                        removeBorder();
                        final File file = fc.getSelectedFile();
                        fileTextField.setText(file.getAbsolutePath());
                        model = new Pair<IconType, String>(IconType.CUSTOM_IMAGE, file.getAbsolutePath());
                        checkSyntax();
                    }
                }
            });
        }
    }

    /**
     * Handles file selection via manual typing in the text field.
     */
    private class FileTextFieldKeyListener implements KeyListener {
        @Override
        public void keyTyped(final KeyEvent e) {
        }

        @Override
        public void keyPressed(final KeyEvent e) {
        }

        @Override
        public void keyReleased(final KeyEvent e) {
            removeBorder();
            if (!fileTextField.getText().isEmpty()) {
                model = new Pair<IconType, String>(IconType.CUSTOM_IMAGE, fileTextField.getText());
                checkSyntax();
            }
        }
    }
}
