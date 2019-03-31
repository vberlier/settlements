package com.vberlier.settlements.generator;

import com.google.common.base.MoreObjects;
import org.apache.logging.log4j.Logger;

abstract public class Job {
    private JobProcessor jobProcessor;
    protected Generator generator;
    protected Logger logger;

    abstract public void run();

    public void setJobProcessor(JobProcessor jobProcessor) {
        this.jobProcessor = jobProcessor;
    }

    public void setGenerator(Generator generator) {
        this.generator = generator;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void submit(Job job) {
        jobProcessor.submit(generator, job);
    }

    public MoreObjects.ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(this).add("generator", generator);
    }

    @Override
    public String toString() {
        return toStringHelper().toString();
    }
}
