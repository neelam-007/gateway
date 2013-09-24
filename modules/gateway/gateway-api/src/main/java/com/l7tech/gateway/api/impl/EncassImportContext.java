package com.l7tech.gateway.api.impl;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * This is used in encas import.
 * Extends PolicyImportContext for ease of use
 *
 * @author Victor Kazakov
 */
@XmlRootElement(name = "EncassImportContext")
@XmlType(name = "EncassImportContextType")
public class EncassImportContext extends PolicyImportContext {
}
