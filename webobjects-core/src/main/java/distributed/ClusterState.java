package distributed;

import java.util.*;

/**
 * User: cap_protect
 * Date: 5/9/12
 * Time: 9:31 AM
 */
public class ClusterState {
    private final SortedSet<NodeState> states;
    private final NodeState me;

    public ClusterState(String myNodeName, Set<String> names) {
        TreeSet set = new TreeSet();

        NodeState foundMe = null;
        int id = 0;
        for (String name : new TreeSet<String>(names)) {
            boolean isMe = myNodeName.equals(name);
            NodeState state = new NodeState(name, isMe, id++);
            if (isMe) {
                if (foundMe != null) {
                    throw new IllegalArgumentException("only one NodeState with me=true allowed");
                } else {
                    foundMe = state;
                }
            }
            set.add(state);
        }
        if (foundMe == null) {
            throw new IllegalArgumentException("one NodeState with me=true should be in cluster");
        }
        me = foundMe;
        states = Collections.unmodifiableSortedSet(set);
    }

    public NodeState getMe() {
        return me;
    }

    public SortedSet<NodeState> getStates() {
        return states;
    }

    @Override
    public String toString() {
        return "ClusterState{" +
                "states=" + states +
                ", me=" + me +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClusterState that = (ClusterState) o;

        if (!me.equals(that.me)) return false;
        if (!states.equals(that.states)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = states.hashCode();
        result = 31 * result + me.hashCode();
        return result;
    }
}
