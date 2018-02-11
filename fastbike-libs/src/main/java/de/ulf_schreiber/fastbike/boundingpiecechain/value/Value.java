package de.ulf_schreiber.fastbike.boundingpiecechain.value;


import java.io.IOException;

/**
 * base class for the type metaobject
 * @param <R>  element getters
 * @param <W>  element setters
 * @param <G> group of elements getters
 * @param <M> group of elements setters
 */
public abstract class Value<
        R extends Value.Reading<R>,
        W extends Value.Writing<R,W> & Value.Reading<R>,
        G extends Value.Grouping<R,G>,
        M extends Value.Merging<R,G,M> & Value.Grouping<R,G>
        >{

    protected final double precision;

    protected Value(double precision) {
        this.precision = precision;
    }

    interface Reading <
            R extends Reading<R>
            > {
        R read();
    }
    interface Writing<
            R extends Reading<R>,
            W extends Writing<R,W>
            > extends Reading<R> {
        W write();
    }
    interface Grouping<
            R extends Reading<R>,
            G extends Grouping<R,G>
            > {
        G read();
    }
    interface Merging <
            R extends Reading<R>,
            G extends Grouping<R,G>,
            M extends Merging<R,G,M>
            > extends Grouping<R,G> {
        M write();
    }

    public interface PublicRead extends Reading<PublicRead> {

    }
    public interface PublicGroup extends Grouping<PublicRead,PublicGroup> {

    }


    public boolean sameAs(R one, R other){
        return true;
    }
    public void copy(R from, W to){}
    public void extendBy(M toExtend, R point){}
    public void extendBy(M toExtend, G aggregate){};

    public W clearWrite(W toClear){
        return toClear;
    }
    public M clearMerge(M toClear){
        return toClear;
    }

    protected final static double max(double a, double b) {
        if(Double.isNaN(a)) return b;
        if(Double.isNaN(b)) return a;
        return Math.max(a,b);
    }
    protected final static double min(double a, double b) {
        if(Double.isNaN(a)) return b;
        if(Double.isNaN(b)) return a;
        return Math.min(a,b);
    }

    /**
     * must not be implemented in classes intended to be extended
     */
    abstract <L extends Editor<L,R,W,B>, B> L createElementWriter();
    abstract <L extends Editor<L,G,M,B>, B> L createAggregateWriter();
    abstract M createMutableBounds();
    abstract W createMutableVal();

    void stringifyPoint(Appendable sw, R point){
        try {
            if(point==null) {
                sw.append("null");
            }else{
                sw.append('[').append(point.toString()).append(']');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static abstract class Editor<L extends Editor<L,R,W,B>,R,W,B>  {
        protected final int weight;

        protected B buffer = null;
        protected int offset;
        protected int size;
        protected int index;
        protected int actualIndex;

        protected final static int layerlen = 0;
        /**
         * @param size bytes per element (as determined by implementors)
         */
        protected Editor(int size, int fieldLimit) {
            this.weight = Math.min(size, fieldLimit);
        }


        @SuppressWarnings("unchecked")
        private L asEditor(){
            return (L) this;
        }
        final public L wrap(B buffer, int elements, int offset) {
            this.buffer = buffer;
            this.offset=offset;

            if(buffer==null) {
                size = 0;
            } else {
                size = elements;
            }
            this.index = 0;
            return this.asEditor();
        }

        final public L moveRelative(int direction) {
            int next = index + direction;
            if(next < 0 || next >= size) throw new IndexOutOfBoundsException();
            index = next;
            actualIndex = offset + index*weight;
            return this.asEditor();
        }

        final public boolean hasNext(){
            return index<size-1;
        }

        final public L moveAbsolute(int next) {
            if(next < 0 || next >= size) throw new IndexOutOfBoundsException();
            index = next;
            actualIndex = offset + index*weight;
            return this.asEditor();
        }

        final public int size() {
            return size;
        }
        int field(int fieldOffset){
            return actualIndex + fieldOffset;
        }

        public abstract B createBuffer(int blocksize);

        public abstract W write();

        public abstract R read();
    }


    protected abstract static class Varing<
            R extends Reading<R>,
            W extends Writing<R, W>,
            V extends Varing<R, W, V> & Writing<R,W>
        > implements Writing<R,W> {
        @SuppressWarnings("unchecked")
        @Override public final R read() {
            return (R) this;
        }
        @SuppressWarnings("unchecked")
        @Override public final W write() {
            return (W) this;
        }
    }
    protected abstract static class VaringAggregate<
            R extends Reading<R>,
            G extends Grouping<R, G>,
            M extends Merging<R, G, M>,
            A extends VaringAggregate<R, G, M, A> & Merging<R,G,M>
            > implements Merging<R,G,M>, Grouping<R,G> {
        @SuppressWarnings("unchecked")
        @Override public final G read() {
            return (G) this;
        }
        @SuppressWarnings("unchecked")
        @Override public M write() {
            return (M) this;
        }
    }
}
