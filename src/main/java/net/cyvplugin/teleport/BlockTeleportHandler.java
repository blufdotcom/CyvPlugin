package net.cyvplugin.teleport;

import net.cyvplugin.CyvPlugin;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBasePressurePlate;
import net.minecraft.block.BlockVine;
import net.minecraft.block.BlockWeb;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BlockTeleportHandler {

    private static final double RAY_DISTANCE = 256.0D;
    private static final double COLLISION_EPSILON = 1.0E-4D;
    private static final double FACE_OFFSET = 0.05D;
    private static final double SEARCH_STEP = 0.01D;
    private static final double VERTICAL_ADJUST_STEP = 0.05D;
    private static final double SNAP_SCALE = 16.0D;
    private static final double LILYPAD_HEIGHT = 0.015625D;
    private static final double Y_EPSILON = 1.0E-8D;
    private static final double IGNORE_RAY_ADVANCE_STEP = 0.1D;
    private static final int IGNORE_RAY_MAX_STEPS = 64;

    public static void teleportThroughBlock() {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        if (player == null || mc.theWorld == null) return;

        MovingObjectPosition hit = rayTraceBlock(player);
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || hit.hitVec == null) {
            CyvPlugin.sendChatMessage("No block in sight");
            return;
        }

        BlockPos blockPos = hit.getBlockPos();
        if (mc.theWorld.getBlockState(new BlockPos(player.posX, player.posY + player.getEyeHeight(), player.posZ)).getBlock() instanceof BlockWeb) {
            Vec3 look = player.getLook(1.0F);
            sendTeleport(player.posX + look.xCoord * 1.5, player.posY + look.yCoord * 1.5, player.posZ + look.zCoord * 1.5);
            return;
        }

        Vec3 faceNormal = new Vec3(
            hit.sideHit.getFrontOffsetX(),
            hit.sideHit.getFrontOffsetY(),
            hit.sideHit.getFrontOffsetZ()
        );
        Vec3 throughDirection = new Vec3(-faceNormal.xCoord, -faceNormal.yCoord, -faceNormal.zCoord);
        Vec3 searchStart = hit.hitVec.addVector(
            throughDirection.xCoord * FACE_OFFSET,
            throughDirection.yCoord * FACE_OFFSET,
            throughDirection.zCoord * FACE_OFFSET
        );
        double centerX = blockPos.getX() + 0.5D;
        double centerZ = blockPos.getZ() + 0.5D;

        for (double distance = 0.0D; distance <= RAY_DISTANCE; distance += SEARCH_STEP) {
            double eyeX = hit.hitVec.xCoord;
            double eyeY = hit.hitVec.yCoord;
            double eyeZ = hit.hitVec.zCoord;

            if (hit.sideHit == EnumFacing.WEST || hit.sideHit == EnumFacing.EAST) {
                eyeX = searchStart.xCoord + throughDirection.xCoord * distance;
                eyeZ = centerZ;
            } else if (hit.sideHit == EnumFacing.NORTH || hit.sideHit == EnumFacing.SOUTH) {
                eyeX = centerX;
                eyeZ = searchStart.zCoord + throughDirection.zCoord * distance;
            } else if (hit.sideHit == EnumFacing.DOWN || hit.sideHit == EnumFacing.UP) {
                eyeX = centerX;
                eyeY = searchStart.yCoord + throughDirection.yCoord * distance;
                eyeZ = centerZ;
            }

            if (!isProbePointClear(eyeX, eyeY, eyeZ)) continue;

            double eyeHeight = player.getEyeHeight();
            double playerHeight = player.height;
            double feetX = eyeX;
            double feetZ = eyeZ;
            double eyeAlignedFeet = eyeY - eyeHeight;
            double feetAtProbe = eyeY;
            double headAlignedFeet = eyeY - playerHeight;

            if (isPlayerBoxClear(feetX, eyeAlignedFeet, feetZ)) {
                sendTeleport(feetX, eyeAlignedFeet, feetZ);
                return;
            }

            BlockPos probeBlockPos = new BlockPos(eyeX, eyeY, eyeZ);
            double blockAboveProbeY = probeBlockPos.getY() + 1.0D;
            double feetWithHeadUnderBlockAbove = blockAboveProbeY - playerHeight;
            if (isPlayerBoxClear(feetX, feetWithHeadUnderBlockAbove, feetZ)) {
                sendTeleport(feetX, feetWithHeadUnderBlockAbove, feetZ);
                return;
            }

            double step = VERTICAL_ADJUST_STEP;
            if (headAlignedFeet <= feetAtProbe) {
                for (double y = headAlignedFeet; y <= feetAtProbe; y += step) {
                    double candidateY = getMarchCandidateY(feetX, y, feetZ);
                    if (!Double.isNaN(candidateY)) {
                        sendTeleport(feetX, candidateY, feetZ);
                        return;
                    }
                }
            } else {
                for (double y = headAlignedFeet; y >= feetAtProbe; y -= step) {
                    double candidateY = getMarchCandidateY(feetX, y, feetZ);
                    if (!Double.isNaN(candidateY)) {
                        sendTeleport(feetX, candidateY, feetZ);
                        return;
                    }
                }
            }
        }

        CyvPlugin.sendChatMessage("No safe position found through the target block");
    }

    public static void teleportToTopOfBlock() {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        if (player == null || mc.theWorld == null) return;

        boolean sneaking = mc.gameSettings.keyBindSneak.isKeyDown();

        if (!sneaking) {
            EntityPlayer target = rayTracePlayer(player);
            if (target != null) {
                player.sendChatMessage("/tp " + target.getName());
                return;
            }
        }

        MovingObjectPosition hit = rayTraceBlock(player);
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            CyvPlugin.sendChatMessage("No block in sight");
            return;
        }

        BlockPos basePos = hit.getBlockPos();
        int x = basePos.getX();
        int z = basePos.getZ();
        double aimX = hit.hitVec != null ? hit.hitVec.xCoord : x + 0.5D;
        double aimZ = hit.hitVec != null ? hit.hitVec.zCoord : z + 0.5D;

        int startY = basePos.getY();
        IBlockState hitState = mc.theWorld.getBlockState(basePos);
        List<AxisAlignedBB> hitBoxes = getCollisionBoxes(basePos, hitState.getBlock(), hitState);
        if (hitBoxes.isEmpty()) {
            startY--;
            if (startY < 0) startY = 0;
        }

        for (int y = startY; y < 256; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            IBlockState state = mc.theWorld.getBlockState(pos);
            Block block = state.getBlock();

            List<AxisAlignedBB> boxes = getCollisionBoxes(pos, block, state);
            if (boxes.isEmpty()) continue;

            for (AxisAlignedBB box : boxes) {
                double surfaceY = (block instanceof net.minecraft.block.BlockFence || block instanceof net.minecraft.block.BlockWall)
                    ? pos.getY() + 1.5D : box.maxY;

                double[] result = findStandPosition(box, surfaceY, aimX, aimZ);
                if (result != null) {
                    sendTeleport(result[0], result[1], result[2]);
                    return;
                }
            }
        }

        CyvPlugin.sendChatMessage("No safe position found on top of block");
    }

    private static double[] findStandPosition(AxisAlignedBB box, double surfaceY, double aimX, double aimZ) {
        double cx = clamp(aimX, box.minX, box.maxX);
        double cz = clamp(aimZ, box.minZ, box.maxZ);
        if (isPlayerBoxClear(cx, surfaceY, cz)) {
            return new double[]{cx, surfaceY, cz};
        }

        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        double halfWidth = player.width / 2.0D - COLLISION_EPSILON;
        double gMinX = box.minX - halfWidth;
        double gMaxX = box.maxX + halfWidth;
        double gMinZ = box.minZ - halfWidth;
        double gMaxZ = box.maxZ + halfWidth;
        double bestDist = Double.MAX_VALUE;
        double bestX = 0, bestZ = 0;

        for (int xi = 0; xi < 5; xi++) {
            double gx = gMinX + (xi / 4.0) * (gMaxX - gMinX);
            for (int zi = 0; zi < 5; zi++) {
                double gz = gMinZ + (zi / 4.0) * (gMaxZ - gMinZ);
                if (isPlayerBoxClear(gx, surfaceY, gz)) {
                    double dx = gx - aimX;
                    double dz = gz - aimZ;
                    double dist = dx * dx + dz * dz;
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestX = gx;
                        bestZ = gz;
                    }
                }
            }
        }

        return bestDist < Double.MAX_VALUE ? new double[]{bestX, surfaceY, bestZ} : null;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static MovingObjectPosition rayTraceBlock(EntityPlayerSP player) {
        Vec3 start = player.getPositionEyes(1.0F);
        Vec3 look = player.getLook(1.0F);
        Vec3 end = start.addVector(look.xCoord * RAY_DISTANCE, look.yCoord * RAY_DISTANCE, look.zCoord * RAY_DISTANCE);
        MovingObjectPosition hit = Minecraft.getMinecraft().theWorld.rayTraceBlocks(start, end, false, false, false);
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return hit;

        Vec3 searchStart = start;
        Vec3 stepDirection = look.normalize();
        for (int attempts = 0; attempts < IGNORE_RAY_MAX_STEPS; attempts++) {
            if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || hit.hitVec == null) return hit;

            BlockPos pos = hit.getBlockPos();
            IBlockState blockState = Minecraft.getMinecraft().theWorld.getBlockState(pos);
            Block block = blockState.getBlock();
            if (!isIgnoredRayHitBlock(block)) return hit;

            searchStart = hit.hitVec.addVector(
                stepDirection.xCoord * IGNORE_RAY_ADVANCE_STEP,
                stepDirection.yCoord * IGNORE_RAY_ADVANCE_STEP,
                stepDirection.zCoord * IGNORE_RAY_ADVANCE_STEP
            );
            hit = Minecraft.getMinecraft().theWorld.rayTraceBlocks(searchStart, end, false, false, false);
        }
        return hit;
    }

    private static boolean isIgnoredRayHitBlock(Block block) {
        return block instanceof BlockVine || block instanceof BlockBasePressurePlate;
    }

    private static boolean isProbePointClear(double x, double y, double z) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        AxisAlignedBB probeBox = new AxisAlignedBB(
            x - player.width / 2.0D, y, z - player.width / 2.0D,
            x + player.width / 2.0D, y, z + player.width / 2.0D
        );
        if (!mc.theWorld.getCollidingBoundingBoxes(player, probeBox.expand(COLLISION_EPSILON, 0.0D, COLLISION_EPSILON)).isEmpty()) return false;
        return !overlapsCobweb(probeBox);
    }

    private static boolean isPlayerBoxClear(double x, double standY, double z) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        AxisAlignedBB playerBox = new AxisAlignedBB(
            x - player.width / 2.0D, standY, z - player.width / 2.0D,
            x + player.width / 2.0D, standY + player.height, z + player.width / 2.0D
        );
        if (!mc.theWorld.getCollidingBoundingBoxes(player, playerBox.expand(COLLISION_EPSILON, 0.0D, COLLISION_EPSILON)).isEmpty()) return false;
        return !overlapsCobweb(playerBox);
    }

    private static boolean overlapsCobweb(AxisAlignedBB box) {
        int minX = net.minecraft.util.MathHelper.floor_double(box.minX);
        int minY = net.minecraft.util.MathHelper.floor_double(box.minY);
        int minZ = net.minecraft.util.MathHelper.floor_double(box.minZ);
        int maxX = net.minecraft.util.MathHelper.floor_double(box.maxX);
        int maxY = net.minecraft.util.MathHelper.floor_double(box.maxY);
        int maxZ = net.minecraft.util.MathHelper.floor_double(box.maxZ);
        Minecraft mc = Minecraft.getMinecraft();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (mc.theWorld.getBlockState(new BlockPos(x, y, z)).getBlock() instanceof BlockWeb) return true;
                }
            }
        }
        return false;
    }

    private static double getMarchCandidateY(double x, double y, double z) {
        double snappedY = Math.floor((y * SNAP_SCALE) + Y_EPSILON) / SNAP_SCALE;
        if (isPlayerBoxClear(x, snappedY, z)) return snappedY;

        if (Math.abs(snappedY - Math.floor(snappedY + Y_EPSILON)) <= Y_EPSILON) {
            double lilyAdjustedY = snappedY + LILYPAD_HEIGHT;
            if (isPlayerBoxClear(x, lilyAdjustedY, z)) return lilyAdjustedY;
        }
        return Double.NaN;
    }

    private static List<AxisAlignedBB> getCollisionBoxes(BlockPos pos, Block block, IBlockState blockState) {
        Minecraft mc = Minecraft.getMinecraft();
        List<AxisAlignedBB> collisionBoxes = new ArrayList<AxisAlignedBB>();
        AxisAlignedBB searchBox = new AxisAlignedBB(
            pos.getX() - 1.0D, pos.getY() - 1.0D, pos.getZ() - 1.0D,
            pos.getX() + 2.0D, pos.getY() + 2.0D, pos.getZ() + 2.0D
        );
        block.addCollisionBoxesToList(mc.theWorld, pos, blockState, searchBox, collisionBoxes, mc.thePlayer);
        if (collisionBoxes.isEmpty()) {
            AxisAlignedBB boundingBox = block.getCollisionBoundingBox(mc.theWorld, pos, blockState);
            if (boundingBox != null) collisionBoxes.add(boundingBox);
        }
        return collisionBoxes;
    }

    private static EntityPlayer rayTracePlayer(EntityPlayerSP player) {
        Minecraft mc = Minecraft.getMinecraft();
        Vec3 eyePos = player.getPositionEyes(1.0F);
        Vec3 look = player.getLook(1.0F);
        Vec3 end = eyePos.addVector(look.xCoord * RAY_DISTANCE, look.yCoord * RAY_DISTANCE, look.zCoord * RAY_DISTANCE);

        EntityPlayer closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Object obj : mc.theWorld.playerEntities) {
            EntityPlayer other = (EntityPlayer) obj;
            if (other == player || other.isDead) continue;

            AxisAlignedBB bb = other.getEntityBoundingBox().expand(0.3, 0.3, 0.3);
            MovingObjectPosition hit = bb.calculateIntercept(eyePos, end);
            if (hit != null) {
                double dist = eyePos.distanceTo(hit.hitVec);
                if (dist < closestDist) {
                    MovingObjectPosition blockHit = mc.theWorld.rayTraceBlocks(eyePos, hit.hitVec, false, false, false);
                    if (blockHit != null && blockHit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
                        continue;
                    closestDist = dist;
                    closest = other;
                }
            }
        }

        return closest;
    }

    private static void sendTeleport(double x, double y, double z) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;
        player.sendChatMessage("/tp " + String.format(Locale.US, "%.6f", x) + " " + String.format(Locale.US, "%.6f", y) + " " + String.format(Locale.US, "%.6f", z));
    }
}