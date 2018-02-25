package de.ulf_schreiber.fastbike.boundingpiecechain;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LinePointFinderTest {



    @Test public void test(){
        final SimpleTree tree = new SimpleTree(16, 0.000001);


        SimpleTree.Writing center = tree.createMutableVal();
        SimpleTree.LinePointFinder linePointFinder = tree.new LinePointFinder(
                tree.copy(coords(center,4,4).read()),
                tree.copy(coords(center, 2,2)).read(),
                tree.copy(coords(center, 6,6)).read()
        );


        final List<SimpleTree.Reading> acc = new ArrayList<>();
        SimpleTree.SearchResult searchResult = tree.new SearchResult() {
            @Override
            void miss() {
                acc.add(null);
                super.miss();
            }

            @Override
            void hit(SimpleTree.Reading read) {
                acc.add(tree.copy(read));
                super.hit(read);
            }
        };

        SimpleTree.Writing from = tree.createMutableVal();
        SimpleTree.Writing to = tree.createMutableVal();

        acc.clear();
        linePointFinder.find(coords(from, 1, 1), coords(to, 0,0), searchResult);

        assertEquals("[null]", acc.toString());

    }

    private SimpleTree.Writing coords(SimpleTree.Writing center, int lat, int lon) {
        center.setLat(lat);
        center.setLon(lon);
        return center;
    }
}