package com.vberlier.settlements.generator;

import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

public class Generator {
    private final World world;
    private final StructureBoundingBox boundingBox;
    private final HeightMap heightMap;
    private final TerrainMap terrainMap;

    public Generator(World world, StructureBoundingBox boundingBox) {
        this.world = world;
        this.boundingBox = boundingBox;
        heightMap = new HeightMap(world, boundingBox);
        heightMap.compute();
        terrainMap = new TerrainMap(heightMap);
        terrainMap.compute();
    }

    public void buildSettlement() {
        for (BlockPos pos : heightMap.getPositions()) {
            world.setBlockState(pos.add(0, 1, 0), Blocks.STAINED_GLASS.getDefaultState());
        }

        for (BlockPos pos : terrainMap.getPositions()) {
            world.setBlockState(pos, Blocks.STAINED_HARDENED_CLAY.getDefaultState());
        }

        System.out.println("Height min y " + heightMap.getMinY());
        System.out.println("Height max y " + heightMap.getMaxY());
        System.out.println("Terrain min y " + terrainMap.getMinY());
        System.out.println("Terrain max y " + terrainMap.getMaxY());
    }
}
