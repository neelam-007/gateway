package com.l7tech.console.util.jfree;

import org.jfree.chart.renderer.xy.XYBarRenderer;
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
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Line2D;

/**
 * A renderer that draws a horizontal line for each time period value, with a
 * vertical bar behind to represent high-low value range on an
 * {@link org.jfree.chart.plot.XYPlot} (requires a
 * {@link TimePeriodValuesWithHighLowCollection}).
 *
 * @author rmak
 */
public class TimePeriodValueWithHighLowRenderer extends XYBarRenderer {
    /**
     * Sets the paint used for y value horizontal line.
     *
     * @param index of series
     * @param paint
     */
    public void setSeriesPaint(int index, Paint paint) {
        // Note: Because {@link XYBarRenderer.draw} uses {@link #getSeriesPaint}
        // to draw the high-low bar, we have to use FillPaint to store the line
        // paint. Thus swapping the wording internally.
        super.setSeriesFillPaint(index, paint);
    }

    /**
     * Sets the paint used for high-low value bar.
     *
     * @param index of series
     * @param paint
     */
    public void setSeriesFillPaint(int index, Paint paint) {
        // Note: {@link XYBarRenderer.draw} uses {@link #getSeriesPaint} to draw
        // the high-low bar.
        super.setSeriesPaint(index, paint);
    }

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
        if (!(dataset instanceof TimePeriodValuesWithHighLowCollection)) {
            throw new IllegalArgumentException(
                    "dataset has wrong type (expected: " +
                    TimePeriodValuesWithHighLowCollection.class.getName() +
                    ", encountered: " + dataset.getClass().getName() + ")");
        }

        TimePeriodValuesWithHighLowCollection dataset_ = (TimePeriodValuesWithHighLowCollection) dataset;

        try {
            if (!getItemVisible(series, item)) {
                return;
            }

            //
            // First, draws the high-low bar.
            //

            final double lowValue = dataset_.getStartYValue(series, item);
            final double highValue = dataset_.getEndYValue(series, item);
            if (Double.isNaN(lowValue) || Double.isNaN(highValue)) {
                return;
            }

            final double translatedLowValue = rangeAxis.valueToJava2D(lowValue, dataArea, plot.getRangeAxisEdge());
            final double translatedHighValue = rangeAxis.valueToJava2D(highValue, dataArea, plot.getRangeAxisEdge());

            final Number startXNumber = dataset_.getStartX(series, item);
            if (startXNumber == null) {
                return;
            }
            final RectangleEdge location = plot.getDomainAxisEdge();
            double translatedStartX = domainAxis.valueToJava2D(startXNumber.doubleValue(), dataArea, location);

            final Number endXNumber = dataset_.getEndX(series, item);
            if (endXNumber == null) {
                return;
            }
            final double translatedEndX = domainAxis.valueToJava2D(endXNumber.doubleValue(), dataArea, location);

            double translatedWidth = Math.max(1, Math.abs(translatedEndX - translatedStartX));
            double translatedHeight = Math.abs(translatedHighValue - translatedLowValue);

            if (getMargin() > 0.0) {
                double cut = translatedWidth * getMargin();
                translatedWidth = translatedWidth - cut;
                translatedStartX = translatedStartX + cut / 2;
            }

            Rectangle2D bar = null;
            final PlotOrientation orientation = plot.getOrientation();
            if (orientation == PlotOrientation.HORIZONTAL) {
                bar = new Rectangle2D.Double(
                    Math.min(translatedLowValue, translatedHighValue),
                    Math.min(translatedStartX, translatedEndX),
                    translatedHeight, translatedWidth);
            }
            else if (orientation == PlotOrientation.VERTICAL) {
                bar = new Rectangle2D.Double(
                    Math.min(translatedStartX, translatedEndX),
                    Math.min(translatedLowValue, translatedHighValue),
                    translatedWidth, translatedHeight);
            }

            Paint itemPaint = getItemPaint(series, item);
            if (getGradientPaintTransformer() != null && itemPaint instanceof GradientPaint) {
                GradientPaint gp = (GradientPaint) itemPaint;
                itemPaint = getGradientPaintTransformer().transform(gp, bar);
            }
            g2.setPaint(itemPaint);
            g2.fill(bar);
            if (isDrawBarOutline() && Math.abs(translatedEndX - translatedStartX) > 3) {
                Stroke stroke = getItemOutlineStroke(series, item);
                Paint paint = getItemOutlinePaint(series, item);
                if (stroke != null && paint != null) {
                    g2.setStroke(stroke);
                    g2.setPaint(paint);
                    g2.draw(bar);
                }
            }

            //
            // Next, draws the y value horizontal line in front.
            //

            Number yNumber = dataset_.getY(series, item);
            if (yNumber == null) {
                return;
            }
            double y = yNumber.doubleValue();
            if (Double.isNaN(y)) {
                return;
            }

            double translatedY = rangeAxis.valueToJava2D(y, dataArea, plot.getRangeAxisEdge());

            Line2D line = state.workingLine;
            if (orientation == PlotOrientation.HORIZONTAL) {
                line.setLine(translatedY, translatedStartX, translatedY, translatedEndX);
            }
            else if (orientation == PlotOrientation.VERTICAL) {
                line.setLine(translatedStartX, translatedY, translatedEndX, translatedY);
            }
            g2.setStroke(getSeriesStroke(series));
            g2.setPaint(getSeriesFillPaint(series));
            g2.draw(line);

            // TODO: we need something better for the item labels
            if (isItemLabelVisible(series, item)) {
                drawItemLabel(g2, orientation, dataset, series, item,
                              bar.getCenterX(), bar.getY(), highValue < 0.0);
            }

            // Adds tooltip trigger area covering the x-range of the data item
            // and the y-range of the plot area.
            if (info != null) {
                Rectangle2D rect = null;
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
