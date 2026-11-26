package src.ScanAlgo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ScanScheduler {
    private ScanScheduler() {}

    public static final class Request {
        public final String id;
        public final int track, index;

        public Request(String id, int track, int index) {
            this.id = id;
            this.track = track;
            this.index = index;
        }
    }

    public static final class Step {
        public final int order;
        public final String id;
        public final int from, to, distance;

        public Step(int order, String id, int from, int to) {
            this.order = order;
            this.id = id;
            this.from = from;
            this.to = to;
            this.distance = Math.abs(to - from);
        }
    }

    public static List<Step> schedule(int initialHead, int maxTrack, boolean movingRight, List<Request> requests) {
        List<Request> lower = new ArrayList<>();
        List<Request> higher = new ArrayList<>();

        for (Request r : requests) {
            if (movingRight) {
                if (r.track >= initialHead) higher.add(r);
                else lower.add(r);
            } else {
                if (r.track <= initialHead) lower.add(r);
                else higher.add(r);
            }
        }

        Comparator<Request> asc = Comparator.comparingInt((Request r) -> r.track).thenComparingInt(r -> r.index);
        Comparator<Request> desc = Comparator.comparingInt((Request r) -> r.track).reversed().thenComparingInt(r -> r.index);
        higher.sort(asc);
        lower.sort(desc);

        List<Step> steps = new ArrayList<>();
        int current = initialHead;
        int order = 1;

        if (movingRight) {
            for (Request r : higher) {
                steps.add(new Step(order++, r.id, current, r.track));
                current = r.track;
            }
            if (!lower.isEmpty()) {
                if (current != maxTrack) {
                    steps.add(new Step(order++, "END", current, maxTrack));
                    current = maxTrack;
                }
                for (Request r : lower) {
                    steps.add(new Step(order++, r.id, current, r.track));
                    current = r.track;
                }
            }
        } else {
            for (Request r : lower) {
                steps.add(new Step(order++, r.id, current, r.track));
                current = r.track;
            }
            if (!higher.isEmpty()) {
                if (current != 0) {
                    steps.add(new Step(order++, "END", current, 0));
                    current = 0;
                }
                for (Request r : higher) {
                    steps.add(new Step(order++, r.id, current, r.track));
                    current = r.track;
                }
            }
        }
        return steps;
    }

    public static int totalSeek(List<Step> steps) {
        int total = 0;
        for (Step s : steps) total += s.distance;
        return total;
    }

    public static double averageSeek(List<Step> steps, int requestCount) {
        return requestCount == 0 ? 0.0 : (double) totalSeek(steps) / requestCount;
    }
}
