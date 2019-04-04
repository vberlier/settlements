package com.vberlier.settlements.command;

import com.vberlier.settlements.event.SettlementEvent;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandBuildSettlement extends CommandBase {
    @Nonnull
    public String getName() {
        return "BuildSettlement";
    }

    @Nonnull
    public List<String> getAliases() {
        return Arrays.asList("build");
    }

    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Nonnull
    public String getUsage(ICommandSender sender) {
        return "commands.buildsettlement.usage";
    }

    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        World world = sender.getEntityWorld();

        if (world.isRemote) {
            return;
        }

        if (args.length != 6) {
            throw new WrongUsageException(getUsage(sender));
        }

        BlockPos blockPos1 = parseBlockPos(sender, args, 0, false);
        BlockPos blockPos2 = parseBlockPos(sender, args, 3, false);

        StructureBoundingBox boundingBox = new StructureBoundingBox(blockPos1, blockPos2);

        if (world.isAreaLoaded(boundingBox)) {
            try {
                MinecraftForge.EVENT_BUS.post(new SettlementEvent.Generate(world, boundingBox));
            } catch (Throwable e) {
                e.printStackTrace();
                throw e;
            }
        } else {
            throw new CommandException("commands.buildsettlement.outOfWorld");
        }
    }

    @Nonnull
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length > 0 && args.length <= 3) {
            return getTabCompletionCoordinate(args, 0, targetPos);
        } else if (args.length > 3 && args.length <= 6) {
            return getTabCompletionCoordinate(args, 3, targetPos);
        } else {
            return Collections.emptyList();
        }
    }
}
