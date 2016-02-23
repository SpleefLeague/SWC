/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.swc;

import com.mongodb.client.MongoDatabase;
import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.chat.ChatChannel;
import com.spleefleague.core.chat.ChatManager;
import com.spleefleague.core.chat.Theme;
import com.spleefleague.core.command.CommandLoader;
import com.spleefleague.core.events.BattleEndEvent;
import com.spleefleague.core.io.EntityBuilder;
import com.spleefleague.core.io.Settings;
import com.spleefleague.core.player.PlayerManager;
import com.spleefleague.core.plugin.GamePlugin;
import com.spleefleague.swc.bracket.Bracket;
import com.spleefleague.swc.game.Arena;
import com.spleefleague.swc.game.Battle;
import com.spleefleague.swc.game.SWCBattleManager;
import com.spleefleague.swc.listener.ConnectionListener;
import com.spleefleague.swc.listener.EnvironmentListener;
import com.spleefleague.swc.listener.GameListener;
import com.spleefleague.swc.player.SWCPlayer;
import java.util.ArrayList;
import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 *
 * @author Jonas
 */
public class SWC extends GamePlugin {

    private boolean queueOpen = true;
    private static SWC instance;
    private Bracket bracket;
    private PlayerManager<SWCPlayer> playerManager;
    private SWCBattleManager battleManager;
    private ChatChannel start, end;
    
    public SWC() {
        super("[SWC]", ChatColor.GRAY + "[" + ChatColor.GOLD + "SWC" + ChatColor.GRAY + "]" + ChatColor.RESET);
    }
    
    @Override
    public void start() {
        instance = this;
        Document doc = getPluginDB().getCollection("Brackets").find(new Document("_id", Settings.get("SWCBracket"))).first();
        bracket = EntityBuilder.load(doc, Bracket.class);
        Arena.init();
        ConnectionListener.init();
        EnvironmentListener.init();
        GameListener.init();
        playerManager = new PlayerManager<>(this, SWCPlayer.class);
        battleManager = new SWCBattleManager();
        CommandLoader.loadCommands(this, "com.spleefleague.swc.commands");
        start = ChatChannel.valueOf("GAME_MESSAGE_SWC_START");
        end = ChatChannel.valueOf("GAME_MESSAGE_SWC_END");
    }
    
    @Override
    public void stop() {
        EntityBuilder.save(bracket, getPluginDB().getCollection("Brackets"));
    }
    
    public PlayerManager<SWCPlayer> getPlayerManager() {
        return playerManager;
    }
    
    public SWCBattleManager getBattleManager() {
        return battleManager;
    }
    
    public Bracket getBracket() {
        return bracket;
    }

    public ChatChannel getEndMessageChannel() {
        return end;
    }

    public ChatChannel getStartMessageChannel() {
        return start;
    }

    @Override
    public boolean spectate(Player target, Player p) {
        SWCPlayer tsjp = getPlayerManager().get(target);
        SWCPlayer sjp = getPlayerManager().get(p);
        if(tsjp.isIngame()) {
            tsjp.getCurrentBattle().addSpectator(sjp);
            return true;
        }
        else {
            p.sendMessage(SWC.getInstance().getChatPrefix() + Theme.ERROR.buildTheme(false) + " You can only spectate arenas you have already visited!");
            return false;
        }
    }
    
    @Override
    public void unspectate(Player p) {
        SWCPlayer sjp = getPlayerManager().get(p);
        for(Battle battle : getBattleManager().getAll()) {
            if(battle.isSpectating(sjp)) {
                battle.removeSpectator(sjp);
            }
        }
    }
    
    @Override
    public boolean isSpectating(Player p) {
        SWCPlayer sjp = getPlayerManager().get(p);
        for(Battle battle : getBattleManager().getAll()) {
            if(battle.isSpectating(sjp)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void dequeue(Player p) {
        SWCPlayer sp = getPlayerManager().get(p);
        sp.setReady(false);
    }

    @Override
    public void cancel(Player p) {
        SWCPlayer sp = getPlayerManager().get(p);
        Battle battle = getBattleManager().getBattle(sp);
        if(battle != null) {
            battle.cancel();    
            ChatManager.sendMessage(SWC.getInstance().getChatPrefix() + Theme.SUPER_SECRET.buildTheme(false) + " The battle on " + battle.getArena().getName() + " has been cancelled.", ChatChannel.STAFF_NOTIFICATIONS);
        }
    }
    
    @Override
    public void surrender(Player p) {
        SWCPlayer sp = getPlayerManager().get(p);
        Battle battle = getBattleManager().getBattle(sp);
        if(battle != null) {
            for(SWCPlayer active : battle.getActivePlayers()) {
                active.sendMessage(SWC.getInstance().getChatPrefix() + Theme.SUPER_SECRET.buildTheme(false) + " " + p.getName() + " has surrendered!");
            }
            battle.removePlayer(sp, true);
        }
    }

    @Override
    public boolean isQueued(Player p) {
        return false;
    }

    @Override
    public boolean isIngame(Player p) {
        SWCPlayer sp = getPlayerManager().get(p);
        return getBattleManager().isIngame(sp);
    }
    
    @Override
    public void cancelAll() {
        for(Battle battle : new ArrayList<>(battleManager.getAll())) {
            battle.cancel();
        }
    }

    @Override
    public void printStats(Player p) {
        
    }

    @Override
    public void requestEndgame(Player p) {
        SWCPlayer sp = SWC.getInstance().getPlayerManager().get(p);
        Battle battle = sp.getCurrentBattle();
        if (battle != null) {
            sp.setRequestingEndgame(true);
            boolean shouldEnd = true;
            for (SWCPlayer spleefplayer : battle.getActivePlayers()) {
                if (!spleefplayer.isRequestingEndgame()) {
                    shouldEnd = false;
                    break;
                }
            }
            if (shouldEnd) {
                battle.end(null, BattleEndEvent.EndReason.ENDGAME);
            }
            else {
                for (SWCPlayer spleefplayer : battle.getActivePlayers()) {
                    if (!spleefplayer.isRequestingEndgame()) {
                        spleefplayer.sendMessage(SWC.getInstance().getChatPrefix() + " " + Theme.WARNING.buildTheme(false) + "Your opponent wants to end this game. To agree enter " + ChatColor.YELLOW + "/endgame.");
                    }
                }
                sp.sendMessage(SWC.getInstance().getChatPrefix() + " " + Theme.WARNING.buildTheme(false) + "You requested this game to be cancelled.");
            }
        }
    }

    @Override
    public void setQueueStatus(boolean open) {
        queueOpen = open;
    }
    
    @Override
    public void syncSave(Player p) {
        SWCPlayer slp = playerManager.get(p);
        if(slp != null) {
            EntityBuilder.save(slp, getPluginDB().getCollection("Players"));
        }
    }
    
    @Override
    public MongoDatabase getPluginDB() {
        return SpleefLeague.getInstance().getMongo().getDatabase("SWC");
    }
    
    public static SWC getInstance() {
        return instance;
    }
}
