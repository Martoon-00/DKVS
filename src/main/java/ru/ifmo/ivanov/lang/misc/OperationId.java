package ru.ifmo.ivanov.lang.misc;

public class OperationId {
    int clientId;
    int id;

    public OperationId() {
    }

    public OperationId(int clientId, int id) {
        this.clientId = clientId;
        this.id = id;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public int getId() {
        return id;
    }

    public void setId(int opId) {
        this.id = opId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OperationId)) return false;

        OperationId that = (OperationId) o;

        if (clientId != that.clientId) return false;
        return id == that.id;

    }

    @Override
    public int hashCode() {
        int result = clientId;
        result = 31 * result + id;
        return result;
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)", clientId, id);
    }
}
