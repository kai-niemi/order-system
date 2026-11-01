package se.cloudneutral.orders.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

public class RandomData {
    private static final Logger logger = LoggerFactory.getLogger(RandomData.class);

    private static final List<String> firstNames = new ArrayList<>();

    private static final List<String> lastNames = new ArrayList<>();

    private static final List<String> cities = new ArrayList<>();

    private static final List<String> countries = new ArrayList<>();

    private static final List<String> products = new ArrayList<>();

    private static final List<String> STREETS = List.of(
            "Main Street",
            "Oak Avenue",
            "Maple Drive",
            "Cedar Lane",
            "Pine Road",
            "Elm Street",
            "Birch Boulevard",
            "Willow Way",
            "Park Avenue",
            "River Road"
    );

    static {
        firstNames.addAll(readLines("random/firstname_female.txt"));
        firstNames.addAll(readLines("random/firstname_male.txt"));
        lastNames.addAll(readLines(("random/surnames.txt")));
        cities.addAll(readLines(("random/cities.txt")));
        products.addAll(readLines(("random/products.txt")));

        for (Locale locale : Locale.getAvailableLocales()) {
            if (StringUtils.hasLength(locale.getDisplayCountry(Locale.US))) {
                countries.add(locale.getDisplayCountry(Locale.US));
            }
        }
    }

    private static List<String> readLines(String path) {
        try (InputStream resource = new ClassPathResource(path).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(resource))) {
            return reader.lines().collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("", e);
        }
        return Collections.emptyList();
    }

    public static <E> E selectRandom(List<E> collection) {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        return collection.get(random.nextInt(collection.size()));
    }

    public static String randomFirstName() {
        return selectRandom(firstNames);
    }

    public static String randomLastName() {
        return selectRandom(lastNames);
    }

    public static String randomCity() {
        return StringUtils.capitalize(selectRandom(cities));
    }

    public static String randomCountry() {
        return selectRandom(countries);
    }

    public static String randomProduct() {
        return selectRandom(products);
    }

    public static String randomZipCode() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    public static String randomEmail(String firstName, String lastName) {
        return (firstName.toLowerCase()
                + "."
                + lastName.toLowerCase()
                + "_" + ThreadLocalRandom.current().nextInt(0,2^16)
                + "@home.com")
                .replace(' ', '.');
    }

    public static String randomAddress() {
        return randomAddresses(1).getFirst();
    }

    public static List<String> randomAddresses(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> "%d %s".formatted(
                        ThreadLocalRandom.current().nextInt(1, 1000),
                        STREETS.get(ThreadLocalRandom.current().nextInt(STREETS.size()))))
                .toList();
    }

    private static final char[] VOWELS = "aeiou".toCharArray();

    private static final char[] CONSONANTS = "bcdfghjklmnpqrstvwxyz".toCharArray();

    public static String randomSku(int length) {
        return "SKU-" + UUID.randomUUID()
                .toString()
                .substring(0, length)
                .toUpperCase(Locale.ROOT);
    }

    public static String randomWord(int length) {
        StringBuilder sb = new StringBuilder();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            if (random.nextBoolean()) {
                sb.append(VOWELS[random.nextInt(VOWELS.length)]);
            } else {
                sb.append(CONSONANTS[random.nextInt(CONSONANTS.length)]);
            }
        }
        return sb.toString();
    }
}

