package com.l7tech.gateway.api.impl;

import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Support class for implementing extensions.
 */
@XmlTransient
public class ExtensionSupport {

    //- PROTECTED

    @XmlAnyAttribute
    protected Map<QName, Object> getAttributeExtensions() {
        return attributeExtensions;
    }

    final protected void setAttributeExtensions( final Map<QName, Object> attributeExtensions ) {
        this.attributeExtensions = attributeExtensions;
    }

    protected Extension getExtension() {
        return extension;
    }

    protected void setExtension( final Extension extension ) {
        this.extension = extension;
    }

    protected List<Object> getExtensions() {
        return extensions;
    }

    protected final void setExtensions( final List<Object> extensions ) {
        this.extensions = extensions;
    }

    /**
     * Gets a unique object from the extensions. The first object that the matcher returns true on will be returned.
     *
     * @param matcher The match to find the unique object. Should return true when the object its given is the one to
     *                return
     * @param <O>     The object type
     * @return The object from the extensions or null if one can't be found
     */
    @Nullable
    public <O> O getUniqueExtension(@NotNull final Functions.Unary<Boolean, Object> matcher) {
        //noinspection unchecked
        return getExtension() == null || getExtension().getExtensions() == null ? null : (O) Functions.grepFirst(getExtension().getExtensions(), new Functions.Unary<Boolean, Object>() {
            @Override
            public Boolean call(Object o) {
                return matcher.call(o);
            }
        });
    }

    /**
     * Adds a unique object to the extensions.
     *
     * @param object  The object to add to the extensions
     * @param matcher The matcher is used to match the find the existing object so that if can be removed/replaced.
     * @param <O>     The type of the object
     */
    public <O> void setUniqueExtension(@Nullable final O object, @NotNull final Functions.Unary<Boolean, Object> matcher) {
        final List<Object> extensions;
        if (getExtension() == null || getExtension().getExtensions() == null) {
            final Extension extension = new Extension();
            setExtension(extension);
            extensions = new ArrayList<>();
        } else {
            //remove the old object
            extensions = Functions.grep(getExtension().getExtensions(), new Functions.Unary<Boolean, Object>() {
                @Override
                public Boolean call(Object o) {
                    return !matcher.call(o);
                }
            });
        }
        if (object != null) {
            extensions.add(object);
        }
        if (extensions.isEmpty()) {
            setExtension(null);
        } else {
            getExtension().setExtensions(extensions);
        }
    }

    //- PRIVATE

    private Extension extension;
    private List<Object> extensions;
    private Map<QName,Object> attributeExtensions;

}
