package com.vberlier.settlements;

import com.vberlier.settlements.job.Job;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.Queue;

public class Generator {
    private Queue<Job> jobs = new LinkedList<>();
    private Logger logger;

    public Generator(Logger logger) {
        this.logger = logger;
    }

    public void submit(Job job) {
        job.setGenerator(this);
        jobs.add(job);

        logger.info("Submitted new job {}", job);
    }
}
