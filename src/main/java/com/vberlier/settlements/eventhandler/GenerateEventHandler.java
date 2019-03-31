package com.vberlier.settlements.eventhandler;

import com.vberlier.settlements.Generator;
import com.vberlier.settlements.SettlementsMod;
import com.vberlier.settlements.event.SettlementEvent;
import com.vberlier.settlements.job.BuildSettlement;
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

        Generator generator = SettlementsMod.instance.getGenerator();

        if (generator != null) {
            generator.submit(new BuildSettlement(world, event.getBoundingBox()));
        }
    }
}
