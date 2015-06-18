package ru.ifmo.ivanov.lang.node;

import ru.ifmo.ivanov.lang.judge.Judge;
import ru.ifmo.ivanov.lang.message.CommandMessage;
import ru.ifmo.ivanov.lang.misc.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.TreeMap;
import java.util.function.Function;

import static ru.ifmo.ivanov.lang.message.RaftMessageFormatter.*;

public class RaftNode extends Service {

    private final PersistentState persistent;

    private State state = null;

    /**
     * About how long node can not become candidate if once found a candidate with higher rank
     */
    private long lastSubmissionTime = Long.MIN_VALUE / 2;
    private static final Function<Integer, Integer> submissionDuration = property_timeout -> 0; //2 * property_timeout;

    public RaftNode(int ordinal) throws IOException {
        super(ordinal);
        persistent = new PersistentState(ordinal).setOnLogApplied(log -> {
            sendToSelf(REMIND_CLIENT_RESPONSE, log.getOperation());
            if (log.getIndex() != -1)
                new Judge().registerCommit(log, getOrdinal());
        });
        setIdentifier(new Identifier(() -> String.format("[state:\u001B[32m%10s\u001B[m, term:\u001B[32m%3d\u001B[m, vote:\u001B[32m%4d\u001B[m, logSize:\u001B[32m%3d\u001B[m, committed:\u001B[32m%3d\u001B[m] - \u001B[36mNode %d\u001B[m",
                state, persistent.getCurrentTerm(), persistent.getVotedFor(), persistent.logSize(), persistent.getCommittedNum(), ordinal)));
    }

    @Override
    protected void init() {
        state = new FollowerState();
    }

    @Override
    protected CommandMessage onMessage(InetSocketAddress address, String command, Object... args) {
        if (checkNeedRest())
            return null;
        return state.onMessage(address, command, args);
    }

    private boolean checkNeedRest() {
        return new Judge().checkNeedRest(this::info, state instanceof LeaderState ? "(leadership was to exhaustive)" : "");
    }

    private void sendToSelf(String command, Object... args) {
        super.sendByLoopback(properties.getAddress(getOrdinal()), command, args);
    }

    private void reReceive(InetSocketAddress address, String command, Object... args) {
        super.sendByLoopback(address, command, args);
    }

    private void convertState(State newState) {
        state.close();
        state = newState;
    }

    @Override
    public void close() {
        super.close();
        state.close();
        persistent.close();
    }

    // ---------------------- States ----------------------

    private abstract class State {
        public State() {
            info("$ New state: " + toString());
        }

        public abstract CommandMessage onMessage(InetSocketAddress address, String command, Object... args);

        public abstract String toString();

        public abstract void close();

        protected final CommandMessage onVoteRequest(InetSocketAddress address, Object... args) {
            int newTerm = (int) args[0];
            int candidateId = (int) args[1];
            int candidateLogSize = (int) args[2];
            int lastLogTerm = (int) args[3];

            boolean ok = false;
            if ((lastLogTerm > persistent.getLastLog().getTermNumber() || lastLogTerm == persistent.getLastLog().getTermNumber() && candidateLogSize >= persistent.logSize())      // up to date
                    && (newTerm > persistent.getCurrentTerm() || newTerm == persistent.getCurrentTerm() && (persistent.getVotedFor() == null || persistent.getVotedFor() == candidateId))) {   // first in term
                persistent.setCurrentTerm(newTerm);
                persistent.setVotedFor(candidateId);
                ok = true;
                info("! Voted for " + candidateId + " in term " + newTerm);
            }

            return new CommandMessage(REQUEST_VOTE_RESPONSE, persistent.getCurrentTerm(), ok).setReceiverName(createIdentifier.apply(candidateId));
        }

        protected final void onApplyOperationRequest(Operation op, Integer leaderId) {
            if (op.getAuthor() != null)
                send(op.getAuthor().toInetSocketAdress(), APPLY_OPERATION_RESPONSE, leaderId, op);
        }
    }

    // ---------------------- Leader ----------------------

    private class LeaderState extends State {

        private final Mediator[] mediators = new Mediator[properties.getNodesNum() + 1];

//        private final int[] matchIndex = new int[properties.getNodesNum() + 1];

        /**
         * log num -> ok response num, only for logs in this term
         */
        private final TreeMap<Integer, Integer> okResponseNum = new TreeMap<>();

        public LeaderState() {
            new Judge().registerLeader(persistent.getCurrentTerm(), getOrdinal());
            for (int i = 1; i < mediators.length; i++)
                if (i != getOrdinal())
                    mediators[i] = new Mediator(i);
//            Arrays.fill(matchIndex, 1);
            persistent.putLog(new Operation(new OperationId(-1, -1), null, Operation.DELETE, PersistentState.RESERVED_KEY));
        }

