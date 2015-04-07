package com.l7tech.policy.exporter;

import com.l7tech.gateway.common.workqueue.WorkQueue;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.WorkQueueable;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.*;

import java.util.logging.Logger;

public class WorkQueueReference extends ExternalReference {
    private final Logger logger = Logger.getLogger(WorkQueueReference.class.getName());

    private static final String REFERENCE = "WorkQueueReference";
    private static final String GOID = "GOID";
    private static final String NAME = "Name";
    private static final String MAX_QUEUE_SIZE = "MaxQueueSize";
    private static final String THREAD_POOL_MAX = "MaxWorkerThreads";
    private static final String REJECT_POLICY = "RejectPolicy";

    private Goid goid;
    private String name;
    private String maxQueueSize;
    private String threadPoolMax;
    private String rejectPolicy;

    private String localName;
    private LocalizeAction localizeType;

    public WorkQueueReference(final ExternalReferenceFinder finder) {
        super(finder);
    }

    public WorkQueueReference(final ExternalReferenceFinder finder, final WorkQueueable assertion) {
        this(finder);
        if (assertion == null) throw new IllegalStateException("WorkQueueable must not be null.");

        name = assertion.getWorkQueueName();
        try {
            WorkQueue wq = getFinder().getWorkQueue(name);
            if (wq != null) {
                goid = wq.getGoid();
                name = wq.getName();
                maxQueueSize = String.valueOf(wq.getMaxQueueSize());
                threadPoolMax = String.valueOf(wq.getThreadPoolMax());
                rejectPolicy = wq.getRejectPolicy();
            }
        } catch (FindException e) {
            logger.warning("Cannot find the work queue entity (Name = " + name + ").");
        }
        localizeType = LocalizeAction.IGNORE;
    }


    @Override
    public String getRefId() {
        String id = null;

        if (!goid.equals(WorkQueue.DEFAULT_GOID)) {
            id = goid.toString();
        }

        return id;
    }

    public Goid getGoid() {
        return goid;
    }

    public void setGoid(Goid goid) {
        this.goid = goid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMaxQueueSize() {
        return maxQueueSize;
    }

    public void setMaxQueueSize(String maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    public String getThreadPoolMax() {
        return threadPoolMax;
    }

    public void setThreadPoolMax(String threadPoolMax) {
        this.threadPoolMax = threadPoolMax;
    }

    public String getRejectPolicy() {
        return rejectPolicy;
    }

    public void setRejectPolicy(String rejectPolicy) {
        this.rejectPolicy = rejectPolicy;
    }

    @Override
    public boolean setLocalizeDelete() {
        localizeType = LocalizeAction.DELETE;
        return true;
    }

    @Override
    public void setLocalizeIgnore() {
        localizeType = LocalizeAction.IGNORE;
    }

    public void setLocalizeReplaceByName(String name) {
        localizeType = LocalizeAction.REPLACE;
        localName = name;
    }

    public static WorkQueueReference parseFromElement(final ExternalReferenceFinder context, final Element elmt) throws InvalidDocumentFormatException {
        // make sure passed element has correct name
        if (!elmt.getNodeName().equals(REFERENCE)) {
            throw new InvalidDocumentFormatException("Expecting element of name " + REFERENCE);
        }

        WorkQueueReference output = new WorkQueueReference(context);
        String val = getParamFromEl(elmt, GOID);
        try {
            output.goid = val != null ? new Goid(val) : WorkQueue.DEFAULT_GOID;
        } catch (IllegalArgumentException e) {
            throw new InvalidDocumentFormatException("Invalid work queue GOID: " + ExceptionUtils.getMessage(e), e);
        }

        output.name = getParamFromEl(elmt, NAME);
        output.maxQueueSize = getParamFromEl(elmt, MAX_QUEUE_SIZE);
        output.threadPoolMax = getParamFromEl(elmt, THREAD_POOL_MAX);
        output.rejectPolicy = getParamFromEl(elmt, REJECT_POLICY);
        return output;
    }

    @Override
    protected void serializeToRefElement(final Element referencesParentElement) {
        final Document doc = referencesParentElement.getOwnerDocument();
        Element referenceElement = doc.createElement(REFERENCE);
        setTypeAttribute(referenceElement);
        referencesParentElement.appendChild(referenceElement);

        addParameterElement(GOID, goid == null ? WorkQueue.DEFAULT_GOID.toString() : goid.toString(), referenceElement);
        addParameterElement(NAME, name, referenceElement);
        addParameterElement(MAX_QUEUE_SIZE, maxQueueSize, referenceElement);
        addParameterElement(THREAD_POOL_MAX, threadPoolMax, referenceElement);
        addParameterElement(REJECT_POLICY, rejectPolicy, referenceElement);
    }

    private void addParameterElement(final String name, final String value, final Element parent) {
        if (value != null) {
            final Element parameterElement = parent.getOwnerDocument().createElement(name);
            final Text txt = DomUtils.createTextNode(parent, value);
            parameterElement.appendChild(txt);
            parent.appendChild(parameterElement);
        }
    }

    @Override
    protected boolean verifyReference() throws InvalidPolicyStreamException {
        if (Syntax.getReferencedNames(name).length > 0) {
            return true;
        }
        try {
            WorkQueue wq = getFinder().getWorkQueue(name);
            return wq != null &&
                    wq.getName().equalsIgnoreCase(name) &&
                    String.valueOf(wq.getMaxQueueSize()).equals(maxQueueSize) &&
                    String.valueOf(wq.getThreadPoolMax()).equals(threadPoolMax) &&
                    wq.getRejectPolicy().equals(rejectPolicy);
        } catch (FindException e) {
            logger.warning("Cannot find work queue: " + name);
            return false;
        }
    }

    @Override
    protected boolean localizeAssertion(final @Nullable Assertion assertionToLocalize) {
        if (localizeType != LocalizeAction.IGNORE) {
            if (assertionToLocalize instanceof WorkQueueable) {
                final WorkQueueable workQueueable = (WorkQueueable) assertionToLocalize;
                if (workQueueable.getWorkQueueName().equalsIgnoreCase(name)) {
                    if (localizeType == LocalizeAction.REPLACE) {
                        workQueueable.setWorkQueueName(localName);
                    } else if (localizeType == LocalizeAction.DELETE) {
                        logger.info("Deleted this assertion from the tree.");
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final WorkQueueReference that = (WorkQueueReference) o;

        if (name != null ? !name.equals(that.name) : that.name != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
