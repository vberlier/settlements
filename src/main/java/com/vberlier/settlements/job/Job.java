package com.vberlier.settlements.job;

import com.google.common.base.MoreObjects;
import com.vberlier.settlements.Generator;

abstract public class Job {
    private Generator generator;

    public Generator getGenerator() {
        return generator;
    }

    public void setGenerator(Generator generator) {
        this.generator = generator;
    }

    abstract public void run();

    public MoreObjects.ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(this);
    }

    @Override
    public String toString() {
        return toStringHelper().toString();
    }
}
