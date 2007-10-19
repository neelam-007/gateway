
// This version of the audit record lookup script builds an XML output representation by hand.
// This is much faster than spinning up an XMLEncoder, but is much more work since
// each field that is to be extracted must be extracted by hand.

// Look up last 24 hours worth of audit records
var auditRecordManager = appContext.getBean("auditRecordManager");
var audits = auditRecordManager.find(new Packages.com.l7tech.common.audit.AuditSearchCriteria(null, null, null, null, null, null, 0, 0, 0));

// Encode as XML
var xml = "<audits>\n";
for each (record in audits.toArray()) {
    xml += "<record id=\"" + record.getOid() + "\">\n";
    xml += "  <message>" + record.getMessage() + "</message>\n";
    for each (detail in record.getDetails().toArray()) {
        xml += "  <detail>";
        xml += "<messageId>" + detail.getMessageId() + "</messageId>";
        xml += "</detail>\n";
    }
    xml += "</record>\n";
}
xml += "</audits>\n";

// Save as context variable "audits"
policyContext.setVariable("audits", xml);

// Assertion succeeds
true;

