package de.ulf_schreiber.fastbike.boundingpiecechain;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class SimpleTreeTest {
    @Test public void hello(){
        SimpleTree tree = new SimpleTree(0.00001d);

        {
            List<Point> line = line(0, 0, 10, 0, 1);
            tree.append(line);
        }


        System.out.println("tree1:\n" + tree);
        assertThat(tree.join(""), equalTo("" +
                "[0.0:0.0(0.0)]" +
                "[5.0:0.0(5.0)]" +
                "[10.0:0.0(5.0)]" +
                ""));

        {
            List<Point> line = line(10, 0, 20, 10, 1);
            tree.append(line);
        }
        System.out.println("tree2:\n" + tree);

        assertThat(tree.join(""), equalTo("" +
                "[0.0:0.0(0.0)]" +
                "[5.0:0.0(5.0)]" +
                "[10.0:0.0(5.0)]" +
                "[10.0:0.0(0.0)]" +
                "[15.0:5.0(7.0710678118654755)]" +
                "[20.0:10.0(7.0710678118654755)]" +
                ""));
    }

    private List<Point> line(int x0, int y0, int x1, int y1, int intermediateSteps) {
        intermediateSteps = Math.max(0,intermediateSteps);
        ArrayList<Point> points = new ArrayList<>(intermediateSteps + 2);
        points.add(new Point(x0,y0,0));

        int xd = x1 - x0;
        int yd = y1 - y0;
        int steps = intermediateSteps + 1;
        double dd = Math.sqrt(xd*xd+yd*yd) / steps;
        double sx = xd/steps;
        double sy = yd/steps;
        for(int i=1;i<=intermediateSteps;i++){
            points.add(new Point(x0+sx*i, y0+sy*i, dd));
        }
        points.add(new Point(x1, y1, dd));

        return points;
    }


    static class Point implements SimpleTree.Reading {
        final double lat;
        final double lng;
        final double dist;

        Point(double lat, double lng, double dist) {
            this.lat = lat;
            this.lng = lng;
            this.dist = dist;
        }

        @Override
        public double getDistance() {
            return dist;
        }

        @Override
        public double getLat() {
            return lat;
        }

        @Override
        public double getLng() {
            return lng;
        }

        @Override
        public SimpleTree.Reading read() {
            return this;
        }

        @Override
        public String toString() {
            return "["+lat+","+lng+"("+dist+")]";
        }
    }
}