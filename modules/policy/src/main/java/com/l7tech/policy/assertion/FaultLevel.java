package com.l7tech.policy.assertion;

import com.l7tech.xml.SoapFaultLevel;

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
public class FaultLevel extends Assertion implements UsesVariables {
    private SoapFaultLevel data = new SoapFaultLevel();
    private boolean soap12 = false;

    public FaultLevel() {
    }

    public SoapFaultLevel getLevelInfo() {
        return data;
    }

    public void setLevelInfo(SoapFaultLevel levelInfo) {
        this.data = levelInfo;
    }

    public String[] getVariablesUsed() {
        return data.getVariablesUsed();
    }

    public boolean isSoap12() {
        return soap12;
    }
    
    public void setSoap12(boolean soap12) {
        this.soap12 = soap12;
    }
}
