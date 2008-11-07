/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Nov 7, 2008
 * Time: 10:45:28 AM
 */
package com.l7tech.server.ems.standardreports;

import net.sf.jasperreports.engine.JRChartCustomizer;
import net.sf.jasperreports.engine.JRChart;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.category.CategoryItemRenderer;

import java.util.List;
import java.awt.*;

public class UsageSummaryChartCustomizer implements JRChartCustomizer {
    public void customize(JFreeChart jFreeChart, JRChart jrChart) {
        System.out.println("customize");
        List list = jFreeChart.getCategoryPlot().getRenderer().getPlot().getCategories();
        for(Object o: list){
            System.out.println("item: " + o);
        }

        CategoryItemRenderer renderer = jFreeChart.getCategoryPlot().getRenderer();
        List<Color> colours = Utilities.getSeriesColours(5);
        renderer.setSeriesPaint(0, colours.get(0));
        renderer.setSeriesPaint(1, colours.get(1));
        renderer.setSeriesPaint(2, colours.get(2));
        renderer.setSeriesPaint(2, colours.get(3));
        renderer.setSeriesPaint(2, colours.get(4));

    }
}
