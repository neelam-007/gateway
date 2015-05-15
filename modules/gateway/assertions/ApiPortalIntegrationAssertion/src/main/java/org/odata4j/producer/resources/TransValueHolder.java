package org.odata4j.producer.resources;

/**
 * Object to hold some values that we can pass around
 *
 * @author rraquepo, 11/25/14
 */
public class TransValueHolder {
    private boolean fastFail;
    private boolean hasError;
    private String lastOperationEntityName;
    private String lastOperationEntityId;
    private String lastOperationMethod;
    private String lastOperationStatus;
    private String lastOperationBody;
    private String lastOperationPayload;
    private int batchCount;

    public TransValueHolder(final boolean fastFail) {
        this.fastFail = fastFail;
    }


    public boolean isFastFail() {
        return fastFail;
    }

    public void setFastFail(boolean fastFail) {
        this.fastFail = fastFail;
    }

    public boolean isHasError() {
        return hasError;
    }

    public void setHasError(boolean hasError) {
        this.hasError = hasError;
    }

    public String getLastOperationEntityName() {
        return lastOperationEntityName;
    }

    public void setLastOperationEntityName(String lastOperationEntityName) {
        this.lastOperationEntityName = lastOperationEntityName;
    }

    public String getLastOperationEntityId() {
        return lastOperationEntityId;
    }

    public void setLastOperationEntityId(String lastOperationEntityId) {
        this.lastOperationEntityId = lastOperationEntityId;
    }

    public String getLastOperationMethod() {
        return lastOperationMethod;
    }

    public void setLastOperationMethod(String lastOperationMethod) {
        this.lastOperationMethod = lastOperationMethod;
    }

    public String getLastOperationStatus() {
        return lastOperationStatus;
    }

    public void setLastOperationStatus(String lastOperationStatus) {
        this.lastOperationStatus = lastOperationStatus;
    }

    public String getLastOperationBody() {
        return lastOperationBody;
    }

    public void setLastOperationBody(String lastOperationBody) {
        this.lastOperationBody = lastOperationBody;
    }

    public String getLastOperationPayload() {
        return lastOperationPayload;
    }

    public void setLastOperationPayload(String lastOperationPayload) {
        this.lastOperationPayload = lastOperationPayload;
    }

    public int getBatchCount() {
        return batchCount;
    }

    public void setBatchCount(int batchCount) {
        this.batchCount = batchCount;
    }
}
