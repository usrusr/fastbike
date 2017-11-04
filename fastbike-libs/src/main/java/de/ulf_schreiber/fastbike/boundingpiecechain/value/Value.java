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
        G group();
    }
    interface Merging <
            R extends Reading<R>,
            G extends Grouping<R,G>,
            M extends Merging<R,G,M>
            > extends Grouping<R,G> {
        M merge();
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
//    public interface BufferNav<L extends BufferNav<L,X,B>,X,B>  {
////        /**
////         * @param buffer
////         * @param offset extra single bytes
////         */
////        L wrap(B buffer, int bufferSize, int offset);
////        L moveRelative(int direction);
////        L moveAbsolute(int next);
////        /** @return highest allowed index+1*/
////        int size();
////        B createBuffer(int elements);
//    }

    /**
     * must not be implemented in classes intended to be extended
     */
    abstract <L extends Editor<L,R,W,B>, B> L createElementWriter();
    abstract <L extends Editor<L,G,M,B>, B> L createAggregateWriter();
    abstract M createMutableBounds();

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

    /**
     * heavy, provided by operation not by storage
     */
    interface EditorI<L extends Editor<L,R,W,B> & EditorI<L,R,W,B>,R,W,B>  {
        L wrap(B buffer, int elements, int offset);
        L moveRelative(int direction);
        L moveAbsolute(int next);
        B createBuffer(int elements);
        R read();
        W write();
    }
//
//    static abstract class BufferNav<L extends BufferNav<L,R,W,B>,R,W,B> implements Editor<L,R,W,B>{

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
        private final L asEditor(){
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
    }


    protected abstract static class Varing<V extends Varing<V> & Writing<V,V>> implements Writing<V,V> {
        @SuppressWarnings("unchecked")
        @Override public final V read() {
            return (V) this;
        }
        @SuppressWarnings("unchecked")
        @Override public final V write() {
            return (V) this;
        }
    }
    protected abstract static class VaringAggregate<A extends VaringAggregate<A,R> & Merging<R,A,A>, R extends Reading<R>> implements Merging<R,A,A> {
        @SuppressWarnings("unchecked")
        @Override public final A group() {
            return (A) this;
        }
        @SuppressWarnings("unchecked")
        @Override public A merge() {
            return (A) this;
        }
    }
}
