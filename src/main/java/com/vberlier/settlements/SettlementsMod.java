package com.vberlier.settlements;

import com.vberlier.settlements.command.CommandBuildSettlement;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = SettlementsMod.MOD_ID, name = SettlementsMod.NAME, version = SettlementsMod.VERSION)
public class SettlementsMod {
    public static final String MOD_ID = "settlements";
    public static final String NAME = "Settlements Mod";
    public static final String VERSION = "0.1.0";

    @Mod.Instance(MOD_ID)
    public static SettlementsMod instance;

    private Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("Initializing {} {}", MOD_ID, VERSION);
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandBuildSettlement());
    }

    public Logger getLogger() {
        return logger;
    }
}
