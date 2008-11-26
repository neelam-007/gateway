/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Nov 12, 2008
 * Time: 5:44:27 PM
 */
package com.l7tech.gateway.standardreports;

import net.sf.jasperreports.engine.JRDefaultScriptlet;

public class SubIntervalScriptletHelper extends JRDefaultScriptlet {
    private ScriptletHelper scriptletHelper;

    public void setScriptletHelper(ScriptletHelper sH){
        scriptletHelper = sH;
    }


}
