package beans;

import registry.RegistryBean;
import registry.RegistryListType;

import java.util.List;

/**
* User: cap_protect
* Date: 5/8/12
* Time: 1:58 AM
*/
@RegistryListType(ImageTreeNode.class)
public interface ImageTreeNode extends List<ImageTreeNode>, RegistryBean {
    Image getImage();
}
