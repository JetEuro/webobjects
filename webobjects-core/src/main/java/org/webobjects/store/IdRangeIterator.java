package org.webobjects.store;

import java.util.Iterator;

/**
 * User: cap_protect
 * Date: 5/9/12
 * Time: 1:18 PM
 */
public interface IdRangeIterator extends Iterator<Long> {
    Long peek();

    IdRangeIterator addFilter(String ...pathes);
}
