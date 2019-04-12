package com.vberlier.settlements.generator;

import com.google.common.graph.ValueGraph;
import com.vberlier.settlements.util.Vec;
import net.minecraft.block.*;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.template.Template;

public class HouseBuilder extends StructureBuilder {
    private ValueGraph<Slot, Integer> graph;

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

    public HouseBuilder(World world, ValueGraph<Slot, Integer> graph, BlockPlanks.EnumType woodVariant) {
        super(world, woodVariant);
        this.graph = graph;

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

        spawnAdjacent(walls, houseDoor, orientation.rotation());

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
                    spawnAdjacent(boxes[0], houseDoor, rotation);
                }

                n--;
            }

            if (n <= 0) {
                break;
            }
        }

        rotateColor();
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
