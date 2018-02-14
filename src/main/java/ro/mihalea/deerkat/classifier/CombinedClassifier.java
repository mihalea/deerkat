package ro.mihalea.deerkat.classifier;

import ro.mihalea.deerkat.model.Category;
import ro.mihalea.deerkat.model.Transaction;

import java.util.*;
import java.util.stream.Collectors;

public class CombinedClassifier extends AbstractClassifier {
    FuzzyClassifier fuzzy = new FuzzyClassifier();
    NaiveClassifier naive = new NaiveClassifier();

    @Override
    public void learn(Transaction transaction) {
        fuzzy.learn(transaction);
        naive.learn(transaction);
    }

    @Override
    public List<CategoryMatch> classify(Transaction item) {
        List<CategoryMatch> fuzzyMatches = fuzzy.classify(item);
        List<CategoryMatch> naiveMatches = naive.classify(item);

        List<Category> fuzzyCategories = fuzzyMatches.stream().map(CategoryMatch::getCategory).collect(Collectors.toList());
        List<Category> naiveCategories = naiveMatches.stream().map(CategoryMatch::getCategory).collect(Collectors.toList());

        Set<Category> uniqueCategories = new HashSet<>();
        uniqueCategories.addAll(fuzzyCategories);
        uniqueCategories.addAll(naiveCategories);

        List<CategoryMatch> matches = new ArrayList<>();
        for(Category category : uniqueCategories) {
            int confidence = 0;

            if(fuzzyCategories.contains(category)) {
                Optional<CategoryMatch> opt = fuzzyMatches.stream()
                        .filter(fm -> fm.getCategory().equals(category))
                        .findFirst();
                if(opt.isPresent()) {
                    confidence += 0.75 * opt.get().getConfidence();
                }
            }

            if(naiveCategories.contains(category)) {
                Optional<CategoryMatch> opt = naiveMatches.stream()
                        .filter(nm -> nm.getCategory().equals(category))
                        .findFirst();
                if(opt.isPresent()) {
                    confidence += 0.25 * opt.get().getConfidence();
                }
            }

            matches.add(new CategoryMatch(category, confidence));
        }

        return matches.stream()
                .filter(cm -> cm.getConfidence() > 30)
                .sorted(Comparator.comparingInt(CategoryMatch::getConfidence))
                .collect(Collectors.toList());
    }
}
