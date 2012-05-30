package com.l7tech.gateway.common.audit;

import org.w3c.dom.*;

import javax.xml.bind.MarshalException;
import java.util.*;

/**
 * Unmarshalling of an audit detail's properties from a DOM element.
 * Properties includes:
 *      "param" =  String[]  AuditDetail::params
 */
public class AuditDetailPropertiesDomUnmarshaller {
    private static final String NS = "http://l7tech.com/audit/detail";

    /**
     * Add the specified AuditRecord as a DOM element as a child of the spec
     *
     * @return the element that was created.
     */
    public Map<String,Object> unmarshal(Element element) throws MarshalException {

        Map<String,Object> result = new HashMap<String,Object>();
        for(int i = 0 ; i < element.getChildNodes().getLength() ; ++i){
            Node thisNode = element.getChildNodes().item(i);
            if( thisNode.getNodeName().equals("params"))
            {
                result.put("params",getParams(thisNode));
            }
        }
        return result;
    }
    
    private String[] getParams(Node paramsNode){
        NodeList paramNodeList = paramsNode.getChildNodes();

        List<String> params = new ArrayList<String>();
        for(int j = 0 ; j < paramNodeList.getLength(); ++j){
            Node paramNode = paramNodeList.item(j);
            if(paramNode.getNodeName().equals("param")){
                if(paramNode.getChildNodes().getLength()<1){
                    params.add("");
                }
                else
                {
                    params.add(paramNode.getChildNodes().item(0).getNodeValue());
                }
            }

        }
        return params.toArray(new String[params.size()]);


    }
}
