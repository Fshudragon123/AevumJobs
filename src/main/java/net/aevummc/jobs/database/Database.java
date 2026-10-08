package net.aevummc.jobs.database;

import net.aevummc.jobs.job.JobData;
import net.aevummc.jobs.job.JobType;
import net.aevummc.jobs.service.PlayerJobs;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public final class Database {
    private final JavaPlugin plugin;
    private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "AevumJobs-DB"); t.setDaemon(true); return t;
    });
    private Connection connection;

    public Database(JavaPlugin plugin) throws SQLException {
        this.plugin = plugin;
        File dir = new File(plugin.getDataFolder(), "database");
        if (!dir.exists() && !dir.mkdirs()) throw new SQLException("Unable to create database directory");
        try { Class.forName("org.sqlite.JDBC"); } catch (ClassNotFoundException ignored) { }
        connection = DriverManager.getConnection("jdbc:sqlite:" + new File(dir, "jobs.db"));
        connection.setAutoCommit(true);
        init();
    }
    private synchronized void init() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("PRAGMA journal_mode=WAL");
            st.executeUpdate("PRAGMA busy_timeout=5000");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS player_jobs (uuid TEXT NOT NULL, job TEXT NOT NULL, level INTEGER NOT NULL DEFAULT 0, xp REAL NOT NULL DEFAULT 0, total_xp INTEGER NOT NULL DEFAULT 0, total_earned REAL NOT NULL DEFAULT 0, actions INTEGER NOT NULL DEFAULT 0, active INTEGER NOT NULL DEFAULT 0, reward50 INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(uuid,job))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS reward_journal (id TEXT PRIMARY KEY, uuid TEXT NOT NULL, job TEXT NOT NULL, level INTEGER NOT NULL, amount REAL NOT NULL, status TEXT NOT NULL, created_at INTEGER NOT NULL)");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_jobs_leaderboard ON player_jobs(job, level DESC, total_xp DESC)");
        }
    }
    public CompletableFuture<PlayerJobs> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerJobs data = new PlayerJobs(uuid);
            try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM player_jobs WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        JobType type = JobType.fromString(rs.getString("job")); if (type == null) continue;
                        JobData d = data.get(type);
                        d.setLevel(rs.getInt("level")); d.setXp(rs.getDouble("xp")); d.setTotalXp(rs.getLong("total_xp"));
                        d.setTotalEarned(rs.getDouble("total_earned")); d.setActions(rs.getLong("actions")); d.setLevel50Rewarded(rs.getInt("reward50") != 0);
                        if (rs.getInt("active") != 0) data.activate(type, Integer.MAX_VALUE);
                    }
                }
            } catch (SQLException e) { plugin.getLogger().severe("Could not load jobs for " + uuid + ": " + e.getMessage()); }
            return data;
        }, executor);
    }
    public CompletableFuture<Void> save(PlayerJobs data) {
        return CompletableFuture.runAsync(() -> {
            try {
                connection.setAutoCommit(false);
                try (PreparedStatement ps = connection.prepareStatement("INSERT INTO player_jobs(uuid,job,level,xp,total_xp,total_earned,actions,active,reward50) VALUES(?,?,?,?,?,?,?,?,?) ON CONFLICT(uuid,job) DO UPDATE SET level=excluded.level,xp=excluded.xp,total_xp=excluded.total_xp,total_earned=excluded.total_earned,actions=excluded.actions,active=excluded.active,reward50=excluded.reward50")) {
                    for (JobData d : data.all()) {
                        ps.setString(1, data.uuid().toString()); ps.setString(2, d.job().key()); ps.setInt(3, d.level()); ps.setDouble(4, d.xp()); ps.setLong(5, d.totalXp()); ps.setDouble(6, d.totalEarned()); ps.setLong(7, d.actions()); ps.setInt(8, data.isActive(d.job()) ? 1 : 0); ps.setInt(9, d.level50Rewarded() ? 1 : 0); ps.addBatch();
                    }
                    ps.executeBatch();
                }
                connection.commit();
            } catch (SQLException e) {
                try { connection.rollback(); } catch (SQLException ignored) { }
                plugin.getLogger().severe("Could not save jobs for " + data.uuid() + ": " + e.getMessage());
            } finally { try { connection.setAutoCommit(true); } catch (SQLException ignored) { } }
        }, executor);
    }
    public CompletableFuture<List<LeaderboardEntry>> leaderboard(JobType job, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<LeaderboardEntry> out = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement("SELECT uuid, level, total_xp FROM player_jobs WHERE job=? AND (level>0 OR total_xp>0) ORDER BY level DESC,total_xp DESC LIMIT ?")) {
                ps.setString(1, job.key()); ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(new LeaderboardEntry(UUID.fromString(rs.getString("uuid")), rs.getInt("level"), rs.getLong("total_xp"))); }
            } catch (Exception e) { plugin.getLogger().warning("Leaderboard query failed: " + e.getMessage()); }
            return out;
        }, executor);
    }
    public void close() { executor.shutdown(); try { if (connection != null && !connection.isClosed()) connection.close(); } catch (SQLException ignored) {} }
    public record LeaderboardEntry(UUID uuid, int level, long totalXp) {}
}