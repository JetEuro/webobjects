package org.webobjects.store;

/**
 * User: cap_protect
 * Date: 5/9/12
 * Time: 1:19 PM
 */
public interface IdRangeIterable {
    IdRangeIterator getIdRangeSelector(Long start, Long end);
}
