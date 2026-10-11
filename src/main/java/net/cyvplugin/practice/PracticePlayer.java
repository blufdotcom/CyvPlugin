package net.cyvplugin.practice;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraft.world.World;

/**Client-side ghost used by the practice mode. Extends EntityPlayerSP so the
 * client world ticks it with the exact same physics as the real player (sprint
 * machine, creative flight double-jump) and the first-person hand can be drawn
 * from it (it is assignable to Minecraft.thePlayer, which the ItemRenderer
 * reads). It never sends any packet to the server: the walking report, ability
 * sync and damage are all neutralised below. isServerWorld() forces the full
 * server-side movement pipeline (friction, slipperiness, sprint, jump).*/
public class PracticePlayer extends EntityPlayerSP {

    public PracticePlayer(World worldIn, GameProfile playerProfile, Minecraft mcIn) {
        super(mcIn, worldIn, mcIn.getNetHandler(), mcIn.thePlayer.getStatFileWriter());
        this.movementInput = new MovementInputFromOptions(mcIn.gameSettings);
    }

    /**Force the real server-side physics pipeline to run on this client-only ghost.*/
    @Override
    public boolean isServerWorld() {
        return true;
    }

    /**The ghost must never report movement/sprint/sneak to the server: that is
     * the frozen real body's job (PracticeHandler.reportBodyToServer).*/
    @Override
    public void onUpdateWalkingPlayer() {
    }

    /**Flight toggles are client-only; syncing abilities would make the server
     * think the real body gained creative flight.*/
    @Override
    public void sendPlayerAbilities() {
    }

    /**Mirror the real player's pushOutOfBlocks, which detects translucent blocks
     * over the full body height instead of just a full cube at the feet.*/
    @Override
    protected boolean pushOutOfBlocks(double x, double y, double z) {
        if (this.noClip) {
            return false;
        } else {
            BlockPos blockpos = new BlockPos(x, y, z);
            double d0 = x - (double)blockpos.getX();
            double d1 = z - (double)blockpos.getZ();

            int entHeight = Math.max((int)Math.ceil(this.height), 1);

            boolean inTranslucentBlock = !this.isHeadspaceFree(blockpos, entHeight);

            if (inTranslucentBlock) {
                int i = -1;
                double d2 = 9999.0D;

                if (this.isHeadspaceFree(blockpos.west(), entHeight) && d0 < d2) {
                    d2 = d0;
                    i = 0;
                }

                if (this.isHeadspaceFree(blockpos.east(), entHeight) && 1.0D - d0 < d2) {
                    d2 = 1.0D - d0;
                    i = 1;
                }

                if (this.isHeadspaceFree(blockpos.north(), entHeight) && d1 < d2) {
                    d2 = d1;
                    i = 4;
                }

                if (this.isHeadspaceFree(blockpos.south(), entHeight) && 1.0D - d1 < d2) {
                    d2 = 1.0D - d1;
                    i = 5;
                }

                float f = 0.1F;

                if (i == 0) {
                    this.motionX = (double)(-f);
                }

                if (i == 1) {
                    this.motionX = (double)f;
                }

                if (i == 4) {
                    this.motionZ = (double)(-f);
                }

                if (i == 5) {
                    this.motionZ = (double)f;
                }
            }

            return false;
        }
    }

    private boolean isHeadspaceFree(BlockPos pos, int height) {
        for (int y = 0; y < height; y++) {
            if (!isOpenBlockSpace(pos.add(0, y, 0))) return false;
        }
        return true;
    }

    private boolean isOpenBlockSpace(BlockPos pos) {
        return !this.worldObj.getBlockState(pos).getBlock().isNormalCube();
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    /**The ghost is invisible to the world: it must not push (nor be pushed by)
     * any entity. Pushing the real body client-side would desync it and look like
     * hacks; pushing other players wouldn't be seen by the server either.*/
    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    protected void collideWithNearbyEntities() {
    }
}
