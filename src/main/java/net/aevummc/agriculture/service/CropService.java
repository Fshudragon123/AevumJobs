package net.aevummc.agriculture.service;

import net.aevummc.agriculture.AevumAgriculture;
import net.aevummc.agriculture.data.AgricultureDatabase;
import net.aevummc.agriculture.model.AgricultureRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Timestamp-driven physical crop fields. Each loaded crop has one visual and one small interaction hitbox. */
public final class CropService {
    private final AevumAgriculture plugin;
    private final Map<String, AgricultureDatabase.CropRow> loaded = new HashMap<>();
    private final Map<String, List<UUID>> visuals = new HashMap<>();
    private final Set<String> pendingHarvest = new HashSet<>();
    private final NamespacedKey visualMarkerKey;

    public CropService(AevumAgriculture plugin) {
        this.plugin = plugin;
        visualMarkerKey = new NamespacedKey(plugin, "crop_visual_entity");
    }

    public void loadChunk(Chunk chunk) {
        int cx=chunk.getX(), cz=chunk.getZ(); UUID world=chunk.getWorld().getUID();
        for (AgricultureDatabase.CropRow row : plugin.database().loadChunk(world,chunk.getWorld().getName(),cx,cz)) {
            if (plugin.registry().crop(row.cropId()) == null) {
                plugin.getLogger().warning("Skipping crop with unknown ID '"+row.cropId()+"' at "+row.worldName()+":"+row.x()+","+row.y()+","+row.z());
                continue;
            }
            loaded.put(row.key(),row);
            updateVisual(row);
        }
    }

    public void unloadChunk(Chunk chunk) {
        UUID world=chunk.getWorld().getUID(); int cx=chunk.getX(),cz=chunk.getZ();
        List<String> keys=loaded.values().stream().filter(r->r.worldId().equals(world)&&(r.x()>>4)==cx&&(r.z()>>4)==cz).map(AgricultureDatabase.CropRow::key).toList();
        for(String key:keys){removeVisual(key);loaded.remove(key);pendingHarvest.remove(key);}
    }

    public void tickGrowth() {
        long now=System.currentTimeMillis();
        for(AgricultureDatabase.CropRow row:new ArrayList<>(loaded.values())) {
            World world=Bukkit.getWorld(row.worldId());
            if(world==null||!world.isChunkLoaded(row.x()>>4,row.z()>>4))continue;
            int stage=stage(row,world,now);
            ItemDisplay display=findDisplay(row.key());
            if(display==null||display.getPersistentDataContainer().getOrDefault(visualMarkerKey,PersistentDataType.INTEGER,-1)!=stage) updateVisual(row);
        }
    }

    public boolean hasCropAt(Block farmland) { return loaded.containsKey(key(farmland.getWorld().getUID(),farmland.getX(),farmland.getY(),farmland.getZ())); }
    public AgricultureDatabase.CropRow cropAt(Block farmland) { return loaded.get(key(farmland.getWorld().getUID(),farmland.getX(),farmland.getY(),farmland.getZ())); }

