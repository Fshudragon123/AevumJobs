package net.aevummc.jobs.listener;

import net.aevummc.jobs.AevumJobs;
import net.aevummc.jobs.config.ConfigManager;
import net.aevummc.jobs.gui.GuiManager;
import net.aevummc.jobs.job.JobType;
import net.aevummc.jobs.service.JobService;
import net.aevummc.jobs.service.PlayerJobs;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class JobListener implements Listener {
    private final AevumJobs plugin; private final JobService jobs; private final ConfigManager config; private final GuiManager gui;
    private final Map<String, Long> playerPlacedBlocks=new ConcurrentHashMap<>();
    private final Map<UUID, Long> breederCooldown=new ConcurrentHashMap<>();
    private final Map<String, UUID> lastBrewingPlayer=new ConcurrentHashMap<>();
    private final Set<UUID> processedDeaths=ConcurrentHashMap.newKeySet();

    public JobListener(AevumJobs plugin,JobService jobs,ConfigManager config,GuiManager gui){this.plugin=plugin;this.jobs=jobs;this.config=config;this.gui=gui;}
    @EventHandler public void onJoin(PlayerJoinEvent e){jobs.load(e.getPlayer().getUniqueId());}
    @EventHandler public void onQuit(PlayerQuitEvent e){jobs.unload(e.getPlayer().getUniqueId());breederCooldown.remove(e.getPlayer().getUniqueId());lastBrewingPlayer.values().removeIf(x->x.equals(e.getPlayer().getUniqueId()));}

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void onPlace(BlockPlaceEvent e){
        Player p=e.getPlayer();String k=key(e.getBlock().getLocation());long now=System.currentTimeMillis();long cooldown=config.config().getLong("anti-abuse.builder.placement-cooldown-ms",30000);
        Long old=playerPlacedBlocks.put(k,now);if(old!=null&&now-old<cooldown)return;
        if(playerPlacedBlocks.size()>100000) playerPlacedBlocks.entrySet().removeIf(entry->now-entry.getValue()>Math.max(cooldown,30000L));
        Material m=e.getBlock().getType();
        if(isFarmerPlant(m))jobs.addXp(p,JobType.FARMER,config.xp(JobType.FARMER,"plant",m.name()));
        else if(isLumberSapling(m))jobs.addXp(p,JobType.LUMBERJACK,config.xp(JobType.LUMBERJACK,"saplings",m.name()));
        else jobs.addXp(p,JobType.BUILDER,config.xp(JobType.BUILDER,"place",null));
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void onBreak(BlockBreakEvent e){
        String k=key(e.getBlock().getLocation());if(playerPlacedBlocks.remove(k)!=null)return;Material m=e.getBlock().getType();Player p=e.getPlayer();
        if(isAgeableCrop(e)&&isFarmerCrop(m))jobs.addXp(p,JobType.FARMER,config.xp(JobType.FARMER,"harvest",m.name()));
        else if(config.actionValues(JobType.MINER,"blocks").contains(m.name()))jobs.addXp(p,JobType.MINER,config.xp(JobType.MINER,"blocks",m.name()));
        else if(config.actionValues(JobType.LUMBERJACK,"blocks").contains(m.name()))jobs.addXp(p,JobType.LUMBERJACK,config.xp(JobType.LUMBERJACK,"blocks",m.name()));
    }
    private boolean isAgeableCrop(BlockBreakEvent e){return e.getBlock().getBlockData() instanceof Ageable a&&a.getAge()==a.getMaximumAge();}
    @EventHandler(ignoreCancelled=true) public void onFish(PlayerFishEvent e){if(e.getState()==PlayerFishEvent.State.CAUGHT_FISH)jobs.addXp(e.getPlayer(),JobType.FISHERMAN,config.xp(JobType.FISHERMAN,"catch",null));}
    @EventHandler(ignoreCancelled=true) public void onBreed(EntityBreedEvent e){if(!(e.getBreeder() instanceof Player p))return;long now=System.currentTimeMillis();long cd=config.config().getLong("anti-abuse.rancher.breeding-cooldown-ms",5000);Long last=breederCooldown.put(p.getUniqueId(),now);if(last==null||now-last>=cd)jobs.addXp(p,JobType.RANCHER,config.xp(JobType.RANCHER,"breed",null));}
    @EventHandler(ignoreCancelled=true) public void onDeath(EntityDeathEvent e){
        org.bukkit.entity.LivingEntity dead=e.getEntity();if(!processedDeaths.add(dead.getUniqueId()))return;Player killer=dead.getKiller();if(killer==null)return;
        if(dead instanceof Player victim){double mult=jobs.warriorMultiplier(killer.getUniqueId(),victim.getUniqueId());if(mult>0)jobs.addXp(killer,JobType.WARRIOR,config.xp(JobType.WARRIOR,"kill",null)*mult);}
        else if(validHunter(dead))jobs.addXp(killer,JobType.HUNTER,config.xp(JobType.HUNTER,"kill",null));
        plugin.getServer().getScheduler().runTaskLater(plugin,()->processedDeaths.remove(dead.getUniqueId()),1L);
    }
    private boolean validHunter(Entity e){return !e.hasMetadata("NPC")&&!e.hasMetadata("npc")&&!e.hasMetadata("CitizensNPC")&&!e.hasMetadata("AevumFakeEntity")&&config.config().getStringList("anti-abuse.hunter.invalid-entities").stream().noneMatch(x->x.equalsIgnoreCase(e.getType().name()));}
    @EventHandler(ignoreCancelled=true) public void onEnchant(EnchantItemEvent e){jobs.addXp(e.getEnchanter(),JobType.ENCHANTER,config.xp(JobType.ENCHANTER,"enchant",null));}
    @EventHandler(ignoreCancelled=true) public void onCraft(CraftItemEvent e){if(e.getWhoClicked() instanceof Player p&&e.getCurrentItem()!=null&&isEquipment(e.getCurrentItem().getType()))jobs.addXp(p,JobType.BLACKSMITH,config.xp(JobType.BLACKSMITH,"craft",null));}
    @EventHandler(ignoreCancelled=true) public void onBook(PlayerEditBookEvent e){if(e.isSigning())jobs.addXp(e.getPlayer(),JobType.SCHOLAR,config.xp(JobType.SCHOLAR,"book",null));}
    @EventHandler(ignoreCancelled=true) public void onOpen(InventoryOpenEvent e){if(e.getPlayer() instanceof Player p&&e.getInventory().getType()==InventoryType.BREWING){Object holder=e.getInventory().getHolder();if(holder instanceof org.bukkit.block.BrewingStand stand)lastBrewingPlayer.put("brew:"+key(stand.getLocation()),p.getUniqueId());}}
    @EventHandler(ignoreCancelled=true) public void onBrew(BrewEvent e){UUID id=lastBrewingPlayer.remove("brew:"+key(e.getBlock().getLocation()));if(id==null)return;Player p=plugin.getServer().getPlayer(id);if(p!=null)jobs.addXp(p,JobType.ALCHEMIST,config.xp(JobType.ALCHEMIST,"brew",null));}
    @EventHandler(ignoreCancelled=true) public void onCartography(InventoryClickEvent e){if(e.getWhoClicked() instanceof Player p&&e.getView().getTopInventory().getType()==InventoryType.CARTOGRAPHY&&e.getRawSlot()==2&&e.getCurrentItem()!=null&&e.getCurrentItem().getType()!=Material.AIR)jobs.addXp(p,JobType.SCHOLAR,config.xp(JobType.SCHOLAR,"map",null));}
    @EventHandler(priority=EventPriority.HIGHEST) public void onGuiClick(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p))return;if(!(e.getView().getTopInventory().getHolder() instanceof GuiManager.Holder h))return;e.setCancelled(true);int slot=e.getRawSlot();PlayerJobs data=jobs.get(p.getUniqueId());if(data==null)return;
        switch(h.menu()){
            case MAIN->{if(slot==49)gui.openStats(p,data);else if(slot==50)gui.openManage(p,data);else if(slot==52)gui.openLeaderboard(p,data);else if(slot==53)p.closeInventory();else if(Arrays.stream(GuiManager.JOB_SLOTS).anyMatch(x->x==slot)){int i=index(GuiManager.JOB_SLOTS,slot);if(i>=0)gui.openProgression(p,data,JobType.values()[i]);}else if(slot>=45&&slot<=47&&data.activeCount()<config.activeLimit())gui.openSelection(p,data);}
            case SELECT->{if(slot==49)gui.openMain(p,data);else if(slot==53)p.closeInventory();else if(Arrays.stream(GuiManager.JOB_SLOTS).anyMatch(x->x==slot)){JobType j=JobType.values()[index(GuiManager.JOB_SLOTS,slot)];if(data.isActive(j))gui.openProgression(p,data,j);else if(data.activeCount()>=config.activeLimit()){p.sendMessage("§6§lAevumJobs §7» §c⚠ 3/3 PROFESSIONS ACTIVE. Leave one profession before joining another.");gui.openManage(p,data);}else{jobs.join(data,j);p.sendMessage(config.message("joined","&6&lAevumJobs &7» &a✓ Profession activated: &f{job}&a.").replace("{job}",j.getDisplayName()).replace('&','§'));gui.openProgression(p,data,j);}}}
            case MANAGE->{if(slot==49)gui.openMain(p,data);else if(slot==53)p.closeInventory();else if(slot>=20&&slot<=22){List<JobType>a=new ArrayList<>(data.active());int i=slot-20;if(i<a.size()){JobType j=a.get(i);if(e.getClick().isRightClick())gui.openConfirm(p,data,j);else gui.openProgression(p,data,j);}}}
            case CONFIRM->{JobType j=h.job();if(slot==15){jobs.leave(data,j);p.sendMessage(config.message("left","&6&lAevumJobs &7» &eProfession left: &f{job}&e. Your progression is preserved.").replace("{job}",j.getDisplayName()).replace('&','§'));gui.openManage(p,data);}else if(slot==13)gui.openManage(p,data);}
            case PROGRESSION->{JobType j=h.job();if(slot==41)gui.openRewards(p,data,j,0);else if(slot==49)gui.openMain(p,data);else if(slot==53)p.closeInventory();else if(slot==43){if(data.isActive(j))gui.openConfirm(p,data,j);else if(data.activeCount()>=config.activeLimit()){p.sendMessage("§6§lAevumJobs §7» §c⚠ 3/3 PROFESSIONS ACTIVE.");gui.openManage(p,data);}else{jobs.join(data,j);p.sendMessage("§6§lAevumJobs §7» §a✓ Profession activated: §f"+j.getDisplayName());gui.openProgression(p,data,j);}}}
            case REWARDS->{JobType j=h.job();if(slot==53)gui.openProgression(p,data,j);else if(slot==48&&h.page()>0)gui.openRewards(p,data,j,h.page()-1);else if(slot==50&&(h.page()+1)*45<50)gui.openRewards(p,data,j,h.page()+1);else if(slot==49&&data.get(j).level()>=50&&!data.get(j).level50Rewarded()){if(plugin.getRewards().claimMaster(p,data,j))p.sendMessage("§6§lAevumJobs §7» §a⭐ Master reward claimed!");gui.openRewards(p,data,j,h.page());}}
            case STATS->{if(slot==53)gui.openMain(p,data);}
            case LEADERBOARD->{if(slot==53)gui.openMain(p,data);}
        }
    }
    private int index(int[]a,int v){for(int i=0;i<a.length;i++)if(a[i]==v)return i;return -1;}
    @EventHandler(ignoreCancelled=true) public void onAnvilSmithing(InventoryClickEvent e){if(!(e.getWhoClicked() instanceof Player p)||e.getRawSlot()<0)return;InventoryType t=e.getView().getTopInventory().getType();if((t==InventoryType.ANVIL&&e.getRawSlot()==2)||(t==InventoryType.SMITHING&&e.getRawSlot()==3)){if(e.getCurrentItem()!=null&&e.getCurrentItem().getType()!=Material.AIR)jobs.addXp(p,JobType.BLACKSMITH,t==InventoryType.ANVIL?config.xp(JobType.BLACKSMITH,"repair",null):config.xp(JobType.BLACKSMITH,"smith",null));}}
    private boolean isEquipment(Material m){String n=m.name();return n.endsWith("_SWORD")||n.endsWith("_PICKAXE")||n.endsWith("_AXE")||n.endsWith("_SHOVEL")||n.endsWith("_HOE")||n.endsWith("_HELMET")||n.endsWith("_CHESTPLATE")||n.endsWith("_LEGGINGS")||n.endsWith("_BOOTS")||n.equals("SHIELD")||n.equals("BOW")||n.equals("CROSSBOW")||n.equals("TRIDENT")||n.equals("FISHING_ROD");}
    private boolean isFarmerPlant(Material m){return config.actionValues(JobType.FARMER,"plant").contains(m.name());} private boolean isFarmerCrop(Material m){return config.actionValues(JobType.FARMER,"harvest").contains(m.name());} private boolean isLumberSapling(Material m){return config.actionValues(JobType.LUMBERJACK,"saplings").contains(m.name());}
    private String key(Location l){return l.getWorld().getUID()+":"+l.getBlockX()+":"+l.getBlockY()+":"+l.getBlockZ();}
}