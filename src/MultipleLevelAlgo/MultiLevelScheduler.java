package src.MultipleLevelAlgo;

import src.Scheduler.CPUScheduler;
import src.Scheduler.CPUScheduler.Block;
import src.Scheduler.CPUScheduler.Job;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Multilevel Queue Scheduler.
 *
 * Queue 1 (foreground): Round Robin with the given time quantum.
 * Queue 2 (background): FCFS, preempted the moment a Queue 1 process arrives.
 * Queue 1 has strict priority over Queue 2.
 *
 * Conventions: time starts at 0, context switch = 0, idle time is shown in the Gantt chart.
 * Ties: earlier arrival first, then input order. A new arrival is queued before a
 * process that has just used up its quantum.
 *
 * Formulas: TAT = CT - AT, WT = TAT - BT, RT = first CPU time - AT,
 * utilization = busy time / total time * 100, throughput = processes / total time.
 */
public final class MultiLevelScheduler implements CPUScheduler {

    /** Everything one simulation produces. */
    public static final class Result {
        public final List<Block> blocks;
        public final Map<Job, Integer> response;   // response time per job
        public final int totalTime;
        public final double avgCompletion, avgTurnaround, avgWaiting, avgResponse;
        public final double utilization;           // percent
        public final double throughput;            // processes per time unit

        private Result(List<Job> jobs, List<Block> blocks, Map<Job, Integer> firstRun, int totalTime, int busy) {
            this.blocks = blocks;
            this.totalTime = totalTime;
            this.response = new IdentityHashMap<>();

            double ct = 0, tat = 0, wt = 0, rt = 0;
            for (Job j : jobs) {
                int r = firstRun.get(j) - j.arrival;
                response.put(j, r);
                ct += j.completion;
                tat += j.turnaround;
                wt += j.waiting;
                rt += r;
            }
            int n = Math.max(1, jobs.size());
            avgCompletion = ct / n;
            avgTurnaround = tat / n;
            avgWaiting = wt / n;
            avgResponse = rt / n;
            utilization = totalTime == 0 ? 0 : busy * 100.0 / totalTime;
            throughput = totalTime == 0 ? 0 : (double) jobs.size() / totalTime;
        }
    }

    @Override
    public String name() {
        return "Multilevel Queue";
    }

    /** Full simulation: Gantt blocks plus every metric. Fills in each Job's completion/turnaround/waiting. */
    public static Result run(List<Job> jobs, int timeQuantum) {
        List<Block> blocks = new ArrayList<>();
        Map<Job, Integer> firstRun = new IdentityHashMap<>();

        for (Job j : jobs) {
            j.remaining = j.burst;
            j.completion = 0;
            j.turnaround = 0;
            j.waiting = 0;
        }

        Deque<Job> pending1 = pendingFor(jobs, 1);   // not yet arrived, sorted by arrival
        Deque<Job> pending2 = pendingFor(jobs, 2);
        Deque<Job> ready1 = new ArrayDeque<>();      // arrived, waiting for the CPU
        Deque<Job> ready2 = new ArrayDeque<>();

        int time = 0, busy = 0, left = jobs.size();

        while (left > 0) {
            admit(pending1, ready1, time);
            admit(pending2, ready2, time);

            if (ready1.isEmpty() && ready2.isEmpty()) {          // idle: jump to the next arrival
                int next = Math.min(nextArrival(pending1), nextArrival(pending2));
                CPUScheduler.appendBlock(blocks, "IDLE", time, next, true);
                time = next;
                continue;
            }

            Job current;
            int run;
            if (!ready1.isEmpty()) {                             // Q1: at most one quantum
                current = ready1.pollFirst();
                run = Math.min(current.remaining, timeQuantum);
            } else {                                             // Q2: until done or a Q1 job arrives
                current = ready2.peekFirst();
                run = Math.min(current.remaining, nextArrival(pending1) - time);
            }

            firstRun.putIfAbsent(current, time);
            CPUScheduler.appendBlock(blocks, current.pid, time, time + run, false);
            current.remaining -= run;
            time += run;
            busy += run;

            admit(pending1, ready1, time);                       // new arrivals go in first...
            admit(pending2, ready2, time);

            if (current.remaining > 0) {
                if (current.queue == 1) ready1.addLast(current); // ...then the preempted Q1 job
            } else {
                if (current.queue == 2) ready2.pollFirst();
                current.completion = time;
                current.turnaround = time - current.arrival;
                current.waiting = current.turnaround - current.burst;
                left--;
            }
        }
        return new Result(jobs, blocks, firstRun, time, busy);
    }

    /** Kept so existing callers still work. */
    public static List<Block> schedule(List<Job> jobs, int timeQuantum) {
        return run(jobs, timeQuantum).blocks;
    }

    public static double[] averages(List<Job> jobs) {
        return CPUScheduler.averages(jobs);
    }

    private static Deque<Job> pendingFor(List<Job> jobs, int queue) {
        List<Job> list = new ArrayList<>();
        for (Job j : jobs) {
            if (j.queue == queue) list.add(j);
        }
        list.sort(Comparator.comparingInt((Job j) -> j.arrival).thenComparingInt(j -> j.index));
        return new ArrayDeque<>(list);
    }

    /** Moves every job that has arrived by 'time' from pending to ready. */
    private static void admit(Deque<Job> pending, Deque<Job> ready, int time) {
        while (!pending.isEmpty() && pending.peekFirst().arrival <= time) {
            ready.addLast(pending.pollFirst());
        }
    }

    private static int nextArrival(Deque<Job> pending) {
        return pending.isEmpty() ? Integer.MAX_VALUE : pending.peekFirst().arrival;
    }
}
