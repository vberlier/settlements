package com.vberlier.settlements.generator;

import com.vberlier.settlements.util.Point;
import com.vberlier.settlements.util.astar.AStar;
import com.vberlier.settlements.util.astar.Node;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import java.util.ArrayList;
import java.util.List;

public class PathBuilder {
    private final World world;
    private final Position[][] terrain;
    private int originX;
    private int originZ;
    private int sizeX;
    private int sizeZ;
    private List<StructureBoundingBox> hitboxes;
    private ArrayList<Integer[]> blocks;

    private final BlockPlanks.EnumType woodVariant;

    public PathBuilder(World world, Position[][] terrain, int originX, int originZ, int sizeX, int sizeZ, BlockPlanks.EnumType woodVariant) {
        this.world = world;
        this.terrain = terrain;
        this.originX = originX;
        this.originZ = originZ;
        this.sizeX = sizeX;
        this.sizeZ = sizeZ;
        this.woodVariant = woodVariant;

        hitboxes = new ArrayList<>();
        blocks = new ArrayList<>();
    }

    public void build(Slot fromSlot, Slot toSlot) {
        BlockPos current = fromSlot.getAnchor();
        BlockPos target = toSlot.getAnchor();

        AStar astar = new AStar(
                sizeX,
                sizeZ,
                new Node(current.getX() - originX, current.getZ() - originZ),
                new Node(target.getX() - originX, target.getZ() - originZ)
        );

        int[][] blocksArray = new int[blocks.size()][];

        for (int i = 0; i < blocks.size(); i++) {
            Integer[] block = blocks.get(i);
            blocksArray[i] = new int[] {block[0], block[1]};
        }

        astar.setBlocks(blocksArray);

        List<Node> path = astar.findPath();

        if (!path.isEmpty()) {
            for (Node node : path) {
                BlockPos pos = new BlockPos(node.getRow() + originX, 100, node.getCol() + originZ);
                world.setBlockState(pos, Blocks.IRON_BLOCK.getDefaultState());
            }
        } else {
            for (Point point : new Point(current.getX(), current.getZ()).line(new Point(target.getX(), target.getZ()))) {
                world.setBlockState(new BlockPos(point.x, 100, point.y), Blocks.REDSTONE_BLOCK.getDefaultState());
            }
        }
    }

    public void computeBlocks() {
        for (StructureBoundingBox hitbox : hitboxes) {
            for (int x = hitbox.minX - 1; x < hitbox.maxX + 1; x++) {
                for (int z = hitbox.minZ - 1; z < hitbox.maxZ + 1; z++) {
                    blocks.add(new Integer[] {x - originX, z - originZ});
                }
            }
        }

        for (int i = 0; i < sizeX; i++) {
            blocks.add(new Integer[] {i, 0});
            blocks.add(new Integer[] {i, sizeZ - 1});
        }

        for (int i = 0; i < sizeZ; i++) {
            blocks.add(new Integer[] {0, i});
            blocks.add(new Integer[] {sizeX - 1, i});
        }

        for (int i = 1; i < sizeX - 1; i++) {
            for (int j = 1; j < sizeZ - 1; j++) {
                Integer[] block = {i, j};
                Position position = terrain[i][j];

                if (position.getLiquids().size() > 0) {
                    blocks.add(block);
                }

                for (int[] offset : Position.neighbors) {
                    Position neighbor = terrain[i + offset[0]][j + offset[1]];

                    if (neighbor.getLiquids().size() > 0) {
                        blocks.add(block);
                    }
                }
            }
        }
    }

    public BlockPos setBlockOrSlab(BlockPos pos) {
        BlockPos below = pos.add(0, -1, 0);

        if (world.isAirBlock(pos) && !world.isAirBlock(below) && setBlock(below)) {
            for (int[] offset : Position.neighbors) {
                BlockPos neighbor = pos.add(offset[0], 0, offset[1]);

                if (!world.isAirBlock(neighbor) && !(world.getBlockState(neighbor).getBlock() instanceof BlockSlab)) {
                    setSlab(pos);
                    break;
                }
            }
            return below;
        } else {
            if (setBlock(pos)) {
                return pos;
            }
        }

        return null;
    }

    public boolean setBlock(BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (!world.isAirBlock(pos.add(0, 1, 0))) {
            return false;
        }

        if (block instanceof BlockDirt || block instanceof BlockGrass || block instanceof BlockSand) {
            world.setBlockState(pos, randomGrassyState());
            world.setBlockState(pos.add(0, -1, 0), Blocks.DIRT.getDefaultState());
        } else if (block instanceof BlockStone) {
            world.setBlockState(pos, randomStonyState());
        } else if (world.containsAnyLiquid(new AxisAlignedBB(pos))) {
            world.setBlockState(pos, Blocks.PLANKS.getDefaultState().withProperty(BlockPlanks.VARIANT, woodVariant));
        } else {
            return false;
        }

        return true;
    }

    public boolean setPlanks(BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (world.containsAnyLiquid(new AxisAlignedBB(pos))) {
            world.setBlockState(pos, Blocks.PLANKS.getDefaultState().withProperty(BlockPlanks.VARIANT, woodVariant));
        } else if (world.isAirBlock(pos)) {
            world.setBlockState(pos, Blocks.WOODEN_SLAB.getDefaultState().withProperty(BlockPlanks.VARIANT, woodVariant).withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP));
        } else {
            return false;
        }

        return true;
    }

    public boolean setSlab(BlockPos pos) {
        BlockPos below = pos.add(0, -1, 0);

        IBlockState state = world.getBlockState(below);
        Block block = state.getBlock();

        if (block instanceof BlockDirt || block instanceof BlockGrass || block instanceof BlockGrassPath) {
            world.setBlockState(pos, Blocks.WOODEN_SLAB.getDefaultState().withProperty(BlockPlanks.VARIANT, woodVariant));
        } else if (block instanceof BlockGravel || block == Blocks.COBBLESTONE || block instanceof BlockStone) {
            world.setBlockState(pos, Blocks.STONE_SLAB.getDefaultState().withProperty(BlockStoneSlab.VARIANT, BlockStoneSlab.EnumType.COBBLESTONE));
        } else {
            return false;
        }

        return true;
    }

    private IBlockState randomGrassyState() {
        switch (world.rand.nextInt(7)) {
            case 0:
                return Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT);
            case 1:
                return Blocks.GRAVEL.getDefaultState();
            case 2:
                return Blocks.GRASS.getDefaultState();
            default:
                return Blocks.GRASS_PATH.getDefaultState();
        }
    }

    private IBlockState randomStonyState() {
        switch (world.rand.nextInt(7)) {
            case 0:
                return Blocks.GRAVEL.getDefaultState();
            case 1:
                return Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT);
            case 2:
                return Blocks.STONE.getDefaultState();
            default:
                return Blocks.COBBLESTONE.getDefaultState();
        }
    }

    public void setHitboxes(List<StructureBoundingBox> hitboxes) {
        this.hitboxes = hitboxes;
    }

    public List<StructureBoundingBox> getHitboxes() {
        return hitboxes;
    }
}
