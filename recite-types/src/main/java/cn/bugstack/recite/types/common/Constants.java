package cn.bugstack.recite.types.common;

public final class Constants {
    private Constants() {}

    public static final int EMBEDDING_DIM = 1024;
    public static final String PG_COLLECTION = "question_vectors";
    public static final String SESSION_PREFIX = "recite:session:";
    public static final String SCORE_SEMAPHORE = "recite:score:slots";
    public static final int SESSION_TTL_HOURS = 2;
    public static final int MAX_SCORE_CONCURRENT = 10;
    public static final int MAX_ANSWER_LENGTH = 5000;
    public static final int FOLLOW_UP_MAX_DEPTH = 3;
}
