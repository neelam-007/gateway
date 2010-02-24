package com.l7tech.policy.exporter;

import com.l7tech.policy.wsp.WspConstants;

/**
 * Provide xml constants for exported policy documents.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 16, 2004<br/>
 */
class ExporterConstants {
    static final String EXPORTED_POL_NS = WspConstants.L7_POLICY_NS + "/export";
    static final String EXPORTED_POL_PREFIX = "exp";
    static final String EXPORTED_DOCROOT_ELNAME = "Export";
    static final String EXPORTED_REFERENCES_ELNAME = "References";
    static final String REF_TYPE_ATTRNAME = "RefType";
    static final String VERSION_ATTRNAME = "Version";
    static final String CURRENT_VERSION = "3.0"; // policy version may be supported as of version 3.1
}
