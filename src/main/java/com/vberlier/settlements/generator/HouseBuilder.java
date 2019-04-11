package com.vberlier.settlements.generator;

import com.google.common.graph.ValueGraph;
import com.vberlier.settlements.SettlementsMod;
import com.vberlier.settlements.util.Vec;
import net.minecraft.block.*;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;

public class HouseBuilder {
    private final World world;
    private final TemplateManager templateManager;
    private ValueGraph<Slot, Integer> graph;
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

    private final Template houseBase;
    private final Template houseBaseFoundation;
    private final Template houseBaseRoof;
    private final Template houseBaseStilts;
    private final Template houseExtension;
    private final Template houseExtensionFoundation;
    private final Template houseExtensionRoof;
    private final Template houseExtensionStilts;
    private final Template houseSmallExtension;
    private final Template houseSmallExtensionFoundation;
    private final Template houseSmallExtensionRoof;
    private final Template houseSmallExtensionStilts;

    public HouseBuilder(World world, ValueGraph<Slot, Integer> graph, BlockPlanks.EnumType woodVariant) {
        this.world = world;
        templateManager = world.getSaveHandler().getStructureTemplateManager();
        this.graph = graph;
        this.woodVariant = woodVariant;

        houseBase = getTemplate("house_base");
        houseBaseFoundation = getTemplate("house_base_foundation");
        houseBaseStilts = getTemplate("house_base_stilts");
        houseBaseRoof = getTemplate("house_base_roof");
        houseExtension = getTemplate("house_extension");
        houseExtensionFoundation = getTemplate("house_extension_foundation");
        houseExtensionStilts = getTemplate("house_extension_stilts");
        houseExtensionRoof = getTemplate("house_extension_roof");
        houseSmallExtension = getTemplate("house_small_extension");
        houseSmallExtensionFoundation = getTemplate("house_small_extension_foundation");
        houseSmallExtensionStilts = getTemplate("house_small_extension_stilts");
        houseSmallExtensionRoof = getTemplate("house_small_extension_roof");

        for (Template template : new Template[]{houseBase, houseBaseFoundation, houseBaseStilts, houseBaseRoof, houseExtension, houseExtensionFoundation, houseExtensionStilts, houseExtensionRoof, houseSmallExtension, houseSmallExtensionFoundation, houseSmallExtensionStilts, houseSmallExtensionRoof}) {
            System.out.println("template: " + template.getAuthor() + " " + template.getSize());
        }
    }

    public void build(Slot slot) {
        BlockPos centerBlock = slot.getCenter().getTerrainBlock();

        Vec orientation = slot.getOrientation(graph);
        Vec extensionOrientation = (orientation.axis() == Vec.Axis.X ? orientation.project(Vec.Axis.Z) : orientation.project(Vec.Axis.X)).mul(-1);

        BlockPos baseSize = houseBase.getSize();

        StructureBoundingBox bb = spawnStructure(houseBase, centerBlock, orientation.rotation());
        spawnAdjacent(bb, houseSmallExtension, extensionOrientation.rotation());

        // TODO: Use stilts for steep surfaces

        bb = spawnStructure(houseBaseFoundation, centerBlock.add(0, -houseBaseFoundation.getSize().getY(), 0), orientation.rotation());
        spawnAdjacent(bb, houseSmallExtensionFoundation, extensionOrientation.rotation());

        bb = spawnStructure(houseBaseRoof, centerBlock.add(0, baseSize.getY() - 1, 0), orientation.rotation());
        spawnAdjacent(bb, houseSmallExtensionRoof, extensionOrientation.rotation(), 2);

        colorIndex = (colorIndex + 1) % colors.length;
    }

    private StructureBoundingBox spawnStructure(Template template, BlockPos pos, Rotation rotation) {
        PlacementSettings settings = new PlacementSettings().setRotation(rotation);
        BlockPos size = template.getSize();

        pos = pos.add(Template.transformedBlockPos(settings, new BlockPos(-size.getX() / 2, 0, -size.getZ() / 2)));

        template.addBlocksToWorld(world, pos, settings);

        StructureBoundingBox bb = new StructureBoundingBox(pos, Template.transformedBlockPos(settings, size.add(-1, -1, -1)).add(pos));

        replaceColor(bb);
        replaceWood(bb);

        return bb;
    }

    private StructureBoundingBox spawnAdjacent(StructureBoundingBox boundingBox, Template template, Rotation rotation) {
        return spawnAdjacent(boundingBox, template, rotation, 0);
    }

    private StructureBoundingBox spawnAdjacent(StructureBoundingBox boundingBox, Template template, Rotation rotation, int inset) {
        BlockPos pos;

        int offset = template.getSize().getX() / 2 - inset;

        switch (rotation) {
            case NONE:
                pos = new BlockPos(boundingBox.maxX + offset, boundingBox.minY, boundingBox.minZ + boundingBox.getZSize() / 2);
                break;
            case CLOCKWISE_90:
                pos = new BlockPos(boundingBox.maxX - boundingBox.getXSize() / 2, boundingBox.minY, boundingBox.maxZ + offset);
                break;
            case CLOCKWISE_180:
                pos = new BlockPos(boundingBox.minX - offset, boundingBox.minY, boundingBox.maxZ - boundingBox.getZSize() / 2);
                break;
            default:
                pos = new BlockPos(boundingBox.minX + boundingBox.getXSize() / 2, boundingBox.minY, boundingBox.minZ - offset);
                break;
        }

        return spawnStructure(template, pos, rotation);
    }

    private Template getTemplate(String name) {
        return templateManager.getTemplate(world.getMinecraftServer(), new ResourceLocation(SettlementsMod.MOD_ID, name));
    }

    private void replaceColor(StructureBoundingBox bb) {
        BlockPos min = new BlockPos(bb.minX, bb.minY, bb.minZ);
        BlockPos max = new BlockPos(bb.maxX, bb.maxY, bb.maxZ);

        for (BlockPos pos : BlockPos.getAllInBox(min, max)) {
            IBlockState state = world.getBlockState(pos);
            Block block = state.getBlock();

            if (block instanceof BlockColored) {
                world.setBlockState(pos, state.withProperty(BlockColored.COLOR, colors[colorIndex]));
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
}