        @Override
        public CommandMessage onMessage(InetSocketAddress address, String command, Object... args) {
            Runnable unexpectedCommand = () -> error(String.format("Unexpected command received in LEADER state: %s", command));
            switch (command) {
                case APPEND_ENTRIES: {
                    int term = (int) args[0];
                    int leaderId = (int) args[1];
                    if (term > persistent.getCurrentTerm()) {
                        convertState(new FollowerState(leaderId));
                        reReceive(address, command, args);
                    }
                    break;
                }
                case APPEND_ENTRIES_RESPONSE: {
                    int term = (int) args[0];
                    if (term > persistent.getCurrentTerm()) {
                        convertState(new FollowerState());
                        break;
                    }
                    int success = (int) args[1];
                    int followerId = (int) args[2];
                    int prevLogIndex = (int) args[3];
                    mediators[followerId].onResponse(success, prevLogIndex);
                    break;
                }
                case REQUEST_VOTE: {
                    int term = (int) args[0];
                    boolean displaced = term > persistent.getCurrentTerm();
                    CommandMessage response = onVoteRequest(address, args);
                    if (displaced) {
                        convertState(new FollowerState());
                    }
                    return response;
                }
                case REQUEST_VOTE_RESPONSE:
                    // ignore
                    break;

                // reminds
                case REMIND_LEADER_HEARTBEAT: {
                    if (persistent.getCurrentTerm() == (int) (args[0])) {
                        mediators[((int) args[1])].talk();
                    }
                    break;
                }
                case REMIND_LEADER_TIMEOUT: {
                    if (persistent.getCurrentTerm() == (int) (args[0]))
                        unexpectedCommand.run();
                    break;
                }
                case REMIND_ELECTION_TIMEOUT: {
                    // ignore
                    break;
                }
                case REMIND_CLIENT_RESPONSE:
                    onApplyOperationRequest((Operation) args[0], getOrdinal());
                    break;

                // client
                case APPLY_OPERATION:
                    Operation op = (Operation) args[0];
                    // check uniqueness
//                    boolean repetition = false;
//                    for (int i = 0; i < persistent.logSize(); i++) {
//                        if (persistent.getLog(i).getOperation().equals(op)) {
//                            repetition = true;
//                            op = persistent.getLog(i).getOperation();
//                        }
//                    }
//                    if (!repetition) {
                    op.setAuthor(new WriteableAdress(address));
                    persistent.putLog(op);
//                    }
                    // process message
//                    op.setResult(new OperationResult("Lol"));
//                    return new CommandMessage(RaftMessageFormatter.APPLY_OPERATION_RESPONSE, getOrdinal(), op);
                    break;
                default:
                    unexpectedCommand.run();
            }

            return null;
        }


        @Override
        public String toString() {
            return "Leader";
        }

        @Override
        public void close() {
            for (Mediator mediator : mediators) {
                if (mediator != null) mediator.close();
            }
        }

        private void incrementRepliedNum(int logIndex) {
            if (persistent.getLog(logIndex).getTermNumber() != persistent.getCurrentTerm())
                return;
            if (logIndex < persistent.getCommittedNum())
                return;

            Integer totalReceivedNum = okResponseNum.compute(logIndex, (key, value) -> (value == null ? 0 : value) + 1);
            info(String.format("! Log #%d received by %d followers", logIndex, totalReceivedNum));
            if (totalReceivedNum == properties.getNodesNum() / 2) {
                info(String.format("! Commiting log #%d", logIndex));
                persistent.setCommittedNum(logIndex + 1);
                okResponseNum.subMap(0, true, logIndex, true).clear();
            }
        }

        private class Mediator {
            private final int nodeNum;

            private int nextIndex = persistent.logSize();

            private final Reminder heartbeat;

            private boolean sync = false;

            public Mediator(int nodeNum) {
                this.nodeNum = nodeNum;
                heartbeat = new Reminder(properties.getTimeout() / 2, () -> sendToSelf(REMIND_LEADER_HEARTBEAT, persistent.getCurrentTerm(), nodeNum)).restart();
                sendAppendEntries();
            }

            private void sendAppendEntries() {
                sendTo(properties.getAddress(nodeNum), createIdentifier.apply(nodeNum), APPEND_ENTRIES,
                        persistent.getCurrentTerm(), getOrdinal(), nextIndex - 1, persistent.getLog(nextIndex - 1).getTermNumber(),
                        nextIndex >= persistent.logSize() ? null : persistent.getLog(nextIndex), persistent.getCommittedNum());
            }

