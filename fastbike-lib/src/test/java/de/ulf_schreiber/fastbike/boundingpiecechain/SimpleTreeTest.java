package de.ulf_schreiber.fastbike.boundingpiecechain;

import jdk.nashorn.internal.runtime.JSONFunctions;
import org.junit.ComparisonFailure;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        same(tree, ("" +
                "[_0.__:_0.__(_0.__)]" +
                "[_5.__:_0.__(_5.__)]" +
                "[10.__:_0.__(_5.__)]" +
                ""));

        {
            List<Point> line = line(10, 0, 20, 10, 1);
            tree.append(line);
        }
        System.out.println("tree2:\n" + tree);

        same(tree, ("" +
                "[_0.__:_0.__(_0.__)]" +
                "[_5.__:_0.__(_5.__)]" +
                "[10.__:_0.__(_5.__)]" +
                "[10.__:_0.__(_0.__)]" +
                "[15.__:_5.__(_7.07)]" +
                "[20.__:10.__(_7.07)]" +
                ""));

        tree.undo();
        System.out.println("tree3:\n" + tree);
        same(tree, ("" +
                "[_0.__:_0.__(_0.__)]" +
                "[_5.__:_0.__(_5.__)]" +
                "[10.__:_0.__(_5.__)]" +
                ""));

        assertThat(tree.undo(), is(true));
        System.out.println("tree4:\n" + tree);
        same(tree, ("" +
                ""));

        assertThat(tree.undo(), is(false));
        System.out.println("tree5:\n" + tree);
        same(tree, ("" +
                ""));

        assertThat(tree.redo(), is(true));
        System.out.println("tree6:\n" + tree);
        same(tree, ("" +
                "[_0.__:_0.__(_0.__)]" +
                "[_5.__:_0.__(_5.__)]" +
                "[10.__:_0.__(_5.__)]" +
                ""));

        assertThat(tree.redo(), is(true));
        System.out.println("tree7:\n" + tree);
        same(tree, ("" +
                "[_0.__:_0.__(_0.__)]" +
                "[_5.__:_0.__(_5.__)]" +
                "[10.__:_0.__(_5.__)]" +
                "[10.__:_0.__(_0.__)]" +
                "[15.__:_5.__(_7.07)]" +
                "[20.__:10.__(_7.07)]" +
                ""));
        assertThat(tree.redo(), is(false));
        System.out.println("tree8:\n" + tree);
        same(tree, ("" +
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
        same(tree, "" +
                "[_0.__:_0.__(_0.__)]" +
                "[_5.__:_5.__(_7.07)]" +
                "[10.__:10.__(_7.07)]" +
                "[10.__:10.__(_0.__)]" +
                "[_5.__:15.__(_7.07)]" +
                "[_0.__:20.__(_7.07)]" +
                "");

        {
            List<Point> line = line(5, 5, 5, 15, 1);
            double _7_07 = Math.sqrt(50);
            tree.replace(_7_07, _7_07*2, line);
        }
        String tree2 = tree.toString();
        System.out.println("tree2:\n" + tree2);

        same(tree, "" +
                "[_0.__:_0.__(_0.__)]" +
                "[_5.__:_5.__(_7.07)]" +
                "[_5.__:_5.__(_0.__)]" +
                "[_5.__:10.__(_5.__)]" +
                "[_5.__:15.__(_5.__)]" +
                "[_5.__:15.__(_0.__)]" +
                "[_0.__:20.__(_7.07)]" +
                "");

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
        same(tree, ("" +
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

        same(tree, ("" +
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
        same(tree, ("" +
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

        same(tree, "" +
                "[_0.__:_0.__(_0.__)]" +
                "[_5.__:_5.__(_7.07)]" +
                "[_5.__:_5.__(_0.__)]" +
                "[_5.__:10.__(_5.__)]" +
                "[_5.__:15.__(_5.__)]" +
                "[_5.__:15.__(_0.__)]" +
                "[_0.__:20.__(_7.07)]" +
                "");
    }

    private void same(SimpleTree tree, String expected) {
            String is = tree.join("\n").trim();
            String should = expected.replace("][", "]\n[").trim();
            if( ! should.equals(is)){
                throw new ComparisonFailure("tree not as expected" +
                        "\n  expected: " + JSONFunctions.quote(should) +
                        "\n    actual: " + JSONFunctions.quote(is) +
                        "\n", should, is);
            }
    }



    @Test public void insertDeep() {
        SimpleTree tree = new TestTree(3);

        {
            List<Point> line = line(0, 0, 10, 10, 25);
            tree.append(line);
        }
        System.out.println("tree0:\n" + tree);
        {
            List<Point> line = line(10, 10, 00, 20, 15);
            tree.append(line);
        }


        System.out.println("tree1:\n" + tree);
        same(tree, ("" +
                "[_0.__:_0.__(_0.__)]" +
                "[__.38:__.38(__.54)]" +
                "[__.77:__.77(__.54)]" +
                "[_1.15:_1.15(__.54)]" +
                "[_1.54:_1.54(__.54)]" +
                "[_1.92:_1.92(__.54)]" +
                "[_2.31:_2.31(__.54)]" +
                "[_2.69:_2.69(__.54)]" +
                "[_3.08:_3.08(__.54)]" +
                "[_3.46:_3.46(__.54)]" +
                "[_3.85:_3.85(__.54)]" +
                "[_4.23:_4.23(__.54)]" +
                "[_4.62:_4.62(__.54)]" +
                "[_5.__:_5.__(__.54)]" +
                "[_5.38:_5.38(__.54)]" +
                "[_5.77:_5.77(__.54)]" +
                "[_6.15:_6.15(__.54)]" +
                "[_6.54:_6.54(__.54)]" +
                "[_6.92:_6.92(__.54)]" +
                "[_7.31:_7.31(__.54)]" +
                "[_7.69:_7.69(__.54)]" +
                "[_8.08:_8.08(__.54)]" +
                "[_8.46:_8.46(__.54)]" +
                "[_8.85:_8.85(__.54)]" +
                "[_9.23:_9.23(__.54)]" +
                "[_9.62:_9.62(__.54)]" +
                "[10.__:10.__(__.54)]" +
                "[10.__:10.__(_0.__)]" +
                "[_9.38:10.63(__.88)]" +
                "[_8.75:11.25(__.88)]" +
                "[_8.13:11.88(__.88)]" +
                "[_7.5_:12.5_(__.88)]" +
                "[_6.88:13.13(__.88)]" +
                "[_6.25:13.75(__.88)]" +
                "[_5.63:14.38(__.88)]" +
                "[_5.__:15.__(__.88)]" +
                "[_4.38:15.63(__.88)]" +
                "[_3.75:16.25(__.88)]" +
                "[_3.13:16.88(__.88)]" +
                "[_2.5_:17.5_(__.88)]" +
                "[_1.88:18.13(__.88)]" +
                "[_1.25:18.75(__.88)]" +
                "[__.63:19.38(__.88)]" +
                "[_0.__:20.__(__.88)]" +
                ""));

        {
            List<Point> line = line(5, 5, 5, 15, 4);
            double _7_07 = Math.sqrt(50);
            tree.replace(_7_07, _7_07*2, line);
        }
        String tree2 = tree.toString();
        System.out.println("tree2:\n" + tree2);

        same(tree, ("" +
                "[_0.__:_0.__(_0.__)]" +
                "[__.38:__.38(__.54)]" +
                "[__.77:__.77(__.54)]" +
                "[_1.15:_1.15(__.54)]" +
                "[_1.54:_1.54(__.54)]" +
                "[_1.92:_1.92(__.54)]" +
                "[_2.31:_2.31(__.54)]" +
                "[_2.69:_2.69(__.54)]" +
                "[_3.08:_3.08(__.54)]" +
                "[_3.46:_3.46(__.54)]" +
                "[_3.85:_3.85(__.54)]" +
                "[_4.23:_4.23(__.54)]" +
                "[_4.62:_4.62(__.54)]" +
                "[_5.__:_5.__(__.54)]" +
                "[_5.__:_5.__(_0.__)]" +
                "[_5.__:_7.__(_2.__)]" +
                "[_5.__:_9.__(_2.__)]" +
                "[_5.__:11.__(_2.__)]" +
                "[_5.__:13.__(_2.__)]" +
                "[_5.__:15.__(_2.__)]" +
                "[_4.38:15.63(__.88)]" +
                "[_3.75:16.25(__.88)]" +
                "[_3.13:16.88(__.88)]" +
                "[_2.5_:17.5_(__.88)]" +
                "[_1.88:18.13(__.88)]" +
                "[_1.25:18.75(__.88)]" +
                "[__.63:19.38(__.88)]" +
                "[_0.__:20.__(__.88)]" +
                ""));

    }
   @Test public void shortenedPiece() {
        SimpleTree tree = new TestTree(3);
       System.out.println("test: ");
        {
            List<Point> line = line(0, 0, 10, 10, 25);
            tree.append(line);
        }

        tree.delete(0, 5);

        same(tree, "" +
                "[_3.85:_3.85(__.44)]\n" +
                "[_4.23:_4.23(__.54)]\n" +
                "[_4.62:_4.62(__.54)]\n" +
                "[_5.__:_5.__(__.54)]\n" +
                "[_5.38:_5.38(__.54)]\n" +
                "[_5.77:_5.77(__.54)]\n" +
                "[_6.15:_6.15(__.54)]\n" +
                "[_6.54:_6.54(__.54)]\n" +
                "[_6.92:_6.92(__.54)]\n" +
                "[_7.31:_7.31(__.54)]\n" +
                "[_7.69:_7.69(__.54)]\n" +
                "[_8.08:_8.08(__.54)]\n" +
                "[_8.46:_8.46(__.54)]\n" +
                "[_8.85:_8.85(__.54)]\n" +
                "[_9.23:_9.23(__.54)]\n" +
                "[_9.62:_9.62(__.54)]\n" +
                "[10.__:10.__(__.54)]\n" +
                "");

        System.out.println("tree1:\n" + tree);


       tree.delete(3, Double.MAX_VALUE);
       System.out.println("tree2:\n" + tree);
       same(tree, "" +
               "[_3.85:_3.85(__.44)]\n" +
               "[_4.23:_4.23(__.54)]\n" +
               "[_4.62:_4.62(__.54)]\n" +
               "[_5.__:_5.__(__.54)]\n" +
               "[_5.38:_5.38(__.54)]\n" +
               "[_5.66:_5.66(__.39)]" +
               "");

       System.out.println("tree2:\n" + tree);

    }




    private List<Point> line(int x0, int y0, int x1, int y1, int intermediateSteps) {
        intermediateSteps = Math.max(0,intermediateSteps);
        ArrayList<Point> points = new ArrayList<>(intermediateSteps + 2);
        points.add(new Point(x0,y0,0));

        double xd = x1 - x0;
        double yd = y1 - y0;
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
            this(16);
        }
        public TestTree(int blockSize) {
            super(blockSize,0.00001d);
        }
        @Override
        protected void stringifyDouble(Appendable sw, double val) throws IOException {
            sw.append(
                    String.format(Locale.ENGLISH, "%5.2f", val)
                    .replace(" 0","  ").replace(" ", "_")
                    .replaceAll("0(?=0*$)","_")
                    .replace("__.__", "_0.__")
            );
        }
    }






}