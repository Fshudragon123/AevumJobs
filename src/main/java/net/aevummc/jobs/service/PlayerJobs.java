package net.aevummc.jobs.service;

import net.aevummc.jobs.job.JobData;
import net.aevummc.jobs.job.JobType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerJobs {
    private final UUID uuid;
    private final EnumMap<JobType, JobData> jobs = new EnumMap<>(JobType.class);
    private final Set<JobType> active = EnumSet.noneOf(JobType.class);
    private final Object lock = new Object();

    public PlayerJobs(UUID uuid) {
        this.uuid = uuid;
        for (JobType type : JobType.values()) jobs.put(type, new JobData(type));
    }
    public UUID uuid() { return uuid; }
    public JobData get(JobType type) { return jobs.get(type); }
    public Collection<JobData> all() { return jobs.values(); }
    public Set<JobType> active() { synchronized (lock) { return EnumSet.copyOf(active); } }
    public boolean isActive(JobType type) { synchronized (lock) { return active.contains(type); } }
    public int activeCount() { synchronized (lock) { return active.size(); } }
    public boolean activate(JobType type, int limit) { synchronized (lock) { if (active.contains(type)) return true; if (active.size() >= limit) return false; active.add(type); return true; } }
    public boolean leave(JobType type) { synchronized (lock) { return active.remove(type); } }
}