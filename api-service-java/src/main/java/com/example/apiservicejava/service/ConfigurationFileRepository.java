package com.example.apiservicejava.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import tools.jackson.databind.ObjectMapper;

@Repository
public class ConfigurationFileRepository {

    private final ObjectMapper objectMapper;
    private final Path baseDir;

    public ConfigurationFileRepository(
            ObjectMapper objectMapper,
            @Value("${app.configurations-db.path:configurations-db}") String baseDirPath
    ) {
        this.objectMapper = objectMapper;
        this.baseDir = Path.of(baseDirPath);
    }

    public void ensureBaseDirExists() throws IOException {
        Files.createDirectories(baseDir);
    }

    public <T> void save(String fileName, T data) throws IOException {
        ensureBaseDirExists();
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(baseDir.resolve(fileName).toFile(), data);
    }

    public <T> T read(String fileName, Class<T> clazz) throws IOException {
        return objectMapper.readValue(baseDir.resolve(fileName).toFile(), clazz);
    }

    public List<String> listJsonFiles() throws IOException {
        ensureBaseDirExists();
        try (Stream<Path> stream = Files.list(baseDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.endsWith(".json"))
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }
    }

    public boolean exists(String fileName) {
        return Files.exists(baseDir.resolve(fileName));
    }
}