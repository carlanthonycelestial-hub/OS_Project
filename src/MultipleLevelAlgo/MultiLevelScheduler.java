package src.MultipleLevelAlgo;

import src.SJFAlgo.SJFScheduler.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Multilevel Queue Scheduler.
 * Queue 1 (Foreground): Round Robin (quantum = 4) or higher priority
 * Queue 2 (Background): FCFS (First-Come, First-Served)
 * Higher queue has absolute priority over lower queue.
 */
public final class MultiLevelScheduler {

    private MultiLevelScheduler() {}

    public static final class Job {
        public final String pid;
        public final int arrival;
        public final int burst;
        public final int queue; // 1 = Foreground (High), 2 = Background (Low)
        public final int index;

        public int remaining;
        public int completion;
        public int turnaround;
        public int waiting;

        public Job(String pid, int arrival, int burst, int queue, int index) {
            this.pid = pid;
            this.arrival = arrival;
            this.burst = burst;
            this.queue = queue;
            this.index = index;
            this.remaining = burst;
        }
    }

    public static List<Block> schedule(List<Job> jobs, int timeQuantum) {
        List<Block> blocks = new ArrayList<>();
        if (jobs.isEmpty()) return blocks;

        // Reset runtime values
        for (Job j : jobs) {
            j.remaining = j.burst;
            j.completion = 0;
            j.turnaround = 0;
            j.waiting = 0;
        }

        List<Job> q1 = new ArrayList<>(); // Queue 1: Round Robin
        List<Job> q2 = new ArrayList<>(); // Queue 2: FCFS

        // Sort by arrival then index for queue entry
        List<Job> unarrived = new ArrayList<>(jobs);
        unarrived.sort(Comparator.comparingInt((Job j) -> j.arrival).thenComparingInt(j -> j.index));

        int time = 0;
        int completedCount = 0;
        int total = jobs.size();

        while (completedCount < total) {
            // Check newly arrived processes
            while (!unarrived.isEmpty() && unarrived.get(0).arrival <= time) {
                Job arr = unarrived.remove(0);
                if (arr.queue == 1) {
                    q1.add(arr);
                } else {
                    q2.add(arr);
                }
            }

            // If both queues empty, jump CPU to next arrival (IDLE)
            if (q1.isEmpty() && q2.isEmpty()) {
                if (!unarrived.isEmpty()) {
                    int nextArrival = unarrived.get(0).arrival;
                    addOrMergeBlock(blocks, "IDLE", time, nextArrival, true);
                    time = nextArrival;
                    continue;
                } else {
                    break;
                }
            }

            // Absolute priority: Queue 1 has strict priority over Queue 2
            if (!q1.isEmpty()) {
                Job current = q1.remove(0);
                int runTime = Math.min(current.remaining, timeQuantum);
                int startTime = time;
                int endTime = time + runTime;

                addOrMergeBlock(blocks, current.pid, startTime, endTime, false);
                current.remaining -= runTime;
                time = endTime;

                // Enqueue any processes that arrived during this time slice
                while (!unarrived.isEmpty() && unarrived.get(0).arrival <= time) {
                    Job arr = unarrived.remove(0);
                    if (arr.queue == 1) {
                        q1.add(arr);
                    } else {
                        q2.add(arr);
                    }
                }

                if (current.remaining == 0) {
                    current.completion = time;
                    current.turnaround = current.completion - current.arrival;
                    current.waiting = current.turnaround - current.burst;
                    completedCount++;
                } else {
                    // Re-enqueue into Q1
                    q1.add(current);
                }
            } else {
                // Queue 2 is active (FCFS, but preemptible if high priority arrives in Q1)
                Job current = q2.get(0);
                // Run until either current completes or a job arrives for Queue 1
                int nextQ1Arrival = Integer.MAX_VALUE;
                for (Job un : unarrived) {
                    if (un.queue == 1) {
                        nextQ1Arrival = Math.min(nextQ1Arrival, un.arrival);
                    }
                }

                int runTime = current.remaining;
                if (nextQ1Arrival < time + runTime) {
                    runTime = Math.max(1, nextQ1Arrival - time);
                }

                int startTime = time;
                int endTime = time + runTime;
                addOrMergeBlock(blocks, current.pid, startTime, endTime, false);
                current.remaining -= runTime;
                time = endTime;

                // Enqueue newly arrived
                while (!unarrived.isEmpty() && unarrived.get(0).arrival <= time) {
                    Job arr = unarrived.remove(0);
                    if (arr.queue == 1) {
                        q1.add(arr);
                    } else {
                        q2.add(arr);
                    }
                }

                if (current.remaining == 0) {
                    q2.remove(0);
                    current.completion = time;
                    current.turnaround = current.completion - current.arrival;
                    current.waiting = current.turnaround - current.burst;
                    completedCount++;
                }
            }
        }

        return blocks;
    }

    private static void addOrMergeBlock(List<Block> blocks, String label, int start, int end, boolean idle) {
        if (start >= end) return;
        if (!blocks.isEmpty()) {
            Block last = blocks.get(blocks.size() - 1);
            if (last.label.equals(label) && last.idle == idle && last.end == start) {
                blocks.set(blocks.size() - 1, new Block(label, last.start, end, idle));
                return;
            }
        }
        blocks.add(new Block(label, start, end, idle));
    }

    public static double[] averages(List<Job> jobs) {
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
}
