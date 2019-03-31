package com.vberlier.settlements.event;

import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.fml.common.eventhandler.Event;

public class SettlementEvent extends Event {
    private final World world;
    private final StructureBoundingBox boundingBox;

    public SettlementEvent(World world, StructureBoundingBox boundingBox) {
        this.world = world;
        this.boundingBox = boundingBox;
    }

    public World getWorld() {
        return world;
    }

    public StructureBoundingBox getBoundingBox() {
        return boundingBox;
    }

    public static class Generate extends SettlementEvent {
        public Generate(World world, StructureBoundingBox boundingBox) {
            super(world, boundingBox);
        }
    }
}
