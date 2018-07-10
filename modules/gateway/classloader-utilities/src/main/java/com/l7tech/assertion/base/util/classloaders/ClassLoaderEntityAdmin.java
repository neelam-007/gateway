package com.l7tech.assertion.base.util.classloaders;

import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.SaveException;

import java.util.List;
import java.util.Map;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.UNCHECKED_WIDE_OPEN;

/**
 * Created with IntelliJ IDEA.
 * User: rtung
 * Date: 2/27/14
 * Time: 2:04 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ClassLoaderEntityAdmin {

    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    public Map<String, String> getDefinedLibrariesToUpload();

    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    public List<String> getInstalledLibraries();

    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    public void addLibrary(String filename, byte[] bytes) throws SaveException;

}
