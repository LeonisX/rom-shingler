package md.leonis.shingler.gui.dto;

public enum NodeStatus {

    FAMILY, MEMBER, ORPHAN, FAMILY_LIST;

    public static boolean isFamily(NodeStatus status) {
        return status == FAMILY || status == FAMILY_LIST;
    }

    public static boolean isMember(NodeStatus status) {
        return status == MEMBER || status == ORPHAN;
    }
}
