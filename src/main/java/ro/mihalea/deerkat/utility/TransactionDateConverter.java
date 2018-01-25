package ro.mihalea.deerkat.utility;

import ro.mihalea.deerkat.model.Transaction;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TransactionDateConverter {
    /**
     * Date format used when processing transaction and posting dates.
     * However, while the {@link java.util.Formatter} describes %B as "Locale-specific full month name",
     * {@link SimpleDateFormat} uses %M
     *
     * @see Transaction#transactionDate
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final String DATE_FORMAT = "MMMM d, yyyy";


    public LocalDate fromString(String date) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        return LocalDate.parse(date, dateFormatter);
    }

    public String toString(LocalDate date) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        return date.format(dateFormatter);
    }

    public LocalDate fromSQL(Date date) {
        return LocalDate.ofEpochDay(date.getTime());
    }

    public Date toSQL(LocalDate date) {
        return new Date(date.toEpochDay());
    }
}
