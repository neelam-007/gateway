package com.l7tech.policy.exporter;

import com.l7tech.policy.wsp.WspConstants;

/**
 * Provide xml constants for exported policy documents.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 16, 2004<br/>
 * $Id$<br/>
 */
public class ExporterConstants {
    public static final String EXPORTED_POL_NS = WspConstants.POLICY_NS + "/export";
    public static final String EXPORTED_POL_PREFIX = "exp";
    public static final String EXPORTED_DOCROOT_ELNAME = "Export";
    public static final String EXPORTED_REFERENCES_ELNAME = "References";
}
