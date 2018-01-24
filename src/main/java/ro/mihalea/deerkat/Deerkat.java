package ro.mihalea.deerkat;

import ro.mihalea.deerkat.exception.*;
import ro.mihalea.deerkat.utility.HtmlProcessor;

public class Deerkat {
    public static void main(String[] args) {
        HtmlProcessor processor = new HtmlProcessor();
        try {
            processor.getTransactions("/home/mircea/Documents/hsbc.html").forEach(System.out::println);
        } catch (FileNotFoundException | FileNotReadableException | TransactionParseException | FileReadingErrorException | TransactionFieldException e) {
            e.printStackTrace();
        }
    }
}