    public boolean plant(Player player, Block farmland, ItemStack seed) {
        if(!player.hasPermission("aevumagriculture.plant"))return false;
        String cropId=plugin.items().seedCropId(seed);
        AgricultureRegistry.Crop crop=plugin.registry().crop(cropId);
        if(crop==null)return false;
        if(farmland.getType()!=Material.FARMLAND){player.sendMessage(plugin.message("Custom seeds must be planted on farmland."));return false;}
        if(!farmland.getRelative(0,1,0).isEmpty()){player.sendMessage(plugin.message("There is no room for that crop to grow."));return false;}
        String key=key(farmland.getWorld().getUID(),farmland.getX(),farmland.getY(),farmland.getZ());
        if(loaded.containsKey(key)){player.sendMessage(plugin.message("A crop is already planted here."));return false;}
        int ownerLimit=plugin.getConfig().getInt("settings.max-crops-per-player",600);
        if(plugin.database().countCrops(player.getUniqueId())>=ownerLimit){player.sendMessage(plugin.message("You have reached your farm limit of "+ownerLimit+" crops."));return false;}
        int chunkLimit=plugin.getConfig().getInt("settings.max-crops-per-chunk",48);
        if(plugin.database().countChunkCrops(farmland.getWorld().getUID(),farmland.getChunk().getX(),farmland.getChunk().getZ())>=chunkLimit){player.sendMessage(plugin.message("This chunk has reached its crop limit."));return false;}
        AgricultureDatabase.CropRow row=new AgricultureDatabase.CropRow(farmland.getWorld().getUID(),farmland.getWorld().getName(),farmland.getX(),farmland.getY(),farmland.getZ(),crop.id(),player.getUniqueId(),System.currentTimeMillis(),0,0);
        seed.setAmount(seed.getAmount()-1);
        if(!plugin.database().insertCrop(row)){
            seed.setAmount(seed.getAmount()+1);player.sendMessage(plugin.message("That crop could not be planted. Please try again."));return false;
        }
        loaded.put(key,row);updateVisual(row);
        player.playSound(farmland.getLocation(),Sound.ITEM_CROP_PLANT,0.6f,1.05f);
        player.sendMessage(plugin.message("Planted "+crop.name()+". It should be ready in about "+ItemFactory.formatTime(crop.growthSeconds())+"."));
        plugin.database().addActivity(player.getUniqueId(),player.getName(),1,0,0,0);
        return true;
    }

    public boolean applyBoneMeal(Player player, Block farmland) {
        AgricultureDatabase.CropRow row=cropAt(farmland);
        if(row==null)return false;
        if(player.getInventory().getItemInMainHand().getType()!=Material.BONE_MEAL && player.getInventory().getItemInOffHand().getType()!=Material.BONE_MEAL)return false;
        int max=plugin.getConfig().getInt("growth.bone-meal-max-applications",4);
        if(row.boneMealUses()>=max){player.sendMessage(plugin.message("This crop has received all the bone meal it can use."));return true;}
        AgricultureRegistry.Crop crop=plugin.registry().crop(row.cropId());
        if(crop==null)return true;
        long increase=(long)(crop.growthSeconds()*1000L*plugin.getConfig().getDouble("growth.bone-meal-growth-fraction",0.16));
        if(!plugin.database().addBoneMeal(row.worldId(),row.x(),row.y(),row.z(),increase)){player.sendMessage(plugin.message("Bone meal could not be applied."));return true;}
        consumeBoneMeal(player);
        AgricultureDatabase.CropRow updated=new AgricultureDatabase.CropRow(row.worldId(),row.worldName(),row.x(),row.y(),row.z(),row.cropId(),row.owner(),row.plantedAt(),row.boostMillis()+increase,row.boneMealUses()+1);
        loaded.put(row.key(),updated);updateVisual(updated);
        player.playSound(farmland.getLocation(),Sound.ITEM_BONE_MEAL_USE,0.7f,1.1f);
        player.sendMessage(plugin.message("Bone meal nourished the "+crop.name()+". Growth accelerated."));
        return true;
    }


