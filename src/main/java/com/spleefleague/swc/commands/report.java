/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.swc.commands;

import com.spleefleague.core.command.BasicCommand;
import com.spleefleague.core.player.Rank;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.core.plugin.CorePlugin;
import com.spleefleague.core.utils.DatabaseConnection;
import com.spleefleague.swc.SWC;
import com.spleefleague.swc.bracket.Battle;
import java.util.UUID;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

/**
 *
 * @author Jonas
 */
public class report extends BasicCommand {

    public report(CorePlugin plugin, String name, String usage) {
        super(plugin, name, usage, Rank.SENIOR_MODERATOR);
    }

    @Override
    protected void run(Player p, SLPlayer slp, Command cmd, String[] args) {
        if(args.length == 0) {
            p.sendMessage(ChatColor.DARK_GRAY + "[===" + ChatColor.GRAY + " Unreported matches " + ChatColor.DARK_GRAY + "===]");
            for(Battle battle : SWC.getInstance().getBracket().getBattles()) {
                if(battle.isOver() && !battle.isReported()) {
                    p.spigot().sendMessage(getReportMessage(battle));
                }
            }
        }
        else if (args.length == 1) {
            try {
                UUID uuid = UUID.fromString(args[0]);
                Battle battle = Battle.getByUUID(uuid);
                if(battle != null && battle.isOver()) {
                    battle.setReported(true);
                    success(slp, "This battle has been set to reported!");
                }
                else {
                    error(p, "Please don't call this command manually.");
                }
            } catch(Exception e) {
                error(p, "Please don't call this command manually.");
            }
        }
        else {
            sendUsage(p);
        }
    }
    
    private BaseComponent[] getReportMessage(Battle battle) {
        String first = DatabaseConnection.getUsername(battle.getFirst().getMCID());
        String second = DatabaseConnection.getUsername(battle.getSecond().getMCID());
        BaseComponent[] message = new ComponentBuilder(SWC.getInstance().getChatPrefix() + " ")
            .append(first).color(net.md_5.bungee.api.ChatColor.GREEN)
            .append(" (" + battle.getScore().getScore(battle.getFirst()) + ")").color(ChatColor.GRAY.asBungee())
            .append(" vs. ").color(net.md_5.bungee.api.ChatColor.DARK_GRAY)
            .append(second).color(net.md_5.bungee.api.ChatColor.RED)
            .append(" (" + battle.getScore().getScore(battle.getFirst()) + ")").color(ChatColor.GRAY.asBungee())
            .append(" - ").color(net.md_5.bungee.api.ChatColor.DARK_GRAY)
            .append("[").color(ChatColor.GRAY.asBungee()).append("Reported").color(ChatColor.GOLD.asBungee()).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/report " + battle.getUUID().toString())).append("]").color(ChatColor.GRAY.asBungee())
            .create();
        return message;
    }
}
