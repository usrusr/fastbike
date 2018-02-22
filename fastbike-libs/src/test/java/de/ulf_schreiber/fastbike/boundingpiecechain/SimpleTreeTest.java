package de.ulf_schreiber.fastbike.boundingpiecechain;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SimpleTreeTest {
    @Test public void appendUndoRedo(){
        SimpleTree tree = new TestTree();

        {
            List<Point> line = line(0, 0, 10, 0, 1);
            tree.append(line);
        }


        System.out.println("tree1:\n" + tree);
        assertThat(tree.join(""), equalTo("" +
                "[_0.__:_0.__(_0.__)]" +
                "[_5.__:_0.__(_5.__)]" +
                "[10.__:_0.__(_5.__)]" +
                ""));

        {
            List<Point> line = line(10, 0, 20, 10, 1);
            tree.append(line);
        }
        System.out.println("tree2:\n" + tree);

        assertThat(tree.join(""), equalTo("" +
                "[_0.__:_0.__(_0.__)]" +
                "[_5.__:_0.__(_5.__)]" +
                "[10.__:_0.__(_5.__)]" +
                "[10.__:_0.__(_0.__)]" +
                "[15.__:_5.__(_7.07)]" +
                "[20.__:10.__(_7.07)]" +
                ""));

        tree.undo();
        System.out.println("tree3:\n" + tree);
        assertThat(tree.join(""), equalTo("" +
                "[_0.__:_0.__(_0.__)]" +
                "[_5.__:_0.__(_5.__)]" +
                "[10.__:_0.__(_5.__)]" +
                ""));

        assertThat(tree.undo(), is(true));
        System.out.println("tree4:\n" + tree);
        assertThat(tree.join(""), equalTo("" +
                ""));

        assertThat(tree.undo(), is(false));
        System.out.println("tree5:\n" + tree);
        assertThat(tree.join(""), equalTo("" +
                ""));

        assertThat(tree.redo(), is(true));
        System.out.println("tree6:\n" + tree);
        assertThat(tree.join(""), equalTo("" +
                "[_0.__:_0.__(_0.__)]" +
                "[_5.__:_0.__(_5.__)]" +
                "[10.__:_0.__(_5.__)]" +
                ""));

        assertThat(tree.redo(), is(true));
        System.out.println("tree7:\n" + tree);
        assertThat(tree.join(""), equalTo("" +
                "[_0.__:_0.__(_0.__)]" +
                "[_5.__:_0.__(_5.__)]" +
                "[10.__:_0.__(_5.__)]" +
                "[10.__:_0.__(_0.__)]" +
                "[15.__:_5.__(_7.07)]" +
                "[20.__:10.__(_7.07)]" +
                ""));
        assertThat(tree.redo(), is(false));
        System.out.println("tree8:\n" + tree);
        assertThat(tree.join(""), equalTo("" +
                "[_0.__:_0.__(_0.__)]" +
                "[_5.__:_0.__(_5.__)]" +
                "[10.__:_0.__(_5.__)]" +
                "[10.__:_0.__(_0.__)]" +
                "[15.__:_5.__(_7.07)]" +
                "[20.__:10.__(_7.07)]" +
                ""));


    }




    @Test public void appendInsert() {
        SimpleTree tree = new TestTree();

        {
            List<Point> line = line(0, 0, 10, 10, 1);
            tree.append(line);
        }
        System.out.println("tree0:\n" + tree);
        {
            List<Point> line = line(10, 10, 00, 20, 1);
            tree.append(line);
        }


        System.out.println("tree1:\n" + tree);
        assertThat(tree.join(""), equalTo("" +
                "[_0.__:_0.__(_0.__)]" +
                "[_5.__:_5.__(_7.07)]" +
                "[10.__:10.__(_7.07)]" +
                "[10.__:10.__(_0.__)]" +
                "[_5.__:15.__(_7.07)]" +
                "[_0.__:20.__(_7.07)]" +
                ""));

        {
            List<Point> line = line(5, 5, 5, 15, 1);
            double _7_07 = Math.sqrt(50);
            tree.replace(_7_07, _7_07*2, line);
        }
        String tree2 = tree.toString();
        System.out.println("tree2:\n" + tree2);

        assertThat(tree.join(""), equalTo("" +
                "[_0.__:_0.__(_0.__)]" +
                "[_5.__:_5.__(_7.07)]" +
                "[_5.__:_5.__(_0.__)]" +
                "[_5.__:10.__(_5.__)]" +
                "[_5.__:15.__(_5.__)]" +
//                "[_5.__:15.__(_0.__)]" +
                "[_0.__:20.__(_7.07)]" +
                ""));

    }

    @Test public void insertEndBetweenNodes() {
        SimpleTree tree = new TestTree();

        {
            List<Point> line = line(0, 0, 10, 10, 1);
            tree.append(line);
        }
        {
            List<Point> line = line(10, 10, 00, 20, 0);
            tree.append(line);
        }


        System.out.println("tree1:\n" + tree);
        assertThat(tree.join(""), equalTo("" +
                "[_0.__:_0.__(_0.__)]" +
                "[_5.__:_5.__(_7.07)]" +
                "[10.__:10.__(_7.07)]" +
                "[10.__:10.__(_0.__)]" +
                "[_0.__:20.__(14.14)]" +
                ""));

        {
            List<Point> line = line(5, 5, 5, 15, 1);
            double _7_07 = Math.sqrt(50);
            tree.replace(_7_07, _7_07*2, line);
        }
        String tree2 = tree.toString();
        System.out.println("tree2:\n" + tree2);

        assertThat(tree.join(""), equalTo("" +
                "[_0.__:_0.__(_0.__)]" +
                "[_5.__:_5.__(_7.07)]" +
                "[_5.__:_5.__(_0.__)]" +
                "[_5.__:10.__(_5.__)]" +
                "[_5.__:15.__(_5.__)]" +
//                "[_5.__:15.__(_0.__)]" +
                "[_0.__:20.__(_7.07)]" +
                ""));

    }

    @Test public void insertBeginBetweenNodes() {
        SimpleTree tree = new TestTree();

        {
            List<Point> line = line(0, 0, 10, 10, 0);
            tree.append(line);
        }
        {
            List<Point> line = line(10, 10, 00, 20, 1);
            tree.append(line);
        }


        System.out.println("tree1:\n" + tree);
        assertThat(tree.join(""), equalTo("" +
                "[_0.__:_0.__(_0.__)]" +
                "[10.__:10.__(14.14)]" +
                "[10.__:10.__(_0.__)]" +
                "[_5.__:15.__(_7.07)]" +
                "[_0.__:20.__(_7.07)]" +
                ""));

        {
            List<Point> line = line(5, 5, 5, 15, 1);
            double _7_07 = Math.sqrt(50);
            tree.replace(_7_07, _7_07*2, line);
        }
        String tree2 = tree.toString();
        System.out.println("tree2:\n" + tree2);

        assertThat(tree.join(""), equalTo("" +
                "[_0.__:_0.__(_0.__)]" +
                "[_5.__:_5.__(_7.07)]" +
                "[_5.__:_5.__(_0.__)]" +
                "[_5.__:10.__(_5.__)]" +
                "[_5.__:15.__(_5.__)]" +
//                "[_5.__:15.__(_0.__)]" +
                "[_0.__:20.__(_7.07)]" +
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

    private static class TestTree extends SimpleTree {
        public TestTree() {
            super(0.00001d);
        }

        @Override
        protected void stringifyDouble(Appendable sw, double val) throws IOException {
            sw.append(
                    String.format(Locale.ENGLISH, "%5.2f", val)
                    .replace(" 0","  ").replace(" ", "_")
                    .replaceAll("0(?=0*$)","_")
                    .replace("__.__", "_0.__")
            );
//            sw.append(formatter.format(val).replaceAll("^0(!\\.)|(?<= )0(!\\.)", " "));
        }
    }
}