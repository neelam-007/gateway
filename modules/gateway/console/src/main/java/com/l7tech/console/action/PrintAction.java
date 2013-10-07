package com.l7tech.console.action;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.print.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Action which prints a Component (scales and centers if the component is larger than the print size).
 */
public class PrintAction extends BaseAction {
    private static final Logger logger = Logger.getLogger(PrintAction.class.getName());
    private static final String PRINT = "Print";
    private final Component component;
    private final PageFormat defaultPageFormat;

    /**
     * @param component         the Component to print.
     * @param defaultPageFormat the PageFormat to use by default (affects default printer settings).
     */
    public PrintAction(@NotNull final Component component, @Nullable final PageFormat defaultPageFormat) {
        this.component = component;
        this.defaultPageFormat = defaultPageFormat;
    }

    @Override
    public String getName() {
        return PRINT;
    }

    @Override
    protected void performAction() {
        final PrinterJob job = PrinterJob.getPrinterJob();
        final PageFormat pageFormat = defaultPageFormat != null ? defaultPageFormat : job.defaultPage();
        job.setPageable(new PrintableComponent(component, pageFormat));
        if (job.printDialog()) {
            try {
                job.print();
            } catch (final PrinterException e) {
                final String msg = "Unable to print: " + ExceptionUtils.getMessage(e);
                logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
                DialogDisplayer.showMessageDialog(component, msg, "Print Error", JOptionPane.ERROR_MESSAGE, null);
            }
        }
    }

    private class PrintableComponent implements Printable, Pageable {
        private final Component component;
        private final PageFormat pageFormat;

        private PrintableComponent(@NotNull final Component component, @NotNull final PageFormat pageFormat) {
            this.component = component;
            this.pageFormat = pageFormat;
        }

        @Override
        public int print(final Graphics graphics, final PageFormat pageFormat, final int pageIndex) throws PrinterException {
            // single page only
            if (pageIndex > 0) {
                return NO_SUCH_PAGE;
            }
            final Dimension componentSize = component.getPreferredSize();
            component.setSize(componentSize);
            final Graphics2D copy = (Graphics2D) graphics.create();
            final AffineTransform at = getScaledCenteredTransformation(pageFormat, componentSize);
            copy.transform(at);
            enableDisableDoubleBuffering(false);
            component.printAll(copy);
            enableDisableDoubleBuffering(true);
            copy.dispose();
            component.revalidate();
            return PAGE_EXISTS;
        }

        private AffineTransform getScaledCenteredTransformation(final PageFormat pageFormat, final Dimension componentSize) {
            final Dimension printSize = new Dimension();
            printSize.setSize(pageFormat.getImageableWidth(), pageFormat.getImageableHeight());
            double scaleFactor = getScaleDownFactor(componentSize.width, printSize.width);

            double scaleWidth = componentSize.width * scaleFactor;
            double scaleHeight = componentSize.height * scaleFactor;
            double x = ((pageFormat.getImageableWidth() - scaleWidth) / 2d) + pageFormat.getImageableX();
            double y = ((pageFormat.getImageableHeight() - scaleHeight) / 2d) + pageFormat.getImageableY();

            AffineTransform at = new AffineTransform();
            // center
            at.translate(x, y);
            // scale
            at.scale(scaleFactor, scaleFactor);
            return at;
        }

        private double getScaleDownFactor(final int original, final int target) {
            double scaleFactor = 1d;
            if (original > target) {
                scaleFactor = (double) target / (double) original;
            }
            return scaleFactor;

        }

        private void enableDisableDoubleBuffering(final boolean enable) {
            RepaintManager currentManager = RepaintManager.currentManager(component);
            if (currentManager.isDoubleBufferingEnabled() != enable) {
                currentManager.setDoubleBufferingEnabled(enable);
            }
        }

        @Override
        public int getNumberOfPages() {
            // single page only
            return 1;
        }

        @Override
        public PageFormat getPageFormat(final int pageIndex) throws IndexOutOfBoundsException {
            return pageFormat;
        }

        @Override
        public Printable getPrintable(final int pageIndex) throws IndexOutOfBoundsException {
            return this;
        }
    }
}
