package net.aevummc.agriculture.listener;

import net.aevummc.agriculture.AevumAgriculture;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;

public final class AgricultureListener implements Listener {
    private final AevumAgriculture plugin;
    public AgricultureListener(AevumAgriculture plugin){this.plugin=plugin;}

    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void onInteract(PlayerInteractEvent event){
        if(event.getAction()!=Action.RIGHT_CLICK_BLOCK||event.getClickedBlock()==null)return;
        Player player=event.getPlayer();Block block=event.getClickedBlock();
        if (event.getHand() != EquipmentSlot.HAND) return;
        String stationId=stationAt(block);
        if(stationId!=null){
            event.setCancelled(true);plugin.gui().openStation(player,stationId);return;
        }
        if(block.getType()==Material.FARMLAND){
            ItemStack hand = player.getInventory().getItemInMainHand();
            boolean customCrop = plugin.crops().hasCropAt(block);
            if (customCrop && hand.getType()==Material.BONE_MEAL) {
                event.setCancelled(true);
                plugin.crops().applyBoneMeal(player,block);
                return;
            }
            if (customCrop) {
                if (!plugin.items().isSeed(hand)) {
                    event.setCancelled(true);
                    plugin.crops().harvest(player, plugin.crops().cropAt(block), block);
                }
                return;
            }
            // Leave vanilla bone-meal behaviour untouched on ordinary farmland.
            if(plugin.items().isSeed(hand)){event.setCancelled(true);plugin.crops().plant(player,block,hand);}
        }
    }

    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void onStationPlace(BlockPlaceEvent event){
        ItemStack hand=event.getItemInHand();
        if(!"station".equals(plugin.items().itemKind(hand)))return;
        String stationId=plugin.items().itemId(hand);
        String expected=plugin.getConfig().getString("stations."+stationId+".material","BARREL");
        Material material=Material.matchMaterial(expected);
        if(material==null||event.getBlockPlaced().getType()!=material){event.setCancelled(true);event.getPlayer().sendMessage(plugin.message("That station kit requires a "+expected.toLowerCase(Locale.ROOT)+" block."));return;}
        if(!(event.getBlockPlaced().getState() instanceof TileState tile)){
            event.setCancelled(true);event.getPlayer().sendMessage(plugin.message("This station block cannot store its secure identity. Contact an administrator."));return;
        }
        tile.getPersistentDataContainer().set(plugin.stationKey(),PersistentDataType.STRING,stationId);
        tile.getPersistentDataContainer().set(plugin.stationOwnerKey(),PersistentDataType.STRING,event.getPlayer().getUniqueId().toString());
        tile.update(true,false);
        event.getPlayer().sendMessage(plugin.message(plugin.getConfig().getString("stations."+stationId+".name",stationId)+" established. Right-click it to open production."));
    }

    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void onBlockBreak(BlockBreakEvent event){
        Block block=event.getBlock();
        if(plugin.crops().hasCropAt(block)){
            event.setCancelled(true);event.getPlayer().sendMessage(plugin.message("Harvest the crop before removing its farmland."));return;
        }
        String station=stationAt(block);
        if(station==null)return;
        if(!(block.getState() instanceof TileState tile))return;
        String owner=tile.getPersistentDataContainer().get(plugin.stationOwnerKey(),PersistentDataType.STRING);
        if(plugin.getConfig().getBoolean("settings.protect-player-stations",true)&&owner!=null&&!owner.equals(event.getPlayer().getUniqueId().toString())&&!event.getPlayer().hasPermission("aevumagriculture.admin")){
            event.setCancelled(true);event.getPlayer().sendMessage(plugin.message("That production station belongs to another player."));return;
        }
        event.setDropItems(false);
        ItemStack kit=plugin.items().station(station,plugin.getConfig().getString("stations."+station+".name",station),block.getType().name(),1);
        block.getWorld().dropItemNaturally(block.getLocation().add(0.5,0.5,0.5),kit);
        block.setType(Material.AIR,false);
    }

    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void onVisualDamage(EntityDamageEvent event){
        NamespacedKey visualKey = new NamespacedKey(plugin,"crop_visual_entity");
        if (event.getEntity() instanceof org.bukkit.entity.Display display
                && display.getPersistentDataContainer().has(visualKey, PersistentDataType.INTEGER)) event.setCancelled(true);
    }

    @EventHandler public void onChunkLoad(ChunkLoadEvent event){plugin.crops().loadChunk(event.getChunk());}
    @EventHandler public void onChunkUnload(ChunkUnloadEvent event){plugin.crops().unloadChunk(event.getChunk());}

    @EventHandler public void onJoin(PlayerJoinEvent event){
        plugin.getServer().getScheduler().runTaskLater(plugin,()->{if(event.getPlayer().isOnline())plugin.packManager().send(event.getPlayer());},100L);
    }

    private String stationAt(Block block){
        if(!(block.getState() instanceof TileState tile))return null;
        return tile.getPersistentDataContainer().get(plugin.stationKey(),PersistentDataType.STRING);
    }
}
