package com.vberlier.settlements.generator;

import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.Queue;

public class JobProcessor {
    private Queue<Job> jobs = new LinkedList<>();
    private Logger logger;

    public JobProcessor(Logger logger) {
        this.logger = logger;
    }

    public void submit(Generator generator, Job job) {
        job.setJobProcessor(this);
        job.setGenerator(generator);
        job.setLogger(logger);
        jobs.add(job);

        logger.info("Submitted new job {}", job);
    }

    public void process() {
        while (!jobs.isEmpty()) {
            Job job = jobs.remove();

            logger.info("Running submitted job {}", job);
            job.run();
        }
    }
}
