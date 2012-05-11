package org.webobjects.utils;

/**
 * User: cap_protect
 * Date: 5/9/12
 * Time: 11:18 AM
 */
public abstract class BasicTypeParser {
    public static BasicTypeParser create(Class primitiveType) {
        if (Long.class.equals(primitiveType)
                || long.class.equals(primitiveType)) {
            return new BasicTypeParser() {
                @Override
                public Object parse(String string) {
                    return Long.parseLong(string);
                }
            };
        } else if (String.class.equals(primitiveType)) {
            return new BasicTypeParser() {
                @Override
                public Object parse(String string) {
                    return string;
                }
            };
        }
        throw new UnsupportedOperationException("primitive type " + primitiveType + " is not supported by StringCaster");
    }

    public abstract Object parse(String string);

    public String toString(Object key) {
        return key.toString();
    }
}
