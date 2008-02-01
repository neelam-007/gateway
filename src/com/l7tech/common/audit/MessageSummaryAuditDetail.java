package com.l7tech.common.audit;

/**
 * Extension of AuditDetail with knowledge of message processing.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class MessageSummaryAuditDetail extends AuditDetail implements ExtendedAuditDetail {

    /**
     * Create an audit detail that will update the response message.
     *
     * @param response The response message value.
     */
    public MessageSummaryAuditDetail(String response) {
        super(Messages.EXCEPTION_INFO);
        if (response == null) response = "";
        this.response = response;
    }

    /**
     * Check if this detail should be saved.
     *
     * @return false, this detail is never saved.
     */
    public boolean shouldSave() {
        return false;
    }

    @Override
    public void setAuditRecord(AuditRecord auditRecord) {
        if (auditRecord instanceof MessageSummaryAuditRecord) {
            MessageSummaryAuditRecord msar = (MessageSummaryAuditRecord) auditRecord;
            msar.responseXml = response;

            // Note: I don't know why we save the number of characters as the
            // content length, hopefully it is usually the same.
            msar.responseContentLength = response.length();
        }
        super.setAuditRecord(auditRecord);
    }

    //- PRIVATE

    private final String response;
}
