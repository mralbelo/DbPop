package org.dandoy.dbpopd.code;

import io.micronaut.context.annotation.Context;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dandoy.dbpopd.ConfigurationService;
import org.dandoy.dbpopd.site.Message;
import org.dandoy.dbpopd.site.MessageType;
import org.dandoy.dbpopd.utils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.nio.file.StandardWatchEventKinds.*;

@Slf4j
@Context
public class FileChangeDetector {
    private WatchService watchService;
    private Thread thread;
    private final Path codePath;
    private final Map<WatchKey, Path> keys = new HashMap<>();
    private final Set<Path> knownFiles = new HashSet<>();
    private final ChangeDetector changeDetector;
    private boolean hasCodeDirectory;
    /**
     * Accumulates the actions and only send the events if nothing happened for a few seconds
     */
    private final List<Runnable> pending = new ArrayList<>();

    public FileChangeDetector(ConfigurationService configurationService, ChangeDetector changeDetector) {
        codePath = configurationService.getCodeDirectory().toPath();
        this.changeDetector = changeDetector;
    }

    void postContruct() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            thread = new Thread(this::threadLoop, "FileChangeDetector");
            thread.setDaemon(true);
            thread.start();
            if (Files.isDirectory(codePath)) {
                try (Stream<Path> stream = Files.walk(codePath)) {
                    stream
                            .filter(Files::isDirectory)
                            .forEach(this::watch);
                }
            }
            for (Path path = codePath.getParent(); path != null; path = path.getParent()) {
                if (Files.isDirectory(path)) {
                    watch(path);
                }
            }
        } catch (IOException e) {
            log.error("Cannot watch the directory " + codePath, e);
        }
    }

    void preDestroy() {
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException e) {
            log.error("Failed to stop the WatchService");
            return;
        }
        try {
            thread.join(2000);
            if (thread.isAlive()) {
                log.info("Failed to stop the thread");
            }
        } catch (InterruptedException e) {
            log.info("Failed to stop the thread", e);
        }
    }

    private void watch(Path path) {
        try {
            log.debug("Watching {}", path);
            WatchKey watchKey = path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            keys.put(watchKey, path);
        } catch (IOException e) {
            log.error("Failed to register the path " + path, e);
        }
    }

    private void watchPathAndSubPaths(Path path) {
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) {
                    watch(path);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("registerAll failed", e);
        }
    }

    private void handlePathChanged(Path path) {
        File file = path.toFile();
        log.debug("File modified: {}", file);
        pending.add(() -> {
            checkHasCodeDirectory(true);
            changeDetector.whenFileChanged(file);
        });
    }

    private void handlePathDeleted(Path path) {
        File file = path.toFile();
        log.debug("File deleted: {}", file);
        pending.add(() -> {
            checkHasCodeDirectory(false);
            changeDetector.whenFileDeleted(file);
        });
    }

    void threadLoop() {
        while (true) {
            try {
                WatchKey watchKey = getWatchKey();
                if (watchKey != null) {
                    List<WatchEvent<?>> watchEvents = watchKey.pollEvents();
                    for (WatchEvent<?> pollEvent : watchEvents) {
                        if (pollEvent.kind() == OVERFLOW) continue;

                        // Check that the event is about Paths
                        if (pollEvent.context() instanceof Path childPath) {
                            log.debug("WatchEvent {}: {}", pollEvent.kind(), childPath);
                            Path parentPath = keys.get(watchKey);
                            Path path = parentPath.resolve(childPath);

                            if (pollEvent.kind() == ENTRY_CREATE) {
                                if (Files.isDirectory(path)) {
                                    watchPathAndSubPaths(path);
                                } else {
                                    knownFiles.add(path);
                                    handlePathChanged(path);
                                }
                            } else if (pollEvent.kind() == ENTRY_DELETE) {
                                if (knownFiles.remove(path)) {
                                    handlePathDeleted(path);
                                }
                            } else if (pollEvent.kind() == ENTRY_MODIFY) {
                                if (knownFiles.contains(path)) {
                                    handlePathChanged(path);
                                }
                            }
                        }
                    }
                    boolean valid = watchKey.reset();
                    if (!valid) {
                        Path removed = keys.remove(watchKey);
                        log.debug("No longer watching {}", removed);
                        if (keys.isEmpty()) {
                            log.error("FileChangeDetector has nothing left to watch");
                        }
                    }
                }
            } catch (ClosedWatchServiceException ignored) {
                log.debug("FileChangeDetector - WatchService closed");
                break;
            } catch (InterruptedException e) {
                log.debug("FileChangeDetector interrupted");
                break;
            }
        }
        log.debug("FileChangeDetector thread stopped");
    }

    private WatchKey getWatchKey() throws InterruptedException {
        WatchKey watchKey;
        if (pending.isEmpty()) {
            watchKey = watchService.take();
        } else {
            watchKey = watchService.poll(1, TimeUnit.SECONDS);
            if (watchKey == null) {
                log.debug("Processing {} events", pending.size());
                changeDetector.holdingChanges(changeSession -> {
                    pending.forEach(Runnable::run);
                    pending.clear();
                });
                return null;
            }
        }
        return watchKey;
    }

    byte[] getHash(@Nullable File file) {
        if (file == null) return null;
        if (!file.isFile()) return null;
        MessageDigest messageDigest = ChangeDetector.getMessageDigest();
        String sql = IOUtils.toString(file);
        sql = ChangeDetector.cleanSql(sql);
        byte[] bytes = sql.getBytes(StandardCharsets.UTF_8);
        return messageDigest.digest(bytes);
    }

    static class HasCodeDirectoryMessage extends Message {
        @Getter
        private final boolean hasCodeDirectory;

        public HasCodeDirectoryMessage(boolean hasCodeDirectory) {
            super(MessageType.HAS_CODE_DIRECTORY);
            this.hasCodeDirectory = hasCodeDirectory;
        }
    }

    private void setHasCodeDirectory(boolean hasCodeDirectory) {
        log.debug("setHasCodeDirectory({})", hasCodeDirectory);
        if (this.hasCodeDirectory != hasCodeDirectory) {
            this.hasCodeDirectory = hasCodeDirectory;
            changeDetector.sendMessage(new HasCodeDirectoryMessage(hasCodeDirectory));
        }
    }

    private void checkHasCodeDirectory(boolean added) {
        if (hasCodeDirectory && added) return;      // We already had code, and a file or directory was added
        try {
            final boolean[] found = {false};
            if (Files.isDirectory(codePath)) {
                Files.walkFileTree(codePath, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        String filename = file.getName(file.getNameCount() - 1).toString();
                        if (filename.toLowerCase().endsWith(".sql")) {
                            found[0] = true;
                            return FileVisitResult.TERMINATE;
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            setHasCodeDirectory(found[0]);
        } catch (IOException e) {
            log.error("checkHasCodeDirectory failed", e);
        }
    }
}
