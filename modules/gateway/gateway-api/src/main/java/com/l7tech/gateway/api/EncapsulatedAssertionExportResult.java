package com.l7tech.gateway.api;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Encass export result. The encass export result encapsulates an exported encass. Extends PolicyExportResult for ease
 * of use
 */
@XmlRootElement(name = "EncapsulatedAssertionExportResult")
@XmlType(name = "EncapsulatedAssertionExportResultType")
public class EncapsulatedAssertionExportResult extends PolicyExportResult {
    EncapsulatedAssertionExportResult() {
        super();
    }
}
