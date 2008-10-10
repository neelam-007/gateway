package com.l7tech.console.action;

import com.l7tech.console.util.Registry;

/**
 * Action to bring up the audit event download window.
 *
 * @author jbufu
 */
public class StartAuditArchiverAction extends SecureAction {

    public StartAuditArchiverAction() {
        super(null);
    }

    public String getName() {
        return "Start Archiver";
    }

    protected void performAction() {
        Registry.getDefault().getAuditAdmin().doAuditArchive();
    }
}
