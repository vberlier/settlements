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
    private final Template houseExtension;
    private final Template houseExtensionRoof;
    private final Template houseSmallExtension;
    private final Template houseSmallExtensionRoof;

    public HouseBuilder(World world, MutableValueGraph<Slot, Integer> graph) {
        this.world = world;
        worldServer = (WorldServer) world;
        minecraftServer = worldServer.getMinecraftServer();
        templateManager = worldServer.getStructureTemplateManager();
        this.graph = graph;

        houseBase = getTemplate("house_base");
        houseBaseRoof = getTemplate("house_base_roof");
        houseExtension = getTemplate("house_extension");
        houseExtensionRoof = getTemplate("house_extension_roof");
        houseSmallExtension = getTemplate("house_small_extension");
        houseSmallExtensionRoof = getTemplate("house_small_extension_roof");
    }

    public void build(Slot slot) {
        Position center = slot.getCenter();
        BlockPos centerBlock = center.getTerrainBlock();

        Vec orientation = slot.getNormal().mul(-1);

        for (Slot adjacentNode : graph.adjacentNodes(slot)) {
            orientation = orientation.add(new Vec(adjacentNode.getCenter().getTerrainBlock()).sub(centerBlock).normalize());
        }

        double east = orientation.cross(Vec.east).length();
        double south = orientation.cross(Vec.south).length();

        Rotation rotation;

        int expandX;
        int expandZ;
        Rotation expandRotation;

        if (east < south) {
            rotation = orientation.x > 0 ? Rotation.NONE : Rotation.CLOCKWISE_180;
            expandX = 0;
            expandZ = orientation.z > 0 ? -1 : 1;
            expandRotation = expandZ > 0 ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90;
        } else {
            rotation = orientation.z > 0 ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90;
            expandX = orientation.x > 0 ? -1 : 1;
            expandZ = 0;
            expandRotation = expandX > 0 ? Rotation.NONE : Rotation.CLOCKWISE_180;
        }

        BlockPos baseSize = houseBase.getSize();

        spawnStructure(houseBase, centerBlock, rotation);

        BlockPos expansionPos = centerBlock.add((baseSize.getX() / 2 + houseExtension.getSize().getX() / 2) * expandX, 0, (baseSize.getX() / 2 + houseExtension.getSize().getX() / 2) * expandZ);
        spawnStructure(houseExtension, expansionPos, expandRotation);

        spawnStructure(houseBaseRoof, centerBlock.add(0, baseSize.getY() - 1, 0), rotation);
        spawnStructure(houseExtensionRoof, expansionPos.add(0, houseExtension.getSize().getY() - 1, 0), expandRotation);
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

    private Rotation rotate(Rotation rotation, int angle) {
        angle += 360;

        while (angle > 0) {
            angle -= 90;

            switch (rotation) {
                case NONE:
                    rotation = Rotation.COUNTERCLOCKWISE_90;
                    break;
                case CLOCKWISE_90:
                    rotation = Rotation.NONE;
                    break;
                case CLOCKWISE_180:
                    rotation = Rotation.CLOCKWISE_90;
                    break;
                default:
                    rotation = Rotation.CLOCKWISE_180;
                    break;
            }
        }

        return rotation;
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
