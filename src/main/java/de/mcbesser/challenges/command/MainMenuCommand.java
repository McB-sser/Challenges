package de.mcbesser.challenges.command;

import de.mcbesser.challenges.service.MainMenuService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MainMenuCommand implements CommandExecutor {

    private final MainMenuService mainMenuService;

    public MainMenuCommand(MainMenuService mainMenuService) {
        this.mainMenuService = mainMenuService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Nur Spieler koennen diesen Befehl nutzen.");
            return true;
        }
        mainMenuService.openMainMenu((Player) sender);
        return true;
    }
}
