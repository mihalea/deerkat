package ro.mihalea.deerkat.utility;

import ro.mihalea.deerkat.model.Transaction;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TransactionDateConverter {
    public LocalDate fromString(String date, String format) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(format);
        return LocalDate.parse(date, dateFormatter);
    }

    public String toString(LocalDate date, String format) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(format);
        return date.format(dateFormatter);
    }

    public LocalDate fromSQL(Date date) {
        return LocalDate.ofEpochDay(date.getTime());
    }

    public Date toSQL(LocalDate date) {
        return new Date(date.toEpochDay());
    }
}
