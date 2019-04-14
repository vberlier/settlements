package com.vberlier.settlements.generator;

import com.google.common.graph.ValueGraph;
import com.vberlier.settlements.util.Vec;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.template.Template;

import java.util.HashSet;
import java.util.Set;

public class HouseBuilder extends StructureBuilder {
    private final ValueGraph<Slot, Vec> graph;
    private final PathBuilder pathBuilder;

    private final Template houseDoor;
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

    private final int wallsHeight;
    private final int foundationHeight;
    private final int stiltsHeight;
    private final int roofHeight;

    private final int extensionLength;
    private final int smallExtensionLength;

    public HouseBuilder(World world, ValueGraph<Slot, Vec> graph, BlockPlanks.EnumType woodVariant, PathBuilder pathBuilder) {
        super(world, woodVariant);
        this.graph = graph;
        this.pathBuilder = pathBuilder;

        houseDoor = getTemplate("house_door");
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

        wallsHeight = houseBase.getSize().getY();
        foundationHeight = houseBaseFoundation.getSize().getY();
        stiltsHeight = houseBaseStilts.getSize().getY();
        roofHeight = houseBaseRoof.getSize().getY();

        extensionLength = houseExtensionRoof.getSize().getX() - 1;
        smallExtensionLength = houseSmallExtensionRoof.getSize().getX() - 1;
    }

    public void build(Slot slot, double maxRadius) {
        BlockPos centerBlock = slot.getCenter().getTerrainBlock();

        Vec orientation = slot.getOrientation(graph);
        Vec extensionOrientation = (orientation.axis() == Vec.Axis.X ? orientation.project(Vec.Axis.Z) : orientation.project(Vec.Axis.X)).mul(-1);

        double factor = slot.getVerticality() * slot.getDryness();

        StructureBoundingBox availableSpace = computeAvailableSpace(centerBlock.add(0, 2 * wallsHeight / 3, 0), maxRadius * factor);

        StructureBoundingBox walls = spawnStructure(houseBase, centerBlock, orientation.rotation());
        StructureBoundingBox foundation = spawnStructure(houseBaseStilts, centerBlock.add(0, -stiltsHeight, 0), orientation.rotation());
        StructureBoundingBox roof = spawnStructure(houseBaseRoof, centerBlock.add(0, wallsHeight - 1, 0), orientation.rotation());

        StructureBoundingBox doorBoundingBox = spawnAdjacent(walls, houseDoor, orientation.rotation());

        Vec[] orientationsArray = {
                extensionOrientation,
                orientation,
                extensionOrientation.mul(-1),
                orientation.mul(-1)
        };

        int n = (int) (5 * (availableSpace.getXSize() * availableSpace.getZSize() / Math.pow(2 * maxRadius, 2))) + world.rand.nextInt(2);

        for (int i = 0; i < orientationsArray.length; i++) {
            Rotation rotation = orientationsArray[i].rotation();

            if (n > 3 && !rotation.equals(orientation.rotation()) && availableSpace.isVecInside(adjacentBlock(walls, rotation, smallExtensionLength + extensionLength))) {
                StructureBoundingBox[] boxes = spawnSmallExtension(walls, foundation, roof, rotation);
                boxes = spawnExtension(boxes[0], boxes[1], boxes[2], rotation);
                n -= 3;

                Rotation angleRotation = orientationsArray[(i + 1) % orientationsArray.length].rotation();

                if (n > 2 && availableSpace.isVecInside(adjacentBlock(boxes[0], angleRotation, extensionLength)) && world.rand.nextBoolean()) {
                    spawnExtension(boxes[0], boxes[1], boxes[2], angleRotation);
                    i++;
                    n -= 2;
                } else if (availableSpace.isVecInside(adjacentBlock(boxes[0], angleRotation, smallExtensionLength))) {
                    spawnSmallExtension(boxes[0], boxes[1], boxes[2], angleRotation);
                    i++;
                    n--;
                }
            } else if (n > 2 && !rotation.equals(orientation.rotation()) && availableSpace.isVecInside(adjacentBlock(walls, rotation, extensionLength))) {
                spawnExtension(walls, foundation, roof, rotation);
                n -= 2;
            } else if (availableSpace.isVecInside(adjacentBlock(walls, rotation, smallExtensionLength))) {
                StructureBoundingBox[] boxes = spawnSmallExtension(walls, foundation, roof, rotation);

                if (rotation.equals(orientation.rotation())) {
                    doorBoundingBox = spawnAdjacent(boxes[0], houseDoor, rotation);
                }

                n--;
            }

            if (n <= 0) {
                break;
            }
        }

        BlockPos anchor = buildHouseFront(new Vec(doorBoundingBox.minX, doorBoundingBox.minY, doorBoundingBox.minZ), orientation);
        slot.setAnchor(anchor);

        world.setBlockState(anchor, Blocks.REDSTONE_BLOCK.getDefaultState());

        rotateColor();
    }

