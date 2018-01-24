package ro.mihalea.deerkat.exception;

/**
 * Exception thrown when an HTML file could not be read
 */
public class FileNotReadableException extends Exception{
    public FileNotReadableException(String s) {
        super(s);
    }
}
