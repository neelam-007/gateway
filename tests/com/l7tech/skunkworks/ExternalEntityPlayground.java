/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks;

import com.l7tech.common.util.XmlUtil;

/**
 * @author alex
 * @version $Revision$
 */
public class ExternalEntityPlayground {
    public static void main( String[] args ) throws Exception {
        XmlUtil.stringToDocument(DOC_WITH_EXTERNAL);
    }

    private static String DOC_WITH_EXTERNAL = 
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +  
            "<!DOCTYPE foo [\n" +
            "  <!ENTITY bar SYSTEM \"bar.xml\">\n" +
            "]>\n" +
            "<foo>\n" +
            "&bar;\n" +
            "</foo>";
}