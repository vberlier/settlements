package com.vberlier.settlements.generator;

import com.vberlier.settlements.SettlementsMod;
import net.minecraft.block.*;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemDoor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;

public class StructureBuilder {
    protected final World world;
    private final TemplateManager templateManager;
    private BlockPlanks.EnumType woodVariant;

    private int colorIndex = 0;
    private final EnumDyeColor[] colors = {
            EnumDyeColor.WHITE,
            EnumDyeColor.YELLOW,
            EnumDyeColor.ORANGE,
            EnumDyeColor.LIGHT_BLUE,
            EnumDyeColor.LIME,
            EnumDyeColor.PINK,
    };

    public StructureBuilder(World world, BlockPlanks.EnumType woodVariant) {
        this.world = world;
        templateManager = world.getSaveHandler().getStructureTemplateManager();
        this.woodVariant = woodVariant;
    }

    public StructureBoundingBox spawnStructure(Template template, BlockPos pos, Rotation rotation) {
        PlacementSettings settings = new PlacementSettings().setRotation(rotation);
        BlockPos size = template.getSize();

        pos = pos.add(Template.transformedBlockPos(settings, new BlockPos(-size.getX() / 2, 0, -size.getZ() / 2)));

        template.addBlocksToWorld(world, pos, settings);

        StructureBoundingBox bb = new StructureBoundingBox(pos, Template.transformedBlockPos(settings, size.add(-1, -1, -1)).add(pos));

        replaceColor(bb);
        replaceWood(bb);

        return bb;
    }

    public StructureBoundingBox spawnAdjacent(StructureBoundingBox boundingBox, Template template, Rotation rotation) {
        return spawnAdjacent(boundingBox, template, rotation, 0);
    }

    public StructureBoundingBox spawnAdjacent(StructureBoundingBox boundingBox, Template template, Rotation rotation, int inset) {
        BlockPos pos = adjacentBlock(boundingBox, rotation, template.getSize().getX() / 2 - inset);
        return spawnStructure(template, pos, rotation);
    }

    public static BlockPos adjacentBlock(StructureBoundingBox boundingBox, Rotation rotation) {
        return adjacentBlock(boundingBox, rotation, 0);
    }

    public static BlockPos adjacentBlock(StructureBoundingBox boundingBox, Rotation rotation, int offset) {
        switch (rotation) {
            case NONE:
                return new BlockPos(boundingBox.maxX + offset, boundingBox.minY, boundingBox.minZ + boundingBox.getZSize() / 2);
            case CLOCKWISE_90:
                return new BlockPos(boundingBox.maxX - boundingBox.getXSize() / 2, boundingBox.minY, boundingBox.maxZ + offset);
            case CLOCKWISE_180:
                return new BlockPos(boundingBox.minX - offset, boundingBox.minY, boundingBox.maxZ - boundingBox.getZSize() / 2);
            default:
                return new BlockPos(boundingBox.minX + boundingBox.getXSize() / 2, boundingBox.minY, boundingBox.minZ - offset);
        }
    }

    public Template getTemplate(String name) {
        Template template = templateManager.getTemplate(world.getMinecraftServer(), new ResourceLocation(SettlementsMod.MOD_ID, name));

        if (template.getSize().equals(new BlockPos(0, 0, 0))) {
            System.out.println("Failed to load structure: " + name);
        }

        return template;
    }

    private void replaceColor(StructureBoundingBox bb) {
        BlockPos min = new BlockPos(bb.minX, bb.minY, bb.minZ);
        BlockPos max = new BlockPos(bb.maxX, bb.maxY, bb.maxZ);

        for (BlockPos pos : BlockPos.getAllInBox(min, max)) {
            IBlockState state = world.getBlockState(pos);
            Block block = state.getBlock();

            if (block instanceof BlockColored) {
                world.setBlockState(pos, state.withProperty(BlockColored.COLOR, colors[colorIndex]));
            } else if (block instanceof BlockStainedGlass) {
                world.setBlockState(pos, state.withProperty(BlockStainedGlass.COLOR, colors[colorIndex]));
            } else if (block instanceof BlockStainedGlassPane) {
                world.setBlockState(pos, state.withProperty(BlockStainedGlassPane.COLOR, colors[colorIndex]));
            }
        }
    }

