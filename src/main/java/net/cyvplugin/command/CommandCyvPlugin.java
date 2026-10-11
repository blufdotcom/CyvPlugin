package net.cyvplugin.command;

import net.cyvplugin.CyvPlugin;
import net.cyvplugin.bridge.CyvForgeBridge;
import net.cyvplugin.bridge.CyvThemeBridge;
import net.cyvplugin.macro.MacroStore;
import net.cyvplugin.practice.MacroSaver;
import net.cyvplugin.practice.PracticeHandler;
import net.cyvplugin.practice.RecordListener;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandCyvPlugin extends CommandBase {
    @Override
    public String getCommandName() {
        return "cyvplugin";
    }

    @Override
    public List<String> getCommandAliases() {
        ArrayList<String> aliases = new ArrayList<String>();
        aliases.add("cyvp");
        aliases.add("cyv");
        return aliases;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/cyvplugin <record [save] | practice | macro run <name> | stop | list>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            printHelp();
            return;
        }

        if (args[0].equalsIgnoreCase("record")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("save")) {
                saveRecording();
            } else {
                startRecording();
            }
            return;
        }

        if (args[0].equalsIgnoreCase("practice")) {
            PracticeHandler.toggle();
            return;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("macro")) {
            return;
        }

        if (args[1].equalsIgnoreCase("list")) {
            listMacros();
        } else if (args[1].equalsIgnoreCase("stop")) {
            try {
                CyvForgeBridge.stopMacro();
                CyvPlugin.sendChatMessage("Macro stopped.");
            } catch (Exception e) {
                CyvPlugin.sendChatMessage("Could not stop the macro: " + e.getMessage());
            }
        } else if (args[1].equalsIgnoreCase("run") && args.length >= 3) {
            try {
                CyvForgeBridge.runMacro(args[2]);
                CyvPlugin.sendChatMessage("Running macro \"" + args[2] + "\".");
            } catch (Exception e) {
                CyvPlugin.sendChatMessage("Could not run the macro: " + e.getMessage());
            }
        }
    }

    private void printHelp() {
        String c1 = CyvThemeBridge.chatColor1();
        StringBuilder sb = new StringBuilder();
        sb.append("\u00a7fCyvPlugin help menu:\u00a7r\n");
        addHelpLine(sb, c1, "record", "Start a practice recording");
        addHelpLine(sb, c1, "record save", "Save the recording to the current macro");
        addHelpLine(sb, c1, "practice", "Toggle practice mode");
        addHelpLine(sb, c1, "macro run <name>", "Run a macro on the server");
        addHelpLine(sb, c1, "macro stop", "Stop the running macro");
        addHelpLine(sb, c1, "macro list", "List saved macros");
        sb.append(c1).append("\u00a7oNote: Use /cyvplugin <command> for a list of subcommands.");
        CyvPlugin.sendChatMessage(sb.toString());
    }

    private void addHelpLine(StringBuilder sb, String c1, String name, String desc) {
        sb.append(c1).append(name).append(": \u00a7f").append(desc).append("\n");
    }

    private void startRecording() {
        EntityPlayer player = PracticeHandler.getControlledPlayer();
        if (player == null) return;
        RecordListener.startRecording(player.posX, player.posY, player.posZ);
        CyvPlugin.sendChatMessage("Recording started.");
    }

    private void saveRecording() {
        if (RecordListener.recordedTicks.isEmpty()) {
            CyvPlugin.sendChatMessage("Nothing recorded to save.");
            return;
        }
        String macroName;
        try {
            macroName = CyvForgeBridge.getCurrentMacro();
        } catch (Exception e) {
            macroName = "macro";
        }
        int ticks = MacroSaver.saveRecorded(macroName);
        if (ticks < 0) {
            CyvPlugin.sendChatMessage("Nothing recorded to save.");
        } else if (ticks == 0) {
            CyvPlugin.sendChatMessage("Nothing meaningful recorded to save.");
        } else {
            CyvPlugin.sendChatMessage("Recording saved to " + macroName + ".json (" + ticks + " ticks)");
        }
    }

    private void listMacros() {
        List<String> names = MacroStore.listNames();
        if (names.isEmpty()) {
            CyvPlugin.sendChatMessage("No macros in " + MacroStore.MACRO_PATH);
            return;
        }
        CyvPlugin.sendChatMessage("Macros: " + String.join(", ", names));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "macro", "record", "practice");
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("record")) return getListOfStringsMatchingLastWord(args, "save");
            if (args[0].equalsIgnoreCase("macro")) return getListOfStringsMatchingLastWord(args, "run", "stop", "list");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("macro") && args[1].equalsIgnoreCase("run")) {
            return getListOfStringsMatchingLastWord(args, MacroStore.listNames());
        }
        return null;
    }
}