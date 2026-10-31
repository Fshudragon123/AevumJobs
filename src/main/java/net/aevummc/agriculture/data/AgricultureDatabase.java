package net.aevummc.agriculture.data;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Small indexed SQLite store. Crop growth is timestamp-based; no per-crop timers are persisted. */
public final class AgricultureDatabase implements AutoCloseable {
    public record CropRow(UUID worldId, String worldName, int x, int y, int z, String cropId,
                          UUID owner, long plantedAt, long boostMillis, int boneMealUses) {
        public String key() { return worldId + ":" + x + ":" + y + ":" + z; }
    }
    public record Profile(UUID uuid, String name, long xp, int level, long harvests, long crafts, long sales) {}
    public record Business(UUID owner, String ownerName, String name, String type, String town, boolean open, long createdAt, long reputation) {}
    public record Stock(String itemId, int amount, double price) {}
    public record Farmer(UUID uuid, String name, long xp, long harvests, long crafts) {}

    private final Logger logger;
    private Connection connection;

    public AgricultureDatabase(File file, Logger logger) throws SQLException {
        this.logger = logger;
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new SQLException("Could not create database directory");
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        try (Statement s = connection.createStatement()) {
            s.execute("PRAGMA journal_mode=WAL");
            s.execute("PRAGMA synchronous=NORMAL");
            s.execute("PRAGMA foreign_keys=ON");
            s.execute("CREATE TABLE IF NOT EXISTS crops(world_uuid TEXT NOT NULL,world_name TEXT NOT NULL,x INTEGER NOT NULL,y INTEGER NOT NULL,z INTEGER NOT NULL,crop_id TEXT NOT NULL,owner_uuid TEXT NOT NULL,planted_at INTEGER NOT NULL,boost_ms INTEGER NOT NULL DEFAULT 0,bone_meal_uses INTEGER NOT NULL DEFAULT 0,PRIMARY KEY(world_uuid,x,y,z))");
            s.execute("CREATE INDEX IF NOT EXISTS idx_crops_owner ON crops(owner_uuid)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_crops_chunk ON crops(world_uuid,x,z)");
            s.execute("CREATE TABLE IF NOT EXISTS profiles(uuid TEXT PRIMARY KEY,name TEXT NOT NULL,xp INTEGER NOT NULL DEFAULT 0,harvests INTEGER NOT NULL DEFAULT 0,crafts INTEGER NOT NULL DEFAULT 0,sales INTEGER NOT NULL DEFAULT 0)");
            s.execute("CREATE TABLE IF NOT EXISTS businesses(owner_uuid TEXT PRIMARY KEY,owner_name TEXT NOT NULL,name TEXT NOT NULL COLLATE NOCASE UNIQUE,type TEXT NOT NULL,town TEXT NOT NULL DEFAULT '',is_open INTEGER NOT NULL DEFAULT 1,created_at INTEGER NOT NULL,reputation INTEGER NOT NULL DEFAULT 0)");
            s.execute("CREATE TABLE IF NOT EXISTS business_stock(owner_uuid TEXT NOT NULL,item_id TEXT NOT NULL,amount INTEGER NOT NULL DEFAULT 0,price REAL NOT NULL DEFAULT 0,PRIMARY KEY(owner_uuid,item_id),FOREIGN KEY(owner_uuid) REFERENCES businesses(owner_uuid) ON DELETE CASCADE)");
            s.execute("CREATE TABLE IF NOT EXISTS market_sales(id INTEGER PRIMARY KEY AUTOINCREMENT,item_id TEXT NOT NULL,quantity INTEGER NOT NULL,unit_price REAL NOT NULL,sold_at INTEGER NOT NULL,seller_uuid TEXT NOT NULL,kind TEXT NOT NULL)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_market_item_time ON market_sales(item_id,sold_at)");
        }
    }

    private Connection db() throws SQLException { if (connection == null || connection.isClosed()) throw new SQLException("Agriculture database is closed"); return connection; }
    private void log(SQLException ex) { logger.log(Level.SEVERE, "AevumAgriculture database operation failed.", ex); }

