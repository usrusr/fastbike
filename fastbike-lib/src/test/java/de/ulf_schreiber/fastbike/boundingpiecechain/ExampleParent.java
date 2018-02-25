package de.ulf_schreiber.fastbike.boundingpiecechain;


/**
 * parent for {@link ExampleChild}, use {@link ExampleChild} as copy/paste template
 */
public abstract class ExampleParent<
        V extends ExampleParent<V,R,W,G,M,B,L,A>,
        R extends ExampleParent.Reading<R> & BaseTree.Reading<R>,
        W extends ExampleParent.Writing<R,W> & ExampleParent.Reading<R> & BaseTree.Writing<R,W>,
        G extends ExampleParent.Grouping<R,G> & BaseTree.Grouping<R,G>,
        M extends ExampleParent.Merging<R,G,M> & ExampleParent.Grouping<R,G> & BaseTree.Merging<R,G,M>,
        B,
        L extends BaseTree.Editor<L,R,W,B>,
        A extends BaseTree.Editor<A,G,M,B>
        > extends BaseTree<V,R,W,G,M,B,L,A> {

    public ExampleParent(int blocksize, double precision) {
        super(blocksize, precision);
    }

    interface Reading <
            R extends Reading<R>
            > extends BaseTree.Reading<R> , ExampleParent.PublicRead{
    }
    interface Writing<
            R extends Reading<R>,
            W extends Writing<R,W>
            > extends Reading<R>, BaseTree.Writing<R,W> {
    }
    interface Grouping<
            R extends Reading<R>,
            G extends Grouping<R,G>
            > extends BaseTree.Grouping<R,G> , ExampleParent.PublicGroup{
    }
    interface Merging <
            R extends Reading<R>,
            G extends Grouping<R,G>,
            M extends Merging<R,G,M>
            > extends Grouping<R,G>, BaseTree.Merging<R,G,M> {
    }
    public interface PublicRead extends BaseTree.PublicRead{

    }
    public interface PublicGroup extends BaseTree.PublicGroup{

    }


}
