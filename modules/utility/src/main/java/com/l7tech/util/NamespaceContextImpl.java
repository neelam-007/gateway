package com.l7tech.util;

import javax.xml.namespace.NamespaceContext;
import java.util.*;

/**
 * Basic Map backed NamespaceContext.
 */
public class NamespaceContextImpl implements NamespaceContext {

    //- PUBLIC

    public NamespaceContextImpl( final Map<String,String> namespacePrefixesToUris ) {
        this.nsPreToUriMap = new HashMap<String,String>();
        this.nsUriToPreMap = new HashMap<String,List<String>>();
        loadNamespaces( namespacePrefixesToUris, nsPreToUriMap, nsUriToPreMap );        
    }

    @Override
    public String getNamespaceURI( final String prefix) {
        return nsPreToUriMap.get(prefix);
    }

    @Override
    public String getPrefix( final String namespaceURI ) {
        String prefix = null;
        Iterator iter = getPrefixes(namespaceURI);
        if(iter.hasNext()) {
            prefix = (String) iter.next();
        }
        return prefix;
    }

    @Override
    public Iterator<String> getPrefixes( final String namespaceURI) {
        List<String> prefixes = nsUriToPreMap.get(namespaceURI);
        Iterator<String> prefixIter;
        if(prefixes!=null) {
            prefixIter = prefixes.iterator();
        }
        else {
            prefixIter = Collections.<String>emptyList().iterator();
        }
        return prefixIter;
    }

    //- PRIVATE

    private final Map<String,String> nsPreToUriMap;
    private final Map<String,List<String>> nsUriToPreMap;

    private void loadNamespaces( final Map<String,String> namespaceMap,
                                 final Map<String,String> newNsPreToUri,
                                 final Map<String,List<String>> newNsUriToPre) {
        for (Map.Entry<String,String> entry : namespaceMap.entrySet()) {
            String nsPrefix = entry.getKey();
            String nsUri = entry.getValue();

            if (nsPrefix.length() == 0 || nsUri.length() == 0) {
                continue;
            }

            newNsPreToUri.put(nsPrefix, nsUri);
            List<String> uriList = newNsUriToPre.get(nsUri);
            if ( uriList == null ) {
                uriList = new ArrayList<String>(4);
                newNsUriToPre.put(nsUri, uriList);
            }
            uriList.add(nsPrefix);
        }
    }
}
