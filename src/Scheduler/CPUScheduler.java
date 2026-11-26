package src.Scheduler;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared interface and models for CPU scheduling algorithms (SJF, Priority, Multilevel Queue).
 */
public interface CPUScheduler {

    /** Common Job representation across CPU schedulers. */
    class Job {
        public final String pid;
        public final int arrival;
        public final int burst;
        public final int priority;
        public final int queue; // 1 = Foreground/RR, 2 = Background/FCFS
        public final int index;

        public int remaining;
        public int completion;
        public int turnaround;
        public int waiting;

        public Job(String pid, int arrival, int burst, int index) {
            this(pid, arrival, burst, 0, 1, index);
        }

        public Job(String pid, int arrival, int burst, int priority, int index) {
            this(pid, arrival, burst, priority, 1, index);
        }

        public Job(String pid, int arrival, int burst, int priority, int queue, int index) {
            this.pid = pid;
            this.arrival = arrival;
            this.burst = burst;
            this.priority = priority;
            this.queue = queue;
            this.index = index;
            this.remaining = burst;
        }
    }

    /** A scheduled block of CPU execution (used by GanttChartPanel). */
    final class Block {
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

    String name();

    /** Helper to calculate average Completion, Turnaround, and Waiting times. */
    static double[] averages(List<Job> jobs) {
        if (jobs.isEmpty()) return new double[] { 0, 0, 0 };
        double ct = 0, tat = 0, wt = 0;
        for (Job j : jobs) {
            ct += j.completion;
            tat += j.turnaround;
            wt += j.waiting;
        }
        int n = jobs.size();
        return new double[] { ct / n, tat / n, wt / n };
    }

    /** Helper to append or merge consecutive execution blocks for the same process. */
    static void appendBlock(List<Block> blocks, String label, int start, int end, boolean idle) {
        if (start >= end) return;
        if (!blocks.isEmpty()) {
            Block last = blocks.get(blocks.size() - 1);
            if (last.idle == idle && last.label.equals(label) && last.end == start) {
                blocks.set(blocks.size() - 1, new Block(label, last.start, end, idle));
                return;
            }
        }
        blocks.add(new Block(label, start, end, idle));
    }
}
