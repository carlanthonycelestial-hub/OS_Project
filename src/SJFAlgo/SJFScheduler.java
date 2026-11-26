package src.SJFAlgo;

import src.Scheduler.CPUScheduler;
import src.Scheduler.CPUScheduler.Block;
import src.Scheduler.CPUScheduler.Job;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Non-preemptive Shortest Job First (SJF) Scheduler.
 */
public final class SJFScheduler implements CPUScheduler {

    @Override
    public String name() {
        return "SJF";
    }

    private static final Comparator<Job> SHORTEST_FIRST = Comparator.comparingInt((Job j) -> j.burst)
            .thenComparingInt(j -> j.arrival)
            .thenComparingInt(j -> j.index);

    public static List<Block> schedule(List<Job> jobs) {
        List<Block> blocks = new ArrayList<>();
        List<Job> pending = new ArrayList<>(jobs);
        int time = 0;

        while (!pending.isEmpty()) {
            Job next = null;
            int earliestArrival = Integer.MAX_VALUE;
            for (Job j : pending) {
                earliestArrival = Math.min(earliestArrival, j.arrival);
                if (j.arrival <= time && (next == null || SHORTEST_FIRST.compare(j, next) < 0)) {
                    next = j;
                }
            }

            if (next == null) {
                blocks.add(new Block("IDLE", time, earliestArrival, true));
                time = earliestArrival;
                continue;
            }

            next.completion = time + next.burst;
            next.turnaround = next.completion - next.arrival;
            next.waiting = next.turnaround - next.burst;
            blocks.add(new Block(next.pid, time, next.completion, false));
            time = next.completion;
            pending.remove(next);
        }
        return blocks;
    }

    public static double[] averages(List<Job> jobs) {
        return CPUScheduler.averages(jobs);
    }
}
