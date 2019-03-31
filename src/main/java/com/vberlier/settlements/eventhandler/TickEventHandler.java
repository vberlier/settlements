package com.vberlier.settlements.eventhandler;

import com.vberlier.settlements.generator.JobProcessor;
import com.vberlier.settlements.SettlementsMod;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod.EventBusSubscriber
public class TickEventHandler {
    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            return;
        }

        JobProcessor jobProcessor = SettlementsMod.instance.getJobProcessor();

        if (jobProcessor != null) {
            jobProcessor.process();
        }
    }
}
