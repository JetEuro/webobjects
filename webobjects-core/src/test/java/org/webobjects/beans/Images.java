package org.webobjects.beans;

import org.webobjects.registry.RegistryBean;
import org.webobjects.registry.IndexedRegistryListType;

import java.util.List;

/**
* User: cap_protect
* Date: 5/8/12
* Time: 1:58 AM
*/
@IndexedRegistryListType(Image.class)
public interface Images extends RegistryBean, List<Image> {
}
