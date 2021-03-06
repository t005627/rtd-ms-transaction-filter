package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import it.gov.pagopa.rtd.transaction_filter.service.TokenConnectorService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * implementation of the {@link Tasklet}, recovers the pan list from a REST service,
 * when eneabled
 */

@Slf4j
@Data
public class BinListRecoveryTasklet implements Tasklet, InitializingBean {

    private TokenConnectorService tokenConnectorService;
    private String binListDirectory;
    private String binFilePattern;
    private String fileName;
    private Boolean dailyRemovalTaskletEnabled = false;
    private Boolean recoveryTaskletEnabled = false;

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    /**
     * Recovers a file contianing the pan list, and optionally applies checksum
     * validation, and extracts the content from a compressed file, if required
     * @param stepContribution
     * @param chunkContext
     * @return task exit status
     * @throws Exception
     */
    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

        binListDirectory = binListDirectory.replaceAll("\\\\", "/");
        Resource[] resources = null;

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        String currentDate = OffsetDateTime.now().format(fmt);

        if (dailyRemovalTaskletEnabled) {

            resources = resolver.getResources("file:/"
                    .concat(binListDirectory.charAt(0) == '/' ?
                            binListDirectory.replaceFirst("/","") : binListDirectory)
                    .concat("/")
                    .concat(binFilePattern));

            try {

                for (Resource resource : resources) {
                    BasicFileAttributes fileAttributes = Files.readAttributes(
                            resource.getFile().toPath(), BasicFileAttributes.class);
                    long fileLastModTime = fileAttributes.lastModifiedTime().toMillis();
                    Instant instant = Instant.ofEpochMilli(fileLastModTime);
                    LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                    String fileLastModifiedDate = localDateTime.format(fmt);
                    if (!fileLastModifiedDate.equals(currentDate)) {
                        resource.getFile().delete();
                    }

                }

            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

        }

        if (recoveryTaskletEnabled) {
            resources = resolver.getResources("file:/"
                    .concat(binListDirectory.charAt(0) == '/' ?
                            binListDirectory.replaceFirst("/","") : binListDirectory)
                    .concat("/")
                    .concat(binFilePattern));

            int fileId=1;
            File outputFile = FileUtils.getFile(binListDirectory
                    .concat("/".concat(
                            String.valueOf(fileId).concat(OffsetDateTime.now().format(fmt).concat("_"))
                                    .concat(fileName != null ? fileName : "binList"))));
            if (resources.length == 0 || !outputFile.exists()) {
                List<File> binListTempFiles = tokenConnectorService.getBinList();
                for (File binListTempFile : binListTempFiles) {
                    outputFile = FileUtils.getFile(binListDirectory.concat("/".concat(
                            String.valueOf(fileId).concat(OffsetDateTime.now().format(fmt).concat("_"))
                                    .concat(fileName != null ? fileName : "binList"))));
                    FileUtils.moveFile(
                            binListTempFile,
                            outputFile);
                    fileId = fileId+1;
                }
                tokenConnectorService.cleanAllTempFiles();
            }
        }
        return RepeatStatus.FINISHED;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(resolver.getResources(binListDirectory),
                "directory must be set");
    }
}
