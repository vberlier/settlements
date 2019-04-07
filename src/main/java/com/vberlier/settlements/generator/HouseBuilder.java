package com.vberlier.settlements.generator;

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

    private final Template houseBase;
    private final Template houseBaseRoof;

    public HouseBuilder(World world) {
        this.world = world;
        worldServer = (WorldServer) world;
        minecraftServer = worldServer.getMinecraftServer();
        templateManager = worldServer.getStructureTemplateManager();

        houseBase = getTemplate("house_base");
        houseBaseRoof = getTemplate("house_base_roof");
    }

    public void build(Slot slot) {
        BlockPos center = slot.getCenter().getTerrainBlock();
        Vec normal = slot.getNormal();

        double east = normal.cross(Vec.east).length();
        double south = normal.cross(Vec.south).length();

        Rotation rotation;

        if (east < south) {
            rotation = normal.x > 0 ? Rotation.NONE : Rotation.CLOCKWISE_180;
        } else {
            rotation = normal.z > 0 ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90;
        }

        spawnStructure(houseBase, center, rotation);
        spawnStructure(houseBaseRoof, center.add(0, houseBase.getSize().getY() - 1, 0), rotation);
    }

    private void spawnStructure(Template template, BlockPos pos, Rotation rotation) {
        int factorX = -1;
        int factorZ = -1;

        switch (rotation) {
            case NONE:
                factorX = -1;
                factorZ = -1;
                break;
            case CLOCKWISE_90:
                factorX = 1;
                factorZ = -1;
                break;
            case CLOCKWISE_180:
                factorX = 1;
                factorZ = 1;
                break;
            case COUNTERCLOCKWISE_90:
                factorX = -1;
                factorZ = 1;
                break;
        }

        BlockPos size = template.getSize();

        pos = pos.add(factorX * size.getX() / 2, 0, factorZ * size.getZ() / 2);

        template.addBlocksToWorld(world, pos, new PlacementSettings().setRotation(rotation));
    }

    private Template getTemplate(String name) {
        return templateManager.getTemplate(minecraftServer, new ResourceLocation(SettlementsMod.MOD_ID, name));
    }
}
