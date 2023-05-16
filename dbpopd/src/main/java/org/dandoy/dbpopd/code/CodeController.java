package org.dandoy.dbpopd.code;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.problem.HttpStatusType;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.dandoy.dbpop.database.ObjectIdentifier;
import org.dandoy.dbpop.database.TableName;
import org.dandoy.dbpopd.ConfigurationService;
import org.dandoy.dbpopd.site.SiteWebSocket;
import org.zalando.problem.Problem;

import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

@Controller("/code")
@Tag(name = "code")
@Slf4j
public class CodeController {
    private final ConfigurationService configurationService;
    private final CodeService codeService;
    private final ChangeDetector changeDetector;
    private final SiteWebSocket siteWebSocket;

    public CodeController(ConfigurationService configurationService, CodeService codeService, ChangeDetector changeDetector, SiteWebSocket siteWebSocket) {
        this.configurationService = configurationService;
        this.codeService = codeService;
        this.changeDetector = changeDetector;
        this.siteWebSocket = siteWebSocket;
    }

    @Get("source/compare")
    public CodeDiff compareSourceToFile() {
        return codeService.compareSourceToFile();
    }

    @Get("source/download")
    public DownloadResult downloadSourceToFile() {
        return codeService.downloadSourceToFile();
    }

    @Get("target/compare")
    public CodeDiff compareTargetToFile() {
        return codeService.compareTargetToFile();
    }

    @Get("target/upload")
    public UploadResult uploadFileToTarget() {
        return codeService.uploadFileToTarget();
    }

    @Get("target/download")
    public DownloadResult downloadTargetToFile() {
        return codeService.downloadTargetToFile();
    }

    record ChangeResponse(String path, String dbname, ObjectIdentifierResponse objectIdentifier, boolean fileChanged, boolean databaseChanged) {}

    record ObjectIdentifierResponse(String type, TableName tableName, ObjectIdentifierResponse parent) {
        static ObjectIdentifierResponse toObjectIdentifierResponse(ObjectIdentifier objectIdentifier) {
            return new ObjectIdentifierResponse(
                    objectIdentifier.getType(),
                    new TableName(
                            objectIdentifier.getCatalog(),
                            objectIdentifier.getSchema(),
                            objectIdentifier.getName()
                    ),
                    objectIdentifier.getParent() == null ? null : toObjectIdentifierResponse(objectIdentifier.getParent())
            );
        }

        public ObjectIdentifier toObjectIdentifier() {
            return new ObjectIdentifier(
                    type, tableName().getCatalog(), tableName().getSchema(), tableName.getTable(),
                    parent == null ? null : parent.toObjectIdentifier()
            );
        }
    }

    @Get("target/changes")
    public Stream<ChangeResponse> targetChanges() {
        Path codePath = configurationService.getCodeDirectory().toPath();
        return changeDetector.getChanges()
                .stream()
                .map(change -> {
                            String path = change.getFile() == null ? null : codePath.relativize(change.getFile().toPath()).toString();
                            ObjectIdentifier objectIdentifier = change.getObjectIdentifier();
                            return new ChangeResponse(
                                    path,
                                    objectIdentifier == null ? null : objectIdentifier.toQualifiedName(),
                                    objectIdentifier == null ? null : ObjectIdentifierResponse.toObjectIdentifierResponse(objectIdentifier),
                                    change.isFileChanged(),
                                    change.isDatabaseChanged()
                            );
                        }
                )
                .sorted(Comparator.comparing(ChangeResponse::path));
    }

    record ApplyChangesRequest(String path, ObjectIdentifierResponse objectIdentifier) {}

    private File safeGetFile(String path) {
        // Resolve the requested path and validate that it is valid
        Path codePath = configurationService.getCodeDirectory().toPath();
        Path resolved = codePath.resolve(path).normalize();
        if (!resolved.startsWith(codePath))
            throw Problem.builder()
                    .withStatus(new HttpStatusType(HttpStatus.BAD_REQUEST))
                    .withTitle("Invalid Path: " + path)
                    .build();

        // Verify the file exists
        File file = resolved.toFile();
        if (!file.exists())
            throw Problem.builder()
                    .withStatus(new HttpStatusType(HttpStatus.NOT_FOUND))
                    .withTitle("File does not exist: " + path)
                    .build();
        return file;
    }

    @Post("target/changes/apply-files")
    public void applyFileChanges(@Body ApplyChangesRequest[] requests) {
        siteWebSocket.holdChanges(() -> {
            for (ApplyChangesRequest change : requests) {
                if (change.path != null) {
                    File file = safeGetFile(change.path);
                    codeService.uploadFileToTarget(file);
                } else {
                    codeService.deleteTargetObject(change.objectIdentifier.toObjectIdentifier());
                }
            }
        });
    }

    @Post("target/changes/apply-dbs")
    public void applyDbChanges(@Body ApplyChangesRequest[] requests) {
        siteWebSocket.holdChanges(() -> {
            for (ApplyChangesRequest change : requests) {
                if (change.objectIdentifier != null) {
                    codeService.downloadTargetToFile(change.objectIdentifier.toObjectIdentifier());
                } else {
                    File file = safeGetFile(change.path());
                    codeService.deleteFile(file);
                }
            }
        });
    }
}
