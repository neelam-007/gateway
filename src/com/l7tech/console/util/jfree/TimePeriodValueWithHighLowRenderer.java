package com.l7tech.console.util.jfree;

import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.axis.ValueAxis;
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
    public TimePeriodValueWithHighLowRenderer() {
        setUseYInterval(true);  // so that the parent class will draw a bar using the high-low values
    }

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
        // Enclose whole method in try-catch block to prevent
        // IndexOutOfBoundsException from bubbling up. This exception may arise
        // when plot is updating while the underlying data is being modified.
        try {
            // First, draws the high-low bar.
            super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item, crosshairState, pass);

            if (!getItemVisible(series, item)) {
                return;
            }

            // Next, draws the y value horizontal line in front.

            TimePeriodValuesWithHighLowCollection dataset_ = (TimePeriodValuesWithHighLowCollection) dataset;

            Number yNumber = dataset_.getY(series, item);
            if (yNumber == null) {
                return;
            }
            double y = yNumber.doubleValue();
            if (Double.isNaN(y)) {
                return;
            }

            double translatedY = rangeAxis.valueToJava2D(y, dataArea, plot.getRangeAxisEdge());

            RectangleEdge location = plot.getDomainAxisEdge();
            Number startXNumber = dataset_.getStartX(series, item);
            if (startXNumber == null) {
                return;
            }
            double translatedStartX = domainAxis.valueToJava2D(startXNumber.doubleValue(), dataArea, location);

            Number endXNumber = dataset_.getEndX(series, item);
            if (endXNumber == null) {
                return;
            }
            double translatedEndX = domainAxis.valueToJava2D(endXNumber.doubleValue(), dataArea, location);

            Line2D line = state.workingLine;
            line.setLine(translatedStartX, translatedY, translatedEndX, translatedY);
            g2.setStroke(getSeriesStroke(series));
            g2.setPaint(getSeriesFillPaint(series));
            g2.draw(line);
        } catch (IndexOutOfBoundsException e) {
            // Can be ignored. Simply skip rendering of this data item.
        }
    }
}
