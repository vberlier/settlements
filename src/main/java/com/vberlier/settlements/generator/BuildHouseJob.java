package com.vberlier.settlements.generator;

public class BuildHouseJob extends Job {
    @Override
    public void run() {
        logger.info("Building a house in {}", generator.boundingBox);
    }
}
