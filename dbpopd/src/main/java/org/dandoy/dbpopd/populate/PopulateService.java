package org.dandoy.dbpopd.populate;

import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.dandoy.dbpop.database.DatabaseCache;
import org.dandoy.dbpop.upload.PopulateDatasetException;
import org.dandoy.dbpop.upload.Populator;
import org.dandoy.dbpop.utils.ExceptionUtils;
import org.dandoy.dbpop.utils.MultiCauseException;
import org.dandoy.dbpopd.ConfigurationService;
import org.dandoy.dbpopd.datasets.DatasetsService;
import org.dandoy.dbpopd.utils.FileUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@Slf4j
public class PopulateService {
    private final ConfigurationService configurationService;
    private final DatasetsService datasetsService;
    private Map<File, Long> fileTimestamps = new HashMap<>();

    public PopulateService(ConfigurationService configurationService, DatasetsService datasetsService) {
        this.configurationService = configurationService;
        this.datasetsService = datasetsService;
    }

    public PopulateResult populate(List<String> dataset) {
        return populate(dataset, false);
    }

    public PopulateResult populate(List<String> datasets, boolean forceStatic) {
        try {
            long t0 = System.currentTimeMillis();
            DatabaseCache databaseCache = configurationService.getTargetDatabaseCache();
            Populator populator = Populator.createPopulator(databaseCache, configurationService.getDatasetsDirectory());
            if (forceStatic) {
                populator.setStaticLoaded(false);
            } else {
                boolean staticChanged = hasStaticChanged();
                populator.setStaticLoaded(!staticChanged);
            }
            int rows = populator.load(datasets);
            long t1 = System.currentTimeMillis();
            datasetsService.setActive(datasets.get(datasets.size() - 1), rows, t1 - t0);

            captureStaticTimestamps();

            return new PopulateResult(rows, t1 - t0);
        } catch (Exception e) {
            ExceptionUtils.getCause(e, PopulateDatasetException.class)
                    .ifPresent(populateDatasetException -> {
                        String dataset = populateDatasetException.getDataset();
                        List<String> causes = MultiCauseException.getCauses(populateDatasetException);
                        datasetsService.setFailedDataset(dataset, causes);
                    });
            throw e;
        }
    }

    private boolean hasStaticChanged() {
        boolean ret = false;
        File datasetsDirectory = configurationService.getDatasetsDirectory();
        File staticDir = new File(datasetsDirectory, "static");
        if (staticDir.isDirectory()) {
            List<File> files = FileUtils.getFiles(staticDir);
            for (File file : files) {
                Long lastModified = fileTimestamps.get(file);
                long thatLastModified = file.lastModified();
                if (lastModified == null) {
                    ret = true; // new file
                } else if (lastModified != thatLastModified) {
                    ret = true; // different timestamp
                }
            }
        }
        return ret;
    }

    private void captureStaticTimestamps() {
        File datasetsDirectory = configurationService.getDatasetsDirectory();
        File staticDir = new File(datasetsDirectory, "static");
        if (staticDir.isDirectory()) {
            List<File> files = FileUtils.getFiles(staticDir);
            Map<File, Long> newMap = new HashMap<>();
            for (File file : files) {
                long lastModified = file.lastModified();
                newMap.put(file, lastModified);
            }
            fileTimestamps = newMap;
        }
    }
}
