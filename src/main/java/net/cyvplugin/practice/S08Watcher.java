package net.cyvplugin.practice;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class S08Watcher {

    private static final double START_EPSILON = 0.0001;

    private static volatile Teleport pending;
    private static NetworkManager installedOn;
    private Teleport tickTeleport;

    private static final class Teleport {
        final double x, y, z;
        final boolean relX, relY, relZ;

        Teleport(double x, double y, double z, boolean relX, boolean relY, boolean relZ) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.relX = relX;
            this.relY = relY;
            this.relZ = relZ;
        }
    }

    private static final class PacketTap extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof S08PacketPlayerPosLook) capture((S08PacketPlayerPosLook) msg);
            ctx.fireChannelRead(msg);
        }
    }

    private static void capture(S08PacketPlayerPosLook packet) {
        pending = new Teleport(
                packet.getX(), packet.getY(), packet.getZ(),
                packet.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.X),
                packet.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.Y),
                packet.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.Z));
    }

    /**Consumed at ClientTick START: the packet has already been applied by then (the
     * scheduled-task flush runs before the tick) and the player has not moved yet, so the
     * input lock can cover this very tick. The teleport is stashed for the END handler.*/
    @SubscribeEvent
    public void onTickStart(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        ensureInstalled();
        Teleport t = pending;
        if (t == null) return;
        pending = null;
        tickTeleport = t;
        RecordListener.startInputLock();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Teleport t = tickTeleport;
        tickTeleport = null;
        if (t == null) return;
        if (PracticeHandler.isActive()) {
            PracticeHandler.onServerBodyTeleported();
            return;
        }
        if (!RecordListener.recording) return;
        if (!arrivedAtStart(t)) return;
        RecordListener.onServerTeleport();
    }

    private static boolean arrivedAtStart(Teleport t) {
        if (!t.relX && !t.relY && !t.relZ) {
            return near(t.x, RecordListener.startX)
                    && near(t.y, RecordListener.startY)
                    && near(t.z, RecordListener.startZ);
        }
        EntityPlayer player = PracticeHandler.getControlledPlayer();
        if (player == null) return false;
        return axis(t.x, t.relX, player.posX, RecordListener.startX)
                && axis(t.y, t.relY, player.posY, RecordListener.startY)
                && axis(t.z, t.relZ, player.posZ, RecordListener.startZ);
    }

    private static boolean axis(double value, boolean relative, double playerPos, double start) {
        if (!relative) return near(value, start);
        return near(playerPos, start) || near(playerPos + value, start);
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < START_EPSILON;
    }

    private static void ensureInstalled() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getNetHandler() == null) return;
        NetworkManager manager = mc.getNetHandler().getNetworkManager();
        if (manager == installedOn) return;
        Channel channel = manager.channel();
        if (channel == null || !channel.isOpen()) return;
        ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.get("cyv_s08") != null) {
            installedOn = manager;
            return;
        }
        if (pipeline.get("decoder") != null) pipeline.addAfter("decoder", "cyv_s08", new PacketTap());
        else pipeline.addBefore("packet_handler", "cyv_s08", new PacketTap());
        installedOn = manager;
    }
}
