package net.aevummc.jobs.reward;

import net.aevummc.jobs.AevumJobs;
import net.aevummc.jobs.config.ConfigManager;
import net.aevummc.jobs.economy.EconomyService;
import net.aevummc.jobs.job.JobType;
import net.aevummc.jobs.service.PlayerJobs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public final class RewardService {
    private final AevumJobs plugin;
    private final ConfigManager config;
    private final EconomyService economy;
    public RewardService(AevumJobs plugin, ConfigManager config, EconomyService economy) { this.plugin=plugin; this.config=config; this.economy=economy; }

    public void handleLevelReward(Player player, PlayerJobs data, JobType job, int level) {
        double amount = config.cashReward(level);
        if (amount > 0 && economy.available()) {
            if (economy.deposit(player, amount)) data.get(job).addEarned(amount);
            else plugin.getLogger().warning("Economy payout failed for " + player.getName() + " at " + job.key() + " level " + level);
        }
        player.sendActionBar(Component.text(job.getIcon()+" "+job.getDisplayName()+" LEVEL "+level+"  •  +"+economy.format(amount), NamedTextColor.GOLD));
        player.showTitle(net.kyori.adventure.title.Title.title(Component.text(job.getIcon()+" "+job.getDisplayName()+" LEVEL UP!", NamedTextColor.GOLD),Component.text("LEVEL "+(level-1)+" → "+level+"  •  +"+economy.format(amount),NamedTextColor.GRAY),net.kyori.adventure.title.Title.Times.times(java.time.Duration.ofMillis(100),java.time.Duration.ofMillis(900),java.time.Duration.ofMillis(300))));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.25f);
        if (config.config().getBoolean("level-up-effects.particles", true)) player.getWorld().spawnParticle(Particle.GLOW, player.getLocation().add(0,1,0), 18, .5,.7,.5,.02);
        if (level == 50) celebrateMaster(player, data, job);
        else plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getGui().openProgression(player, data, job));
    }

    private void celebrateMaster(Player player, PlayerJobs data, JobType job) {
        player.showTitle(net.kyori.adventure.title.Title.title(Component.text("✦ PROFESSION MASTERED ✦", NamedTextColor.GOLD),Component.text(job.getIcon()+" "+job.getDisplayName()+" • LEVEL 50",NamedTextColor.YELLOW),net.kyori.adventure.title.Title.Times.times(java.time.Duration.ofMillis(150),java.time.Duration.ofMillis(1600),java.time.Duration.ofMillis(450))));
        player.sendActionBar(Component.text("⭐ MASTER REWARD • $30,000", NamedTextColor.GOLD));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, .95f);
        Location l=player.getLocation().add(0,1,0);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,l,60,1,1.2,1,.05);
        List<ItemStack> rewards = buildMasterRewards(job);
        if (countEmpty(player) < rewards.size()) {
            data.get(job).setLevel50Rewarded(false);
            player.sendMessage("§6§lAevumJobs §7» §eYour inventory is full. Make space, then run §6/jobs §eto claim your Master reward.");
            return;
        }
        for(ItemStack reward:rewards) player.getInventory().addItem(reward);
        data.get(job).setLevel50Rewarded(true);
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getGui().openRewards(player, data, job));
    }

    public boolean claimMaster(Player player, PlayerJobs data, JobType job) {
        if (data.get(job).level() < 50 || data.get(job).level50Rewarded()) return false;
        if (player.getInventory().firstEmpty() == -1) { player.sendMessage("§6§lAevumJobs §7» §cMake space in your inventory before claiming the Master reward."); return false; }
        List<ItemStack> rewards=buildMasterRewards(job); if(countEmpty(player)<rewards.size()) return false; for(ItemStack reward:rewards)player.getInventory().addItem(reward); data.get(job).setLevel50Rewarded(true); plugin.getJobs().save(data); return true;
    }
    private List<ItemStack> buildMasterRewards(JobType job) {
        String path="master-rewards."+job.key();
        if(job==JobType.ALCHEMIST && config.config().getBoolean(path+".bundle", true)) {
            List<ItemStack> out=new ArrayList<>(); String[] defaults={"FIRE_RESISTANCE","STRENGTH","REGENERATION"}; List<String> types=config.config().getStringList(path+".potions"); if(types.isEmpty()) types=Arrays.asList(defaults);
            for(String type:types){ItemStack x=new ItemStack(Material.POTION);org.bukkit.inventory.meta.PotionMeta pm=(org.bukkit.inventory.meta.PotionMeta)x.getItemMeta(); try{pm.setBasePotionType(org.bukkit.potion.PotionType.valueOf(type.toUpperCase(Locale.ROOT)));}catch(Exception ignored){} pm.displayName(Component.text(config.config().getString(path+".name","🧪 Master Alchemist's Bundle"),NamedTextColor.GOLD)); x.setItemMeta(pm);out.add(x);} return out;
        }
        return List.of(buildConfiguredItem(job));
    }
    private ItemStack buildConfiguredItem(JobType job) {
        String path="master-rewards."+job.key(); Material material=Material.matchMaterial(config.config().getString(path+".material", defaultMaterial(job).name())); if(material==null)material=defaultMaterial(job);
        ItemStack item=new ItemStack(material);ItemMeta meta=item.getItemMeta(); meta.displayName(Component.text(config.config().getString(path+".name",defaultName(job)),NamedTextColor.GOLD));meta.lore(List.of(Component.text("Aevum Masterwork",NamedTextColor.GRAY),Component.text("Level 50 Profession Reward",NamedTextColor.DARK_GRAY)));item.setItemMeta(meta);
        if(job==JobType.ENCHANTER || job==JobType.SCHOLAR){if(meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta sm){Enchantment e=resolveEnchant(config.config().getString(path+".stored-enchant","MENDING"));if(e!=null)sm.addStoredEnchant(e,config.config().getInt(path+".stored-level",1),true);item.setItemMeta(sm);}}
        for(String entry:config.config().getStringList(path+".enchants")){String[] p=entry.split(":",2);Enchantment e=resolveEnchant(p[0]);if(e!=null){int lvl=p.length>1?Integer.parseInt(p[1]):1;item.addUnsafeEnchantment(e,lvl);}} return item;
    }
    private int countEmpty(Player p){int n=0;for(ItemStack x:p.getInventory().getStorageContents())if(x==null||x.getType()==Material.AIR)n++;return n;}
    private Enchantment resolveEnchant(String key){ try { return Enchantment.getByName(key.toUpperCase(Locale.ROOT)); } catch(Exception e){return null;} }
    private Material defaultMaterial(JobType j){ return switch(j){case MINER,BUILDER->Material.DIAMOND_PICKAXE; case FARMER->Material.DIAMOND_HOE; case LUMBERJACK,RANCHER->Material.DIAMOND_AXE; case FISHERMAN->Material.FISHING_ROD; case WARRIOR->Material.DIAMOND_SWORD; case HUNTER->Material.BOW; case ALCHEMIST->Material.POTION; case ENCHANTER,SCHOLAR->Material.ENCHANTED_BOOK; case BLACKSMITH->Material.DIAMOND_CHESTPLATE;}; }
    private String defaultName(JobType j){ return switch(j){case MINER->"⛏ Master Miner's Pickaxe";case FARMER->"🌾 Master Farmer's Hoe";case LUMBERJACK->"🪓 Master Lumberjack's Axe";case FISHERMAN->"🎣 Master Fisherman's Rod";case RANCHER->"🐄 Master Rancher's Axe";case WARRIOR->"⚔ Master Warrior's Sword";case HUNTER->"🏹 Master Hunter's Bow";case BUILDER->"🧱 Master Builder's Pickaxe";case ALCHEMIST->"🧪 Master Alchemist's Bundle";case ENCHANTER->"✨ Master Enchanter's Book";case BLACKSMITH->"💎 Master Blacksmith's Equipment";case SCHOLAR->"📚 Master Scholar's Book";}; }
}