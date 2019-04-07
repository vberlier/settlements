package com.vberlier.settlements.generator;

import com.google.common.graph.MutableValueGraph;
import com.vberlier.settlements.SettlementsMod;
import com.vberlier.settlements.util.Vec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;

public class HouseBuilder {
    private final World world;
    private final WorldServer worldServer;
    private final MinecraftServer minecraftServer;
    private final TemplateManager templateManager;
    private MutableValueGraph<Slot, Integer> graph;

    private final Template houseBase;
    private final Template houseBaseRoof;

    public HouseBuilder(World world, MutableValueGraph<Slot, Integer> graph) {
        this.world = world;
        worldServer = (WorldServer) world;
        minecraftServer = worldServer.getMinecraftServer();
        templateManager = worldServer.getStructureTemplateManager();
        this.graph = graph;

        houseBase = getTemplate("house_base");
        houseBaseRoof = getTemplate("house_base_roof");
    }

    public void build(Slot slot) {
        BlockPos center = slot.getCenter().getTerrainBlock();

        Vec orientation = slot.getNormal();

        for (Slot adjacentNode : graph.adjacentNodes(slot)) {
            orientation = orientation.add(new Vec(adjacentNode.getCenter().getTerrainBlock()).sub(center).normalize());
        }

        double east = orientation.cross(Vec.east).length();
        double south = orientation.cross(Vec.south).length();

        Rotation rotation;

        if (east < south) {
            rotation = orientation.x > 0 ? Rotation.NONE : Rotation.CLOCKWISE_180;
        } else {
            rotation = orientation.z > 0 ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90;
        }

        spawnStructure(houseBase, center, rotation);
        spawnStructure(houseBaseRoof, center.add(0, houseBase.getSize().getY() - 1, 0), rotation);
    }

    private int[] getRotationFactor(Rotation rotation) {
        switch (rotation) {
            case NONE:
                return new int[]{-1, -1};
            case CLOCKWISE_90:
                return new int[]{1, -1};
            case CLOCKWISE_180:
                return new int[]{1, 1};
            default:
                return new int[]{-1, 1};
        }
    }

    private void spawnStructure(Template template, BlockPos pos, Rotation rotation) {
        int[] rotationFactor = getRotationFactor(rotation);
        int factorX = rotationFactor[0];
        int factorZ = rotationFactor[1];

        BlockPos size = template.getSize();

        pos = pos.add(factorX * size.getX() / 2, 0, factorZ * size.getZ() / 2);

        template.addBlocksToWorld(world, pos, new PlacementSettings().setRotation(rotation));
    }

    private Template getTemplate(String name) {
        return templateManager.getTemplate(minecraftServer, new ResourceLocation(SettlementsMod.MOD_ID, name));
    }
}
