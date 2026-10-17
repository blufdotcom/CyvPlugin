package net.cyvplugin.command;

import net.cyvplugin.practice.PracticeHandler;
import net.cyvplugin.teleport.TPHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class TPCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "tp";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/tp <x> <y> <z> [yaw] [pitch] | /tp <player> [x] [y] [z]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public List<String> getCommandAliases() {
        return new ArrayList<String>();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            throw new CommandException("Usage: " + getCommandUsage(sender));
        }

        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return;

        if (PracticeHandler.isActive()) {
            throw new CommandException("TP disabled in practice mode.");
        }

        if (args.length < 3 || (!args[0].startsWith("~") && !isNumeric(args[0]))) {
            player.sendChatMessage("/tp " + String.join(" ", args));
            return;
        }

        double x = parseCoord(args[0], player.posX);
        double y = parseCoord(args[1], player.posY);
        double z = parseCoord(args[2], player.posZ);

        float yaw = player.rotationYaw;
        float pitch = player.rotationPitch;
        boolean hasRotation = false;

        if (args.length >= 4) {
            try {
                yaw = Float.parseFloat(args[3]);
                hasRotation = true;
            } catch (NumberFormatException e) {
                throw new CommandException("Invalid yaw: " + args[3]);
            }
        }
        if (args.length >= 5) {
            try {
                pitch = Float.parseFloat(args[4]);
                hasRotation = true;
            } catch (NumberFormatException e) {
                throw new CommandException("Invalid pitch: " + args[4]);
            }
        }

        String cmd = "/tp " + args[0] + " " + args[1] + " " + args[2];
        player.sendChatMessage(cmd);

        TPHandler.startTracking(x, y, z, yaw, pitch, hasRotation);
    }

    private static boolean isNumeric(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static double parseCoord(String arg, double current) throws CommandException {
        if (arg.startsWith("~")) {
            String offset = arg.substring(1);
            if (offset.isEmpty()) {
                return current;
            }
            try {
                return current + Double.parseDouble(offset);
            } catch (NumberFormatException e) {
                throw new CommandException("Invalid relative coordinate: " + arg);
            }
        }
        try {
            return Double.parseDouble(arg);
        } catch (NumberFormatException e) {
            throw new CommandException("Invalid coordinate: " + arg);
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        return null;
    }
}