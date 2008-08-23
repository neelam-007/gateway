package com.l7tech.manager.automator.jaxb;


import com.l7tech.gateway.common.service.PublishedService;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Set;
import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Apr 16, 2008
 * Time: 8:58:34 AM
 * Wrap a published service so that we can get around the unmodifiable set returned by getHttpMethods
 * Not extending as creating an instance of this would require copying the PublishedService.
 */
@XmlRootElement
public class JaxbPublishedService {

    public JaxbPublishedService(){
    }

    public JaxbPublishedService(PublishedService pService){
        this.pService = pService;
    }

    /*
    * This method is marked as XmlTransient in PublishedService as it returns an UnmodifiableSet which
    * jaxb cannot work with.
    * */
    public Set<String> getHttpMethods() {
        Set<String> unModifiableSet = pService.getHttpMethods();
        Set<String> returnSet = new HashSet<String>();
        for(String s: unModifiableSet){
            returnSet.add(s);
        }
        return returnSet;
    }

    public PublishedService getPService() {
        return pService;
    }

    public void setPService(PublishedService pService) {
        this.pService = pService;
    }

    private PublishedService pService;

}
