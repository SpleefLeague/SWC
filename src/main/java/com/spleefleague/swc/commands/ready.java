/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.swc.commands;

import com.spleefleague.core.command.BasicCommand;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.core.plugin.CorePlugin;
import com.spleefleague.swc.SWC;
import com.spleefleague.swc.player.SWCPlayer;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

/**
 *
 * @author Jonas
 */
public class ready extends BasicCommand {

    public ready(CorePlugin plugin, String name, String usage) {
        super(plugin, name, usage);
    }

    @Override
    protected void run(Player p, SLPlayer slp, Command cmd, String[] args) {
        if(args.length == 0) {
            SWCPlayer swcp = SWC.getInstance().getPlayerManager().get(p);
            if(swcp.isReady()) {
                swcp.setReady(false);
                success(p, "You are no longer ready.");
            }
            else {
                swcp.setReady(true);
                success(p, "You are now ready.");
            }
        }
        else {
            sendUsage(p);
        }
    }
}
