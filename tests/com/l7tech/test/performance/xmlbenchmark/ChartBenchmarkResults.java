package com.l7tech.test.performance.xmlbenchmark;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.plot.PlotOrientation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Quick and dirty chart generator for the XML-Benchmarking japex data.  Using JFreeChart.
 *
 * User: vchan
 */
public class ChartBenchmarkResults {

    private static final String KEY_PARSING = "-parsing";
    private static final String KEY_SCHEMA = "-schema";
    private static final String KEY_XSLT = "-xslt";
    private static final String KEY_XPATH = "-xpath";

    private File file;
    private Document doc;
//    private ArrayList<String[]> results;
    private HashMap<String, ArrayList<ResultRecord>> resultsMap;

    public ChartBenchmarkResults(String resultFile) {

        // initialize the hashmap
        this.resultsMap = new HashMap<String, ArrayList<ResultRecord>>();
        this.resultsMap.put(KEY_PARSING, new ArrayList<ResultRecord>());
        this.resultsMap.put(KEY_SCHEMA, new ArrayList<ResultRecord>());
        this.resultsMap.put(KEY_XSLT, new ArrayList<ResultRecord>());
        this.resultsMap.put(KEY_XPATH, new ArrayList<ResultRecord>());

        // read the input file
        this.file = new File(resultFile);
        System.out.println("Input file " + resultFile + " exists: " + file.exists());
        if (file.exists()) {
            readFile();
        } else {
            throw new IllegalArgumentException("Input file does not exist.");
        }
    }

    public static final void main(String[] args) {

        ChartBenchmarkResults charter = new ChartBenchmarkResults(args[0]);

        String resultDir = "";
        if (args.length > 1 && new File(args[1]).exists()) {
            resultDir = args[1] + (args[1].endsWith("/")? "" : "/");

            System.out.println("Output to " + resultDir);
        }

        try {
            charter.parseResult();
            charter.chartResults(KEY_PARSING, resultDir + "xml-bench-parsing.jpg");
            charter.chartResults(KEY_SCHEMA, resultDir + "xml-bench-schema.jpg");
            charter.chartResults(KEY_XSLT, resultDir + "xml-bench-xslt.jpg");
            charter.chartResults(KEY_XPATH, resultDir + "xml-bench-xpath.jpg");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    protected void readFile() {

        FileInputStream ins = null;
        try {

            ins = new FileInputStream(file);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            dbf.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");

            DocumentBuilder builder = dbf.newDocumentBuilder();
            doc = builder.parse(new InputSource(ins));

        } catch (SAXException ex) {
            ex.printStackTrace();
        } catch (ParserConfigurationException pex) {
            pex.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ioe) {}
            }
        }
    }


    protected void parseResult() throws Exception {

        NodeList nodes = doc.getElementsByTagName("testCase");

        try {
            ResultRecord rec = null;
            for (int i=0; i<nodes.getLength(); i++) {

                rec = new ResultRecord(nodes.item(i));

                if (resultsMap.containsKey(rec.category))
                    resultsMap.get(rec.category).add(rec);
            }

        } catch (XPathExpressionException ex) {
            throw new Exception("XPath eval failed.", ex);
        }
    }

    protected void chartResults(String testCategory, String fileName)
    {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();

        for (ResultRecord rec : resultsMap.get(testCategory)) {
            ds.setValue(rec.getResult(), "tps", rec.name);
            System.out.println(rec.toString());
        }

        JFreeChart chart = ChartFactory.createBarChart3D(
           "XML-Benchmark " + testCategory,     // Chart name
           "Packages",                          // X axis label
           "Throughput (tps)",                  // Y axis value
           ds,                                  // data set
           PlotOrientation.VERTICAL,
           true, true, false);

        // Creating a JPEG image
        try
        {
           ChartUtilities.saveChartAsJPEG(new File(fileName), chart, 500, 300);
            System.out.println("Chart: " + fileName + " created. ok.");
        }
        catch (IOException e)
        {
           System.err.println("Problem occurred creating chart.");
        }
    }


    private static final String[] paths =
            {"@name", "resultIterations/text()", "resultTime/text()", "resultValue/text()"};

    protected class ResultRecord {

        String category;
        String name;
        String resultIterations;
        String resultTime;
        String resultValue;

        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xp = xpathFactory.newXPath();

        public ResultRecord(Node n) throws XPathExpressionException {

            this.name = xp.evaluate(paths[0], n);
            this.resultIterations = xp.evaluate(paths[1], n);
            this.resultTime = xp.evaluate(paths[2], n);
            this.resultValue = xp.evaluate(paths[3], n);
            checkCategory();
        }

        private void checkCategory()
        {
            if (!(checkName(KEY_PARSING) || checkName(KEY_SCHEMA) || checkName(KEY_XSLT) || checkName(KEY_XPATH))) {
                category = "";
            }
        }

        private boolean checkName(String key) {

            if (name.indexOf(key) > 0) {

                name = name.substring(0, name.indexOf(key));

                if (name.indexOf('*') >= 0 ) {
                    resultValue = "0";
                }
                
                category = key;
                return true;
            }
            return false;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();

            sb.append(category).append("-").append(name).append("; ");
            sb.append(resultIterations).append("; ").append(resultTime).append("; ");
            sb.append(resultValue);

            return sb.toString();
        }

        public Double getResult() {

            try {
                return Double.parseDouble(resultValue);

            } catch (NumberFormatException nfe) {
                return new Double(0.0);
            }
        }

    }

}
