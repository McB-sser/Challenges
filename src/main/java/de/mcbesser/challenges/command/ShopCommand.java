package de.mcbesser.challenges.command;

import de.mcbesser.challenges.service.ShopService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {

    private final ShopService shopService;

    public ShopCommand(ShopService shopService) {
        this.shopService = shopService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            shopService.openShop((Player) sender);
        } else {
            sender.sendMessage("Nur Spieler koennen diesen Befehl nutzen.");
        }
        return true;
    }
}
