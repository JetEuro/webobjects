package distributed;

/**
 * User: cap_protect
 * Date: 5/9/12
 * Time: 9:31 AM
 */
public class NodeState implements Comparable<NodeState> {
    private final String name;
    private final boolean me;
    private final int id;

    NodeState(String name, boolean me, int id) {
        this.name = name;
        this.me = me;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public boolean isMe() {
        return me;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "NodeState{" +
                "name='" + name + '\'' +
                ", me=" + me +
                ", id=" + id +
                '}';
    }

    public int compareTo(NodeState o) {
        return name.compareTo(o.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodeState nodeState = (NodeState) o;

        if (!name.equals(nodeState.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
