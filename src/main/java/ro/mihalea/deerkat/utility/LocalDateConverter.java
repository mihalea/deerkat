package ro.mihalea.deerkat.utility;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateConverter {
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
