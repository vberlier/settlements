package com.vberlier.settlements.generator;

import com.vberlier.settlements.SettlementsMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
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
        buildWalls(slot);
        buildRoof(slot);
    }

    private void buildWalls(Slot slot) {
        BlockPos size = houseBase.getSize();
        BlockPos pos = slot.getCenter().getTerrainBlock().add(-size.getX() / 2, 0, -size.getZ() / 2);

        houseBase.addBlocksToWorld(world, pos, new PlacementSettings());
    }

    private void buildRoof(Slot slot) {
        BlockPos size = houseBaseRoof.getSize();
        BlockPos pos = slot.getCenter().getTerrainBlock().add(-size.getX() / 2, houseBase.getSize().getY() - 1, -size.getZ() / 2);

        houseBaseRoof.addBlocksToWorld(world, pos, new PlacementSettings());
    }

    private Template getTemplate(String name) {
        return templateManager.getTemplate(minecraftServer, new ResourceLocation(SettlementsMod.MOD_ID, name));
    }
}
