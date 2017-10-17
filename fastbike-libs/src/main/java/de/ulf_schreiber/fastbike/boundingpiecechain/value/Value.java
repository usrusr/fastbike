package de.ulf_schreiber.fastbike.boundingpiecechain.value;


/**
 * base class for the type metaobject
 * @param <R>  element getters
 * @param <W>  element setters
 * @param <G> group of elements getters
 * @param <M> group of elements setters
 */
public abstract class Value<
        R extends Value.Reading<R> & Value.CT,
        W extends Value.Writing<R,W> & Value.Reading<R> & Value.CT,
        G extends Value.Grouping<R,G> & Value.CT,
        M extends Value.Merging<R,G,M> & Value.Grouping<R,G> & Value.CT
        >{

    protected final double precision;

    protected Value(double precision) {
        this.precision = precision;
    }

    interface Reading <
            R extends Reading<R> & CT
        > {
//        boolean sameAs(R other, double precision);
    }
    interface Writing<
            R extends Reading<R> & CT,
            W extends Writing<R,W> & CT
        > extends Reading<R> {
    }
    interface Grouping<
            R extends Reading<R> & CT,
            G extends Grouping<R,G> & CT
        > {
    }
    interface Merging <
            R extends Reading<R> & CT,
            G extends Grouping<R,G> & CT,
            M extends Merging<R,G,M>  & CT
        > extends Grouping<R,G> {
    }
    public interface PublicRead extends Reading<PublicRead>, CT {

    }
    public interface PublicGroup extends Grouping<PublicRead,PublicGroup>, CT {

    }


    public boolean sameAs(R one, R other){
        return true;
    }
    public void copy(R from, W to){}
    public void extendBy(M toExtend, R point){}
    public void extendBy(M toExtend, G aggregate){};


    public interface BufferLooking<X extends CT>  {
        /**
         * @param array
         * @param index in groups elements
         * @param offset extra single elements
         */
        void at(Object array, int index, int offset);
        void move(int direction);
        /** @return highest allowed index+1*/
        int size();
    }
    protected abstract <L extends BufferLooking<W> & Writing<R,W> & CT> L createElementWriter();
    protected abstract <L extends BufferLooking<M> & Merging<R,G,M> & CT> L createAggregateWriter();

    public Reading<R> createElementReader(){
        return createAggregateWriter();
    }
    public Grouping<R,G> createAggregateReader(){
        return createAggregateWriter();
    }

    interface CT {

    }
}