    private void replaceWood(StructureBoundingBox bb) {
        BlockPos min = new BlockPos(bb.minX, bb.minY, bb.minZ);
        BlockPos max = new BlockPos(bb.maxX, bb.maxY, bb.maxZ);

        for (BlockPos pos : BlockPos.getAllInBox(min, max)) {
            IBlockState state = world.getBlockState(pos);
            Block block = state.getBlock();

            if (block instanceof BlockLog) {
                IBlockState newState;

                if (woodVariant == BlockPlanks.EnumType.ACACIA || woodVariant == BlockPlanks.EnumType.DARK_OAK) {
                    newState = Blocks.LOG2.getDefaultState().withProperty(BlockNewLog.VARIANT, woodVariant);
                } else {
                    newState = Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, woodVariant);
                }

                for (IProperty property : state.getProperties().keySet()) {
                    if (!property.getName().equals("variant")) {
                        newState = newState.withProperty(property, state.getValue(property));
                    }
                }

                world.setBlockState(pos, newState);
            } else if (block == Blocks.OAK_STAIRS || block == Blocks.SPRUCE_STAIRS || block == Blocks.BIRCH_STAIRS || block == Blocks.JUNGLE_STAIRS || block == Blocks.ACACIA_STAIRS || block == Blocks.DARK_OAK_STAIRS) {
                IBlockState newState;

                switch (woodVariant) {
                    case SPRUCE:
                        newState = Blocks.SPRUCE_STAIRS.getDefaultState();
                        break;
                    case BIRCH:
                        newState = Blocks.BIRCH_STAIRS.getDefaultState();
                        break;
                    case JUNGLE:
                        newState = Blocks.JUNGLE_STAIRS.getDefaultState();
                        break;
                    case ACACIA:
                        newState = Blocks.ACACIA_STAIRS.getDefaultState();
                        break;
                    case DARK_OAK:
                        newState = Blocks.DARK_OAK_STAIRS.getDefaultState();
                        break;
                    default:
                        newState = Blocks.OAK_STAIRS.getDefaultState();
                        break;
                }

                for (IProperty property : state.getProperties().keySet()) {
                    if (!property.getName().equals("variant")) {
                        newState = newState.withProperty(property, state.getValue(property));
                    }
                }

                world.setBlockState(pos, newState);
            } else {
                for (IProperty property : state.getProperties().keySet()) {
                    if (property.getValueClass() == BlockPlanks.EnumType.class) {
                        world.setBlockState(pos, state.withProperty(property, woodVariant));
                        break;
                    }
                }
            }
        }
    }

    public void rotateColor() {
        colorIndex = (colorIndex + 1) % colors.length;
    }

    public StructureBoundingBox computeAvailableSpace(BlockPos centerBlock, double maxRadius) {
        int y = centerBlock.getY();

        int minX = centerBlock.getX();
        int maxX = centerBlock.getX();
        int minZ = centerBlock.getZ();
        int maxZ = centerBlock.getZ();

        boolean changed = true;

        while (changed) {
            boolean expandNorth = centerBlock.getZ() - minZ < maxRadius;
            boolean expandSouth = maxZ - centerBlock.getZ() < maxRadius;

            for (int x = minX; x < maxX; x++) {
                BlockPos north = new BlockPos(x, y, minZ - 1);
                BlockPos south = new BlockPos(x, y, maxZ + 1);

                if (expandNorth && !world.isAirBlock(north)) {
                    expandNorth = false;
                }

                if (expandSouth && !world.isAirBlock(south)) {
                    expandSouth = false;
                }
            }

            boolean expandWest = centerBlock.getX() - minX < maxRadius;
            boolean expandEast = maxX - centerBlock.getX() < maxRadius;

            for (int z = minZ; z < maxZ; z++) {
                BlockPos west = new BlockPos(minX - 1, y, z);
                BlockPos east = new BlockPos(maxX + 1, y, z);

                if (expandWest && !world.isAirBlock(west)) {
                    expandWest = false;
                }

                if (expandEast && !world.isAirBlock(east)) {
                    expandEast = false;
                }
            }

            if (expandNorth) {
                minZ--;
            }

            if (expandSouth) {
                maxZ++;
            }

            if (expandWest) {
                minX--;
            }

            if (expandEast) {
                maxX++;
            }

            changed = expandNorth || expandSouth || expandWest || expandEast;
        }

        return new StructureBoundingBox(minX, 0, minZ, maxX, 255, maxZ);
    }
}
