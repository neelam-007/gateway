package com.l7tech.server.util;

import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.CommonMessages;

/**
 * @author steve
*/
public final class AuditingOperationListener implements SoapUtil.OperationListener {
    private final Audit audit;

    public AuditingOperationListener(final Audit audit) {
        this.audit = audit;
    }

    public void notifyNoStyle(final String name) {
        audit.logAndAudit(CommonMessages.WSDL_OPERATION_NO_STYLE, name);
    }

    public void notifyPartName(final String name) {
        audit.logAndAudit(CommonMessages.WSDL_OPERATION_PART_TYPE, name);
    }


    public void notifyBadStyle(final String operationStyle, final String name) {
        audit.logAndAudit(CommonMessages.WSDL_OPERATION_BAD_STYLE, operationStyle, name);
    }

    public void notifyNoNames(final String name) {
        audit.logAndAudit( CommonMessages.WSDL_OPERATION_NO_QNAMES_FOR_OP, name);
    }
}
