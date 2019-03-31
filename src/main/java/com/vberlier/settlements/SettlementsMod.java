package com.vberlier.settlements;

import com.vberlier.settlements.command.CommandBuildSettlement;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

@Mod(modid = SettlementsMod.MODID, name = SettlementsMod.NAME, version = SettlementsMod.VERSION)
public class SettlementsMod {
    public static final String MODID = "settlements";
    public static final String NAME = "Settlements Mod";
    public static final String VERSION = "0.1.0";

    private static Logger logger;

    @Mod.Instance(MODID)
    public static SettlementsMod instance;

    private Generator generator;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("Initializing {} {}", MODID, VERSION);
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandBuildSettlement());

        logger.info("Creating generator");
        generator = new Generator(logger);
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        logger.info("Resetting generator");
        generator = null;
    }

    @Nullable
    public Generator getGenerator() {
        return generator;
    }
}