    public synchronized List<CropRow> loadChunk(UUID world, String worldName, int chunkX, int chunkZ) {
        List<CropRow> rows = new ArrayList<>();
        String sql = "SELECT x,y,z,crop_id,owner_uuid,planted_at,boost_ms,bone_meal_uses FROM crops WHERE world_uuid=? AND x>=? AND x<? AND z>=? AND z<?";
        try (PreparedStatement p = db().prepareStatement(sql)) {
            p.setString(1, world.toString()); p.setInt(2, chunkX * 16); p.setInt(3, chunkX * 16 + 16); p.setInt(4, chunkZ * 16); p.setInt(5, chunkZ * 16 + 16);
            try (ResultSet r = p.executeQuery()) { while (r.next()) rows.add(new CropRow(world, worldName, r.getInt(1), r.getInt(2), r.getInt(3), r.getString(4), UUID.fromString(r.getString(5)), r.getLong(6), r.getLong(7), r.getInt(8))); }
        } catch (SQLException ex) { log(ex); }
        return rows;
    }
    public synchronized boolean insertCrop(CropRow row) {
        try (PreparedStatement p = db().prepareStatement("INSERT INTO crops(world_uuid,world_name,x,y,z,crop_id,owner_uuid,planted_at,boost_ms,bone_meal_uses) VALUES(?,?,?,?,?,?,?,?,?,?)")) {
            p.setString(1,row.worldId().toString());p.setString(2,row.worldName());p.setInt(3,row.x());p.setInt(4,row.y());p.setInt(5,row.z());p.setString(6,row.cropId());p.setString(7,row.owner().toString());p.setLong(8,row.plantedAt());p.setLong(9,row.boostMillis());p.setInt(10,row.boneMealUses());p.executeUpdate();return true;
        } catch (SQLException ex) { if (!ex.getMessage().toLowerCase(Locale.ROOT).contains("unique")) log(ex); return false; }
    }
    public synchronized boolean deleteCrop(UUID world,int x,int y,int z) {
        try (PreparedStatement p=db().prepareStatement("DELETE FROM crops WHERE world_uuid=? AND x=? AND y=? AND z=?")) {p.setString(1,world.toString());p.setInt(2,x);p.setInt(3,y);p.setInt(4,z);return p.executeUpdate()>0;} catch(SQLException ex){log(ex);return false;}
    }
    public synchronized boolean resetCropGrowth(UUID world,int x,int y,int z,long plantedAt) {
        try (PreparedStatement p=db().prepareStatement("UPDATE crops SET planted_at=?,boost_ms=0,bone_meal_uses=0 WHERE world_uuid=? AND x=? AND y=? AND z=?")) {
            p.setLong(1,plantedAt);p.setString(2,world.toString());p.setInt(3,x);p.setInt(4,y);p.setInt(5,z);return p.executeUpdate()>0;
        } catch(SQLException ex){log(ex);return false;}
    }
    public synchronized boolean addBoneMeal(UUID world,int x,int y,int z,long millis) {
        try (PreparedStatement p=db().prepareStatement("UPDATE crops SET boost_ms=boost_ms+?,bone_meal_uses=bone_meal_uses+1 WHERE world_uuid=? AND x=? AND y=? AND z=?")) {p.setLong(1,millis);p.setString(2,world.toString());p.setInt(3,x);p.setInt(4,y);p.setInt(5,z);return p.executeUpdate()>0;} catch(SQLException ex){log(ex);return false;}
    }
    public synchronized int countCrops(UUID owner) {
        try(PreparedStatement p=db().prepareStatement("SELECT COUNT(*) FROM crops WHERE owner_uuid=?")){p.setString(1,owner.toString());try(ResultSet r=p.executeQuery()){return r.next()?r.getInt(1):0;}}catch(SQLException ex){log(ex);return Integer.MAX_VALUE;}
    }
    public synchronized int countChunkCrops(UUID world,int chunkX,int chunkZ) {
        try(PreparedStatement p=db().prepareStatement("SELECT COUNT(*) FROM crops WHERE world_uuid=? AND x>=? AND x<? AND z>=? AND z<?")){p.setString(1,world.toString());p.setInt(2,chunkX*16);p.setInt(3,chunkX*16+16);p.setInt(4,chunkZ*16);p.setInt(5,chunkZ*16+16);try(ResultSet r=p.executeQuery()){return r.next()?r.getInt(1):Integer.MAX_VALUE;}}catch(SQLException ex){log(ex);return Integer.MAX_VALUE;}
    }

