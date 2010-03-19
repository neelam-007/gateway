/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.io.IOException;

/**
 * @author alex
 */
public class NewPreorderIterator implements Iterator<Assertion> {
    private final AssertionTranslator translator;
    private final Iterator delegate;

    private Assertion last = null;

    public NewPreorderIterator(final Assertion origRoot,
                               final AssertionTranslator translator)
            throws PolicyAssertionException
    {
        this.translator = translator;

        final Assertion root;
        try {
            root = WspReader.getDefault().parsePermissively(WspWriter.getPolicyXml(origRoot), WspReader.INCLUDE_DISABLED);
            updateAssertionTemporaryData(root, origRoot);
        } catch (IOException e) {
            throw new RuntimeException(e); // Can't happen
        }

        final Assertion translated = translator == null ? root : translator.translate(root);
        final CompositeAssertion comp;
        if (translated instanceof CompositeAssertion) {
            comp = (CompositeAssertion) root;
        } else {
            this.delegate = Arrays.asList(translated).iterator();
            return;
        }
        final List<Assertion> results = new ArrayList<Assertion>();
        results.add(root);
        collect(comp, results);
        this.delegate = results.iterator();
    }

    private void updateAssertionTemporaryData(Assertion newAssertion, Assertion oldAssertion) {
        if(!newAssertion.getClass().equals(oldAssertion.getClass())) {
            return;
        }

        if(newAssertion instanceof CompositeAssertion) {
            CompositeAssertion newCompAssertion = (CompositeAssertion)newAssertion;
            CompositeAssertion oldCompAssertion = (CompositeAssertion)oldAssertion;
            Iterator itOld = oldCompAssertion.children();

            for(Iterator itNew = newCompAssertion.children();itNew.hasNext() && itOld.hasNext();) {
                Assertion newChild = (Assertion)itNew.next();
                Assertion oldChild = (Assertion)itOld.next();
                updateAssertionTemporaryData(newChild, oldChild);
            }
        } else {
            newAssertion.updateTemporaryData(oldAssertion);
        }
    }

    private void collect(final CompositeAssertion root,
                         final List<Assertion> results)
            throws PolicyAssertionException
    {
        final List<Assertion> kids = root.getChildren();
        for (int i = 0; i < kids.size(); i++) {
            Assertion kid = kids.get(i);

            // Translate if necessary
            final Assertion translated = translator == null ? kid : translator.translate(kid);

            // Re-parent
            translated.setParent(root);
            kids.set(i, translated);

            // Recurse
            results.add(translated);
            if (translated instanceof CompositeAssertion) {
                collect((CompositeAssertion) translated, results);
            }

            if ( translator != null ) {
                translator.translationFinished(kid);
            }
        }
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public Assertion next() {
        return last = (Assertion) delegate.next();
    }

    @Override
    public void remove() {
        final CompositeAssertion parent = last.getParent();
        if (parent == null) throw new UnsupportedOperationException("Can't remove root");
        parent.removeChild(last);
        delegate.remove();
    }
}
