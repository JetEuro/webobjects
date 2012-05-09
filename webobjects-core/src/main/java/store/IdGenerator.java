package store;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: cap_protect
 * Date: 5/7/12
 * Time: 8:39 PM
 */
public abstract class IdGenerator {
    public enum Type {
        SEQUENTIAL, SECURE_RANDOM, RANDOM
    }

    interface SeedGettable {
        long getSeed();
    }

    public abstract long newId();

    public static IdGenerator getGenerator(Type type, Long seed) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        switch (type) {
            case SECURE_RANDOM:
                return new SecureRandomIdGenerator(seed);
            case SEQUENTIAL:
                return new SequentialIdGenerator(seed);
            case RANDOM:
                return new RandomIdGenerator(seed);
            default:
                throw new UnsupportedOperationException("failed to create IdGenerator for type " + type);
        }
    }

    public static IdGenerator getGenerator(Type type) {
        return getGenerator(type, null);
    }

    private static class SecureRandomIdGenerator extends IdGenerator {
        private SecureRandom srnd;

        public SecureRandomIdGenerator(Long seed) {
            if (seed == null) {
                srnd = new SecureRandom();
            } else {
                byte[] seedArray = ByteBuffer.allocate(8).putLong(seed).array();
                srnd = new SecureRandom(seedArray);
            }
        }

        @Override
        public long newId() {
            return srnd.nextLong();
        }
    }

    private static class SequentialIdGenerator extends IdGenerator implements SeedGettable {
        private AtomicLong id;

        public SequentialIdGenerator(Long seed) {
            if (seed == null) {
                seed = 0L;
            }
            id = new AtomicLong(seed);
        }

        @Override
        public long newId() {
            return id.incrementAndGet();
        }

        public long getSeed() {
            return id.get();
        }
    }

    private static class RandomIdGenerator extends IdGenerator {
        private Random rnd;

        public RandomIdGenerator(Long seed) {
            if (seed == null) {
                rnd = new Random();
            } else {
                rnd = new Random(seed);
            }
        }

        @Override
        public long newId() {
            return rnd.nextLong();
        }
    }
}
