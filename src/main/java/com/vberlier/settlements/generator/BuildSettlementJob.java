package com.vberlier.settlements.generator;

public class BuildSettlementJob extends Job {
    @Override
    public void run() {
        logger.info("Building 3 houses");
        submit(new BuildHouseJob());
        submit(new BuildHouseJob());
        submit(new BuildHouseJob());
    }
}
