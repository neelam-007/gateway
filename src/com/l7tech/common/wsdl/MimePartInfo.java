package com.l7tech.common.wsdl;

import java.io.Serializable;
import java.util.Vector;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class MimePartInfo implements Serializable {
    protected String name;
    protected Object[] contentTypes = null;
    private int maxLength;

    public MimePartInfo() {
    }

    public MimePartInfo(String name, String contentType) {
        this.name = name;
        contentTypes = new String[1];
        contentTypes[0] = contentType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object[] getContentTypes() {
        return contentTypes;
    }

    public void setContentTypes(Object[] contentTypes) {
        this.contentTypes = contentTypes;
    }

    public void addContentType(String contentType) {
        Vector newContentTypes = new Vector();
        for (int i = 0; i < contentTypes.length; i++) {
            newContentTypes.add(contentTypes[i]);
        }
        // add the new content type to the list
        newContentTypes.add(contentType);
        contentTypes = newContentTypes.toArray();
    }

    public String retrieveAllContentTypes() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < contentTypes.length; i++) {
            sb.append((String) contentTypes[i]).append(", ");
        }

        String resultString = sb.toString();

        // don't show the last 2 characters
        return resultString.substring(0, resultString.length()-2);
    }

    public boolean validateContentType(String contentType) {
        for (int i = 0; i < contentTypes.length; i++) {
            String validContentType = (String) contentTypes[i];
            if(validContentType.equals(contentType) ||
                 (validContentType.equals("*/*")) ||
                 (validContentType.equals("text/enriched") && contentType.equals("text/plain"))) {
                // content type is valid
                return true;
            }
        }
        // not found
        return false;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }
}
