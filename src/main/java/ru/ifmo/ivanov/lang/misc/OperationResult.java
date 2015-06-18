package ru.ifmo.ivanov.lang.misc;

public class OperationResult {
    String result;

    public OperationResult() {
    }

    public OperationResult(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return result;
    }
}
