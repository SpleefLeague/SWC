/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.swc.game;

import com.comphenix.packetwrapper.WrapperPlayServerPlayerInfo;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.chat.ChatChannel;
import com.spleefleague.core.chat.ChatManager;
import com.spleefleague.core.chat.Theme;
import com.spleefleague.core.events.BattleEndEvent;
import com.spleefleague.core.events.BattleEndEvent.EndReason;
import com.spleefleague.core.events.BattleStartEvent;
import com.spleefleague.core.events.BattleStartEvent.StartReason;
import com.spleefleague.core.io.EntityBuilder;
import com.spleefleague.core.listeners.FakeBlockHandler;
import com.spleefleague.core.player.GeneralPlayer;
import com.spleefleague.core.player.PlayerState;
import com.spleefleague.core.player.Rank;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.core.plugin.GamePlugin;
import com.spleefleague.core.utils.Area;
import com.spleefleague.core.utils.FakeArea;
import com.spleefleague.core.utils.FakeBlock;
import com.spleefleague.swc.SWC;
import com.spleefleague.swc.player.SWCPlayer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;
import net.minecraft.server.v1_8_R3.NBTTagString;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 *
 * @author Jonas
 */
public class Battle implements com.spleefleague.core.queue.Battle<Arena, SWCPlayer> {

    private final Arena arena;
    private final List<SWCPlayer> players, spectators, disconnects;
    private final Map<SWCPlayer, PlayerData> data;
    private final ChatChannel cc;
    private int ticksPassed = 0;
    private BukkitRunnable clock;
    private Scoreboard scoreboard;
    private boolean inCountdown = false;
    private final FakeArea spawnCages, field;
    private final com.spleefleague.swc.bracket.Battle match;
    
    protected Battle(Arena arena, List<SWCPlayer> players, com.spleefleague.swc.bracket.Battle match) {
        this.match = match;
        this.arena = arena;
        this.players = players;
        this.spectators = new ArrayList<>();
        this.disconnects = new ArrayList<>();
        this.data = new LinkedHashMap<>();
        this.spawnCages = new FakeArea();
        this.field = new FakeArea();
        for(Area area : arena.getField()) {
            for(Block block : area.getBlocks()) {
                this.field.addBlock(new FakeBlock(block.getLocation(), Material.SNOW_BLOCK));
            }
        }
        this.cc = ChatChannel.createTemporaryChannel("GAMECHANNEL" + this.hashCode(), null, Rank.DEFAULT, false, false);
    }

    @Override
    public Arena getArena() {
        return arena;
    }
    
    public com.spleefleague.swc.bracket.Battle getMatch() {
        return match;
    }

    public Collection<SWCPlayer> getPlayers() {
        return players;
    }

    public Collection<SWCPlayer> getDisconnectPlayers() {
        return disconnects;
    }

    public boolean isNormalSpleef() {
        return players.size() == 2;
    }

    public void addSpectator(SWCPlayer sp) {
        Location spawn = arena.getSpectatorSpawn();
        if (spawn != null) {
            sp.teleport(spawn);
        }
        sp.setScoreboard(scoreboard);
        sp.sendMessage(Theme.INCOGNITO + "You are now spectating the battle on " + ChatColor.GREEN + arena.getName());
        FakeBlockHandler.addArea(spawnCages, sp.getPlayer());
        FakeBlockHandler.addArea(field, sp.getPlayer());
        FakeBlockHandler.removeArea(arena.getDefaultSnow(), false, sp.getPlayer());
        SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(sp.getPlayer());
        slp.setState(PlayerState.SPECTATING);
        slp.addChatChannel(cc);
        for(SWCPlayer spl : getActivePlayers()) {
            spl.showPlayer(sp.getPlayer());
            sp.showPlayer(spl.getPlayer());
        }
        for(SWCPlayer spl : spectators) {
            spl.showPlayer(sp);
            sp.showPlayer(spl);
        }
        spectators.add(sp);
        hidePlayers(sp);
    }