            private void talk() {
                heartbeat.restart();
                sendAppendEntries();
            }

            public void onResponse(int success, int prevLogIndex) {
                if (success == -1)
                    return;
                else if (success == 1) {
                    sync = true;
                } else {
                    if (sync)
                        error(String.format("Has synchronized with %d, but been responded 'fail'", nodeNum));
                }

                if (prevLogIndex + 1 == nextIndex) {
                    if (sync) {
                        incrementRepliedNum(nextIndex);
                        nextIndex = Math.min(nextIndex + 1, persistent.logSize());
                    } else {
                        nextIndex = Math.max(nextIndex - 1, 1);
                    }
                }
                if (nextIndex < persistent.logSize()) {
                    talk();
                }
            }

            public void close() {
                heartbeat.destroy();
            }
        }
    }

    // ---------------------- Follower ----------------------

    private class FollowerState extends State {
        private final Reminder leaderWaitingReminder = new Reminder(properties.getTimeout(), () -> sendToSelf(REMIND_LEADER_TIMEOUT, persistent.getCurrentTerm())).restart();

        private Integer lastKnownLeader;

        public FollowerState() {
        }

        public FollowerState(int knownLeaderId) {
            lastKnownLeader = knownLeaderId;
        }

        @Override
        public CommandMessage onMessage(InetSocketAddress address, String command, Object... args) {
            Runnable unexpectedCommand = () -> error(String.format("Unexpected command received in FOLLOWER state: %s", command));
            switch (command) {
                case APPEND_ENTRIES:
                    int term = (int) args[0];
                    int leaderId = (int) args[1];
                    if (term < persistent.getCurrentTerm())
                        break;

                    persistent.setCurrentTerm(term);
                    leaderWaitingReminder.restart();
                    lastKnownLeader = leaderId;

                    int prevLogIndex = (int) args[2];
                    int prevLogTerm = (int) args[3];
                    LogItem item = (LogItem) args[4];
                    int commitIndex = (int) args[5];
                    int ok = item == null ? -1 : persistent.logSize() > prevLogIndex && persistent.getLog(prevLogIndex).getTermNumber() == prevLogTerm ? 1 : 0;
                    if (ok == 1) {
                        if (persistent.logSize() > item.getIndex() && persistent.getLog(item.getIndex()).getTermNumber() == item.getTermNumber()) {
                            // have already received this log, don't enforce
                        } else {
                            if (!persistent.enforceLog(item)) {
                                error(String.format("Attempt to overwrite committed log. Committed %d logs, overwrote since %d", persistent.getCommittedNum(), item.getIndex()));
                            }
                            persistent.setCommittedNum(commitIndex);
                        }
                    }

                    return new CommandMessage(APPEND_ENTRIES_RESPONSE, persistent.getCurrentTerm(), ok, getOrdinal(), prevLogIndex)
                            .setReceiverName(createIdentifier.apply((int) args[1]));
                case APPEND_ENTRIES_RESPONSE:
                    // ignore
                    break;
                case REQUEST_VOTE:
                    return onVoteRequest(address, args);
                case REQUEST_VOTE_RESPONSE:
                    // ignore
                    break;

                // reminds
                case REMIND_LEADER_HEARTBEAT: {
                    // ignore
                    break;
                }
                case REMIND_LEADER_TIMEOUT: {
                    if (persistent.getCurrentTerm() == (int) (args[0])) {
                        if (System.currentTimeMillis() - lastSubmissionTime < submissionDuration.apply(properties.getTimeout())) {
                            info("! No viable leader, but 'submission' effect is still applied");
                        } else {
                            info("! No viable leader");
                            convertState(new CandidateState());
                        }
                    }
                    break;
                }
                case REMIND_ELECTION_TIMEOUT: {
                    // ignore
                    break;
                }
                case REMIND_CLIENT_RESPONSE:
                    respondToClient((Operation) args[0]);
                    break;

                //client
                case APPLY_OPERATION:
                    Operation op = (Operation) args[0];
                    op.setAuthor(new WriteableAdress(address));
                    respondToClient(op);
                    break;

                default:
                    unexpectedCommand.run();
            }
            return null;
        }

        private void respondToClient(Operation arg) {
            onApplyOperationRequest(arg, lastKnownLeader);
        }

        @Override
        public String toString() {
            return "Follower";
        }

        @Override
        public void close() {
            leaderWaitingReminder.destroy();
        }
    }

    // ---------------------- Candidate ----------------------

