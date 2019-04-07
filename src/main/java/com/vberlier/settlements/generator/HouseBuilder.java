package com.vberlier.settlements.generator;

import com.google.common.graph.ValueGraph;
import com.vberlier.settlements.SettlementsMod;
import com.vberlier.settlements.util.Vec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;

public class HouseBuilder {
    private final World world;
    private final WorldServer worldServer;
    private final MinecraftServer minecraftServer;
    private final TemplateManager templateManager;
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

    public HouseBuilder(World world, ValueGraph<Slot, Integer> graph) {
        this.world = world;
        worldServer = (WorldServer) world;
        minecraftServer = worldServer.getMinecraftServer();
        templateManager = worldServer.getStructureTemplateManager();
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
    }

    public void build(Slot slot) {
        BlockPos centerBlock = slot.getCenter().getTerrainBlock();

        Vec orientation = slot.getOrientation(graph);
        Vec extensionOrientation = (orientation.axis() == Vec.Axis.X ? orientation.project(Vec.Axis.Z) : orientation.project(Vec.Axis.X)).mul(-1);

        BlockPos baseSize = houseBase.getSize();

        StructureBoundingBox bb = spawnStructure(houseBase, centerBlock, orientation.rotation());
        spawnAdjacent(bb, houseSmallExtension, orientation.inverse().rotation());
        bb = spawnAdjacent(bb, houseSmallExtension, extensionOrientation.rotation());
        bb = spawnAdjacent(bb, houseExtension, extensionOrientation.rotation());
        spawnAdjacent(bb, houseSmallExtension, orientation.rotation());

        bb = spawnStructure(houseBaseStilts, centerBlock.add(0, -houseBaseStilts.getSize().getY(), 0), orientation.rotation());
        spawnAdjacent(bb, houseSmallExtensionStilts, orientation.inverse().rotation());
        bb = spawnAdjacent(bb, houseSmallExtensionStilts, extensionOrientation.rotation());
        bb = spawnAdjacent(bb, houseExtensionStilts, extensionOrientation.rotation());
        spawnAdjacent(bb, houseSmallExtensionStilts, orientation.rotation());

        bb = spawnStructure(houseBaseRoof, centerBlock.add(0, baseSize.getY() - 1, 0), orientation.rotation());
        spawnAdjacent(bb, houseSmallExtensionRoof, orientation.inverse().rotation(), 2);
        bb = spawnAdjacent(bb, houseSmallExtensionRoof, extensionOrientation.rotation(), 2);
        bb = spawnAdjacent(bb, houseExtensionRoof, extensionOrientation.rotation(), 2);
        spawnAdjacent(bb, houseSmallExtensionRoof, orientation.rotation(), 2);
    }

    private StructureBoundingBox spawnStructure(Template template, BlockPos pos, Rotation rotation) {
        PlacementSettings settings = new PlacementSettings().setRotation(rotation);
        BlockPos size = template.getSize();

        pos = pos.add(Template.transformedBlockPos(settings, new BlockPos(-size.getX() / 2, 0, -size.getZ() / 2)));

        template.addBlocksToWorld(world, pos, settings);

        return new StructureBoundingBox(pos, Template.transformedBlockPos(settings, size.add(-1, -1, -1)).add(pos));
    }

    private StructureBoundingBox spawnAdjacent(StructureBoundingBox boundingBox, Template template, Rotation rotation) {
        return spawnAdjacent(boundingBox, template, rotation, 0);
    }

    private StructureBoundingBox spawnAdjacent(StructureBoundingBox boundingBox, Template template, Rotation rotation, int inset) {
        BlockPos pos;

        int offset = template.getSize().getX() / 2 - inset;

        switch (rotation) {
            case NONE:
                pos = new BlockPos(boundingBox.maxX + offset, boundingBox.minY, boundingBox.minZ + boundingBox.getZSize() / 2);
                break;
            case CLOCKWISE_90:
                pos = new BlockPos(boundingBox.maxX - boundingBox.getXSize() / 2, boundingBox.minY, boundingBox.maxZ + offset);
                break;
            case CLOCKWISE_180:
                pos = new BlockPos(boundingBox.minX - offset, boundingBox.minY, boundingBox.maxZ - boundingBox.getZSize() / 2);
                break;
            default:
                pos = new BlockPos(boundingBox.minX + boundingBox.getXSize() / 2, boundingBox.minY, boundingBox.minZ - offset);
                break;
        }

        return spawnStructure(template, pos, rotation);
    }

    private Template getTemplate(String name) {
        return templateManager.getTemplate(minecraftServer, new ResourceLocation(SettlementsMod.MOD_ID, name));
    }
}
