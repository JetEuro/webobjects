/**
 * User: captain-protect
 * Date: 5/4/12
 * Time: 5:15 PM
 */
public class WebObject {
    private ObjectsContainer container;

    private WebReference self;

    WebObject(ObjectsContainer container, WebReference self) {
        this.container = container;
        this.self = self;
    }

    public boolean readBoolean(String field) {
        return container.getStorer().readBoolean(self, field);
    }

    public void writeBoolean(String field, boolean value) {
        container.getStorer().writeBoolean(self, field, value);
    }

    public short readShort(String field) {
        return container.getStorer().readShort(self, field);
    }

    public void writeShort(String field, short value) {
        container.getStorer().writeShort(self, field, value);
    }

    public byte readByte(String field) {
        return container.getStorer().readByte(self, field);
    }

    public void writeByte(String field, byte value) {
        container.getStorer().writeByte(self, field, value);
    }

    public int readInt(String field) {
        return container.getStorer().readInt(self, field);
    }

    public void writeInt(String field, int value) {
        container.getStorer().writeInt(self, field, value);
    }

    public long readLong(String field) {
        return container.getStorer().readLong(self, field);
    }

    public void writeLong(String field, long value) {
        container.getStorer().writeLong(self, field, value);
    }

    public WebReference readReference(String field) {
        return container.getStorer().readReference(self, field);
    }

    public void writeReference(String field, WebReference reference) {
        container.getStorer().writeReference(self, field);
    }

    public WebObject dereference(String field) {
        WebReference ref = readReference(field);
        return container.getDereferncer().dereference(self, ref);
    }


    public boolean[] readBooleanArray(String field) {
        return container.getStorer().readBooleanArray(self, field);
    }

    public void writeBooleanArray(String field, boolean []value) {
        container.getStorer().writeBooleanArray(self, field, value);
    }

    public short[] readShortArray(String field) {
        return container.getStorer().readShortArray(self, field);
    }

    public void writeShortArray(String field, short []value) {
        container.getStorer().writeShortArray(self, field, value);
    }

    public byte[] readByteArray(String field) {
        return container.getStorer().readByteArray(self, field);
    }

    public void writeByteArray(String field, byte []value) {
        container.getStorer().writeByteArray(self, field, value);
    }

    public int[] readIntArray(String field) {
        return container.getStorer().readIntArray(self, field);
    }

    public void writeIntArray(String field, int []value) {
        container.getStorer().writeIntArray(self, field, value);
    }

    public long[] readLongArray(String field) {
        return container.getStorer().readLongArray(self, field);
    }

    public void writeLongArray(String field, long []value) {
        container.getStorer().writeLongArray(self, field, value);
    }

    public WebReference[] readReferenceArray(String field) {
        return container.getStorer().readReferenceArray(self, field);
    }

    public void writeReferenceArray(String field, WebReference []reference) {
        container.getStorer().writeReferenceArray(self, field);
    }
}
