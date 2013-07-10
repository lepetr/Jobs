/**
 * Jobs Plugin for Bukkit
 * Copyright (C) 2011 Zak Ford <zak.j.ford@gmail.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.zford.jobs.bukkit.listeners;

import java.util.List;

import me.zford.jobs.Jobs;
import me.zford.jobs.Player;
import me.zford.jobs.bukkit.BukkitUtil;
import me.zford.jobs.bukkit.JobsPlugin;
import me.zford.jobs.bukkit.actions.BlockActionInfo;
import me.zford.jobs.bukkit.actions.EntityActionInfo;
import me.zford.jobs.bukkit.actions.ItemActionInfo;
import me.zford.jobs.config.ConfigManager;
import me.zford.jobs.container.ActionType;
import me.zford.jobs.container.JobsPlayer;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

public class JobsPaymentListener implements Listener {
    private JobsPlugin plugin;
    private final String furnaceOwnerMetadata = "jobsFurnaceOwner";
    private final String brewingOwnerMetadata = "jobsBrewingOwner";
    private final String mobSpawnerMetadata = "jobsMobSpawner";
    
    public JobsPaymentListener(JobsPlugin plugin){
        this.plugin = plugin;
    }
    
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onBlockBreak(BlockBreakEvent event) {
        // remove furnace metadata for broken block
        Block block = event.getBlock();
        if (block == null)
            return;
        
        if (block.getType().equals(Material.FURNACE) && block.hasMetadata(furnaceOwnerMetadata))
            block.removeMetadata(furnaceOwnerMetadata, plugin);
        
        // make sure plugin is enabled
        if(!plugin.isEnabled()) return;
        
        Player player = BukkitUtil.wrapPlayer(event.getPlayer());
        
        if (!player.isOnline())
            return;
        
        // check if in creative
        if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE) && !ConfigManager.getJobsConfiguration().payInCreative())
            return;
        
        if (!Jobs.getPermissionHandler().hasWorldPermission(player, player.getLocation().getWorld()))
            return;
        
        // restricted area multiplier
        double multiplier = ConfigManager.getJobsConfiguration().getRestrictedMultiplier(player);
        JobsPlayer jPlayer = Jobs.getPlayerManager().getJobsPlayer(player.getName());
        Jobs.action(jPlayer, new BlockActionInfo(block, ActionType.BREAK), multiplier);
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block == null)
            return;
        
        // make sure plugin is enabled
        if(!plugin.isEnabled()) return;
        
        // check to make sure you can build
        if(!event.canBuild()) return;

        Player player = BukkitUtil.wrapPlayer(event.getPlayer());
        
        if (!player.isOnline())
            return;
        
        // check if in creative
        if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE) && !ConfigManager.getJobsConfiguration().payInCreative())
            return;
        
        if (!Jobs.getPermissionHandler().hasWorldPermission(player, player.getLocation().getWorld()))
            return;
        
        // restricted area multiplier
        double multiplier = ConfigManager.getJobsConfiguration().getRestrictedMultiplier(player);
        JobsPlayer jPlayer = Jobs.getPlayerManager().getJobsPlayer(player.getName());
        Jobs.action(jPlayer, new BlockActionInfo(block, ActionType.PLACE), multiplier);
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onPlayerFish(PlayerFishEvent event) {
        // make sure plugin is enabled
        if(!plugin.isEnabled()) return;

        Player player = BukkitUtil.wrapPlayer(event.getPlayer());
        
        // check if in creative
        if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE) && !ConfigManager.getJobsConfiguration().payInCreative())
            return;

        if (!Jobs.getPermissionHandler().hasWorldPermission(player, player.getLocation().getWorld()))
            return;
        
        // restricted area multiplier
        double multiplier = ConfigManager.getJobsConfiguration().getRestrictedMultiplier(player);
        
        if (event.getState().equals(PlayerFishEvent.State.CAUGHT_FISH) && event.getCaught() instanceof Item) {
            JobsPlayer jPlayer = Jobs.getPlayerManager().getJobsPlayer(player.getName());
            ItemStack items = ((Item) event.getCaught()).getItemStack();
            Jobs.action(jPlayer, new ItemActionInfo(items, ActionType.FISH), multiplier);
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onInventoryCraft(InventoryClickEvent e) {
        if (!(e instanceof CraftItemEvent))
            return;
        CraftItemEvent event = (CraftItemEvent) e;
        // make sure plugin is enabled
        if(!plugin.isEnabled()) return;
        
        // If event is nothing, do nothing
        if (event.getAction() == InventoryAction.NOTHING)
            return;
        
        CraftingInventory inv = event.getInventory();
        
        if (!(inv instanceof CraftingInventory) || !event.getSlotType().equals(SlotType.RESULT))
            return;
        
        Recipe recipe = event.getRecipe();
        
        if (recipe == null)
            return;
        
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player))
            return;
        
        org.bukkit.entity.Player bukkitPlayer = (org.bukkit.entity.Player) event.getWhoClicked();
        Player player = BukkitUtil.wrapPlayer(bukkitPlayer);
        
        ItemStack resultStack = recipe.getResult();
        
        if (resultStack == null)
            return;
        
        if (!Jobs.getPermissionHandler().hasWorldPermission(player, player.getLocation().getWorld()))
            return;
        
        // check if in creative
        if (bukkitPlayer.getGameMode().equals(GameMode.CREATIVE) && !ConfigManager.getJobsConfiguration().payInCreative())
            return;
        
        double multiplier = ConfigManager.getJobsConfiguration().getRestrictedMultiplier(player);
        JobsPlayer jPlayer = Jobs.getPlayerManager().getJobsPlayer(player.getName());
        Jobs.action(jPlayer, new ItemActionInfo(resultStack, ActionType.CRAFT), multiplier);
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onInventoryRepair(InventoryClickEvent event) {
        // make sure plugin is enabled
        if(!plugin.isEnabled()) return;
        Inventory inv = event.getInventory();
        
        // If event is nothing, do nothing
        if (event.getAction() == InventoryAction.NOTHING)
            return;
        
        // must be anvil inventory
        if (!(inv instanceof AnvilInventory))
            return;
        
        // Must be "container" slot 9
        if (!event.getSlotType().equals(SlotType.CONTAINER) || event.getSlot() != 2)
            return;
        
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player))
            return;
        
        org.bukkit.entity.Player bukkitPlayer = (org.bukkit.entity.Player) event.getWhoClicked();
        Player player = BukkitUtil.wrapPlayer(bukkitPlayer);
        
        ItemStack resultStack = event.getCurrentItem();
        
        if (resultStack == null)
            return;
        
        if (!Jobs.getPermissionHandler().hasWorldPermission(player, player.getLocation().getWorld()))
            return;
        
        // check if in creative
        if (bukkitPlayer.getGameMode().equals(GameMode.CREATIVE) && !ConfigManager.getJobsConfiguration().payInCreative())
            return;
        
        double multiplier = ConfigManager.getJobsConfiguration().getRestrictedMultiplier(player);
        JobsPlayer jPlayer = Jobs.getPlayerManager().getJobsPlayer(player.getName());
        Jobs.action(jPlayer, new ItemActionInfo(resultStack, ActionType.REPAIR), multiplier);
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onEnchantItem(EnchantItemEvent event) {
        // make sure plugin is enabled
        if(!plugin.isEnabled()) return;
        Inventory inv = event.getInventory();
        
        if (!(inv instanceof EnchantingInventory))
            return;
        
        // restricted area multiplier
        List<HumanEntity> viewers = event.getViewers();
        if (viewers.size() == 0)
            return;
        org.bukkit.entity.Player bukkitPlayer = null;
        for (HumanEntity viewer : event.getViewers()) {
            if (viewer instanceof org.bukkit.entity.Player) {
                bukkitPlayer = (org.bukkit.entity.Player) viewer;
                break;
            }
        }
        
        if (bukkitPlayer == null)
            return;
        
        Player player = BukkitUtil.wrapPlayer(bukkitPlayer);
        
        ItemStack resultStack = ((EnchantingInventory) inv).getItem();
        
        if (resultStack == null)
            return;
        
        if (!Jobs.getPermissionHandler().hasWorldPermission(player, player.getLocation().getWorld()))
            return;
        
        // check if in creative
        if (bukkitPlayer.getGameMode().equals(GameMode.CREATIVE) && !ConfigManager.getJobsConfiguration().payInCreative())
            return;
        
        double multiplier = ConfigManager.getJobsConfiguration().getRestrictedMultiplier(player);
        JobsPlayer jPlayer = Jobs.getPlayerManager().getJobsPlayer(player.getName());
        Jobs.action(jPlayer, new ItemActionInfo(resultStack, ActionType.ENCHANT), multiplier);
    }
    
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        if (!plugin.isEnabled())
            return;
        Block block = event.getBlock();
        if (block == null)
            return;
        
        if (!block.hasMetadata(furnaceOwnerMetadata))
            return;
        List<MetadataValue> data = block.getMetadata(furnaceOwnerMetadata);
        if (data.isEmpty())
            return;
        
        // only care about first
        MetadataValue value = data.get(0);
        String playerName = value.asString();
        Player player = Jobs.getServer().getPlayerExact(playerName);
        if (player == null || !player.isOnline())
            return;
        
        if (!Jobs.getPermissionHandler().hasWorldPermission(player, player.getLocation().getWorld()))
            return;
        
        double multiplier = ConfigManager.getJobsConfiguration().getRestrictedMultiplier(player);
        JobsPlayer jPlayer = Jobs.getPlayerManager().getJobsPlayer(player.getName());
        Jobs.action(jPlayer, new ItemActionInfo(event.getResult(), ActionType.SMELT), multiplier);
    }
    
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onBrewEvent(BrewEvent event) {
        if (!plugin.isEnabled())
            return;
        Block block = event.getBlock();
        if (block == null)
            return;
        
        if (!block.hasMetadata(brewingOwnerMetadata))
            return;
        List<MetadataValue> data = block.getMetadata(brewingOwnerMetadata);
        if (data.isEmpty())
            return;
        
        // only care about first
        MetadataValue value = data.get(0);
        String playerName = value.asString();
        Player player = Jobs.getServer().getPlayerExact(playerName);
        if (player == null || !player.isOnline())
            return;
        
        if (!Jobs.getPermissionHandler().hasWorldPermission(player, player.getLocation().getWorld()))
            return;
        
        double multiplier = ConfigManager.getJobsConfiguration().getRestrictedMultiplier(player);
        JobsPlayer jPlayer = Jobs.getPlayerManager().getJobsPlayer(player.getName());
        Jobs.action(jPlayer, new ItemActionInfo(event.getContents().getIngredient(), ActionType.BREW), multiplier);
    }
    
    @EventHandler(priority=EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        // Entity that died must be living
        if(!(event.getEntity() instanceof LivingEntity))
            return;
        LivingEntity lVictim = (LivingEntity)event.getEntity();

        // mob spawner, no payment or experience
        if (lVictim.hasMetadata(mobSpawnerMetadata)) {
            lVictim.removeMetadata(mobSpawnerMetadata, plugin);
            return;
        }
        
        // make sure plugin is enabled
        if(!plugin.isEnabled())
            return;
        
        if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent){
            EntityDamageByEntityEvent e = (EntityDamageByEntityEvent)event.getEntity().getLastDamageCause();
            org.bukkit.entity.Player pDamager = null;
            if(e.getDamager() instanceof org.bukkit.entity.Player) {
                pDamager = (org.bukkit.entity.Player) e.getDamager();
            } else if(e.getDamager() instanceof Projectile && ((Projectile)e.getDamager()).getShooter() instanceof org.bukkit.entity.Player) {
                pDamager = (org.bukkit.entity.Player)((Projectile)e.getDamager()).getShooter();
            } else if(e.getDamager() instanceof Tameable) {
                Tameable t = (Tameable) e.getDamager();
                if (t.isTamed() && t.getOwner() instanceof org.bukkit.entity.Player) {
                    pDamager = (org.bukkit.entity.Player) t.getOwner();
                }
            }
            if(pDamager != null) {
                // check if in creative
                if (pDamager.getGameMode().equals(GameMode.CREATIVE) && !ConfigManager.getJobsConfiguration().payInCreative())
                    return;
                
                Player player = BukkitUtil.wrapPlayer(pDamager);
                if (!Jobs.getPermissionHandler().hasWorldPermission(player, player.getLocation().getWorld()))
                    return;
                
                // restricted area multiplier
                double multiplier = ConfigManager.getJobsConfiguration().getRestrictedMultiplier(player);
                // pay
                JobsPlayer jDamager = Jobs.getPlayerManager().getJobsPlayer(player.getName());
                Jobs.action(jDamager, new EntityActionInfo(lVictim.getType(), ActionType.KILL), multiplier);
            }
        }
    }
    
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if(!(event.getEntity() instanceof LivingEntity))
            return;
        if(!event.getSpawnReason().equals(SpawnReason.SPAWNER))
            return;
        if(ConfigManager.getJobsConfiguration().payNearSpawner())
            return;
        LivingEntity creature = (LivingEntity)event.getEntity();
        creature.setMetadata(mobSpawnerMetadata, new FixedMetadataValue(plugin, true));
    }
    
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.isEnabled())
            return;
        
        Block block = event.getClickedBlock();
        if (block == null)
            return;
        
        if (block.getType().equals(Material.FURNACE)) {
            if (block.hasMetadata(furnaceOwnerMetadata))
                block.removeMetadata(furnaceOwnerMetadata, plugin);
            
            block.setMetadata(furnaceOwnerMetadata, new FixedMetadataValue(plugin, event.getPlayer().getName()));
        } else if (block.getType().equals(Material.BREWING_STAND)) {
            if (block.hasMetadata(brewingOwnerMetadata))
                block.removeMetadata(brewingOwnerMetadata, plugin);
            
            block.setMetadata(brewingOwnerMetadata, new FixedMetadataValue(plugin, event.getPlayer().getName()));
        }
    }
}