    private class CandidateState extends State {
        private final Reminder electionReminder = new Reminder(properties.getTimeout(), () -> sendToSelf(REMIND_ELECTION_TIMEOUT, persistent.getCurrentTerm()));

        private int receivedVotesNumber;

        private int initiatedElectionNumber = 0; // just for fun

        public CandidateState() {
            initElection();
        }

        private void initElection() {
            persistent.setCurrentTerm(persistent.getCurrentTerm() + 1);
            persistent.setVotedFor(getOrdinal());
            receivedVotesNumber = 1;
            electionReminder.restart();
            initiatedElectionNumber++;
            info(String.format("! Initiating election #%d", persistent.getCurrentTerm()));
            if (initiatedElectionNumber == 6)
                info("!! Panic, to much elections");
            for (InetSocketAddress address : properties.getNodesIter()) {
                if (address.equals(properties.getAddress(getOrdinal())))
                    continue;
                sendTo(address, createIdentifier.apply(properties.getNodeNum(address)), REQUEST_VOTE,
                        persistent.getCurrentTerm(), getOrdinal(), persistent.logSize(), persistent.getLastLog().getTermNumber());
            }
        }

        @Override
        public CommandMessage onMessage(InetSocketAddress address, String command, Object... args) {
            Runnable unexpectedCommand = () -> error(String.format("Unexpected command received in CANDIDATE state: %s", command));
            switch (command) {
                case APPEND_ENTRIES: {
                    int term = (int) args[0];
                    int leaderId = (int) args[1];
                    if (term >= persistent.getCurrentTerm()) {
                        info(String.format("! Active leader found (term %d), stoping election", persistent.getCurrentTerm()));
                        convertState(new FollowerState(leaderId));
                        reReceive(address, command, args);
                    } else {
                        return new CommandMessage(APPEND_ENTRIES_RESPONSE, persistent.getCurrentTerm(), 0, getOrdinal(), args[2]).setReceiverName(createIdentifier.apply((int) args[1]));
                    }
                }
                case APPEND_ENTRIES_RESPONSE:
                    // ignore
                    break;
                case REQUEST_VOTE: {
                    int term = (int) args[0];
                    int candidateId = (int) args[1];
                    if (term == persistent.getCurrentTerm() && candidateId > getOrdinal()) {
                        info(String.format("! Candidate with higher id (%d) found (term %d), stopping election for this node", candidateId, persistent.getCurrentTerm()));
                        lastSubmissionTime = System.currentTimeMillis();
                        convertState(new FollowerState(candidateId));
                        return new CommandMessage(REQUEST_VOTE_RESPONSE, persistent.getCurrentTerm(), false).setReceiverName(createIdentifier.apply(candidateId))
                                .setReceiverName(createIdentifier.apply(candidateId));
                    }
                    return onVoteRequest(address, args);
                }
                case REQUEST_VOTE_RESPONSE: {
                    boolean granted = (boolean) args[1];
                    int receiversTerm = (int) args[0];
                    if (granted && receiversTerm == persistent.getCurrentTerm()) {
                        receivedVotesNumber++;
                        info("! Collected " + receivedVotesNumber + " votes in term " + persistent.getCurrentTerm());
                        if (receivedVotesNumber * 2 > properties.getNodesNum()) {
                            convertState(new LeaderState());
                        }
                    }
                    if (receiversTerm > persistent.getCurrentTerm()) {
                        persistent.setCurrentTerm(receiversTerm);
                        convertState(new FollowerState());
                    }
                    break;
                }

                //reminds
                case REMIND_LEADER_HEARTBEAT: {
                    if (persistent.getCurrentTerm() == (int) (args[0])) {
                        unexpectedCommand.run();
                    }
                    break;
                }
                case REMIND_LEADER_TIMEOUT: {
                    if (persistent.getCurrentTerm() == (int) (args[0])) {
                        unexpectedCommand.run();
                    }
                    break;
                }
                case REMIND_ELECTION_TIMEOUT: {
                    int term = (int) (args[0]);
                    if (persistent.getCurrentTerm() == term)
                        initElection();
                    break;
                }
                case REMIND_CLIENT_RESPONSE:
                    respondToClient((Operation) args[0]);
                    break;

                //client
                case APPLY_OPERATION:
                    Operation op = (Operation) args[0];
                    op.setAuthor(new WriteableAdress(address));
                    respondToClient(op);
                    break;

                default:
                    unexpectedCommand.run();
            }
            return null;
        }

        private void respondToClient(Operation arg) {
            onApplyOperationRequest(arg, -1);
        }

        @Override
        public String toString() {
            return "Candidate";
        }

        @Override
        public void close() {
            electionReminder.destroy();
        }
    }
}
