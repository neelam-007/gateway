
// This version of the audit record lookup script uses java.beans.XMLEncoder,
// so it saves all of the info, but is much slower, and the output format is hard to parse
// without using java.beans.XMLDecoder on the other end.

// Look up last 24 hours worth of audit records
var auditRecordManager = appContext.getBean("auditRecordManager");
var audits = auditRecordManager.find(new Packages.com.l7tech.common.audit.AuditSearchCriteria(null, null, null, null, null, null, 0, 0, 0));

// Encode as XML
var output = new java.io.ByteArrayOutputStream();
var encoder = new java.beans.XMLEncoder(output);
encoder.writeObject(audits);
encoder.close();

// Save as context variable "audits"
policyContext.setVariable("audits", output.toString());

// Assertion succeeds
true;
