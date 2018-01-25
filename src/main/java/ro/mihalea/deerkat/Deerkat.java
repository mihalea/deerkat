package ro.mihalea.deerkat;

import lombok.extern.log4j.Log4j2;
import ro.mihalea.deerkat.exception.database.DatabaseConnectionException;
import ro.mihalea.deerkat.exception.database.DatabaseStatementException;
import ro.mihalea.deerkat.model.Transaction;
import ro.mihalea.deerkat.repository.TransactionRepository;

import java.time.LocalDate;

@Log4j2
public class Deerkat {
    public static void main(String[] args) {
        try {
            TransactionRepository repository = new TransactionRepository("deerkat.db");

            Transaction testTransaction = Transaction.builder()
                    .transactionDate(LocalDate.now())
                    .postingDate(LocalDate.now())
                    .details("FROM MIRCEA")
                    .amount(69d).build();
            repository.create(testTransaction);

            repository.getAll().forEach(System.out::println);


        } catch (DatabaseConnectionException | DatabaseStatementException e) {
            e.printStackTrace();
        }
    }
}
