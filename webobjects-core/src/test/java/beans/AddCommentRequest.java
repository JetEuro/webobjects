package beans;

import beans.User;

/**
* User: cap_protect
* Date: 5/8/12
* Time: 2:00 AM
*/
public interface AddCommentRequest extends Request {
    User getUser();

    String getText();

    void setText(String text);
}
