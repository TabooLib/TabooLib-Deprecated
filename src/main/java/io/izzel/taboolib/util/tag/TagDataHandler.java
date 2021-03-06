package io.izzel.taboolib.util.tag;

import com.google.common.collect.Sets;
import io.izzel.taboolib.TabooLib;
import io.izzel.taboolib.module.inject.TListener;
import io.izzel.taboolib.util.tag.TagPlayerData;
import io.izzel.taboolib.util.tag.TagUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

/**
 * @author sky
 * @since 2018-05-23 0:37
 */
@TListener
public class TagDataHandler implements Listener {

    private static final io.izzel.taboolib.util.tag.TagDataHandler handler = new io.izzel.taboolib.util.tag.TagDataHandler();
    private final HashMap<UUID, TagPlayerData> playersData = new HashMap<>();
    private boolean enabled = false;

    public void setEnabled() {
        if (!enabled) {
            enabled = true;
            Bukkit.getOnlinePlayers().forEach(this::downloadPlayerVariable);
        }
    }

    public void resetMainScoreboard() {
        Set<Team> teams = Sets.newHashSet(Bukkit.getScoreboardManager().getMainScoreboard().getTeams());
        for (Team team : teams) {
            team.unregister();
        }
    }

    public TagPlayerData unregisterPlayerData(Player player) {
        return playersData.remove(player.getUniqueId());
    }

    public TagPlayerData getPlayerData(Player player) {
        return playersData.get(player.getUniqueId());
    }

    public TagPlayerData getPlayerDataComputeIfAbsent(Player player) {
        return playersData.computeIfAbsent(player.getUniqueId(), x -> new TagPlayerData(player));
    }

    public String getPrefix(Player player) {
        return getPlayerDataComputeIfAbsent(player).getPrefix();
    }

    public String getSuffix(Player player) {
        return getPlayerDataComputeIfAbsent(player).getSuffix();
    }

    public String getDisplay(Player player) {
        return getPlayerDataComputeIfAbsent(player).getNameDisplay();
    }

    public boolean isNameVisibility(Player player) {
        return getPlayerDataComputeIfAbsent(player).isNameVisibility();
    }

    public void setPrefix(Player player, String prefix) {
        updatePlayerVariable(getPlayerDataComputeIfAbsent(player).setPrefix(prefix));
        updatePlayerListName(player);
    }

    public void setSuffix(Player player, String suffix) {
        updatePlayerVariable(getPlayerDataComputeIfAbsent(player).setSuffix(suffix));
        updatePlayerListName(player);
    }

    public void setPrefixAndSuffix(Player player, String prefix, String suffix) {
        updatePlayerVariable(getPlayerDataComputeIfAbsent(player).setPrefix(prefix).setSuffix(suffix));
        updatePlayerListName(player);
    }

    public void setNameVisibility(Player player, boolean v) {
        updatePlayerVariable(getPlayerDataComputeIfAbsent(player).setNameVisibility(v));
        updatePlayerListName(player);
    }

    public void resetVariable(Player player) {
        updatePlayerVariable(getPlayerDataComputeIfAbsent(player).reset());
        updatePlayerListName(player);
    }

    public void reset(Player player) {
        updatePlayerVariable(getPlayerDataComputeIfAbsent(player).reset());
    }

    private void downloadPlayerVariable(Player player) {
        Scoreboard scoreboard = TagUtils.getScoreboardComputeIfAbsent(player);
        playersData.values().forEach(playerData -> updateTeamVariable(scoreboard, playerData));
    }

    private void updatePlayerVariable(TagPlayerData playerData) {
        setEnabled();
        Bukkit.getOnlinePlayers().forEach(online -> updateTeamVariable(TagUtils.getScoreboardComputeIfAbsent(online), playerData));
    }

    private void updatePlayerListName(Player player) {
        setEnabled();
        TagPlayerData playerData = getPlayerDataComputeIfAbsent(player);
        player.setPlayerListName(!playerData.getNameDisplay().equals(player.getName()) ? playerData.getPrefix() + playerData.getNameDisplay() + playerData.getSuffix() : playerData.getNameDisplay());
    }

    private void updateTeamVariable(Scoreboard scoreboard, TagPlayerData playerData) {
        setEnabled();
        Team entryTeam = TagUtils.getTeamComputeIfAbsent(scoreboard, playerData.getTeamHash());
        if (!entryTeam.getEntries().contains(playerData.getNameDisplay())) {
            entryTeam.addEntry(playerData.getNameDisplay());
        }
        if (entryTeam.getPrefix() == null || !entryTeam.getPrefix().equals(playerData.getPrefix())) {
            entryTeam.setPrefix(playerData.getPrefix());
        }
        if (entryTeam.getSuffix() == null || !entryTeam.getSuffix().equals(playerData.getSuffix())) {
            entryTeam.setSuffix(playerData.getSuffix());
        }
        Team.OptionStatus option = entryTeam.getOption(Team.Option.NAME_TAG_VISIBILITY);
        if (option == Team.OptionStatus.ALWAYS && !playerData.isNameVisibility()) {
            entryTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        } else if (option == Team.OptionStatus.NEVER && playerData.isNameVisibility()) {
            entryTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        }
        if (TabooLib.getConfig().getBoolean("TABLIST-AUTO-CLEAN-TEAM", true)) {
            TagUtils.cleanEmptyTeamInScoreboard(scoreboard);
        }
    }

    private void cancelPlayerVariable(Player player, TagPlayerData playerData) {
        if (playerData == null) {
            return;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = TagUtils.getScoreboardComputeIfAbsent(player);
            Team entryTeam = scoreboard.getTeam(playerData.getTeamHash());
            if (entryTeam != null && entryTeam.getEntries().contains(playerData.getNameDisplay())) {
                entryTeam.removeEntry(playerData.getNameDisplay());
            }
            TagUtils.cleanEntryInScoreboard(scoreboard, playerData.getNameDisplay());
            TagUtils.cleanEmptyTeamInScoreboard(scoreboard);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (enabled) {
            downloadPlayerVariable(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        cancelPlayerVariable(e.getPlayer(), unregisterPlayerData(e.getPlayer()));
    }

    public static io.izzel.taboolib.util.tag.TagDataHandler getHandler() {
        return handler;
    }
}
