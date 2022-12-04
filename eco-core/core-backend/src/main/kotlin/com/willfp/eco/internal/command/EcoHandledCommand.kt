package com.willfp.eco.internal.command

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.CommandBase
import com.willfp.eco.core.command.NotificationException
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandMap
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil

abstract class EcoHandledCommand(
    private val plugin: EcoPlugin,
    private val name: String,
    private val permission: String,
    private val playersOnly: Boolean
) : CommandBase, CommandExecutor, TabCompleter {


    val subCommands = mutableListOf<CommandBase>()

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): Boolean {

        if (!command.name.equals(name, true)) {
            return false
        }

        if (args != null) {
            handleExecution(sender, args.toList())
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): MutableList<String>? {
       return handleTabComplete(sender, args?.toList() ?: listOf()).toMutableList()
    }

    override fun getPlugin() = this.plugin

    override fun getName() = this.name

    override fun getPermission() = this.permission

    override fun isPlayersOnly() = this.playersOnly

    override fun getSubcommands() = this.subCommands

    override fun addSubcommand(command: CommandBase): CommandBase {
        TODO("Not yet implemented")
    }

    fun CommandBase.handleExecution(sender: CommandSender, args: List<String>) {
        if (!canExecute(sender, this, plugin)) {
            return
        }

        if (args.isNotEmpty()) {
            for (subCommand in subcommands) {
                if (subCommand.name.equals(args[0], true) && !canExecute(
                        sender,
                        subCommand,
                        plugin
                    )
                ) {
                    return
                }

                subCommand.handleExecution(sender, args.subList(1, args.size))
                return
            }
        }

        try {
            notifyFalse(isPlayersOnly && sender !is Player, "not-player")

            if (sender is Player) {
                onExecute(sender, args)
            } else {
                onExecute(sender, args)
            }

        } catch (e: NotificationException) {
            sender.sendMessage(plugin.langYml.getMessage(e.key))
            return
        }
    }

    /*
        protected final List<String> handleTabCompletion(@NotNull final CommandSender sender,
                                                     @NotNull final String[] args) {

        if (!sender.hasPermission(this.getPermission())) {
            return null;
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();

            StringUtil.copyPartialMatches(
                    args[0],
                    this.getSubcommands().stream()
                            .filter(subCommand -> sender.hasPermission(subCommand.getPermission()))
                            .map(CommandBase::getName)
                            .collect(Collectors.toList()),
                    completions
            );

            Collections.sort(completions);

            if (!completions.isEmpty()) {
                return completions;
            }
        }

        if (args.length >= 2) {
            HandledCommand command = null;

            for (CommandBase subcommand : this.getSubcommands()) {
                if (!sender.hasPermission(subcommand.getPermission())) {
                    continue;
                }

                if (args[0].equalsIgnoreCase(subcommand.getName())) {
                    command = (HandledCommand) subcommand;
                }
            }

            if (command != null) {
                return command.handleTabCompletion(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }

        if (this.getTabCompleter() != null) {
            return this.getTabCompleter().tabComplete(sender, Arrays.asList(args));
        } else {
            List<String> completions = new ArrayList<>(this.tabComplete(sender, Arrays.asList(args)));
            if (sender instanceof Player player) {
                completions.addAll(this.tabComplete(player, Arrays.asList(args)));
            }
            return completions;
        }
    }
     */
    fun CommandBase.handleTabComplete(sender: CommandSender, args: List<String>): List<String> {
        if (!sender.hasPermission(permission) || args.isEmpty()) return emptyList()

        val completions = subCommands.filter { sender.hasPermission(it.permission) }.map { it.name }.sorted()

        return when (args.size) {
            1 -> {
                val list = mutableListOf<String>()
                StringUtil.copyPartialMatches(args[0], completions, list)
                list
            }

            else -> completions
        }
    }

    companion object {
        fun getCommandMap(): CommandMap {
            try {
                val field = Bukkit.getServer().javaClass.getDeclaredField("commandMap")
                field.trySetAccessible()
                return field.get(Bukkit.getServer()) as CommandMap
            } catch (e: Exception) {
                throw NullPointerException("Command map wasn't found!")
            }
        }

        fun canExecute(sender: CommandSender, command: CommandBase, plugin: EcoPlugin): Boolean {
            if (!sender.hasPermission(command.permission) && sender is Player) {
                sender.sendMessage(plugin.langYml.noPermission)
                return false
            }

            return true
        }
    }
}