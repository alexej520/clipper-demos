package clipper.javaexample;

import clipper.*;
import java.util.*;

public class JavaExample {
    public static void main(String[] args) {
        boolean printASCII = true;
        ClipType clipType = ClipType.Union;

        List<IntPoint> subject = ClipperUtils.newPath();
        subject.add(new IntPoint(0, 0));
        subject.add(new IntPoint(10, 0));
        subject.add(new IntPoint(10, 10));
        subject.add(new IntPoint(0, 10));

        List<IntPoint> clip = ClipperUtils.newPath();
        clip.add(new IntPoint(5, -1));
        clip.add(new IntPoint(11, 5));
        clip.add(new IntPoint(5, 11));
        clip.add(new IntPoint(-1, 5));

        List<List<IntPoint>> solution = ClipperUtils.newPaths();

        Clipper clipper = new Clipper();
        clipper.addPath(subject, PolyType.Subject, true);
        clipper.addPath(clip, PolyType.Clip, true);
        boolean success = clipper.execute(clipType, solution, PolyFillType.EvenOdd);
        if (!success) {
            System.out.println("error");
        } else {
            if (!printASCII) {
                for (int i = 0; i < solution.size(); i++) {
                    System.out.println("polygon: " + i);

                    List<IntPoint> path = solution.get(i);
                    for (IntPoint pt : path) {
                        System.out.println(pt);
                    }

                    System.out.println();
                }
            } else {
                IntRect bound = ClipperUtils.getBound(solution);

                long left = bound.getLeft();
                long right = bound.getRight();
                long top = bound.getTop();
                long bottom = bound.getBottom();

                if (solution.size() != 1
                        || Math.abs(right - left) > 80
                        || Math.abs(top - bottom) > 30
                        || solution.get(0).size() > 99
                        || subject.size() > 99
                        || clip.size() > 99) {
                    System.out.println("success, but solution, clip or subject is too big or has more than 1 polygon or empty");
                } else {
                    System.out.println("\n----------------------------------------");
                    System.out.println("Subject");
                    printPolygon(subject, left, right, top, bottom);
                    System.out.println("\n----------------------------------------");
                    System.out.println("Clip");
                    printPolygon(clip, left, right, top, bottom);
                    System.out.println("\n----------------------------------------");
                    System.out.println(clipType);
                    printPolygon(solution.get(0), left, right, top, bottom);
                }
            }
        }
    }

    private static boolean printPolygon(List<IntPoint> polygon, long left, long right, long top, long bottom) {
        if (polygon.size() > 99) {
            return false;
        }
        polygon = new ArrayList<>(polygon);
        Map<IntPoint, Integer> points = new HashMap<>();
        for (int i = 0; i < polygon.size(); i++) {
            points.put(polygon.get(i), i);
        }
        polygon.sort((o1, o2) -> {
            if (o1.getY() > o2.getY()) return -1;
            if (o1.getY() < o2.getY()) return 1;
            return Long.compare(o1.getX(), o2.getX());
        });
        IntPoint prev = new IntPoint(0, bottom - top);
        for (IntPoint pt : polygon) {
            long newLines = prev.getY() - pt.getY();
            boolean newLine = false;
            for (int i = 0; i < newLines; i++) {
                System.out.println();
                newLine = true;
            }
            long spaces = newLine ? pt.getX() - left + 1 : pt.getX() - prev.getX();
            for (int i = 0; i < spaces - 1; i++) {
                System.out.print("  ");
            }
            if (newLines > 0 || spaces > 1) {
                int position = points.get(pt);
                System.out.print(Math.abs(position) < 10 ? " " + position : position);
            }
            prev = pt;
        }
        System.out.println();
        return true;
    }
}
