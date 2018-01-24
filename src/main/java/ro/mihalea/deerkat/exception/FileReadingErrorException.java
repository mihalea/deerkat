package ro.mihalea.deerkat.exception;

public class FileReadingErrorException extends Exception {
    public FileReadingErrorException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
