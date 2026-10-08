package net.aevummc.jobs.gui;

import net.aevummc.jobs.AevumJobs;
import net.aevummc.jobs.config.ConfigManager;
import net.aevummc.jobs.database.Database;
import net.aevummc.jobs.job.JobData;
import net.aevummc.jobs.job.JobType;
import net.aevummc.jobs.reward.RewardService;
import net.aevummc.jobs.service.JobService;
import net.aevummc.jobs.service.PlayerJobs;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public final class GuiManager {
    public enum Menu { MAIN, SELECT, MANAGE, CONFIRM, PROGRESSION, REWARDS, STATS, LEADERBOARD }
    public static final int[] JOB_SLOTS = {10,12,14,16,19,21,23,25,28,30,32,34};

    private final AevumJobs plugin;
    private final ConfigManager config;
    private final JobService jobs;
    private final RewardService rewards;

    public GuiManager(AevumJobs plugin, ConfigManager config, JobService jobs, RewardService rewards) {
        this.plugin = plugin; this.config = config; this.jobs = jobs; this.rewards = rewards;
    }

    public void openMain(Player p, PlayerJobs d) {
        Inventory inv = Bukkit.createInventory(new Holder(Menu.MAIN,null,null),54, title("gui.main-title","🏛 A E V U M • J O B S"));
        decorate(inv); addJobCards(inv,d); addActive(inv,d);
        inv.setItem(49,item(Material.BOOK,"§6§lYour Statistics",List.of("§7Every profession. Total XP. Total earned.","§eClick to view.")));
        inv.setItem(50,item(Material.LECTERN,"§6§lManage Professions",List.of("§7Change your active professions.","§eClick to manage.")));
        inv.setItem(52,item(Material.GOLD_INGOT,"§6§lLeaderboards",List.of("§7Top specialists across Aevum.","§eClick to view.")));
        inv.setItem(53,item(Material.BARRIER,"§cClose",List.of()));
        p.openInventory(inv);
    }

    public void openSelection(Player p, PlayerJobs d) {
        Inventory inv = Bukkit.createInventory(new Holder(Menu.SELECT,null,null),54,title("gui.selection-title","Choose Your Profession"));
        decorate(inv); addJobCards(inv,d);
        inv.setItem(45,item(Material.IRON_INGOT,"§6§lActive Slots",List.of("§7Used: §f"+d.activeCount()+"§7 / §f"+config.activeLimit())));
        inv.setItem(49,item(Material.ARROW,"§eBack",List.of())); inv.setItem(53,item(Material.BARRIER,"§cClose",List.of()));
        p.openInventory(inv);
    }

    public void openManage(Player p, PlayerJobs d) {
        Inventory inv = Bukkit.createInventory(new Holder(Menu.MANAGE,null,null),54,title("gui.manage-title","Manage Your Professions"));
        decorate(inv); int slot=20;
        for(JobType j:d.active()) {
            JobData jd=d.get(j);
            inv.setItem(slot++,item(j.getMaterial(),"§6§l"+j.getIcon()+" "+j.getDisplayName(),
                    List.of("§7Level: §f"+jd.level(),"§7XP: §f"+Math.round(jd.xp()),"","§eLeft click §7to view","§cRight click §7to leave")));
        }
        inv.setItem(49,item(Material.ARROW,"§eBack",List.of())); inv.setItem(53,item(Material.BARRIER,"§cClose",List.of())); p.openInventory(inv);
    }

    public void openConfirm(Player p, PlayerJobs d, JobType j) {
        JobData jd=d.get(j);
        Inventory inv=Bukkit.createInventory(new Holder(Menu.CONFIRM,j,null),27,"§8Leave "+j.getDisplayName()+"?");
        decorate(inv);
        inv.setItem(11,item(j.getMaterial(),"§c§lLEAVE "+j.getDisplayName(),List.of("§7Your progression will be preserved.","","§7Level: §f"+jd.level(),"§7XP: §f"+Math.round(jd.xp()),"§7Total earned: §f$"+String.format("%,.0f",jd.totalEarned()))));
        inv.setItem(13,item(Material.RED_CONCRETE,"§c§l✕ CANCEL",List.of()));
        inv.setItem(15,item(Material.LIME_CONCRETE,"§a§l✓ CONFIRM",List.of()));
        p.openInventory(inv);
    }

    public void openProgression(Player p, PlayerJobs d, JobType j) {
        JobData jd=d.get(j);
        Inventory inv=Bukkit.createInventory(new Holder(Menu.PROGRESSION,j,null),54,title("gui.progress-title","✦ Profession Progression"));
        decorate(inv);
        inv.setItem(4,item(j.getMaterial(),"§6§l"+j.getIcon()+" "+j.getDisplayName(),
                List.of("§7"+j.getDescription(),"","§fLevel §6§l"+jd.level(),"§7XP §f"+Math.round(jd.xp())+" / "+reqText(jd.level()))));
        int[] ms={10,19,28,37,46,52}; int[] levels={1,10,20,30,40,50};
        for(int i=0;i<levels.length;i++){
            int level=levels[i]; String state=jd.level()>level?"§a✓ Completed":jd.level()==level?"§6★ Current":"§8🔒 Locked";
            inv.setItem(ms[i],item(level<=jd.level()?Material.GOLD_BLOCK:Material.IRON_BLOCK,"§6§lLEVEL "+level,
                    List.of(state,"§7Cash: §f"+money(config.cashReward(level)),level==50?"§7Master Reward: §e"+masterName(j):"§7Progress milestone")));
        }
        inv.setItem(31,item(Material.EXPERIENCE_BOTTLE,"§6§lPROGRESS",List.of(progressBar(jd),"§7Total XP: §f"+jd.totalXp(),"§7Actions: §f"+jd.actions())));
        inv.setItem(41,item(Material.CHEST,"§6§lVIEW ALL REWARDS",List.of("§7See all 50 level rewards.","§eClick to view")));
        if(d.isActive(j)){
            inv.setItem(43,item(Material.RED_CONCRETE,"§c§lLEAVE PROFESSION",List.of("§7Progression will be preserved.","§eClick to manage")));
        }else{
            String slotText=d.activeCount()>=config.activeLimit()?"§c"+config.activeLimit()+"/"+config.activeLimit()+" PROFESSIONS ACTIVE":"§7Activate this profession";
            inv.setItem(43,item(Material.LIME_CONCRETE,"§a§lJOIN PROFESSION",List.of(slotText,"§eClick to join")));
        }
        inv.setItem(49,item(Material.ARROW,"§eBack",List.of())); inv.setItem(53,item(Material.BARRIER,"§cClose",List.of())); p.openInventory(inv);
    }

    public void openRewards(Player p, PlayerJobs d, JobType j){openRewards(p,d,j,0);}
    public void openRewards(Player p, PlayerJobs d, JobType j, int page) {
        Inventory inv=Bukkit.createInventory(new Holder(Menu.REWARDS,j,null,page),54,title("gui.rewards-title","Profession Rewards"));
        decorate(inv);
        int start=page*45+1,end=Math.min(50,start+44);
        for(int level=start;level<=end;level++){
            String state=d.get(j).level()>level?"§a✓ Completed":d.get(j).level()==level?"§6★ Current":"§8🔒 Locked";
            String extra=level==50?"§e+ Master Reward":"§7Money only";
            inv.setItem(9+(level-start),item(level==50?Material.NETHER_STAR:Material.GOLD_NUGGET,"§6§lLEVEL "+level,
                    List.of("§7Cash: §f"+money(config.cashReward(level)),extra,state)));
        }
        if(page>0) inv.setItem(48,item(Material.ARROW,"§ePrevious",List.of()));
        if(end<50) inv.setItem(50,item(Material.ARROW,"§eNext",List.of()));
        if(d.get(j).level()>=50&&!d.get(j).level50Rewarded())
            inv.setItem(49,item(Material.NETHER_STAR,"§6§l⭐ CLAIM MASTER REWARD",List.of("§7Requires storage space.","§eClick to claim")));
        else
            inv.setItem(49,item(Material.GOLD_BLOCK,"§6§lMASTER REWARD",List.of(d.get(j).level50Rewarded()?"§a✓ Claimed":"§8Reach Level 50")));
        inv.setItem(53,item(Material.ARROW,"§eBack",List.of())); p.openInventory(inv);
    }

    public void openStats(Player p, PlayerJobs d) {
        Inventory inv=Bukkit.createInventory(new Holder(Menu.STATS,null,null),54,title("gui.stats-title","AevumJobs Statistics"));
        decorate(inv); int slot=10;
        for(JobType j:JobType.values()){
            JobData jd=d.get(j);
            inv.setItem(slot++,item(j.getMaterial(),"§6§l"+j.getIcon()+" "+j.getDisplayName(),
                    List.of("§7Level: §f"+jd.level(),"§7XP: §f"+Math.round(jd.xp()),"§7Total XP: §f"+jd.totalXp(),"§7Actions: §f"+jd.actions(),"§7Earned: §f"+money(jd.totalEarned()))));
            if(slot==17)slot=19;if(slot==26)slot=28;if(slot==35)slot=37;if(slot==44)break;
        }
        int highest=d.all().stream().mapToInt(JobData::level).max().orElse(0);
        long totalXp=d.all().stream().mapToLong(JobData::totalXp).sum();
        double totalEarned=d.all().stream().mapToDouble(JobData::totalEarned).sum();
        inv.setItem(49,item(Material.EXPERIENCE_BOTTLE,"§6§lACCOUNT TOTALS",List.of("§7Active: §f"+d.activeCount()+" / "+config.activeLimit(),"§7Highest Level: §f"+highest,"§7Total XP: §f"+totalXp,"§7Total Earned: §f"+money(totalEarned))));
        inv.setItem(53,item(Material.ARROW,"§eBack",List.of())); p.openInventory(inv);
    }

    public void openLeaderboard(Player p, PlayerJobs d) {
        Inventory inv=Bukkit.createInventory(new Holder(Menu.LEADERBOARD,null,null),54,title("gui.leaderboard-title","AevumJobs Leaderboards"));
        decorate(inv); p.openInventory(inv);
        for(int i=0;i<JobType.values().length;i++){
            JobType job=JobType.values()[i]; int slot=JOB_SLOTS[i];
            inv.setItem(slot,item(job.getMaterial(),"§6§l"+job.getIcon()+" "+job.getDisplayName(),List.of("§7Loading top 6 specialists...")));
            jobs.leaderboard(job,6).thenAccept(list->Bukkit.getScheduler().runTask(plugin,()->{
                if(!p.isOnline()||!(p.getOpenInventory().getTopInventory().getHolder() instanceof Holder h)||h.menu()!=Menu.LEADERBOARD)return;
                List<String> lore=new ArrayList<>(); int rank=1;
                if(list.isEmpty())lore.add("§8No ranked players yet.");
                for(Database.LeaderboardEntry e:list){
                    Player online=Bukkit.getPlayer(e.uuid()); String name=online!=null?online.getName():Optional.ofNullable(Bukkit.getOfflinePlayer(e.uuid()).getName()).orElse(e.uuid().toString().substring(0,8));
                    String medal=switch(rank){case 1->"§e🥇";case 2->"§f🥈";case 3->"§6🥉";default->"§7"+rank;};
                    lore.add(medal+" §f"+name+" §7• §fLv."+e.level()+" §8("+e.totalXp()+" XP)");
                    rank++;
                }
                p.getOpenInventory().getTopInventory().setItem(slot,item(job.getMaterial(),"§6§l"+job.getIcon()+" "+job.getDisplayName(),lore));
            }));
        }
        inv.setItem(53,item(Material.ARROW,"§eBack",List.of()));
    }

    private void addJobCards(Inventory inv, PlayerJobs d) {
        JobType[] all=JobType.values();
        for(int i=0;i<all.length;i++){
            JobType j=all[i]; JobData jd=d.get(j);
            String status=d.isActive(j)?"§a● ACTIVE":"§8○ INACTIVE";
            inv.setItem(JOB_SLOTS[i],item(j.getMaterial(),"§6§l"+j.getIcon()+" "+j.getDisplayName(),
                    List.of("§7"+j.getDescription(),"",status,"§7Level: §f"+jd.level(),"§7XP: §f"+Math.round(jd.xp())+" / "+reqText(jd.level()),"§7Progress: §f"+progressPct(jd),"§7Total XP: §f"+jd.totalXp(),"§7Total Earned: §f"+money(jd.totalEarned()),"","§eClick to view")));
        }
    }

    private void addActive(Inventory inv,PlayerJobs d){
        int[] slots={45,46,47}; int i=0;
        for(JobType j:d.active()){
            JobData jd=d.get(j);
            inv.setItem(slots[i++],item(j.getMaterial(),"§6§l"+j.getIcon()+" "+j.getDisplayName(),List.of("§7Level: §f"+jd.level(),"§7XP: §f"+Math.round(jd.xp()),"§eClick to view")));
        }
        while(i<3)inv.setItem(slots[i++],item(Material.GRAY_STAINED_GLASS_PANE,"§8＋ Empty Profession Slot",List.of("§7Click to choose a profession.")));
    }

    private void decorate(Inventory inv){
        for(int s:new int[]{0,1,2,3,5,6,7,8,9,17,18,26,27,35,36,44,45,48,51,52}) inv.setItem(s,item(Material.BLACK_STAINED_GLASS_PANE,"§8",List.of()));
    }
    private ItemStack item(Material mat,String name,List<String> lore){
        ItemStack stack=new ItemStack(mat); ItemMeta meta=stack.getItemMeta();
        meta.setDisplayName(org.bukkit.ChatColor.stripColor(name));
        List<String> clean=new ArrayList<>(); for(String l:lore)clean.add(org.bukkit.ChatColor.stripColor(l));
        meta.setLore(clean); stack.setItemMeta(meta); return stack;
    }
    private String title(String path,String fallback){return config.config().getString(path,fallback);}
    private String reqText(int level){return level>=50?"MAX":String.format("%,.0f XP",config.requiredXp(level));}
    private String money(double v){return String.format("$%,.0f",v);}
    private String progressPct(JobData d){if(d.level()>=50)return "100%";return String.format("%.0f%%",Math.min(100,d.xp()/config.requiredXp(d.level())*100));}
    private String progressBar(JobData d){if(d.level()>=50)return "§6████████████████████ 100%";int filled=(int)Math.round(Math.min(1,d.xp()/config.requiredXp(d.level()))*20);return "§6"+"█".repeat(filled)+"§8"+"░".repeat(20-filled)+" §f"+progressPct(d);}
    private String masterName(JobType j){return config.config().getString("master-rewards."+j.key()+".name","Master "+j.getDisplayName());}
    public static final class Holder implements InventoryHolder{
        private final Menu menu; private final JobType job; private final UUID target; private final int page;
        public Holder(Menu menu,JobType job,UUID target){this(menu,job,target,0);}
        public Holder(Menu menu,JobType job,UUID target,int page){this.menu=menu;this.job=job;this.target=target;this.page=page;}
        public Menu menu(){return menu;} public JobType job(){return job;} public UUID target(){return target;} public int page(){return page;}
        @Override public Inventory getInventory(){return null;}
    }
}
