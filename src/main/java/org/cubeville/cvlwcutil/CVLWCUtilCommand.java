package org.cubeville.cvlwcutil;

import com.griefcraft.bukkit.EntityBlock;
import com.griefcraft.cache.BlockCache;
import com.griefcraft.lwc.LWC;
import com.griefcraft.model.LWCPlayer;
import com.griefcraft.model.Protection;
import com.griefcraft.scripting.event.LWCProtectionRegisterEvent;
import com.griefcraft.scripting.event.LWCProtectionRegistrationPostEvent;
import com.griefcraft.sql.PhysDB;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.cubeville.commons.commands.Command;
import org.cubeville.commons.commands.CommandParameterString;
import org.cubeville.commons.commands.CommandResponse;
import org.cubeville.commons.utils.BlockUtils;

import java.util.*;

public class CVLWCUtilCommand extends Command {

    final private CVLWCUtil plugin;
    final private BukkitScheduler scheduler;
    final private HashMap<UUID, Long> lockedCounters;
    final private HashMap<UUID, Long> unlockedCounters;

    public CVLWCUtilCommand(CVLWCUtil plugin) {
        super("");
        addBaseParameter(new CommandParameterString());

        this.lockedCounters = new HashMap<>();
        this.unlockedCounters = new HashMap<>();
        this.plugin = plugin;
        this.scheduler = Bukkit.getScheduler();
    }

    public CommandResponse execute(Player sender, Set<String> set, Map<String, Object> map, List<Object> baseParameters) {

        UUID uuid = sender.getUniqueId();
        if(lockedCounters.containsKey(uuid) || unlockedCounters.containsKey(uuid)) {
            return new CommandResponse(ChatColor.RED + "Wait until your previous CVLWCUtil command is finished running.");
        }

        LWC lwc = LWC.getInstance();
        LWCPlayer lwcPlayer = lwc.wrapPlayer(sender);
        if(baseParameters.get(0).toString().equals("count")) {
            List<Block> blocks = BlockUtils.getBlocksInWESelection(sender, 10000000);
            lockedCounters.put(uuid, 0L);
            unlockedCounters.put(uuid, 0L);
            int i = 0;
            for (Block block : blocks) {
                scheduler.runTaskLater(plugin, () -> {
                    if(!(lwc.findProtection(block) == null)) {
                        lockedCounters.put(uuid, lockedCounters.get(uuid) + 1);
                    } else if(lwc.isProtectable(block)) {
                        unlockedCounters.put(uuid, unlockedCounters.get(uuid) + 1);
                    }
                }, (long) (0.00001 * i));
                i++;
            }
            scheduler.runTaskLater(plugin, () -> {
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "Locked LWC protections: " + ChatColor.GOLD + lockedCounters.get(uuid));
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "Unlocked LWC protections: " + ChatColor.GOLD + unlockedCounters.get(uuid));
                lockedCounters.remove(uuid);
                unlockedCounters.remove(uuid);
            }, (long) (0.00001 * i) + 1);
            return new CommandResponse(ChatColor.LIGHT_PURPLE + "Counting the LWC protections, please wait.");
        } else if(baseParameters.get(0).equals("privateall")) {
            List<Block> blocks = BlockUtils.getBlocksInWESelection(sender, 10000000);
            int i = 0;
            lockedCounters.put(uuid, 0L);
            for (Block block : blocks) {
                scheduler.runTaskLater(plugin, () -> {
                    if(lwc.isProtectable(block) && lwc.findProtection(block) == null) {
                        PhysDB physDB = lwc.getPhysicalDatabase();
                        String worldName = block.getWorld().getName();
                        int blockX;
                        int blockY;
                        int blockZ;
                        if(block instanceof EntityBlock) {
                            Entity entity = EntityBlock.getEntity();
                            blockX = EntityBlock.POSITION_OFFSET + entity.getUniqueId().hashCode();
                            blockY = EntityBlock.POSITION_OFFSET + entity.getUniqueId().hashCode();
                            blockZ =  EntityBlock.POSITION_OFFSET + entity.getUniqueId().hashCode();
                        } else {
                            blockX = block.getX();
                            blockY = block.getY();
                            blockZ = block.getZ();
                        }
                        lwc.removeModes(lwcPlayer);
                        LWCProtectionRegisterEvent event = new LWCProtectionRegisterEvent(lwcPlayer.getBukkitPlayer(), block);
                        lwc.getModuleLoader().dispatchEvent(event);
                        BlockCache blockCache = BlockCache.getInstance();
                        int blockID = blockCache.getBlockId(block);
                        if(blockID < 0) {
                            sender.sendMessage(ChatColor.RED + "bad block id. contact admin");
                        }
                        Protection protection;
                        if(block instanceof EntityBlock) {
                            protection = physDB.registerProtection(EntityBlock.ENTITY_BLOCK_ID,
                                    Protection.Type.matchType("Private"), worldName, lwcPlayer.getUniqueId().toString(), "",
                                    blockX, blockY, blockZ);
                        } else {
                            protection = physDB.registerProtection(blockID,
                                    Protection.Type.matchType("Private"), worldName, lwcPlayer.getUniqueId().toString(), "",
                                    blockX, blockY, blockZ);
                        }
                        if(protection != null) {
                            protection.removeCache();
                            LWC.getInstance().getProtectionCache().addProtection(protection);
                            lwc.getModuleLoader().dispatchEvent(new LWCProtectionRegistrationPostEvent(protection));
                        }
                        lockedCounters.put(uuid, lockedCounters.get(uuid) + 1);
                    }
                }, (long) (0.00001 * i));
                i++;
            }
            scheduler.runTaskLater(plugin, () -> {
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "Number of LWC protections added: " + ChatColor.GOLD + lockedCounters.get(uuid));
                lockedCounters.remove(uuid);
            }, (long) (0.00001 * i) + 1);
            return new CommandResponse(ChatColor.LIGHT_PURPLE + "Adding the LWC protections, please wait.");
        } else if(baseParameters.get(0).equals("removeall")) {
            List<Block> blocks = BlockUtils.getBlocksInWESelection(sender, 10000000);
            int i = 0;
            unlockedCounters.put(uuid, 0L);
            for (Block block : blocks) {
                scheduler.runTaskLater(plugin, () -> {
                    if (lwc.isProtectable(block) && lwc.findProtection(block) != null) {
                        Protection protection = lwc.findProtection(block);
                        protection.remove();
                        unlockedCounters.put(uuid, unlockedCounters.get(uuid) + 1);
                    }
                }, (long) (0.00001 * i));
                i++;
            }
            scheduler.runTaskLater(plugin, () -> {
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "Number of LWC protections removed: " + ChatColor.GOLD + unlockedCounters.get(uuid));
                unlockedCounters.remove(uuid);
            }, (long) (0.00001 * i) + 1);
            return new CommandResponse(ChatColor.LIGHT_PURPLE + "Removing the LWC protections, please wait.");
        }
        return new CommandResponse(ChatColor.RED + "Invalid Command! " + ChatColor.LIGHT_PURPLE + "Usage: /cvlwcutil <count | privateall | removeall>");
    }
}
