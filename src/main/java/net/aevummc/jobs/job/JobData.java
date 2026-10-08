package net.aevummc.jobs.job;

import java.util.concurrent.atomic.AtomicLong;

public final class JobData {
    private final JobType job;
    private int level;
    private double xp;
    private long totalXp;
    private double totalEarned;
    private long actions;
    private boolean level50Rewarded;
    private final AtomicLong version = new AtomicLong();

    public JobData(JobType job) { this.job = job; }
    public JobType job() { return job; }
    public int level() { return level; }
    public double xp() { return xp; }
    public long totalXp() { return totalXp; }
    public double totalEarned() { return totalEarned; }
    public long actions() { return actions; }
    public boolean level50Rewarded() { return level50Rewarded; }
    public long version() { return version.get(); }
    public void setLevel(int value) { level = Math.max(0, Math.min(50, value)); version.incrementAndGet(); }
    public void setXp(double value) { xp = Math.max(0, value); version.incrementAndGet(); }
    public void setTotalXp(long value) { totalXp = Math.max(0, value); version.incrementAndGet(); }
    public void setTotalEarned(double value) { totalEarned = Math.max(0, value); version.incrementAndGet(); }
    public void setActions(long value) { actions = Math.max(0, value); version.incrementAndGet(); }
    public void setLevel50Rewarded(boolean value) { level50Rewarded = value; version.incrementAndGet(); }
    public void addXp(double amount) { if (amount > 0) { xp += amount; totalXp += Math.round(amount); version.incrementAndGet(); } }
    public void addActions(long amount) { if (amount > 0) { actions += amount; version.incrementAndGet(); } }
    public void addEarned(double amount) { if (amount > 0) { totalEarned += amount; version.incrementAndGet(); } }
}