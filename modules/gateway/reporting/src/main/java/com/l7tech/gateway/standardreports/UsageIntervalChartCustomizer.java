/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Nov 19, 2008
 * Time: 1:35:19 PM
 */
package com.l7tech.gateway.standardreports;

import net.sf.jasperreports.engine.JRChartCustomizer;
import net.sf.jasperreports.engine.JRChart;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.ui.TextAnchor;

import java.awt.*;

public class UsageIntervalChartCustomizer implements JRChartCustomizer {
    public void customize(JFreeChart jFreeChart, JRChart jrChart) {
        CategoryItemRenderer renderer = jFreeChart.getCategoryPlot().getRenderer();
        java.util.List<Color> colours = Utilities.getSeriesColours(1);

        for(int i = 0; i < colours.size(); i++){
            renderer.setSeriesPaint(i, colours.get(i));
        }

        BarRenderer barRenderer = (BarRenderer) jFreeChart.getCategoryPlot().getRenderer();
        barRenderer.setMaximumBarWidth(0.3);
        barRenderer.setSeriesPositiveItemLabelPosition(0, new ItemLabelPosition(ItemLabelAnchor.CENTER, TextAnchor.CENTER));

//        DefaultCategoryDataset dC = new DefaultCategoryDataset();
////        dC.addValue(100, "Row 1", "Column 1");
//
//        String chartKey = jrChart.getKey();
//
//        UsageReportHelper helper = Utilities.getHelper(chartKey);
//        Map<String, Long> groupToValueMap = helper.getReportTotalsMap();
//        LinkedHashMap<Integer, String> groupIndexToGroupMap = helper.getGroupIndexToGroupMap();
//
//        int index = 1;
//        for(Map.Entry<String, Long> e: groupToValueMap.entrySet()){
//            String group = groupIndexToGroupMap.get(index);
//            dC.addValue(e.getValue(), "# Successful Requests", group);
//            index++;
//        }
//
//        jFreeChart.getCategoryPlot().setDataset(dC);

    }
}
