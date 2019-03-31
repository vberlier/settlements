package com.vberlier.settlements.generator;

import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

public class Generator {
    private final World world;
    private final StructureBoundingBox boundingBox;
    private final HeightMap heightMap;

    public Generator(World world, StructureBoundingBox boundingBox) {
        this.world = world;
        this.boundingBox = boundingBox;
        heightMap = new HeightMap(world, boundingBox);
    }

    public void buildSettlement() {
        for (BlockPos pos : heightMap.getPositions()) {
            world.setBlockState(pos.add(0, 1, 0), Blocks.STAINED_GLASS.getDefaultState());
        }
    }
}
