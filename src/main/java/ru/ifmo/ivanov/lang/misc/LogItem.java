package ru.ifmo.ivanov.lang.misc;

public class LogItem {
    Operation operation;
    int termNumber;
    int index;

    private LogItem() {
    }

    public LogItem(Operation operation, int termNumber, int index) {
        this.operation = operation;
        this.termNumber = termNumber;
        this.index = index;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public int getTermNumber() {
        return termNumber;
    }

    public void setTermNumber(int termNumber) {
        this.termNumber = termNumber;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

}
