package cz.muni.fi.sdipr.api.entities;

/**
 * Entity used in kafka auth topic as json object
 * @author Milos Silhar (433614)
 */
public class KafkaAuthKeyEntity {
    private String operation;
    private String compKey;
    private String expiresAt;

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getCompKey() {
        return compKey;
    }

    public void setCompKey(String compKey) {
        this.compKey = compKey;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

}
