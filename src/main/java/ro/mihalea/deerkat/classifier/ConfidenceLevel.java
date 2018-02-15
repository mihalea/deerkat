package ro.mihalea.deerkat.classifier;

/**
 * Confidence level of the categories used when drawing the UI
 */
public class ConfidenceLevel {
    /**
     * No category set
     */
    public static int NONE = 0;
    /**
     * Probability is above a certain threshold and needs user confirmation
     */
    public static int NEED_CONFIRMATION = 70;
    /**
     * Probability is above a certain threshold and doesn't need any user intervention
     */
    public static int PRETTY_SURE = 95;
    /**
     * The category has been set by the user and is 100% correct
     */
    public static int USER_SET = 101;
}
