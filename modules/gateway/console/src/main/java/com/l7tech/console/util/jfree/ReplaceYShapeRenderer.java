package com.l7tech.console.util.jfree;

import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.time.TimeTableXYDataset;
import org.jfree.data.time.TimePeriod;
import org.jfree.ui.RectangleEdge;
import org.jfree.util.ShapeUtilities;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * A renderer that draws a shape at the midpoint of each time period, using
 * a replaced y value, if the actual y value is non-zero (requires a
 * {@link org.jfree.data.time.TimeTableXYDataset}).
 *
 * @author rmak
 */
public class ReplaceYShapeRenderer extends AbstractXYItemRenderer {
    /** The replacement y values for each series. */
    private DoubleList _seriesYValues = new DoubleList();

    /** The default replacement y values for each series. */
    private double _defaultYValue = 0.;

    /**
     * Returns the replacement y value for a series.
     *
     * @param series the series index
     * @return the replacement y value
     */
    public double getSeriesYValue(int series) {
        double result = _defaultYValue;
        Double value = _seriesYValues.getDouble(series);
        if (value != null) {
            result = value.doubleValue();
        }
        return result;
    }

    /**
     * Sets the replacement y value used for a series and sends a
     * {@link org.jfree.chart.event.RendererChangeEvent} to all registered listeners.
     *
     * @param series the series index (zero-based)
     * @param value  the replacement y value
     */
    public void setSeriesYValue(int series, double value) {
        setSeriesYValue(series, value, true);
    }

    /**
     * Sets the replacement y value used for a series and, if requested, sends a
     * {@link org.jfree.chart.event.RendererChangeEvent} to all registered listeners.
     *
     * @param series the series index (zero-based)
     * @param value  the replacement y value
     * @param notify notify listeners?
     */
    public void setSeriesYValue(int series, double value, boolean notify) {
        _seriesYValues.setDouble(series, new Double(value));
        if (notify) {
            notifyListeners(new RendererChangeEvent(this));
        }
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
        try {
            // do nothing if item is not visible
            if (!getItemVisible(series, item)) {
                return;
            }

            // get the data point...
            TimeTableXYDataset dataset_ = (TimeTableXYDataset) dataset;
            TimePeriod period = dataset_.getTimePeriod(item);
            double x = ((double) period.getStart().getTime() + (double) period.getEnd().getTime()) / 2.;
            double y = dataset_.getYValue(series, item);
            if (y == 0.) {
                return;     // Don't plot shape if actual y value is zero.
            }
            if (Double.isNaN(y)) {
                return;
            }
            // Now apply the replacement y value.
            y = getSeriesYValue(series);

            PlotOrientation orientation = plot.getOrientation();
            RectangleEdge xAxisLocation = plot.getDomainAxisEdge();
            RectangleEdge yAxisLocation = plot.getRangeAxisEdge();
            double transX = domainAxis.valueToJava2D(x, dataArea, xAxisLocation);
            double transY = rangeAxis.valueToJava2D(y, dataArea, yAxisLocation);

            Shape shape = getItemShape(series, item);
            if (orientation == PlotOrientation.HORIZONTAL) {
                shape = ShapeUtilities.createTranslatedShape(shape, transY, transX);
            } else if (orientation == PlotOrientation.VERTICAL) {
                shape = ShapeUtilities.createTranslatedShape(shape, transX, transY);
            }

            Shape entityArea = shape;
            if (shape.intersects(dataArea)) {
                g2.setPaint(getItemPaint(series, item));
                g2.fill(shape);
            }

            // add an entity for the item...
            // setup for collecting optional entity info...
            EntityCollection entities = null;
            if (info != null) {
                entities = info.getOwner().getEntityCollection();
            }

            if (entities != null) {
                addEntity(entities, entityArea, dataset, series, item, transX, transY);
            }
        } catch (IndexOutOfBoundsException e) {
            // Probably the data item has just been deleted. Just skip rendering it.
        }
    }
}
