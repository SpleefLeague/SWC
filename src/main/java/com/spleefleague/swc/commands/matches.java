package com.spleefleague.swc.commands;

import com.spleefleague.core.command.BasicCommand;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.core.plugin.CorePlugin;
import com.spleefleague.swc.SWC;
import com.spleefleague.swc.game.Battle;
import com.spleefleague.swc.player.SWCPlayer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

/**
 * Created by Josh on 05/03/2016.
 */
public class matches extends BasicCommand {

    public matches(CorePlugin plugin, String name, String usage) {
        super(plugin, name, usage);
    }

    @Override
    protected void run(Player player, SLPlayer slPlayer, Command command, String[] strings) {
        if(SWC.getInstance().getBattleManager().getAll().isEmpty()) {
            error(slPlayer, "There are currently no SWC matches running!");
            return;
        }
        player.sendMessage(ChatColor.DARK_GRAY + "[========== " + ChatColor.GRAY + "Current SWC Matches" + ChatColor.DARK_GRAY + " ==========]");
        SWC.getInstance().getBattleManager().getAll().forEach((Battle battle) -> {
            if(battle.getActivePlayers().isEmpty()) {
                return;
            }
            ComponentBuilder message = new ComponentBuilder(SWC.getInstance().getChatPrefix());
            battle.getActivePlayers().forEach((SWCPlayer swcPlayer) -> {
                message.append(" " + swcPlayer.getName()).color(net.md_5.bungee.api.ChatColor.GREEN);
                if(battle.getActivePlayers().indexOf(swcPlayer) != battle.getActivePlayers().size()) {
                    message.append(" vs. ").color(net.md_5.bungee.api.ChatColor.DARK_GRAY);
                }
            });
            message.append(" - ").color(net.md_5.bungee.api.ChatColor.DARK_GRAY)
                    .append("[").color(ChatColor.GRAY.asBungee()).append("Click to Spectate").color(ChatColor.GOLD.asBungee()).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/spectate " + battle.getActivePlayers().get(0).getName())).append("]").color(ChatColor.GRAY.asBungee())
                    .create();
            player.spigot().sendMessage(message.create());
        });
    }

}
