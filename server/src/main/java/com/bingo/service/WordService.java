package com.bingo.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads the bundled dictionary and serves random words on demand,
 * excluding any words the caller has already chosen.
 */
@Service
public class WordService {

    private List<String> dictionary = new ArrayList<>();

    @PostConstruct
    public void loadDictionary() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/words/dictionary.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            dictionary = reader.lines()
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .filter(w -> !w.isEmpty() && !w.startsWith("#"))
                    .distinct()
                    .collect(Collectors.toList());
        }
    }

    /**
     * Return up to {@code count} random words from the dictionary,
     * excluding any word in {@code excludeWords}.
     *
     * @param count        how many words to return (capped at available pool size)
     * @param excludeWords words already in use — none of these will be returned
     * @return list of randomly selected words, uppercased
     * @throws IllegalArgumentException if count is less than 1 or more than 99
     */
    public List<String> randomWords(int count, Collection<String> excludeWords) {
        if (count < 1 || count > 99) {
            throw new IllegalArgumentException("Count must be between 1 and 99.");
        }

        Set<String> excluded = excludeWords.stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        List<String> pool = dictionary.stream()
                .filter(w -> !excluded.contains(w))
                .collect(Collectors.toList());

        if (pool.isEmpty()) {
            return List.of();
        }

        Collections.shuffle(pool);
        return pool.subList(0, Math.min(count, pool.size()));
    }

    /** Total number of words in the dictionary. */
    public int dictionarySize() {
        return dictionary.size();
    }
}
