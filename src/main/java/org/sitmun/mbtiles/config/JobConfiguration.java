package org.sitmun.mbtiles.config;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.sitmun.mbtiles.jobs.MBTilesTask;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.BatchConfigurationException;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@Slf4j
public class JobConfiguration {

  @Value("${spring.batch.job.corePoolSize:2}")
  int corePoolSize;

  @Value("${spring.batch.job.maxPoolSize:4}")
  int maxPoolSize;

  @Value("${spring.batch.job.queueCapacity:10}")
  int queueCapacity;

  @Bean
  public TaskExecutor taskExecutor() {
    log.info("Creating ThreadPoolTaskExecutor for async job execution");
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);
    executor.setMaxPoolSize(maxPoolSize);
    executor.setQueueCapacity(queueCapacity);
    executor.setThreadNamePrefix("batch-job-");
    executor.initialize();
    return executor;
  }

  @Bean
  public @NotNull JobLauncher batchJobLauncher(
      @NotNull JobRepository jobRepository, @NotNull TaskExecutor taskExecutor) {
    log.info("Creating TaskExecutorJobLauncher with async TaskExecutor");
    TaskExecutorJobLauncher taskExecutorJobLauncher = new TaskExecutorJobLauncher();
    taskExecutorJobLauncher.setJobRepository(jobRepository);
    taskExecutorJobLauncher.setTaskExecutor(taskExecutor);
    try {
      taskExecutorJobLauncher.afterPropertiesSet();
      return taskExecutorJobLauncher;
    } catch (Exception e) {
      throw new BatchConfigurationException("Unable to configure the default job launcher", e);
    }
  }

  @Bean
  public Job mbTilesJob(JobRepository jobRepository, Step mbtilesStep) {
    log.info(
        "Creating MBTiles job with JobRepository: {}", jobRepository.getClass().getSimpleName());
    return new JobBuilder("mbtilesJob", jobRepository).start(mbtilesStep).build();
  }

  @Bean
  public Step mbtilesStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      Tasklet mbTilesTask) {
    log.info(
        "Creating MBTiles step with JobRepository: {}", jobRepository.getClass().getSimpleName());
    return new StepBuilder("mbtilesStep", jobRepository)
        .tasklet(mbTilesTask, transactionManager)
        .build();
  }

  @Bean
  public Tasklet mbTilesTask(MBTilesTask task) {
    log.info("Creating MBTiles tasklet with Task: {}", task.getClass().getSimpleName());
    return (contribution, chunkContext) -> {
      task.execute(chunkContext.getStepContext());
      return RepeatStatus.FINISHED;
    };
  }
}
