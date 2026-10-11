package net.cyvplugin.practice;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**Inert player controller swapped in while practice mode is active: the ghost
 * lives in a "parallel dimension" and must not break/place blocks, hit blocks,
 * use items or interact with entities. Everything else (inherited behaviour) is
 * kept so the client still functions normally.*/
public class PracticePlayerController extends PlayerControllerMP {

    public PracticePlayerController(Minecraft mcIn, NetHandlerPlayClient netHandler) {
        super(mcIn, netHandler);
    }

    @Override
    public boolean onPlayerDestroyBlock(BlockPos pos, EnumFacing side) {
        return false;
    }

    @Override
    public boolean clickBlock(BlockPos loc, EnumFacing face) {
        return false;
    }

    @Override
    public void resetBlockRemoving() {
    }

    @Override
    public boolean onPlayerDamageBlock(BlockPos posBlock, EnumFacing directionFacing) {
        return false;
    }

    @Override
    public boolean onPlayerRightClick(EntityPlayerSP player, WorldClient worldIn, ItemStack heldStack, BlockPos hitPos, EnumFacing side, Vec3 hitVec) {
        return false;
    }

    @Override
    public boolean sendUseItem(EntityPlayer playerIn, World worldIn, ItemStack itemStackIn) {
        return false;
    }

    @Override
    public void attackEntity(EntityPlayer playerIn, Entity targetEntity) {
    }

    @Override
    public boolean interactWithEntitySendPacket(EntityPlayer playerIn, Entity targetEntity) {
        return false;
    }

    @Override
    public boolean func_178894_a(EntityPlayer p_178894_1_, Entity p_178894_2_, MovingObjectPosition p_178894_3_) {
        return false;
    }
}