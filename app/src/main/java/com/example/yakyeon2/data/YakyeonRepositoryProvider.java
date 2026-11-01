package com.example.yakyeon2.data;

public final class YakyeonRepositoryProvider {

    private static YakyeonRepository repository = new InMemoryYakyeonRepository();

    private YakyeonRepositoryProvider() {
    }

    public static YakyeonRepository get() {
        return repository;
    }

    public static void setForTest(YakyeonRepository replacement) {
        repository = replacement == null ? new InMemoryYakyeonRepository() : replacement;
    }
}
