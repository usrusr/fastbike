package de.ulf_schreiber.fastbike.boundingpiecechain.value;


import de.ulf_schreiber.fastbike.boundingpiecechain.Read;

import java.nio.ByteBuffer;

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


    public interface BufferLooking<L extends BufferLooking<L>>  {
        /**
         * @param buffer
         * @param offset extra single bytes
         */
        L at(ByteBuffer buffer, int offset);
        L moveRelative(int direction);
        L moveAbsolute(int next);
        /** @return highest allowed index+1*/
        int size();
    }
    protected abstract Looking<?> createElementWriter();

    interface CT {

    }


    /**
     * heavy, provided by operation not by storage
     */
    public abstract class Looking<L> implements BufferLooking<Looking<L>> , Writing<R,W> {
        protected final int weight;
        protected ByteBuffer buffer = null;
        protected int offset;
        protected int size;
        protected int index;
        protected int actualIndex;

        protected final static int layerlen = 0;
        /**
         * @param size bytes per element (as determined by implementors)
         */
        protected Looking(int size, int fieldLimit) {
            this.weight = Math.min(size, fieldLimit);
        }

        @Override
        final public Looking<L> at(ByteBuffer buffer, int offset) {
            this.buffer = buffer;
            this.offset=offset;

            if(buffer==null) {
                size = 0;
            } else {
                size = (buffer.capacity() - offset) / weight;
            }
            this.index = 0;
            return this;
        }

        @Override
        final public Looking<L> moveRelative(int direction) {
            int next = index + direction;
            if(next < 0 || next >= size) throw new IndexOutOfBoundsException();
            index = next;
            actualIndex = offset + index*weight;
            return this;
        }

        @Override
        final public Looking<L> moveAbsolute(int next) {
            if(next < 0 || next >= size) throw new IndexOutOfBoundsException();
            index = next;
            actualIndex = offset + index*weight;
            return this;
        }

        @Override
        final public int size() {
            return size;
        }
    }

    public abstract class Varing<V extends Varing<V> & Writing<R,W> & CT> implements Writing<R,W> {

    }
}
