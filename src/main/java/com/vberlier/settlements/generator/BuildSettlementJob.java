package com.vberlier.settlements.generator;

import com.vberlier.settlements.generator.BuildHouseJob;
import com.vberlier.settlements.generator.Job;

public class BuildSettlementJob extends Job {
    @Override
    public void run() {
        logger.info("Building 3 houses");
        submit(new BuildHouseJob());
        submit(new BuildHouseJob());
        submit(new BuildHouseJob());
    }
}
