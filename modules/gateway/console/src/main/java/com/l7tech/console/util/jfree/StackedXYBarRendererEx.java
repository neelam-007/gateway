package com.l7tech.console.util.jfree;

import org.jfree.chart.renderer.xy.StackedXYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.time.TimeTableXYDataset;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * Same as {@link StackedXYBarRenderer} except that the tooltip trigger area
 * spans the entire y-range of the plot area.
 *
 * @author rmak
 */
public class StackedXYBarRendererEx extends StackedXYBarRenderer {
    public void drawItem(Graphics2D g2,
                         XYItemRendererState state,
                         Rectangle2D dataArea,
                         PlotRenderingInfo info,
                         XYPlot plot,
                         ValueAxis domainAxis,
                         ValueAxis rangeAxis,
                         XYDataset dataset,
                         int series,
                         int item,
                         CrosshairState crosshairState,
                         int pass) {
        try {
            // Lets the parent class draws the item except adding
            // entity for the data item.
            super.drawItem(g2, state, dataArea, null /* info */, plot, domainAxis,
                           rangeAxis, dataset, series, item, crosshairState, pass);

            // Adds tooltip trigger area covering the x-range of the data item
            // and the y-range of the plot area.
            if (info != null) {
                final TimeTableXYDataset dataset_ = (TimeTableXYDataset)dataset;
                final RectangleEdge location = plot.getDomainAxisEdge();

                final Number startXNumber = dataset_.getStartX(series, item);
                if (startXNumber == null) {
                    return;
                }
                double translatedStartX = domainAxis.valueToJava2D(startXNumber.doubleValue(), dataArea, location);

                final Number endXNumber = dataset_.getEndX(series, item);
                if (endXNumber == null) {
                    return;
                }
                final double translatedEndX = domainAxis.valueToJava2D(endXNumber.doubleValue(), dataArea, location);

                double translatedWidth = Math.max(1, Math.abs(translatedEndX - translatedStartX));

                Rectangle2D rect = null;
                final PlotOrientation orientation = plot.getOrientation();
                if (orientation == PlotOrientation.HORIZONTAL) {
                    rect = new Rectangle2D.Double(
                            dataArea.getMinX(),
                            Math.min(translatedStartX, translatedEndX),
                            dataArea.getWidth(),
                            translatedWidth);
                } else if (orientation == PlotOrientation.VERTICAL) {
                    rect = new Rectangle2D.Double(
                            Math.min(translatedStartX, translatedEndX),
                            dataArea.getMinY(),
                            translatedWidth,
                            dataArea.getHeight());
                }

                EntityCollection entities = info.getOwner().getEntityCollection();
                if (entities != null) {
                    String tip = null;
                    final XYToolTipGenerator generator = getToolTipGenerator(series, item);
                    if (generator != null) {
                        tip = generator.generateToolTip(dataset, series, item);
                    }
                    String url = null;
                    if (getURLGenerator() != null) {
                        url = getURLGenerator().generateURL(dataset, series, item);
                    }
                    XYItemEntity entity = new XYItemEntity(rect, dataset, series, item, tip, url);
                    entities.add(entity);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            // Probably the data item has just been deleted. Just skip rendering it.
        }
    }
}
