import java.security.SecureRandom;
import java.util.Random;

/**
 * User: captain-protect
 * Date: 5/4/12
 * Time: 5:25 PM
 */
public class WebReference {
    private static final Random ID_RANDOM = new SecureRandom();

    private final long id;

    WebReference(long id) {
        this.id = id;
    }

    public WebReference() {
        id = ID_RANDOM.nextLong();
    }

    long getId() {
        return id;
    }
}
