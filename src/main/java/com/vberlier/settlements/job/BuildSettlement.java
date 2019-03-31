package com.vberlier.settlements.job;

import com.google.common.base.MoreObjects;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

public class BuildSettlement extends Job {
    private World world;
    private StructureBoundingBox boundingBox;

    public BuildSettlement(World world, StructureBoundingBox boundingBox) {
        this.world = world;
        this.boundingBox = boundingBox;
    }

    @Override
    public void run() {

    }

    @Override
    public MoreObjects.ToStringHelper toStringHelper() {
        return super.toStringHelper().add("boundingBox", boundingBox);
    }
}