    public void harvest(Player player,AgricultureDatabase.CropRow row,Block farmland) {
        if(!player.hasPermission("aevumagriculture.harvest"))return;
        if(plugin.getConfig().getBoolean("settings.protect-player-crops",true)&&!row.owner().equals(player.getUniqueId())&&!player.hasPermission("aevumagriculture.admin")){
            player.sendMessage(plugin.message("That crop belongs to another farmer."));return;
        }
        String key=row.key();
        if(pendingHarvest.contains(key))return;
        AgricultureRegistry.Crop crop=plugin.registry().crop(row.cropId());
        if(crop==null)return;
        World world=Bukkit.getWorld(row.worldId());if(world==null)return;
        if(stage(row,world,System.currentTimeMillis())<5){
            long remaining=Math.max(0,(long)(crop.growthSeconds()*1000L)-(System.currentTimeMillis()-row.plantedAt()+row.boostMillis()));
            player.sendMessage(plugin.message(crop.name()+" is still growing. Approximately "+ItemFactory.formatTime((int)(remaining/1000))+" remain."));return;
        }
        pendingHarvest.add(key);
        List<String> regrowth = plugin.getConfig().getStringList("growth.natural-regrowth-crops");
        boolean regrows = regrowth.stream().anyMatch(id -> id.equalsIgnoreCase(crop.id()));
        if (regrows) {
            AgricultureDatabase.CropRow reset = new AgricultureDatabase.CropRow(row.worldId(), row.worldName(), row.x(), row.y(), row.z(), row.cropId(), row.owner(), System.currentTimeMillis(), 0L, 0);
            if (!plugin.database().resetCropGrowth(row.worldId(), row.x(), row.y(), row.z(), reset.plantedAt())) { pendingHarvest.remove(key); return; }
            loaded.put(key, reset);
            updateVisual(reset);
        } else {
            if(!plugin.database().deleteCrop(row.worldId(),row.x(),row.y(),row.z())){pendingHarvest.remove(key);return;}
            loaded.remove(key);removeVisual(key);
        }
        pendingHarvest.remove(key);
        int amount=ThreadLocalRandom.current().nextInt(crop.minYield(),crop.maxYield()+1);
        give(player,plugin.items().produce(crop,amount));
        if(ThreadLocalRandom.current().nextDouble()<crop.seedReturnChance())give(player,plugin.items().seed(crop,1));
        int min=plugin.getConfig().getInt("progression.harvest-xp-min",4),max=plugin.getConfig().getInt("progression.harvest-xp-max",9);
        int xp=ThreadLocalRandom.current().nextInt(Math.max(1,min),Math.max(min+1,max+1));
        var profile=plugin.database().addActivity(player.getUniqueId(),player.getName(),xp,1,0,0);
        player.playSound(farmland.getLocation(),Sound.BLOCK_CROP_BREAK,0.75f,1.0f);
        world.spawnParticle(Particle.HAPPY_VILLAGER,farmland.getLocation().add(0.5,1.0,0.5),5,0.2,0.25,0.2,0.01);
        player.sendMessage(plugin.message("Harvested "+amount+" × "+crop.name()+". +"+xp+" Agriculture XP (Level "+profile.level()+")."));
    }

    public boolean craft(Player player,String recipeId,String stationId) {
        if(!player.hasPermission("aevumagriculture.process"))return false;
        AgricultureRegistry.Recipe recipe=plugin.registry().recipe(recipeId);
        if(recipe==null||!recipe.station().equalsIgnoreCase(stationId))return false;
        for(var ingredient:recipe.ingredients().entrySet()){
            if(count(player,ingredient.getKey())<ingredient.getValue()){
                player.sendMessage(plugin.message("Missing ingredient: "+ingredient.getValue()+" × "+plugin.registry().itemName(ingredient.getKey())+"."));return false;
            }
        }
        for(var ingredient:recipe.ingredients().entrySet())remove(player,ingredient.getKey(),ingredient.getValue());
        ItemStack result=plugin.items().createById(recipe.result(),recipe.amount());
        if(result==null){player.sendMessage(plugin.message("Recipe output '"+recipe.result()+"' is not registered."));return false;}
        give(player,result);
        var profile=plugin.database().addActivity(player.getUniqueId(),player.getName(),recipe.xp(),0,1,0);
        player.playSound(player.getLocation(),Sound.BLOCK_SMITHING_TABLE_USE,0.65f,1.15f);
        player.sendMessage(plugin.message("Prepared "+recipe.name()+". +"+recipe.xp()+" Agriculture XP (Level "+profile.level()+")."));return true;
    }

    public void shutdown() {
        for(List<UUID> ids:visuals.values())for(UUID id:ids){var entity=Bukkit.getEntity(id);if(entity!=null)entity.remove();}
        visuals.clear();loaded.clear();pendingHarvest.clear();
    }

