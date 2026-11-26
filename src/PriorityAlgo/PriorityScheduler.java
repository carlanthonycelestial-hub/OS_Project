package src.PriorityAlgo;

import src.Scheduler.CPUScheduler;
import src.Scheduler.CPUScheduler.Block;
import src.Scheduler.CPUScheduler.Job;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Preemptive Priority Scheduler (lower number = higher priority).
 */
public final class PriorityScheduler implements CPUScheduler {

    @Override
    public String name() {
        return "Priority (Preemptive)";
    }

    private static final Comparator<Job> HIGHER_PRIORITY = Comparator.comparingInt((Job j) -> j.priority)
            .thenComparingInt(j -> j.arrival)
            .thenComparingInt(j -> j.index);

    public static List<Block> schedule(List<Job> jobs) {
        List<Block> blocks = new ArrayList<>();
        int n = jobs.size();
        int completed = 0;
        int time = 0;

        for (Job j : jobs) {
            j.remaining = j.burst;
        }

        while (completed < n) {
            Job best = null;
            int nextArrival = Integer.MAX_VALUE;
            for (Job j : jobs) {
                if (j.remaining == 0) continue;
                if (j.arrival <= time) {
                    if (best == null || HIGHER_PRIORITY.compare(j, best) < 0) {
                        best = j;
                    }
                } else {
                    nextArrival = Math.min(nextArrival, j.arrival);
                }
            }

            if (best == null) {
                CPUScheduler.appendBlock(blocks, "IDLE", time, nextArrival, true);
                time = nextArrival;
                continue;
            }

            int runUntil = Math.min(time + best.remaining, nextArrival);
            CPUScheduler.appendBlock(blocks, best.pid, time, runUntil, false);
            best.remaining -= (runUntil - time);
            time = runUntil;

            if (best.remaining == 0) {
                best.completion = time;
                best.turnaround = best.completion - best.arrival;
                best.waiting = best.turnaround - best.burst;
                completed++;
            }
        }
        return blocks;
    }

    public static double[] averages(List<Job> jobs) {
        return CPUScheduler.averages(jobs);
    }
}
