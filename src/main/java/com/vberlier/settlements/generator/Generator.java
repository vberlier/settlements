package com.vberlier.settlements.generator;

import com.google.common.base.MoreObjects;
import com.vberlier.settlements.SettlementsMod;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

public class Generator {
    public final World world;
    public final StructureBoundingBox boundingBox;

    public Generator(World world, StructureBoundingBox boundingBox) {
        this.world = world;
        this.boundingBox = boundingBox;
    }

    public void buildSettlement() {
        JobProcessor jobProcessor = SettlementsMod.instance.getJobProcessor();

        if (jobProcessor != null) {
            jobProcessor.submit(this, new BuildSettlementJob());
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("world", world.provider.getDimensionType().getName())
                .add("boudingBox", boundingBox)
                .toString();
    }
}
