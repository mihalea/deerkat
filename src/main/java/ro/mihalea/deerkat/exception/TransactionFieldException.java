package ro.mihalea.deerkat.exception;

/**
 * Exception thrown when the statement in HTML format contains an unknown field or an erroneous number of fields
 */
public class TransactionFieldException extends Exception {
    public TransactionFieldException(String s) {
        super(s);
    }
}
