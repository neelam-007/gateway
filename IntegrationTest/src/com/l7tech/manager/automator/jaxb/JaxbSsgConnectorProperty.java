package com.l7tech.manager.automator.jaxb;

import com.l7tech.common.transport.SsgConnector;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: May 16, 2008
 * Time: 3:25:31 PM
 * To change this template use File | Settings | File Templates.
 */
@XmlRootElement
public class JaxbSsgConnectorProperty {

    private SsgConnector ssgConnector;
    private Map properties;

    public Map getProperties() {
        return properties;
    }

    public void setProperties(Map properties) {
        this.properties = properties;
    }

    public SsgConnector getSsgConnector() {
        return ssgConnector;
    }

    public void setSsgConnector(SsgConnector ssgConnector) {
        this.ssgConnector = ssgConnector;
    }
}