package src.MultipleLevelAlgo;

import src.Scheduler.CPUScheduler;
import src.Scheduler.CPUScheduler.Block;
import src.Scheduler.CPUScheduler.Job;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Multilevel Queue Scheduler.
 * Queue 1 (Foreground): Round Robin (quantum = 4 by default)
 * Queue 2 (Background): FCFS
 * Queue 1 has strict priority over Queue 2.
 */
public final class MultiLevelScheduler implements CPUScheduler {

    @Override
    public String name() {
        return "Multilevel Queue";
    }

    public static List<Block> schedule(List<Job> jobs, int timeQuantum) {
        List<Block> blocks = new ArrayList<>();
        if (jobs.isEmpty()) return blocks;

        for (Job j : jobs) {
            j.remaining = j.burst;
            j.completion = 0;
            j.turnaround = 0;
            j.waiting = 0;
        }

        List<Job> q1 = new ArrayList<>();
        List<Job> q2 = new ArrayList<>();

        List<Job> unarrived = new ArrayList<>(jobs);
        unarrived.sort(Comparator.comparingInt((Job j) -> j.arrival).thenComparingInt(j -> j.index));

        int time = 0;
        int completedCount = 0;
        int total = jobs.size();

        while (completedCount < total) {
            while (!unarrived.isEmpty() && unarrived.get(0).arrival <= time) {
                Job arr = unarrived.remove(0);
                if (arr.queue == 1) q1.add(arr);
                else q2.add(arr);
            }

            if (q1.isEmpty() && q2.isEmpty()) {
                if (!unarrived.isEmpty()) {
                    int nextArrival = unarrived.get(0).arrival;
                    CPUScheduler.appendBlock(blocks, "IDLE", time, nextArrival, true);
                    time = nextArrival;
                    continue;
                } else {
                    break;
                }
            }

            if (!q1.isEmpty()) {
                Job current = q1.remove(0);
                int runTime = Math.min(current.remaining, timeQuantum);
                int startTime = time;
                int endTime = time + runTime;

                CPUScheduler.appendBlock(blocks, current.pid, startTime, endTime, false);
                current.remaining -= runTime;
                time = endTime;

                while (!unarrived.isEmpty() && unarrived.get(0).arrival <= time) {
                    Job arr = unarrived.remove(0);
                    if (arr.queue == 1) q1.add(arr);
                    else q2.add(arr);
                }

                if (current.remaining == 0) {
                    current.completion = time;
                    current.turnaround = current.completion - current.arrival;
                    current.waiting = current.turnaround - current.burst;
                    completedCount++;
                } else {
                    q1.add(current);
                }
            } else {
                Job current = q2.get(0);
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
                CPUScheduler.appendBlock(blocks, current.pid, startTime, endTime, false);
                current.remaining -= runTime;
                time = endTime;

                while (!unarrived.isEmpty() && unarrived.get(0).arrival <= time) {
                    Job arr = unarrived.remove(0);
                    if (arr.queue == 1) q1.add(arr);
                    else q2.add(arr);
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

    public static double[] averages(List<Job> jobs) {
        return CPUScheduler.averages(jobs);
    }
}
