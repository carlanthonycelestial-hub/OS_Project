package src.LookAlgo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class LookScheduler {
    private LookScheduler() {
    }

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

    public static List<Step> schedule(int initialHead, boolean movingRight, List<Request> requests) {
        List<Request> lower = new ArrayList<>();
        List<Request> higher = new ArrayList<>();

        for (Request r : requests) {
            if (movingRight) {
                if (r.track >= initialHead)
                    higher.add(r);
                else
                    lower.add(r);
            } else {
                if (r.track <= initialHead)
                    lower.add(r);
                else
                    higher.add(r);
            }
        }

        Comparator<Request> asc = Comparator.comparingInt((Request r) -> r.track).thenComparingInt(r -> r.index);
        Comparator<Request> desc = Comparator.comparingInt((Request r) -> r.track).reversed()
                .thenComparingInt(r -> r.index);

        higher.sort(asc);
        lower.sort(desc);

        List<Request> ordered = new ArrayList<>(requests.size());
        if (movingRight) {
            ordered.addAll(higher);
            ordered.addAll(lower);
        } else {
            ordered.addAll(lower);
            ordered.addAll(higher);
        }

        List<Step> steps = new ArrayList<>(ordered.size());
        int current = initialHead;
        for (int i = 0; i < ordered.size(); i++) {
            Request r = ordered.get(i);
            steps.add(new Step(i + 1, r.id, current, r.track));
            current = r.track;
        }
        return steps;
    }

    public static int totalSeek(List<Step> steps) {
        int total = 0;
        for (Step s : steps)
            total += s.distance;
        return total;
    }

    public static double averageSeek(List<Step> steps) {
        return steps.isEmpty() ? 0.0 : (double) totalSeek(steps) / steps.size();
    }
}
