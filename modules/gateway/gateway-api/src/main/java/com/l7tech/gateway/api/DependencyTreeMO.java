package com.l7tech.gateway.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * This was created: 11/4/13 as 3:20 PM
 *
 * @author Victor Kazakov
 */
@XmlRootElement(name = "DependencyTree")
@XmlType(propOrder = {"options", "searchObjectItem", "dependencies"})
@XmlAccessorType(XmlAccessType.PROPERTY)
public class DependencyTreeMO extends DependencyResults<DependencyMO> {
    protected DependencyTreeMO(){}
}
