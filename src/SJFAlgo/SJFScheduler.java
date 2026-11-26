package src.SJFAlgo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SJFScheduler {
    private SJFScheduler() {
    }

    public static final class Job {
        public final String pid;
        public final int arrival, burst, index;
        public int completion, turnaround, waiting;

        public Job(String pid, int arrival, int burst, int index) {
            this.pid = pid;
            this.arrival = arrival;
            this.burst = burst;
            this.index = index;
        }
    }

    public static final class Block {
        public final String label;
        public final int start, end;
        public final boolean idle;

        public Block(String label, int start, int end, boolean idle) {
            this.label = label;
            this.start = start;
            this.end = end;
            this.idle = idle;
        }
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

            if (next == null) { // nobody has arrived yet: CPU idles until the next arrival
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
        double ct = 0, tat = 0, wt = 0;
        for (Job j : jobs) {
            ct += j.completion;
            tat += j.turnaround;
            wt += j.waiting;
        }
        int n = jobs.size();
        return new double[] { ct / n, tat / n, wt / n };
    }
}
