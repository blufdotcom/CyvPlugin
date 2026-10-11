package net.cyvplugin.command;

import net.cyvplugin.CyvPlugin;
import net.cyvplugin.bridge.CyvForgeBridge;
import net.cyvplugin.macro.MacroStore;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
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
        return new ArrayList<String>(Collections.singletonList("cyvp"));
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/cyvplugin macro run <name> | stop | list";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2 || !args[0].equalsIgnoreCase("macro")) {
            CyvPlugin.sendChatMessage(getCommandUsage(sender));
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
        } else {
            CyvPlugin.sendChatMessage("/cyvplugin macro run <name> | stop | list");
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
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "macro");
        if (args.length == 2) return getListOfStringsMatchingLastWord(args, "run", "stop", "list");
        if (args.length == 3 && args[1].equalsIgnoreCase("run")) {
            return getListOfStringsMatchingLastWord(args, MacroStore.listNames());
        }
        return null;
    }
}