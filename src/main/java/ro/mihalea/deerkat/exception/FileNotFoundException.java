package ro.mihalea.deerkat.exception;

/**
 * Exception thrown when an HTML file could not be found on disk
 */
public class FileNotFoundException  extends Exception{
    public FileNotFoundException(String s) {
        super(s);
    }
}
