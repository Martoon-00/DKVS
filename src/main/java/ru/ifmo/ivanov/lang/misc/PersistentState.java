package ru.ifmo.ivanov.lang.misc;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.MappingIterator;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@SuppressWarnings("FieldCanBeLocal")
public class PersistentState implements AutoCloseable {
    public static final String DEFAULT_FILENAME = "dkvs_%d.log";

    public static final String RESERVED_KEY = "__RESERVED__";

    private final Map<String, String> map = new HashMap<>();
    private final ArrayList<LogItem> logs = new ArrayList<>();
    private int currentTerm = 0;
    private Integer votedFor = null;
    private int committedNum = 0;

    private Consumer<LogItem> onLogApplied = op -> {
    };

    private File file;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private JsonGenerator jsonGenerator;


    public PersistentState(int nodeNumber) throws IOException {
        this(String.format(DEFAULT_FILENAME, nodeNumber));
    }

    public PersistentState(String fileName) throws IOException {
        file = new File(fileName);
        if (file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                JsonParser jsonParser = new JsonFactory().createJsonParser(in);

                MappingIterator<JOperation> iterator = objectMapper.readValues(jsonParser, JOperation.class);
                while (iterator.hasNext()) {
                    JOperation op = iterator.next();
                    currentTerm = op.currentTerm;
                    votedFor = op.votedFor;
                    committedNum = op.commitedNum;
                    if (op.getLog() != null) {
                        logs.add(op.getLog());
                    }
                }
                for (int i = 0; i < Math.min(logs.size(), committedNum); i++) {
                    enforceLog(logs.get(i), false);
                }
            }
        }

        jsonGenerator = new JsonFactory().createJsonGenerator(new BufferedWriter(new FileWriter(fileName, true))).useDefaultPrettyPrinter();
        if (logs.size() == 0) {
            putLog(new Operation(new OperationId(-1, -1), null, Operation.DELETE, RESERVED_KEY));
            // PAY ATTENTION: not unique id  ---->  ^^^^^^^^
            committedNum++;
        }
    }

    private void write(JOperation op) {
        try {
            if (jsonGenerator.isClosed())
                return;
            objectMapper.writeValue(jsonGenerator, op);
            jsonGenerator.flush();
        } catch (IOException e) {
            throw new RuntimeException("Error while writing to journal: ", e);
        }
    }

    private void applyNewLog(LogItem logItem) {
        String res;
        Operation op = logItem.getOperation();
        switch (op.command) {
            case Operation.SET:
                map.put(op.args[0], op.args[1]);
                res = "STORED";
                break;
            case Operation.DELETE:
                res = map.remove(op.args[0]);
                if (res == null) res = "NOT_FOUND";
                else res = "DELETED";
                break;
            case Operation.GET:
                res = map.get(op.args[0]);
                if (res == null) res = "NOT_FOUND";
                else res = String.format("VALUE %s %s", op.args[0], res);
                break;
            case Operation.PING:
                res = "PONG";
                break;
            default:
                throw new RuntimeException(String.format("Unrecognized command %s.", op.command));
        }
        op.setResult(new OperationResult(res));
        onLogApplied.accept(logItem);
    }

    public int getCurrentTerm() {
        return currentTerm;
    }

    public void setCurrentTerm(int currentTerm) {
        if (currentTerm == getCurrentTerm())
            return;
        votedFor = null;
        write(new JOperation(currentTerm, votedFor, committedNum, null));
        this.currentTerm = currentTerm;
    }

    public Integer getVotedFor() {
        return votedFor;
    }

    public void setVotedFor(Integer votedFor) {
        write(new JOperation(currentTerm, votedFor, committedNum, null));
        this.votedFor = votedFor;
    }

    public int getCommittedNum() {
        return committedNum;
    }

    /**
     * Sets commit index to specified value, but no more than log size.
     *
     * @return new value
     */
    public int setCommittedNum(int commitedNum) {
        commitedNum = Math.min(commitedNum, logs.size());
        write(new JOperation(currentTerm, votedFor, commitedNum, null));
        while (this.committedNum < commitedNum)
            applyNewLog(logs.get(this.committedNum++));
        return commitedNum;
    }

    /**
     * @return true if operation was put in the log, otherwise false (operation is immediate)
     */
    public boolean putLog(Operation op) {
        if (op.command.equals(Operation.GET) || op.command.equals(Operation.PING)) {
            applyNewLog(new LogItem(op, currentTerm, -1));
            return false;
        }
        enforceLog(new LogItem(op, currentTerm, logs.size()));
        return true;
    }

    private boolean enforceLog(LogItem item, boolean write) {
        if (item.getIndex() < committedNum)
            return false;
        if (item.getIndex() > logSize())
            throw new RuntimeException("Enforced item has too large index id");

        if (write)
            write(new JOperation(currentTerm, votedFor, committedNum, item));
        logs.subList(item.getIndex(), logs.size()).clear();
        logs.add(item);
        return true;
    }

    public boolean enforceLog(LogItem item) {
        return enforceLog(item, true);
    }

    public LogItem getLog(int i) {
        return logs.get(i);
    }

    public LogItem getLastLog() {
        return logs.isEmpty() ? null : logs.get(logs.size() - 1);
    }

    public int logSize() {
        return logs.size();
    }

    public PersistentState setOnLogApplied(Consumer<LogItem> onLogApplied) {
        this.onLogApplied = onLogApplied;
        return this;
    }

    public void close() {
        try {
            jsonGenerator.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteAllDefault() {
        Pattern isJournal = Pattern.compile(PersistentState.DEFAULT_FILENAME.replaceAll("%d", "\\\\d+"));
        File[] files = new File(".").listFiles((dir, name) -> {
            return isJournal.matcher(name).matches();
        });
        for (File file : files) {
            file.delete();
        }
    }

    public void delete() {
        close();
        file.delete();
    }

    private static class JOperation {
        private int currentTerm;
        private Integer votedFor;
        private int commitedNum;
        private LogItem log;

        public JOperation() {
        }

        public JOperation(int currentTerm, Integer votedFor, int commitedNum, LogItem log) {
            this.currentTerm = currentTerm;
            this.votedFor = votedFor;
            this.commitedNum = commitedNum;
            this.log = log;
        }

        public LogItem getLog() {
            return log;
        }

        public void setLog(LogItem log) {
            this.log = log;
        }

        public int getCurrentTerm() {
            return currentTerm;
        }

        public void setCurrentTerm(int currentTerm) {
            this.currentTerm = currentTerm;
        }

        public Integer getVotedFor() {
            return votedFor;
        }

        public void setVotedFor(Integer votedFor) {
            this.votedFor = votedFor;
        }

        public int getCommitedNum() {
            return commitedNum;
        }

        public void setCommitedNum(int commitedNum) {
            this.commitedNum = commitedNum;
        }
    }
}

