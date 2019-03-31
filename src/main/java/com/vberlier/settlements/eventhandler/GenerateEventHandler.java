package com.vberlier.settlements.eventhandler;

import com.vberlier.settlements.generator.Generator;
import com.vberlier.settlements.event.SettlementEvent;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
public class GenerateEventHandler {
    @SubscribeEvent
    public static void generate(SettlementEvent.Generate event) {
        World world = event.getWorld();

        if (world.isRemote) {
            return;
        }

        Generator generator = new Generator(world, event.getBoundingBox());
        generator.buildSettlement();
    }
}
