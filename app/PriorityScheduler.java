package app;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PriorityScheduler {
    private PriorityScheduler() {}

    public static final class Job {
        public final String pid;
        public final int arrival, burst, priority, index;
        public int remaining, completion, turnaround, waiting;

        public Job(String pid, int arrival, int burst, int priority, int index) {
            this.pid = pid;
            this.arrival = arrival;
            this.burst = burst;
            this.priority = priority;
            this.index = index;
            this.remaining = burst;
        }
    }

    private static final Comparator<Job> HIGHER_PRIORITY =
            Comparator.comparingInt((Job j) -> j.priority)
                    .thenComparingInt(j -> j.arrival)
                    .thenComparingInt(j -> j.index);

    public static List<SJFScheduler.Block> schedule(List<Job> jobs) {
        List<SJFScheduler.Block> blocks = new ArrayList<>();
        int n = jobs.size();
        int completed = 0;
        int time = 0;

        while (completed < n) {
            Job best = null;
            int nextArrival = Integer.MAX_VALUE;
            for (Job j : jobs) {
                if (j.remaining == 0) continue;
                if (j.arrival <= time) {
                    if (best == null || HIGHER_PRIORITY.compare(j, best) < 0) best = j;
                } else {
                    nextArrival = Math.min(nextArrival, j.arrival);
                }
            }

            if (best == null) {
                appendBlock(blocks, "IDLE", time, nextArrival, true);
                time = nextArrival;
                continue;
            }

            int runUntil = Math.min(time + best.remaining, nextArrival);
            appendBlock(blocks, best.pid, time, runUntil, false);
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

    private static void appendBlock(List<SJFScheduler.Block> blocks, String label, int start, int end, boolean idle) {
        if (start >= end) return;
        if (!blocks.isEmpty()) {
            SJFScheduler.Block last = blocks.get(blocks.size() - 1);
            if (last.idle == idle && last.label.equals(label) && last.end == start) {
                blocks.set(blocks.size() - 1, new SJFScheduler.Block(label, last.start, end, idle));
                return;
            }
        }
        blocks.add(new SJFScheduler.Block(label, start, end, idle));
    }

    public static double[] averages(List<Job> jobs) {
        double ct = 0, tat = 0, wt = 0;
        for (Job j : jobs) {
            ct += j.completion;
            tat += j.turnaround;
            wt += j.waiting;
        }
        int n = jobs.size();
        return new double[]{ct / n, tat / n, wt / n};
    }
}
