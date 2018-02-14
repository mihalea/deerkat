package ro.mihalea.deerkat;

import org.junit.Before;
import org.junit.Test;
import ro.mihalea.deerkat.model.Transaction;
import ro.mihalea.deerkat.repository.CategorySqlRepository;
import ro.mihalea.deerkat.repository.CsvRepository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ClassifierAccuracyAnalyser {
    List<Transaction> trainingData;
    CsvRepository transactionRepository;
    CategorySqlRepository categoryRepository;

    @Before
    public void setUp() throws Exception {
        String db_location = ClassifierAccuracyAnalyser.class.getClassLoader().getResource("deerkat.sqlite").getPath();
        categoryRepository = new CategorySqlRepository(db_location);

        String csv_location = ClassifierAccuracyAnalyser.class.getClassLoader().getResource("model_data.csv").getPath();
        Path path = Paths.get(csv_location);
        transactionRepository = new CsvRepository(path, false);
        transactionRepository.setCategoryRepository(categoryRepository);

        trainingData = transactionRepository.getAll();


    }

    @Test
    public void randomSampling() {

    }
}