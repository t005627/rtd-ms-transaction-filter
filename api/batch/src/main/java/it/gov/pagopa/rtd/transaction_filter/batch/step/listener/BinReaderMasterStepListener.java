package it.gov.pagopa.rtd.transaction_filter.batch.step.listener;

import it.gov.pagopa.rtd.transaction_filter.service.BinStoreService;
import it.gov.pagopa.rtd.transaction_filter.service.WriterTrackerService;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Implementation of {@link StepExecutionListener}, to be used to log and define the exit status of a step
 */

@Slf4j
@Data
public class BinReaderMasterStepListener implements StepExecutionListener {

    private BinStoreService binStoreService;
    private WriterTrackerService writerTrackerService;


    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("Starting processing for bin range");
    }

    @SneakyThrows
    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {

        ExitStatus exitStatus = stepExecution.getExitStatus();

        List<CountDownLatch> countDownLatchList = writerTrackerService.getCountDownLatches();

        if (countDownLatchList != null) {
            for (CountDownLatch countDownLatch : countDownLatchList) {
                countDownLatch.await();
            }
        }

        binStoreService.closeAllWriters();


        return exitStatus;

    }

}
