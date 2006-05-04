package com.l7tech.policy.assertion;

import com.l7tech.common.xml.SoapFaultLevel;

/**
 * This assertions allows the overriding of the PolicyEnforcementContext's SoapFaultLevel
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 4, 2006<br/>
 *
 * @see SoapFaultLevel
 */
public class FaultLevel extends Assertion {
    private SoapFaultLevel data = new SoapFaultLevel();

    public FaultLevel() {
    }

    public SoapFaultLevel getLevelInfo() {
        return data;
    }

    public void setLevelInfo(SoapFaultLevel levelInfo) {
        this.data = levelInfo;
    }
}