    private int stage(AgricultureDatabase.CropRow row,World world,long now) {
        AgricultureRegistry.Crop crop=plugin.registry().crop(row.cropId());if(crop==null)return 0;
        double multiplier=plugin.getConfig().getDouble("growth.hydrated-farmland-speed-multiplier",1.0);
        Block block=world.getBlockAt(row.x(),row.y(),row.z());
        if(block.getBlockData() instanceof Farmland farmland && farmland.getMoisture()<4) multiplier=plugin.getConfig().getDouble("growth.dry-farmland-speed-multiplier",0.82);
        double elapsed=Math.max(0,now-row.plantedAt()+row.boostMillis())*multiplier;
        double progress=elapsed/(crop.growthSeconds()*1000.0);
        return Math.max(0,Math.min(5,(int)Math.floor(progress*5.0)));
    }

    private void updateVisual(AgricultureDatabase.CropRow row) {
        World world=Bukkit.getWorld(row.worldId());if(world==null||!world.isChunkLoaded(row.x()>>4,row.z()>>4))return;
        removeVisual(row.key());
        AgricultureRegistry.Crop crop=plugin.registry().crop(row.cropId());if(crop==null)return;
        int stage=stage(row,world,System.currentTimeMillis());
        Location location=new Location(world,row.x()+0.5,row.y()+1.02,row.z()+0.5);
        ItemDisplay display=world.spawn(location,ItemDisplay.class,entity->{
            entity.setItemStack(plugin.items().cropStage(crop,stage));
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            entity.setBillboard(Display.Billboard.VERTICAL);
            entity.setPersistent(false);entity.setInvulnerable(true);entity.setSilent(true);entity.setGravity(false);
            entity.setViewRange(0.8f);
            entity.getPersistentDataContainer().set(visualMarkerKey,PersistentDataType.INTEGER,stage);
        });
        visuals.put(row.key(),List.of(display.getUniqueId()));
    }

    private ItemDisplay findDisplay(String key) {
        List<UUID> ids=visuals.get(key);if(ids==null||ids.isEmpty())return null;
        var entity=Bukkit.getEntity(ids.get(0));return entity instanceof ItemDisplay d?d:null;
    }
    private void removeVisual(String key) {
        List<UUID> ids=visuals.remove(key);if(ids==null)return;
        for(UUID id:ids){var entity=Bukkit.getEntity(id);if(entity!=null)entity.remove();}
    }
    private Block block(AgricultureDatabase.CropRow row) {
        World world=Bukkit.getWorld(row.worldId());return world==null?null:world.getBlockAt(row.x(),row.y(),row.z());
    }
    private String key(UUID world,int x,int y,int z){return world+":"+x+":"+y+":"+z;}
    private void consumeBoneMeal(Player player){
        ItemStack main=player.getInventory().getItemInMainHand();ItemStack off=player.getInventory().getItemInOffHand();
        if(main.getType()==Material.BONE_MEAL){main.setAmount(main.getAmount()-1);return;}
        if(off.getType()==Material.BONE_MEAL)off.setAmount(off.getAmount()-1);
    }
    private int count(Player player,String id){int total=0;for(ItemStack item:player.getInventory().getContents())if(id.equals(plugin.items().itemId(item)))total+=item.getAmount();return total;}
    private void remove(Player player,String id,int amount){int left=amount;ItemStack[] contents=player.getInventory().getContents();for(int i=0;i<contents.length&&left>0;i++){ItemStack item=contents[i];if(!id.equals(plugin.items().itemId(item)))continue;int take=Math.min(left,item.getAmount());item.setAmount(item.getAmount()-take);if(item.getAmount()<=0)contents[i]=null;left-=take;}player.getInventory().setContents(contents);}
    private void give(Player player,ItemStack stack){var leftovers=player.getInventory().addItem(stack);leftovers.values().forEach(item->player.getWorld().dropItemNaturally(player.getLocation(),item));}
}
