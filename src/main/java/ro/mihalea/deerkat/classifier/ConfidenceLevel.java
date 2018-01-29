package ro.mihalea.deerkat.classifier;

/**
 * Confidence level of the categories used when drawing the UI
 */
public enum ConfidenceLevel {
    /**
     * No category set
     */
    NONE,
    /**
     * Probability is above {@link AbstractClassifier#NEED_CONFIRMATION_VALUE} and needs user confirmation
     */
    NEED_CONFIRMATION,
    /**
     * Probability is above {@link AbstractClassifier#AUTOMATIC_MATCH_VALUE} and doesn't need any user intervention
     */
    PRETTY_SURE,
    /**
     * The category has been set by the user and is 100% correct
     */
    USER_SET
}
