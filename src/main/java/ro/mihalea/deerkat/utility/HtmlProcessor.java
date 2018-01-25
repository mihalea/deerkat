package ro.mihalea.deerkat.utility;

import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ro.mihalea.deerkat.exception.*;
import ro.mihalea.deerkat.exception.processor.FileNotFoundException;
import ro.mihalea.deerkat.exception.processor.FileNotReadableException;
import ro.mihalea.deerkat.exception.processor.FileReadingErrorException;
import ro.mihalea.deerkat.model.Transaction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HtmlProcessor is used to scrape HTML files and extract transactions from them
 */
@Log4j2
public class HtmlProcessor {
    /**
     * Field used to convert LocalDates to String when parsing
     */
    private final TransactionDateConverter converter = new TransactionDateConverter();

    /**
     * Open an HTML file and try to extract transactions from it by scraping the document, and return the resulting
     * Transaction objects
     *
     * @param file Path to an HTML file containing transactions obtained from HSBC UAE
     * @return A list of Transaction objects created by scraping the file
     */
    public List<Transaction> getTransactions(String file) throws FileNotFoundException, FileNotReadableException, FileReadingErrorException, TransactionParseException, TransactionFieldException {
        log.debug("getTransactions() method has been initiated");

        String html = this.readFile(file);
        return this.parseFile(html);
    }

    /**
     * Parse HTML content to extract transactions and return them in a list
     *
     * @param html HTML content
     * @return List of Transaction objects
     */
    private List<Transaction> parseFile(String html) throws TransactionParseException, TransactionFieldException {
        Document doc = Jsoup.parse(html);

        // Extract all table rows containing trasaction data
        Elements tableRows = doc.select("table.hsbcTableStyle07 tr.hsbcTableRow05");
        log.debug("Found {} transaction rows", tableRows.size());

        List<Transaction> transactions = new ArrayList<>();

        for (Element row : tableRows) {
            Elements dataElements = row.select("td");

            // Remove the column which usually holds the "Cr" string which is not needed
            dataElements.remove(dataElements.size() - 1);

            // Remove the second to last element which usually holds the "ARE" string
            dataElements.remove(dataElements.size() - 2);

            try {
                int columnIndex = 0;
                Transaction.TransactionBuilder builder = Transaction.builder();
                for (Element data : dataElements) {
                    switch (columnIndex) {
                        // Transaction date column
                        case 0:
                            builder.transactionDate(converter.fromString(data.text()));
                            break;
                        // Posting date column
                        case 1:
                            builder.postingDate(converter.fromString(data.text()));
                            break;
                        // Details column
                        case 2:
                            builder.details(data.text());
                            break;
                        // Amount column
                        case 3:
                            // Remove the thousand separator which may otherwise lead to errors when parsing to a double
                            String sanitisedNumber = data.text().replace(",", "");
                            builder.amount(Double.parseDouble(sanitisedNumber));
                            break;
                    }

                    columnIndex++;
                }

                if (columnIndex != 4) {
                    // There is an extra field in the row that has thrown off the counter
                    throw new TransactionFieldException("Unknown transaction field found in the table row. Found " +
                            columnIndex + " fields");
                }

                transactions.add(builder.build());
            } catch (DateTimeParseException e) {
                throw new TransactionParseException("Failed to parse the date format!", e);
            } catch (NumberFormatException e) {
                throw new TransactionParseException("Failed to parse the number column!", e);
            }
        }

        log.info("Parsed {} transactions", transactions.size());
        return transactions;
    }

    /**
     * Read the contents of the file into a string and return it
     *
     * @param file Path to an HTML file containing transactions obtained from HSBC UAE
     * @return Content of the HTML file
     */
    private String readFile(String file) throws FileNotFoundException, FileNotReadableException, FileReadingErrorException {
        Path path = Paths.get(file);

        if (Files.notExists(path)) {
            throw new FileNotFoundException("The requested file could not be opened: " + path.toAbsolutePath().toUri());
        }

        if (!Files.isReadable(path)) {
            throw new FileNotReadableException("The requested file could not be read: " + path.toAbsolutePath().toUri());
        }

        try {
            // Read all the lines from the file and return them
            return Files.readAllLines(path).stream()
                    .collect(Collectors.joining("\n", "", ""));
        } catch (IOException e) {
            throw new FileReadingErrorException("The requested HTML file could not be read successfully: "
                    + path.toAbsolutePath().toUri(), e);
        }
    }
}
