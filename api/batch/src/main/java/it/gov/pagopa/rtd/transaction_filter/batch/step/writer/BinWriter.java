package it.gov.pagopa.rtd.transaction_filter.batch.step.writer;

import it.gov.pagopa.rtd.transaction_filter.service.BinStoreService;
import it.gov.pagopa.rtd.transaction_filter.service.WriterTrackerService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.batch.item.ItemWriter;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Implementation of {@link ItemWriter}, to be used for read/processed Par files
 */

@Slf4j
@Data
@RequiredArgsConstructor
public class BinWriter implements ItemWriter<String> {

    private final BinStoreService binStoreService;
    private final WriterTrackerService writerTrackerService;

    @Override
    public void write(List<? extends String> binRangeList) {

        binRangeList.forEach(binRangeStr-> {
            {
                String[] binRange = binRangeStr.split(";");
                try {
                    binStoreService.write(binRange[0], binRange[1]);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    log.error("Encountered error on " + binRangeStr);
                }
            }
        });

    }

}
