package ro.mihalea.deerkat;

import lombok.extern.log4j.Log4j2;
import ro.mihalea.deerkat.exception.TransactionFieldException;
import ro.mihalea.deerkat.exception.TransactionParseException;
import ro.mihalea.deerkat.exception.processor.FileNotFoundException;
import ro.mihalea.deerkat.exception.processor.FileNotReadableException;
import ro.mihalea.deerkat.exception.processor.FileReadingErrorException;
import ro.mihalea.deerkat.exception.repository.RepositoryCreateException;
import ro.mihalea.deerkat.exception.repository.RepositoryInitialisationException;
import ro.mihalea.deerkat.model.Transaction;
import ro.mihalea.deerkat.repository.CsvRepository;
import ro.mihalea.deerkat.repository.IRepository;
import ro.mihalea.deerkat.utility.HtmlProcessor;

import java.util.List;

@Log4j2
public class Deerkat {
    public static void main(String[] args) {
        try {
            HtmlProcessor processor = new HtmlProcessor();
            IRepository<Transaction, Integer> repository = new CsvRepository("deerkat.csv");
            List<Transaction> transactionList = processor.getTransactions("/home/mircea/Documents/hsbc.html");
            repository.addAll(transactionList);

        } catch (RepositoryInitialisationException | RepositoryCreateException | FileReadingErrorException |
                FileNotFoundException | TransactionFieldException | FileNotReadableException |
                TransactionParseException e) {
            e.printStackTrace();
        }
    }
}
