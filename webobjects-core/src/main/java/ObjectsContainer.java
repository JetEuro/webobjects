/**
 * User: captain-protect
 * Date: 5/4/12
 * Time: 5:15 PM
 */
public class ObjectsContainer {
    private WebReference rootReference;
    private WebObject root = new WebObject(this, rootReference);

    public Storer getStorer() {
        return new CassandraStorer();
    }

    private WebObject getRoot() {
        return root;
    }

    public interface Storer {
        boolean readBoolean(WebReference self, String field);

        void writeBoolean(WebReference self, String field, boolean value);

        short readShort(WebReference self, String field);

        void writeShort(WebReference self, String field, short value);

        byte readByte(WebReference self, String field);

        void writeByte(WebReference self, String field, byte value);

        int readInt(WebReference self, String field);

        void writeInt(WebReference self, String field, int value);

        long readLong(WebReference self, String field);

        void writeLong(WebReference self, String field, long value);

        WebReference readReference(WebReference self, String field);

        void writeReference(WebReference self, String field);

        boolean[] readBooleanArray(WebReference self, String field);

        void writeBooleanArray(WebReference self, String field, boolean[] value);

        short[] readShortArray(WebReference self, String field);

        void writeShortArray(WebReference self, String field, short[] value);

        byte[] readByteArray(WebReference self, String field);

        void writeByteArray(WebReference self, String field, byte[] value);

        int[] readIntArray(WebReference self, String field);

        void writeIntArray(WebReference self, String field, int[] value);

        long[] readLongArray(WebReference self, String field);

        void writeLongArray(WebReference self, String field, long[] value);

        WebReference[] readReferenceArray(WebReference self, String field);

        void writeReferenceArray(WebReference self, String field);
    }

    public class Dereferencer {
        public WebObject dereference(WebReference ref, WebReference webReference) {
            return null;
        }
    }

    public Dereferencer getDereferncer() {
        return null;
    }

    private class CassandraStorer implements Storer {
    }
}
