package com.l7tech.server.wsdm.faults;

import com.l7tech.server.wsdm.Namespaces;
import static com.l7tech.server.wsdm.faults.WsAddressingFault.WsaFaultDetailType.PROBLEM_HEADER_QNAME;
import org.w3c.dom.Node;

import java.util.HashMap;

/**
 * @author jbufu
 */
public class InvalidWsAddressingHeaderFault extends WsAddressingFault {

    private FaultCode faultCode = FaultCode.INVALID_ADDRESSING_HEADER;

    public InvalidWsAddressingHeaderFault(final String errMsg, Node problemHeader) {
        super(errMsg);
        addWsaFaultDetail(PROBLEM_HEADER_QNAME, new HashMap<String,String>() {{ put("errMsg", errMsg); }}, problemHeader.getLocalName());
    }
    public InvalidWsAddressingHeaderFault(String errMsg, FaultCode faultCode, Node problemHeader) {
        this(errMsg, problemHeader);
        this.faultCode = faultCode;
    }

    @Override
    protected String getFaultString() {
        return "A header representing a Message Addressing Property is not valid and the message cannot be processed";
    }

    @Override
    protected String getFaultCode() {
        return getNamespacePrefix(Namespaces.WSA) + ":" + faultCode;
    }

    /**
     * The predefined [SubCode] and [SubSubCode]s for the invalid addressing header
     */
    public enum FaultCode {
        INVALID_ADDRESSING_HEADER("InvalidAddressingHeader"),
        INVALID_ADDRESS("InvalidAddress"),
        INVALID_EPR("InvalidEPR"),
        INVALID_CARDINALITY("InvalidCardinality"),
        MISSING_ADDRESS_IN_EPR("MissingAddressInEPR"),
        DUPLICATE_MESSAGE_ID("DuplicateMessageID"),
        ACTION_MISMATCH("ActionMismatch"),
        ONLY_ANNONYMOUS_ADDRESS_SUPPORTED("OnlyAnonymousAddressSupported"),
        ONLY_NON_ANNONYMOUS_ADDRESS_SUPPORTED("OnlyNonAnonymousAddressSupported");

        private final String elementName;

        FaultCode(String elementName) {
            this.elementName = elementName;
        }

        @Override
        public String toString() {
            return elementName;
        }
    }
}
