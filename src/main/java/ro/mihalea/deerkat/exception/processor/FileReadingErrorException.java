package ro.mihalea.deerkat.exception.processor;

/**
 * Exception thrown when the processor can't read the content of the HTML file
 */
public class FileReadingErrorException extends Exception {
    public FileReadingErrorException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
