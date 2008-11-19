/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Nov 7, 2008
 * Time: 10:45:28 AM
 */
package com.l7tech.server.ems.standardreports;

import net.sf.jasperreports.engine.JRChartCustomizer;
import net.sf.jasperreports.engine.JRChart;
import net.sf.jasperreports.engine.JRChartDataset;
import net.sf.jasperreports.engine.JRDatasetRun;
import net.sf.jasperreports.charts.fill.JRFillCategoryDataset;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.general.Dataset;
import org.jfree.data.general.DatasetGroup;
import org.jfree.ui.TextAnchor;

import java.util.List;
import java.awt.*;

public class UsageSummaryChartCustomizer implements JRChartCustomizer {
    public void customize(JFreeChart jFreeChart, JRChart jrChart) {
//        System.out.println("customize");
//        List list = jFreeChart.getCategoryPlot().getRenderer().getPlot().getCategories();
//        for(Object o: list){
//            System.out.println("item: " + o);
//        }

        CategoryItemRenderer renderer = jFreeChart.getCategoryPlot().getRenderer();
        List<Color> colours = Utilities.getSeriesColours(1);

        for(int i = 0; i < colours.size(); i++){
            renderer.setSeriesPaint(i, colours.get(i));
        }

        BarRenderer barRenderer = (BarRenderer) jFreeChart.getCategoryPlot().getRenderer();
        barRenderer.setMaximumBarWidth(0.3);
    }
}
