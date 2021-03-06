package com.l7tech.policy.assertion.xmlsec;

import java.util.List;

/**
 *
 */
public interface HasPermittedXencAlgorithmList {
    List<String> getXEncAlgorithmList();

    /**
     * Update the encryption algorithm list.  It is important to update the algorithm with the highest preference.
     * @param newList the new list of algorithms to use.  May or may not be nullable depending on subclass.
     */
    void setXEncAlgorithmList(List<String> newList);
}
