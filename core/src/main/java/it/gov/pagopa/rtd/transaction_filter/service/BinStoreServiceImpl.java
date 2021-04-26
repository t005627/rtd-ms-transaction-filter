package it.gov.pagopa.rtd.transaction_filter.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.TreeSet;

/**
* Implementation of {@link ParStoreService}
 */

@Service
@RequiredArgsConstructor
@Slf4j
class BinStoreServiceImpl implements BinStoreService {

    private final List<BufferedWriter> bufferedWriterList;
    private final TreeSet<String> binSet;
    private String workingBinDirectory;
    private Long numberPerFile;
    private Long currentNumberOfData = 0L;

    @SneakyThrows
    @Override
    public void write(String par) {
        BufferedWriter bufferedWriter = getBufferedWriter();
        assert bufferedWriter != null;
        bufferedWriter.write(par.concat("\n"));
    }

    @Override
    public synchronized void store(String bin) {
        binSet.add(bin);
    }

    @Override
    public Boolean hasBin(String bin) {
        return binSet.contains(bin);
    }

    @SneakyThrows
    @Override
    public void clearAll() {
        binSet.clear();
        for (BufferedWriter bufferedWriter : bufferedWriterList) {
            bufferedWriter.close();
        }
    }

    @SneakyThrows
    @Override
    public void clearStoreSet() {
        binSet.clear();
    }

    @SneakyThrows
    @Override
    public void closeAllWriters() {
       for (BufferedWriter bufferedWriter : bufferedWriterList) {
           bufferedWriter.close();
       }
    }

    @SneakyThrows
    private BufferedWriter getBufferedWriter() {
        int data = bufferedWriterList.size();
        Long currentData =  getCurrentNumberOfData();
        if (Math.multiplyExact(data,numberPerFile) > currentData) {
            return bufferedWriterList.get(bufferedWriterList.size()-1);
        } else {
            synchronized (bufferedWriterList) {
                data = bufferedWriterList.size();
                if (Math.multiplyExact(data,numberPerFile) > currentData) {
                    return bufferedWriterList.get(bufferedWriterList.size()-1);
                } else {
                    return openBufferedWriter(bufferedWriterList.size());
                }
            }
        }
    }

    @SneakyThrows
    private synchronized BufferedWriter openBufferedWriter(int pageNumber) {
        BufferedWriter bufferedWriter;
        if (pageNumber == bufferedWriterList.size()-1) {
            bufferedWriter = bufferedWriterList.get(pageNumber-1);
        } else {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResource(workingBinDirectory);
            bufferedWriter = Files.newBufferedWriter(
                    Paths.get(resource.getFile().getAbsolutePath().concat("/".concat
                            ("temp".concat(String.valueOf(pageNumber)).concat(".csv")))),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            bufferedWriterList.add(bufferedWriter);
        }
        return bufferedWriter;
    }

    private synchronized Long getCurrentNumberOfData() {
        currentNumberOfData = currentNumberOfData + 1;
        return currentNumberOfData;
    }

    @Override
    public void setWorkingBinDirectory(String workingBinDirectory) {
        this.workingBinDirectory = workingBinDirectory;
    }

    @Override
    public void setNumberPerFile(Long numberPerFile) {
        this.numberPerFile = numberPerFile;
    }

    @Override
    public void setCurrentNumberOfData(Long currentNumberOfData) {
        this.currentNumberOfData = currentNumberOfData;
    }

}