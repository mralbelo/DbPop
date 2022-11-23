package org.dandoy.dbpopd;

import io.micronaut.context.annotation.Property;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import org.dandoy.dbpop.database.TableName;
import org.dandoy.dbpop.download.Downloader;
import org.dandoy.dbpop.download.Where;
import org.dandoy.dbpop.upload.Populator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Singleton
public class DbpopdService {
    private final Populator populator;
    private final Downloader.Builder downloadBuilder;

    public DbpopdService(
            @Property(name = "dbpopd.configuration.path") String configurationPath
    ) {
        File configurationDir = toConfigurationDir(configurationPath);
        File datasetsDirectory = new File(configurationDir, "datasets");
        populator = Populator.builder()
                .setDirectory(datasetsDirectory)
                .build();
        downloadBuilder = Downloader.builder()
                .setDirectory(datasetsDirectory);
    }

    private static File toConfigurationDir(String configurationPath) {
        try {
            File configurationDir = new File(configurationPath).getCanonicalFile();
            File configurationFile = new File(configurationDir, "dbpop.properties");
            if (!configurationFile.canRead()) {
                throw new RuntimeException("Cannot read the configuration file: %s".formatted(configurationFile));
            }
            return configurationDir;
        } catch (IOException e) {
            throw new RuntimeException("Invalid configuration path: %s".formatted(configurationPath), e);
        }
    }

    @PreDestroy
    void shutdown() {
        if (populator != null) {
            populator.close();
        }
    }

    public PopulateResult populate(List<String> datasets) {
        long t0 = System.currentTimeMillis();
        int rows = populator.load(datasets);
        long t1 = System.currentTimeMillis();
        return new PopulateResult(rows, t1 - t0);
    }

    public void download(String dataset, TableName tableName, Map<String, Object> whereMap) {
        try (Downloader downloader = downloadBuilder
                .setDataset(dataset)
                .build()) {
            List<Where> wheres = whereMap.entrySet().stream()
                    .map(entry -> new Where(entry.getKey(), entry.getValue()))
                    .toList();
            downloader.download(tableName, wheres);
        }
    }

    record PopulateResult(int rows, long millis) {
    }
}
