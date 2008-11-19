/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Nov 6, 2008
 * Time: 5:07:43 PM
 */
package com.l7tech.server.ems.standardreports;

import net.sf.jasperreports.engine.JRChartCustomizer;
import net.sf.jasperreports.engine.JRChart;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.ui.TextAnchor;

import java.util.List;
import java.awt.*;

public class PerformanceSummaryChartCustomizer implements JRChartCustomizer {
    public void customize(JFreeChart jFreeChart, JRChart jrChart) {
//        System.out.println("customize");
//        List list = jFreeChart.getCategoryPlot().getRenderer().getPlot().getCategories();
//        for(Object o: list){
//            System.out.println("item: " + o);
//        }

        CategoryItemRenderer renderer = jFreeChart.getCategoryPlot().getRenderer();
        List<Color> colours = Utilities.getSeriesColours(3);

        for(int i = 0; i < colours.size(); i++){
            renderer.setSeriesPaint(i, colours.get(i));
        }

        BarRenderer barRenderer = (BarRenderer) jFreeChart.getCategoryPlot().getRenderer();
        barRenderer.setMaximumBarWidth(0.3);
//        for(int i = 0; i < colours.size(); i++){
//            ItemLabelPosition test = barRenderer.getSeriesPositiveItemLabelPosition(i);
//            System.out.println("Text anchor: " + test.getTextAnchor().toString() + " Label Anchor: " + test.getItemLabelAnchor().toString());
//            ItemLabelPosition labelPosition = new ItemLabelPosition(ItemLabelAnchor.OUTSIDE7, TextAnchor.BOTTOM_LEFT);
//            barRenderer.setSeriesPositiveItemLabelPosition(i, labelPosition );
//        }

    }
}