    public synchronized Profile profile(UUID uuid,String name) {
        try(PreparedStatement p=db().prepareStatement("INSERT INTO profiles(uuid,name) VALUES(?,?) ON CONFLICT(uuid) DO UPDATE SET name=excluded.name")){p.setString(1,uuid.toString());p.setString(2,name);p.executeUpdate();}
        catch(SQLException ex){log(ex);}
        try(PreparedStatement p=db().prepareStatement("SELECT xp,harvests,crafts,sales,name FROM profiles WHERE uuid=?")){p.setString(1,uuid.toString());try(ResultSet r=p.executeQuery()){if(r.next()){long xp=r.getLong(1);return new Profile(uuid,r.getString(5),xp,levelForXp(xp),r.getLong(2),r.getLong(3),r.getLong(4));}}}catch(SQLException ex){log(ex);}
        return new Profile(uuid,name,0,1,0,0,0);
    }
    public synchronized Profile addActivity(UUID uuid,String name,long xp,long harvests,long crafts,long sales) {
        profile(uuid,name);
        try(PreparedStatement p=db().prepareStatement("UPDATE profiles SET xp=xp+?,harvests=harvests+?,crafts=crafts+?,sales=sales+?,name=? WHERE uuid=?")){p.setLong(1,Math.max(0,xp));p.setLong(2,Math.max(0,harvests));p.setLong(3,Math.max(0,crafts));p.setLong(4,Math.max(0,sales));p.setString(5,name);p.setString(6,uuid.toString());p.executeUpdate();}catch(SQLException ex){log(ex);}
        return profile(uuid,name);
    }
    private int levelForXp(long xp) {
        int level=1; long remaining=Math.max(0,xp); long need=90;
        while(level<50 && remaining>=need){remaining-=need;level++;need=90L+(level-1L)*55L;}
        return level;
    }
    public synchronized List<Farmer> topFarmers(int limit) {
        List<Farmer> list=new ArrayList<>();
        try(PreparedStatement p=db().prepareStatement("SELECT uuid,name,xp,harvests,crafts FROM profiles ORDER BY xp DESC,harvests DESC LIMIT ?")){p.setInt(1,Math.max(1,limit));try(ResultSet r=p.executeQuery()){while(r.next())list.add(new Farmer(UUID.fromString(r.getString(1)),r.getString(2),r.getLong(3),r.getLong(4),r.getLong(5)));}}catch(SQLException ex){log(ex);}return list;
    }