    public boolean isSpectating(SWCPlayer sjp) {
        return spectators.contains(sjp);
    }

    public void removeSpectator(SWCPlayer sp) {
        List<Player> ingamePlayers = new ArrayList<>();
        for(SWCPlayer p : getActivePlayers()) {
            sp.getPlayer().hidePlayer(p.getPlayer());
            p.getPlayer().hidePlayer(sp.getPlayer());
            ingamePlayers.add(p.getPlayer());
        }
        Bukkit.getScheduler().runTaskLater(SWC.getInstance(), () -> {
            List<PlayerInfoData> list = new ArrayList<>();
            SpleefLeague.getInstance().getPlayerManager().getAll().forEach((SLPlayer slPlayer) -> list.add(new PlayerInfoData(WrappedGameProfile.fromPlayer(slPlayer.getPlayer()), ((CraftPlayer) slPlayer.getPlayer()).getHandle().ping, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(slPlayer.getRank().getColor() + slPlayer.getName()))));
            WrapperPlayServerPlayerInfo packet = new WrapperPlayServerPlayerInfo();
            packet.setAction(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            packet.setData(list);
            ingamePlayers.forEach((Player p) -> packet.sendPacket(p));

            list.clear();
            ingamePlayers.forEach((Player p) -> {
                SLPlayer generalPlayer = SpleefLeague.getInstance().getPlayerManager().get(p);
                list.add(new PlayerInfoData(WrappedGameProfile.fromPlayer(p), ((CraftPlayer)p).getHandle().ping, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(generalPlayer.getRank().getColor() + generalPlayer.getName())));
            });
            packet.setData(list);
            packet.sendPacket(sp.getPlayer());
        },10);
        resetPlayer(sp);
    }

    /**
     * Called when a player quits - handles cooldown/reconnection.
     *
     * @param sp player quitting.
     */
    public void playerQuit(final SWCPlayer sp) {
        players.remove(sp);
        disconnects.add(sp);
        sp.setDisconnectLocation(sp.getLocation());
        getActivePlayers().forEach((SWCPlayer swcPlayer) -> swcPlayer.sendMessage(SWC.getInstance().getChatPrefix() + " " + Theme.ERROR.buildTheme(false) + sp.getName() + " has left the game, and has 2.5 minutes to reconnect."));
        Bukkit.getScheduler().runTaskLater(SWC.getInstance(), () -> {
            for(SWCPlayer swcPlayer : players) {
                if(swcPlayer.getUniqueId().equals(sp.getUniqueId())) {
                    if(disconnects.contains(sp)) {
                        //Failsafe.
                        disconnects.remove(sp);
                    }
                    return;
                }
            }
            disconnects.remove(sp);
            removePlayer(sp, false);
        }, 3000);
    }

    /**
     * Handle disconnected player re-join.
     *
     * @param sp player re-joining.
     * @param slp slPlayer re-joining.
     * @param oldLocation player's old location.
     */
    public void rejoin(SWCPlayer sp, SLPlayer slp, Location oldLocation) {
        disconnects.removeIf((SWCPlayer swcPlayer) -> swcPlayer.getUniqueId().equals(sp.getUniqueId()));
        getActivePlayers().forEach((SWCPlayer swcPlayer) -> {
            swcPlayer.sendMessage(SWC.getInstance().getChatPrefix() + " " + Theme.SUCCESS.buildTheme(false) + sp.getName() + " has re-joined the game!");
            swcPlayer.sendMessage(SWC.getInstance().getChatPrefix() + " " + Theme.SUCCESS.buildTheme(false) + "The battle has been resumed!");
            FakeBlockHandler.addBlock(new FakeBlock(swcPlayer.getLocation().clone().subtract(0, 1, 0), Material.SNOW_BLOCK), true, players.toArray(new SWCPlayer[players.size()]));
            swcPlayer.showPlayer(sp);
            sp.showPlayer(swcPlayer);
        });

        players.add(sp);
        FakeBlockHandler.addArea(spawnCages, sp.getPlayer());
        FakeBlockHandler.addArea(field, sp.getPlayer());
        FakeBlockHandler.removeArea(arena.getDefaultSnow(), false, sp.getPlayer());
        FakeBlockHandler.addBlock(new FakeBlock(oldLocation.clone().subtract(0, 1, 0), Material.SNOW_BLOCK), true, players.toArray(new SWCPlayer[players.size()]));
        sp.setIngame(true);
        sp.setReady(false);
        slp.setState(PlayerState.INGAME);
        slp.addChatChannel(cc);
        sp.setScoreboard(scoreboard);
        sp.teleport(oldLocation);
    }

    public void removePlayer(SWCPlayer sp, boolean surrender) {
        if(!surrender) {
            for (SWCPlayer pl : getActivePlayers()) {
                pl.sendMessage(SWC.getInstance().getChatPrefix() + " " + Theme.ERROR.buildTheme(false) + sp.getName() + " has left the game!");
            }
            for (SWCPlayer pl : spectators) {
                pl.sendMessage(SWC.getInstance().getChatPrefix() + " " + Theme.ERROR.buildTheme(false) + sp.getName() + " has left the game!");
            }
        }
        resetPlayer(sp);
        ArrayList<SWCPlayer> activePlayers = getActivePlayers();
        if (activePlayers.size() == 1) {
            end(activePlayers.get(0), surrender ? EndReason.SURRENDER : EndReason.QUIT);
        }
    }

    @Override
    public ArrayList<SWCPlayer> getActivePlayers() {
        ArrayList<SWCPlayer> active = new ArrayList<>();
        for (SWCPlayer sp : players) {
            if (sp.isIngame()) {
                active.add(sp);
            }
        }
        return active;
    }

    private void resetPlayer(SWCPlayer sp) {
        SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(sp.getPlayer());
        if(slp == null) {
            return;
        }
        FakeBlockHandler.removeArea(spawnCages, slp.getPlayer());
        FakeBlockHandler.removeArea(field, false, slp.getPlayer());
        FakeBlockHandler.addArea(arena.getDefaultSnow(), false, slp.getPlayer());
        if (spectators.contains(sp)) {
            spectators.remove(sp);
        }
        else {
            sp.setIngame(false);
            sp.setFrozen(false);
            sp.setRequestingReset(false);
            sp.setRequestingEndgame(false);
            sp.closeInventory();
            data.get(sp).restoreOldData();
        }
        sp.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        sp.teleport(SpleefLeague.getInstance().getSpawnManager().getNext().getLocation());
        slp.removeChatChannel(cc);
        slp.setState(PlayerState.IDLE);
        slp.resetVisibility();
    }

    public void end(SWCPlayer winner, EndReason reason) {
        saveGameHistory(winner);
        if(reason == EndReason.CANCEL) {
            if(reason == EndReason.CANCEL) {
                ChatManager.sendMessage(SWC.getInstance().getChatPrefix(), Theme.INCOGNITO.buildTheme(false) + "The battle has been cancelled by a moderator.", cc);
            }
            match.reset();
        }
        else if(reason != EndReason.ENDGAME) {
            announceWinner(winner);
            match.end(winner.getTournamentParticipant());
            SWC.getInstance().getBracket().saveBattle(match);
        }
        else {
            match.reset();
        }
        for (SWCPlayer sp : new ArrayList<>(spectators)) {
            resetPlayer(sp);
        }
        for (SWCPlayer sp : getActivePlayers()) {
            resetPlayer(sp);
        }
        Bukkit.getPluginManager().callEvent(new BattleEndEvent(this, reason));
        cleanup();
    }

    private void cleanup() {
        clock.cancel();
        resetField();
        arena.registerGameEnd();
        SWC.getInstance().getBattleManager().remove(this);
        ChatManager.unregisterChannel(cc);
    }

    public void resetField() {
        for (FakeBlock blocks : field.getBlocks()) {
            blocks.setType(Material.SNOW_BLOCK);
        }
        FakeBlockHandler.update(field);
    }

    public void cancel() {
        end(null, EndReason.CANCEL);
    }

    public void onArenaLeave(SWCPlayer player) {
        if (isInCountdown() || !getDisconnectPlayers().isEmpty()) {
            player.teleport(data.get(player).getSpawn());
        }
        else {
            for (SWCPlayer sp : getActivePlayers()) {
                if (sp != player) {
                    PlayerData playerdata = this.data.get(sp);
                    playerdata.increasePoints();
                    match.getScore().setScore(playerdata.getPoints(), sp.getTournamentParticipant());
                    scoreboard.getObjective("rounds").getScore(sp.getName()).setScore(playerdata.getPoints());
                    if (playerdata.getPoints() < arena.getMaxRating()) {
                        int round = 0;
                        for (PlayerData pd : data.values()) {
                            round += pd.getPoints();
                        }
                        ChatManager.sendMessage(SWC.getInstance().getChatPrefix(), Theme.INFO.buildTheme(false) + sp.getName() + " has won round " + round, cc);
                        startRound();
                    }
                    else {
                        end(sp, EndReason.NORMAL);
                    }
                }
            }
            if (arena.getScoreboards() != null) {
                int[] score = new int[arena.getSize()];
                int i = 0;
                for (PlayerData pd : data.values()) {
                    score[i++] = pd.getPoints();
                }
                for (com.spleefleague.swc.game.scoreboards.Scoreboard scoreboard : arena.getScoreboards()) {
                    scoreboard.setScore(score);
                }
            }
        }
    }

    public void start(StartReason reason) {
        for(SWCPlayer player : players) {
            GamePlugin.unspectateGlobal(player);
            GamePlugin.dequeueGlobal(player);    
        }
        BattleStartEvent event = new BattleStartEvent(this, reason);
        Bukkit.getPluginManager().callEvent(event);
        if(!event.isCancelled()) {
            arena.registerGameStart();
            if(arena.getScoreboards() != null) {
                for(com.spleefleague.swc.game.scoreboards.Scoreboard scoreboard : arena.getScoreboards()) {
                    scoreboard.setScore(new int[arena.getSize()]);
                }
            }
            ChatManager.registerChannel(cc);
            SWC.getInstance().getBattleManager().add(this);
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective objective = scoreboard.registerNewObjective("rounds", "dummy");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            objective.setDisplayName(ChatColor.GRAY + "00:00:00 | " + ChatColor.RED + "Score:");
            String playerNames = "";
            FakeBlockHandler.removeArea(arena.getDefaultSnow(), false, players.toArray(new SWCPlayer[players.size()]));
            FakeBlockHandler.addArea(field, false, players.toArray(new SWCPlayer[players.size()]));
            FakeBlockHandler.addArea(spawnCages, GeneralPlayer.toBukkitPlayer(players.toArray(new SWCPlayer[players.size()])));

            for (int i = 0; i < players.size(); i++) {
                SWCPlayer sp = players.get(i);
                if (i == 0) {
                    playerNames = ChatColor.RED + sp.getName();
                }
                else if (i == players.size() - 1) {
                    playerNames += ChatColor.GREEN + " and " + ChatColor.RED + sp.getName();
                }
                else {
                    playerNames += ChatColor.GREEN + ", " + ChatColor.RED + sp.getName();
                }
                match.getScore().setScore(0, sp.getTournamentParticipant());    
                sp.setReady(false);
                sp.setIngame(true);
                sp.setFrozen(true);
                sp.setRequestingReset(false);
                Player p = sp.getPlayer();
                SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(p);
                this.data.put(sp, new PlayerData(sp, arena.getSpawns()[i]));
                p.eject();
                p.teleport(arena.getSpawns()[i]);
                p.setHealth(p.getMaxHealth());
                p.setFoodLevel(20);
                p.setScoreboard(scoreboard);
                p.setGameMode(GameMode.ADVENTURE);
                p.closeInventory();
                p.getInventory().clear();
                p.getInventory().addItem(getShovel());
                for (PotionEffect effect : p.getActivePotionEffects()) {
                    p.removePotionEffect(effect.getType());
                }
                for (SWCPlayer sp1 : players) {
                    if (sp != sp1) {
                        sp.showPlayer(sp1.getPlayer());
                    }
                }
                p.setFlying(false);
                p.setAllowFlight(false);
                slp.addChatChannel(cc);
                scoreboard.getObjective("rounds").getScore(sp.getName()).setScore(data.get(sp).getPoints());
                slp.setState(PlayerState.INGAME);
            }
            ChatManager.sendMessage(SWC.getInstance().getChatPrefix(), Theme.SUCCESS.buildTheme(false) + "Beginning match on " + ChatColor.WHITE + arena.getName() + ChatColor.GREEN + " between " + ChatColor.RED + playerNames + ChatColor.GREEN + "!", SWC.getInstance().getStartMessageChannel());
            getSpawnCageBlocks();
            FakeBlockHandler.addArea(spawnCages, false, GeneralPlayer.toBukkitPlayer(players.toArray(new SWCPlayer[players.size()])));
            hidePlayers();
            startClock();
            startRound();
        }
    }
    
    private void hidePlayers() {
        List<SWCPlayer> battlePlayers = getActivePlayers();
        battlePlayers.addAll(spectators);
        for(SWCPlayer sjp : SWC.getInstance().getPlayerManager().getAll()) {
            hidePlayers(sjp);
        }
    }
    
    private void hidePlayers(SWCPlayer target) {
        List<SWCPlayer> battlePlayers = getActivePlayers();
        battlePlayers.addAll(spectators);
        for(SWCPlayer active : battlePlayers) {
            if(!battlePlayers.contains(target)) {
                target.hidePlayer(active.getPlayer());
                active.hidePlayer(target.getPlayer());
            }
        }
    }

    public void startRound() {
        inCountdown = true;
        resetField();
        createSpawnCages();
        for (SWCPlayer sp : getActivePlayers()) {
            sp.setFrozen(true);
            sp.setRequestingReset(false);
            sp.setRequestingEndgame(false);
            sp.setFireTicks(0);
            sp.teleport(this.data.get(sp).getSpawn());
        }
        BukkitRunnable br = new BukkitRunnable() {
            private int secondsLeft = 3;

            @Override
            public void run() {
                if (secondsLeft > 0) {
                    ChatManager.sendMessage(SWC.getInstance().getChatPrefix(), secondsLeft + "...", cc);
                    secondsLeft--;
                }
                else {
                    ChatManager.sendMessage(SWC.getInstance().getChatPrefix(), "GO!", cc);
                    for (SWCPlayer sp : getActivePlayers()) {
                        sp.setFrozen(false);
                    }
                    onDone();
                    super.cancel();
                }
            }

            public void onDone() {
                for (SWCPlayer sp : getActivePlayers()) {
                    sp.teleport(getData(sp).getSpawn().clone().add(0, 0.3, 0));
                    sp.setFrozen(false);
                }
                removeSpawnCages();
                inCountdown = false;
            }
        };
        br.runTaskTimer(SWC.getInstance(), 20, 20);
    }

    private void updateScoreboardTime() {
        if (scoreboard == null) {
            return;
        }
        Objective objective = scoreboard.getObjective("rounds");
        if (objective != null) {
            String s = DurationFormatUtils.formatDuration(ticksPassed * 50, "HH:mm:ss", true);
            objective.setDisplayName(ChatColor.GRAY.toString() + s + " | " + ChatColor.RED + "Score:");
        }
    }

    private void startClock() {
        clock = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isInCountdown()) {
                    ticksPassed++;
                    updateScoreboardTime();
                }
            }
        };
        clock.runTaskTimer(SWC.getInstance(), 0, 1);
    }

    private void createSpawnCages() {
        for(FakeBlock block : spawnCages.getBlocks()) {
            block.setType(Material.GLASS);
        }
        FakeBlockHandler.update(spawnCages);
    }    

    private void removeSpawnCages() {
        for(FakeBlock block : spawnCages.getBlocks()) {
            block.setType(Material.AIR);
        }
        FakeBlockHandler.update(spawnCages);
    }
    
    private void getSpawnCageBlocks() {
        for(Location spawn : arena.getSpawns()) {
            spawnCages.add(getCageBlocks(spawn, Material.AIR));
        }
    }
    
    private FakeArea getCageBlocks(Location loc, Material m) {
        loc = loc.getBlock().getLocation();
        FakeArea area = new FakeArea();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) {
                    area.addBlock(new FakeBlock(loc.clone().add(x, 2, z), m));
                } else {
                    for (int y = 0; y <= 2; y++) {
                        area.addBlock(new FakeBlock(loc.clone().add(x, y, z), m));
                    }
                }
            }
        }
        return area;
    }

    public boolean isInCountdown() {
        return inCountdown;
    }
    
    public FakeArea getField() {
        return field;
    }

    private void announceWinner(SWCPlayer winner) {
        SWCPlayer loser = null;
        for(SWCPlayer swcp : players) {
            if(swcp != winner) {
                loser = swcp;
                break;
            }
        }
        if(loser != null) {
            ChatManager.sendMessage(SWC.getInstance().getChatPrefix(), ChatColor.GREEN + "Game in arena " + ChatColor.WHITE + arena.getName() + ChatColor.GREEN + " is over. " + ChatColor.RED + winner.getName() + ChatColor.GREEN + " has won against " + ChatColor.RED + loser.getName() + "!", SWC.getInstance().getEndMessageChannel());
        }
        else {
            ChatManager.sendMessage(SWC.getInstance().getChatPrefix(), ChatColor.GREEN + "Game in arena " + ChatColor.WHITE + arena.getName() + ChatColor.GREEN + " is over. " + ChatColor.RED + winner.getName() + ChatColor.GREEN + " has won the match!", SWC.getInstance().getEndMessageChannel());
        }
    }

    private void saveGameHistory(SWCPlayer winner) {
        GameHistory gh = new GameHistory(this, winner, match.getUUID());
        Bukkit.getScheduler().runTaskAsynchronously(SWC.getInstance(), () -> EntityBuilder.save(gh, SWC.getInstance().getPluginDB().getCollection("GameHistory")));
    }

    public PlayerData getData(SWCPlayer sp) {
        return data.get(sp);
    }

    public int getDuration() {
        return ticksPassed;
    }
    
    private static ItemStack getShovel() {
        net.minecraft.server.v1_8_R3.ItemStack stack = CraftItemStack.asNMSCopy(new ItemStack(Material.DIAMOND_SPADE));
        NBTTagCompound tag = stack.hasTag() ? stack.getTag() : new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        list.add(new NBTTagString("minecraft:snow"));
        tag.set("CanDestroy", list);
        tag.setBoolean("Unbreakable", true);
        stack.setTag(tag);
        return CraftItemStack.asBukkitCopy(stack);
    }

    public static class PlayerData {

        private int points;
        private final Location spawn;
        private final SWCPlayer sp;
        private final GameMode oldGamemode;
        private final ItemStack[] oldInventory;

        public PlayerData(SWCPlayer sp, Location spawn) {
            this.sp = sp;
            this.spawn = spawn;
            this.points = 0;
            Player p = sp.getPlayer();
            oldGamemode = p.getGameMode();
            oldInventory = p.getInventory().getContents();
        }

        public Location getSpawn() {
            return spawn;
        }

        public int getPoints() {
            return points;
        }

        public void increasePoints() {
            this.points++;
        }

        public SWCPlayer getPlayer() {
            return sp;
        }

        public void restoreOldData() {
            Player p = sp.getPlayer();
            p.setGameMode(oldGamemode);
            p.setFlying(false);
            p.getInventory().setContents(oldInventory);
        }
    }
}
