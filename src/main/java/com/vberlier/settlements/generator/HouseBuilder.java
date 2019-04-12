package com.vberlier.settlements.generator;

import com.google.common.graph.ValueGraph;
import com.vberlier.settlements.util.Vec;
import net.minecraft.block.*;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.template.Template;

public class HouseBuilder extends StructureBuilder {
    private ValueGraph<Slot, Integer> graph;

    private final Template houseBase;
    private final Template houseBaseFoundation;
    private final Template houseBaseRoof;
    private final Template houseBaseStilts;
    private final Template houseExtension;
    private final Template houseExtensionFoundation;
    private final Template houseExtensionRoof;
    private final Template houseExtensionStilts;
    private final Template houseSmallExtension;
    private final Template houseSmallExtensionFoundation;
    private final Template houseSmallExtensionRoof;
    private final Template houseSmallExtensionStilts;

    private final int wallsHeight;
    private final int foundationHeight;
    private final int stiltsHeight;
    private final int roofHeight;

    public HouseBuilder(World world, ValueGraph<Slot, Integer> graph, BlockPlanks.EnumType woodVariant) {
        super(world, woodVariant);
        this.graph = graph;

        houseBase = getTemplate("house_base");
        houseBaseFoundation = getTemplate("house_base_foundation");
        houseBaseStilts = getTemplate("house_base_stilts");
        houseBaseRoof = getTemplate("house_base_roof");
        houseExtension = getTemplate("house_extension");
        houseExtensionFoundation = getTemplate("house_extension_foundation");
        houseExtensionStilts = getTemplate("house_extension_stilts");
        houseExtensionRoof = getTemplate("house_extension_roof");
        houseSmallExtension = getTemplate("house_small_extension");
        houseSmallExtensionFoundation = getTemplate("house_small_extension_foundation");
        houseSmallExtensionStilts = getTemplate("house_small_extension_stilts");
        houseSmallExtensionRoof = getTemplate("house_small_extension_roof");

        wallsHeight = houseBase.getSize().getY();
        foundationHeight = houseBaseFoundation.getSize().getY();
        stiltsHeight = houseBaseStilts.getSize().getY();
        roofHeight = houseBaseRoof.getSize().getY();
    }

    public void build(Slot slot, double maxRadius) {
        BlockPos centerBlock = slot.getCenter().getTerrainBlock();

        Vec orientation = slot.getOrientation(graph);
        Vec extensionOrientation = (orientation.axis() == Vec.Axis.X ? orientation.project(Vec.Axis.Z) : orientation.project(Vec.Axis.X)).mul(-1);

        StructureBoundingBox availableSpace = computeAvailableSpace(centerBlock.add(0, 2 * wallsHeight / 3, 0), maxRadius);

        for (int x = availableSpace.minX; x < availableSpace.maxX; x++) {
            for (int z = availableSpace.minZ; z < availableSpace.maxZ; z++) {
                world.setBlockState(new BlockPos(x, centerBlock.getY() + 2 * wallsHeight / 3, z), Blocks.STAINED_GLASS.getDefaultState());
            }
        }

        StructureBoundingBox bb = spawnStructure(houseBase, centerBlock, orientation.rotation());
        spawnAdjacent(bb, houseSmallExtension, extensionOrientation.rotation());

        // TODO: Use stilts for steep surfaces

        bb = spawnStructure(houseBaseFoundation, centerBlock.add(0, -foundationHeight, 0), orientation.rotation());
        spawnAdjacent(bb, houseSmallExtensionFoundation, extensionOrientation.rotation());

        bb = spawnStructure(houseBaseRoof, centerBlock.add(0, wallsHeight - 1, 0), orientation.rotation());
        spawnAdjacent(bb, houseSmallExtensionRoof, extensionOrientation.rotation(), 2);

        rotateColor();
    }
}