    public synchronized boolean createBusiness(UUID owner,String ownerName,String name,String type,String town) {
        try(PreparedStatement p=db().prepareStatement("INSERT INTO businesses(owner_uuid,owner_name,name,type,town,is_open,created_at) VALUES(?,?,?,?,?,1,?)")){p.setString(1,owner.toString());p.setString(2,ownerName);p.setString(3,name);p.setString(4,type);p.setString(5,town==null?"":town);p.setLong(6,System.currentTimeMillis());p.executeUpdate();return true;}catch(SQLException ex){if(!ex.getMessage().toLowerCase(Locale.ROOT).contains("unique"))log(ex);return false;}
    }
    public synchronized Business business(UUID owner) {
        try(PreparedStatement p=db().prepareStatement("SELECT owner_uuid,owner_name,name,type,town,is_open,created_at,reputation FROM businesses WHERE owner_uuid=?")){p.setString(1,owner.toString());try(ResultSet r=p.executeQuery()){if(r.next())return new Business(owner,r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getInt(6)==1,r.getLong(7),r.getLong(8));}}catch(SQLException ex){log(ex);}return null;
    }
    public synchronized List<Business> openBusinesses(int limit) {
        List<Business> list = new ArrayList<>();
        try (PreparedStatement p = db().prepareStatement("SELECT owner_uuid,owner_name,name,type,town,is_open,created_at,reputation FROM businesses WHERE is_open=1 ORDER BY reputation DESC,created_at ASC LIMIT ?")) {
            p.setInt(1, Math.max(1, Math.min(100, limit)));
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) list.add(new Business(UUID.fromString(r.getString(1)), r.getString(2), r.getString(3), r.getString(4), r.getString(5), r.getInt(6) == 1, r.getLong(7), r.getLong(8)));
            }
        } catch (SQLException ex) { log(ex); }
        return list;
    }
    public synchronized Business businessByName(String name) {
        try(PreparedStatement p=db().prepareStatement("SELECT owner_uuid,owner_name,name,type,town,is_open,created_at,reputation FROM businesses WHERE name=? COLLATE NOCASE")){p.setString(1,name);try(ResultSet r=p.executeQuery()){if(r.next())return new Business(UUID.fromString(r.getString(1)),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getInt(6)==1,r.getLong(7),r.getLong(8));}}catch(SQLException ex){log(ex);}return null;
    }
    public synchronized boolean setBusinessOpen(UUID owner,boolean open) {
        try(PreparedStatement p=db().prepareStatement("UPDATE businesses SET is_open=? WHERE owner_uuid=?")){p.setInt(1,open?1:0);p.setString(2,owner.toString());return p.executeUpdate()>0;}catch(SQLException ex){log(ex);return false;}
    }
    public synchronized void depositStock(UUID owner,String itemId,int amount) {
        try(PreparedStatement p=db().prepareStatement("INSERT INTO business_stock(owner_uuid,item_id,amount,price) VALUES(?,?,?,0) ON CONFLICT(owner_uuid,item_id) DO UPDATE SET amount=amount+excluded.amount")){p.setString(1,owner.toString());p.setString(2,itemId);p.setInt(3,amount);p.executeUpdate();}catch(SQLException ex){log(ex);}
    }
    public synchronized boolean setStockPrice(UUID owner,String itemId,double price) {
        try(PreparedStatement p=db().prepareStatement("INSERT INTO business_stock(owner_uuid,item_id,amount,price) VALUES(?,?,0,?) ON CONFLICT(owner_uuid,item_id) DO UPDATE SET price=excluded.price")){p.setString(1,owner.toString());p.setString(2,itemId);p.setDouble(3,price);p.executeUpdate();return true;}catch(SQLException ex){log(ex);return false;}
    }
    public synchronized boolean takeStock(UUID owner,String itemId,int amount) {
        try(PreparedStatement p=db().prepareStatement("UPDATE business_stock SET amount=amount-? WHERE owner_uuid=? AND item_id=? AND amount>=?")){p.setInt(1,amount);p.setString(2,owner.toString());p.setString(3,itemId);p.setInt(4,amount);return p.executeUpdate()>0;}catch(SQLException ex){log(ex);return false;}
    }
    public synchronized List<Stock> stock(UUID owner) {
        List<Stock> list=new ArrayList<>();try(PreparedStatement p=db().prepareStatement("SELECT item_id,amount,price FROM business_stock WHERE owner_uuid=? AND amount>0 ORDER BY item_id")){p.setString(1,owner.toString());try(ResultSet r=p.executeQuery()){while(r.next())list.add(new Stock(r.getString(1),r.getInt(2),r.getDouble(3)));}}catch(SQLException ex){log(ex);}return list;
    }
    public synchronized Stock stock(UUID owner,String itemId) {
        try(PreparedStatement p=db().prepareStatement("SELECT item_id,amount,price FROM business_stock WHERE owner_uuid=? AND item_id=?")){p.setString(1,owner.toString());p.setString(2,itemId);try(ResultSet r=p.executeQuery()){if(r.next())return new Stock(r.getString(1),r.getInt(2),r.getDouble(3));}}catch(SQLException ex){log(ex);}return null;
    }
    public synchronized void recordSale(String itemId,int quantity,double unitPrice,UUID seller,String kind) {
        try(PreparedStatement p=db().prepareStatement("INSERT INTO market_sales(item_id,quantity,unit_price,sold_at,seller_uuid,kind) VALUES(?,?,?,?,?,?)")){p.setString(1,itemId);p.setInt(2,quantity);p.setDouble(3,unitPrice);p.setLong(4,System.currentTimeMillis());p.setString(5,seller.toString());p.setString(6,kind);p.executeUpdate();}catch(SQLException ex){log(ex);}
    }
    public synchronized int soldLast24h(String itemId) {
        try(PreparedStatement p=db().prepareStatement("SELECT COALESCE(SUM(quantity),0) FROM market_sales WHERE item_id=? AND sold_at>=? AND kind='npc'")){p.setString(1,itemId);p.setLong(2,System.currentTimeMillis()-86_400_000L);try(ResultSet r=p.executeQuery()){return r.next()?r.getInt(1):0;}}catch(SQLException ex){log(ex);return 0;}
    }
    public synchronized void addBusinessReputation(UUID owner,long amount) {
        try(PreparedStatement p=db().prepareStatement("UPDATE businesses SET reputation=reputation+? WHERE owner_uuid=?")){p.setLong(1,amount);p.setString(2,owner.toString());p.executeUpdate();}catch(SQLException ex){log(ex);}
    }
    @Override public synchronized void close() { if(connection!=null)try{connection.close();}catch(SQLException ex){log(ex);} }
}
