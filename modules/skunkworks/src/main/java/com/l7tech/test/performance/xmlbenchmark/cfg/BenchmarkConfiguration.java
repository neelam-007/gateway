//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-325 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2008.04.23 at 05:07:08 PM PDT 
//


package com.l7tech.test.performance.xmlbenchmark.cfg;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for BenchmarkConfiguration complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="BenchmarkConfiguration">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="benchmark-test" type="{http://l7tech.com/xmlbench}TestConfiguration" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BenchmarkConfiguration", namespace = "http://l7tech.com/xmlbench", propOrder = {
    "benchmarkTest"
})
public class BenchmarkConfiguration {

    @XmlElement(name = "benchmark-test", namespace = "http://l7tech.com/xmlbench", required = true)
    protected List<TestConfiguration> benchmarkTest;

    /**
     * Gets the value of the benchmarkTest property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the benchmarkTest property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getBenchmarkTest().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TestConfiguration }
     * 
     * 
     */
    public List<TestConfiguration> getBenchmarkTest() {
        if (benchmarkTest == null) {
            benchmarkTest = new ArrayList<TestConfiguration>();
        }
        return this.benchmarkTest;
    }

}
