package com.l7tech.server.hpsoam.metrics;

import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Pair;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.gateway.common.service.PublishedService;

import javax.wsdl.Binding;
import javax.wsdl.Operation;
import javax.wsdl.WSDLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Maps http://openview.hp.com/xmlns/soa/1/perf:ServicePerformance
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 19, 2007<br/>
 */
public class ServicePerformance {
    private final Logger logger = Logger.getLogger(ServicePerformance.class.getName());
    private Goid serviceGOID;
    private String localName;
    private String ns;
    private ArrayList<OperationPerformance> opPerfs = new ArrayList<OperationPerformance>();


    public ServicePerformance(PublishedService ps) {
        serviceGOID = ps.getGoid();
        localName = ps.getName();
        try {
            Wsdl parsedWSDL = ps.parsedWsdl();
            ns = parsedWSDL.getDefinition().getTargetNamespace();
            // todo, is it possible that operations would have different namespace from service?
            Collection opnames = SoapUtil.getOperationNames(parsedWSDL);
            for (Object opname : opnames) {
                opPerfs.add(new OperationPerformance(opname.toString(), ns));
            }
        } catch (WSDLException e) {
            logger.log(Level.WARNING, "cannot parse wsdl", e);
        }
    }

    public Goid getServiceGOID() {
        return serviceGOID;
    }

    public String getLocalName() {
        return localName;
    }

    public String getNs() {
        return ns;
    }

    public ArrayList<OperationPerformance> getOpPerfs() {
        return opPerfs;
    }

    public OperationPerformance getOrMakeOperationPerformance(PolicyEnforcementContext context) {
        try {
            final Pair<Binding,Operation> pair = context.getBindingAndOperation();
            Operation thisop = pair.right;
            String thisns = "";
            if (context.getRequest().getSoapKnob().getPayloadNames().length > 0) {
                thisns = context.getRequest().getSoapKnob().getPayloadNames()[0].getNamespaceURI();
            }
            for (OperationPerformance op : opPerfs) {
                if (op.getLocalName().equals(thisop.getName())) {
                    if (op.getNs().equals(thisns)) {
                        return op;
                    }

                }
            }
            OperationPerformance output = new OperationPerformance(thisop.getName(), thisns);
            opPerfs.add(output);
            return output;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "problem getting operation ", e);
            throw new RuntimeException(e);
        }
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(" id " + serviceGOID);
        buf.append(" name " + localName);
        buf.append(" ns " + ns);
        for (OperationPerformance op : opPerfs) {
            buf.append(op.toString());
        }
        return buf.toString();
    }
}