    private BlockPos buildHouseFront(Vec housePosition, Vec orientation) {
        Vec sideways = orientation.cross(Vec.up).normalize();
        Vec cornerOffset = sideways.mul(2);

        Vec[] pillars = new Vec[]{housePosition.sub(cornerOffset).add(orientation.mul(3)), housePosition.add(cornerOffset).add(orientation.mul(3))};

        double groundDistance = 0;

        for (Vec pillar : pillars) {
            Vec ground = pillar.add(Vec.down);

            while (world.isAirBlock(ground.block()) || world.containsAnyLiquid(new AxisAlignedBB(ground.block()))) {
                ground = ground.add(Vec.down);
            }

            groundDistance = Math.max(pillar.sub(ground).length(), groundDistance);
        }

        if (groundDistance > 2) {
            for (Vec pillar : pillars) {
                IBlockState state = getLogBlockState();
                for (int i = -1; i <= groundDistance + 1; i++) {
                    world.setBlockState(pillar.add(0, -i, 0).block(), state);
                }
            }

            for (double i = -2; i <= 2; i += 0.5) {
                for (double j = -1; j <= 3; j += 0.5) {
                    BlockPos pos = housePosition.add(sideways.mul(i)).add(orientation.mul(j)).block();
                    if (pathBuilder.setPlanks(pos)) {
                        world.setBlockToAir(pos.add(0, 1, 0));
                    }
                }
            }
            return housePosition.add(orientation.mul(4)).block();
        }

        int y = housePosition.block().getY();

        for (double i = -1; i <= 1; i += 0.5) {
            for (double j = -1; j <= 1; j += 0.5) {
                BlockPos pos = housePosition.add(sideways.mul(i)).add(orientation.mul(j)).block();
                BlockPos placed = pathBuilder.setBlockOrSlab(pos);

                if (placed != null) {
                    world.setBlockToAir(pos.add(0, 1, 0));

                    if (placed != pos) {
                        y = placed.getY();
                    }
                }
            }
        }

        Vec anchor = orientation.add(housePosition.x, y, housePosition.z);

        for (double i = -2; i <= 2; i += 0.5) {
            for (double j = -2; j <= 2; j += 0.5) {
                BlockPos pos = anchor.add(sideways.mul(i)).add(orientation.mul(j)).block();
                BlockPos placed = pathBuilder.setBlockOrSlab(pos);

                if (placed != null) {
                    world.setBlockToAir(pos.add(0, 1, 0));

                    if (placed != pos) {
                        y = placed.getY();
                    }
                }
            }
        }

        return anchor.add(orientation.mul(3)).project(Vec.Axis.X, Vec.Axis.Z).add(0, y, 0).block();
    }

    private StructureBoundingBox[] spawnSmallExtension(StructureBoundingBox walls, StructureBoundingBox foundation, StructureBoundingBox roof, Rotation rotation) {
        return new StructureBoundingBox[]{
                spawnAdjacent(walls, houseSmallExtension, rotation),
                spawnAdjacent(foundation, houseSmallExtensionStilts, rotation),
                spawnAdjacent(roof, houseSmallExtensionRoof, rotation, 2)
        };
    }

    private StructureBoundingBox[] spawnExtension(StructureBoundingBox walls, StructureBoundingBox foundation, StructureBoundingBox roof, Rotation rotation) {
        return new StructureBoundingBox[]{
                spawnAdjacent(walls, houseExtension, rotation),
                spawnAdjacent(foundation, houseExtensionStilts, rotation),
                spawnAdjacent(roof, houseExtensionRoof, rotation, 2)
        };
    }
}
