package net.cyvplugin.command;

import net.cyvplugin.practice.PracticeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class TPFCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "tpf";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/tpf <yaw> [pitch]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public List<String> getCommandAliases() {
        List<String> aliases = new ArrayList<String>();
        aliases.add("tpface");
        return aliases;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            throw new CommandException("Usage: " + getCommandUsage(sender));
        }

        if (PracticeHandler.isActive()) {
            throw new CommandException("TP disabled in practice mode.");
        }

        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        float currentYaw = player != null ? player.rotationYaw : 0.0f;
        float currentPitch = player != null ? player.rotationPitch : 0.0f;

        float yaw;
        float pitch;

        try {
            yaw = parseRotation(args[0], currentYaw);
        } catch (NumberFormatException e) {
            throw new CommandException("Invalid yaw: " + args[0]);
        }

        if (args.length >= 2) {
            try {
                pitch = parseRotation(args[1], currentPitch);
            } catch (NumberFormatException e) {
                throw new CommandException("Invalid pitch: " + args[1]);
            }
        } else {
            pitch = currentPitch;
        }

        if (player != null) {
            player.rotationYaw = yaw;
            player.rotationPitch = pitch;
            player.prevRotationYaw = yaw;
            player.prevRotationPitch = pitch;
        }
    }

    private static float parseRotation(String arg, float current) {
        if (arg.startsWith("~")) {
            String offset = arg.substring(1);
            if (offset.isEmpty()) return current;
            return current + Float.parseFloat(offset);
        }
        return Float.parseFloat(arg);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        return null;
    }
}