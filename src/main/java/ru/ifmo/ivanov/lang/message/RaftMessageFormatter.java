package ru.ifmo.ivanov.lang.message;

import ru.ifmo.ivanov.lang.misc.LogItem;
import ru.ifmo.ivanov.lang.misc.Operation;

public class RaftMessageFormatter extends MessageFormatter {
    public static final String APPEND_ENTRIES = "AppendEntries";
    public static final String APPEND_ENTRIES_RESPONSE = "@/AppendEntries";
    public static final String REQUEST_VOTE = "RequestVote";
    public static final String REQUEST_VOTE_RESPONSE = "@/RequestVote";

    public static final String REMIND_LEADER_TIMEOUT = "&LeaderTimeout";
    public static final String REMIND_LEADER_HEARTBEAT = "&LeaderHeartbeat";
    public static final String REMIND_ELECTION_TIMEOUT = "&ElectionTimeout";
    public static final String REMIND_CLIENT_RESPONSE = "&ClientResponse";
    public static final String REMIND_REQUESTED_APPLYING  = "&RequestedApplying";

    public static final String APPLY_OPERATION = "ApplyOperation";
    public static final String APPLY_OPERATION_RESPONSE = "@/ApplyOperation";

    public RaftMessageFormatter(boolean pretty) {
        super(pretty);
        addFormat(new Format(APPEND_ENTRIES, Integer.class /*term*/, Integer.class /*leader id*/, Integer.class /*prevLogIndex*/, Integer.class /*prevLogTerm*/,
                LogItem.class /*entry*/, Integer.class /*leaderCommitIndex*/));  // when heartbeat, entry == null
        addFormat(new Format(APPEND_ENTRIES_RESPONSE, Integer.class /*term*/, Integer.class /*success*/, Integer.class /*follower id*/, Integer.class /*prevLogIndex*/)); // success == -1 when empty request
        addFormat(new Format(REQUEST_VOTE, Integer.class /*candidate's term*/, Integer.class /*candidate id*/, Integer.class/*logSize*/, Integer.class/*lastLogTerm*/));
        addFormat(new Format(REQUEST_VOTE_RESPONSE, Integer.class /*receiver's term*/, Boolean.class/*vote granted*/));

        addFormat(new Format(REMIND_LEADER_TIMEOUT, Integer.class /*term*/));
        addFormat(new Format(REMIND_LEADER_HEARTBEAT, Integer.class /*term*/, Integer.class /*to whom*/));
        addFormat(new Format(REMIND_ELECTION_TIMEOUT, Integer.class /*term*/));
        addFormat(new Format(REMIND_CLIENT_RESPONSE, Operation.class /*operation*/));
        addFormat(new Format(REMIND_REQUESTED_APPLYING, Operation.class /*operation*/));

        addFormat(new Format(APPLY_OPERATION, Operation.class /*operation*/));
        addFormat(new Format(APPLY_OPERATION_RESPONSE, Integer.class /*possible leader id, -1 if not known*/, Operation.class /*operation with result*/));
            // if fail, args[0] != node number which received message, and operation.result is set to null
    }

    public RaftMessageFormatter() {
        this(false);
    }
}
