package com.l7tech.common.xml.tarari;

/**
 * Represents resources held in kernel-memory by the Tarari driver, namely
 * a document in one buffer and a token list in the other.
 * <p>
 * This interface does not statically depend on any Tarari classes, so it is safe to refer to from classes
 * that need to load on a Windows VM without Tarari installed.
 * <p>
 * Code that needs to talk to the RAX API, and hence which already has dependencies on Tarari classes being loaded,
 * can feel free to downcast an instance of this to {@link TarariMessageContextImpl} for access to the
 * Tarari RAXContext and XMLDocument.
 */
public interface TarariMessageContext {
    /**
     * Free all resources used by this message context.
     */
    void close();

    /**
     * @return the {@link GlobalTarariContext} compiler generation count that was in effect when this
     *         TarariMessageContext was produced.
     */
    long getCompilerGeneration();
}
