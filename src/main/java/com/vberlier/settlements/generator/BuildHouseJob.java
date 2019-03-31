package com.vberlier.settlements.generator;

import com.vberlier.settlements.generator.Job;

public class BuildHouseJob extends Job {
    @Override
    public void run() {
        logger.info("Building a house in {}", generator.boundingBox);
    }
}
