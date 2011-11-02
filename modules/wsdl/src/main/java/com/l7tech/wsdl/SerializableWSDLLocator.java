package com.l7tech.wsdl;

import javax.wsdl.xml.WSDLLocator;
import java.io.Serializable;

/**
 * Extension of WSDLLocator for locators that support serialization
 */
public interface SerializableWSDLLocator extends WSDLLocator, Serializable {
}
