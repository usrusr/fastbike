package de.ulf_schreiber.fastbike.boundingpiecechain;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * base class for the type metaobject
 * @param <R>  element getters
 * @param <W>  element setters
 * @param <G> group of elements getters
 * @param <M> group of elements setters
 */
public abstract class Value<
        V extends Value<V,R,W,G,M,B,L,A>,
        R extends Value.Reading<R>,
        W extends Value.Writing<R,W> & Value.Reading<R>,
        G extends Value.Grouping<R,G>,
        M extends Value.Merging<R,G,M> & Value.Grouping<R,G>,
        B,
        L extends Value.Editor<L,R,W,B>,
        A extends Value.Editor<A,G,M,B>
    > implements Iterable<R>{



    final int blocksize = 16;

    boolean samePoint(R a, R b){
        return sameAs(a,b);
    }


    abstract class Node<N extends Node<N>> {

        B array;
        int elements = 0;
        abstract void build(BoundingBuilder builder);
        abstract int depth();

        protected abstract boolean calculateNext(NodeIterator state, double skip);
    }
    class LeafNode extends Node<LeafNode> {
        LeafNode(Iterator<? extends R> it, L elemLooker, M mutableBoundsReturn) {
            array = elemLooker.createBuffer(blocksize);
            elemLooker.wrap(array, blocksize,0);
            while(it.hasNext() && elements < blocksize){
                R next = it.next();
                copy(next, elemLooker.write());
                extendBy(mutableBoundsReturn, next);
                elemLooker.moveRelative(1);
                elements++;
            }
        }

        @Override int depth() { return 0; }
        @Override
        public void build(BoundingBuilder builder) {
            V meta = builder.tree();
            if(builder.toVisit < precision) return;

            L looker = builder.valueLooker;
            looker.wrap(array, builder.tree().blocksize, 0);

            for(int i=0;i<elements;i++){
                double dist = looker.read().getDistance();

                double restToSkip = builder.toSkip-dist;
                if(restToSkip> precision){
                    builder.toSkip = restToSkip;
                }else{
                    builder.toSkip = 0;
                    extendBy(builder.result, looker.read());
                    double restToVisit = builder.toVisit-dist;
                    if(restToSkip< precision){
                        builder.toVisit=0;
                        return;
                    }else{
                        builder.toVisit = restToVisit;
                    }
                }
                looker.moveRelative(1);
            }
        }

        @Override
        protected boolean calculateNext(NodeIterator state, double skip) {
            state.leaf=this; // for next iteration
            L looker = state.valueLooker;
            if(state.doneInCur < elements) {
                looker.wrap(array, elements, 0).moveAbsolute(state.doneInCur);

                while (skip > precision) {
                    state.doneInCur++;
                    skip -= looker.moveRelative(1).read().getDistance();
                }
            }
            if(state.doneInCur>=elements){
                int stackDepth = state.grpstack.size();
                int idx = stackDepth -1;
                while(idx>=0){ // zip all deeper levels to first element
                    GroupNode groupNode = state.grpstack.get(idx);
                    int cur = state.idxstack.get(idx);
                    cur++;
                    if (cur < groupNode.elements) {
                        state.idxstack.set(idx, cur);
                        Node<?> child = groupNode.children[cur];
                        idx++;
                        while(idx < stackDepth){
                            GroupNode newParent = GroupNode.class.cast(child);
                            state.grpstack.set(idx, newParent);
                            state.idxstack.set(idx, 0);
                            child = newParent.children[0];
                        }
                        state.doneInCur = 0;
                        LeafNode cast = LeafNode.class.cast(child);
                        cast.calculateNext(state, skip);
                        return false;
                    }
                    idx--;
                }
                return false;
            }
            copy(looker.read(), state.next);
            if(looker.hasNext()) looker.moveRelative(1);
            state.doneInCur++;
            return true;
        }
    }

    class GroupNode extends Node<GroupNode> {


        Node<?>[] children;

        int depth;

        @Override
        int depth() {
            return depth;
        }

        GroupNode(Iterator<? extends R> it, L elemLooker, A groupLooker, M mutableBoundsReturn, Node<?> firstChild, int depth){
            children = new Node[blocksize];
            array = groupLooker.createBuffer(blocksize);
            this.depth = depth;

            groupLooker.wrap(array, 0, 0);

            M groupLookerWrite = groupLooker.write();
            G mutableBoundsReturnRead = mutableBoundsReturn.read();

            if(firstChild!=null) { // creating a new root
                children[0]=firstChild;
                clearMerge(groupLookerWrite);
                extendBy(groupLookerWrite, mutableBoundsReturnRead);
            }

            while(it.hasNext() && elements<blocksize){
                Node<?> newNode;

                if(depth==1){
                    groupLooker.moveAbsolute(elements);
                    newNode = new LeafNode(it, elemLooker, groupLookerWrite);
                } else {
                    clearMerge(mutableBoundsReturn);
                    newNode = new GroupNode(it, elemLooker, groupLooker, mutableBoundsReturn, null, depth-1);
                    groupLooker.wrap(array, elements, 0);
                    clearMerge(groupLookerWrite);
                    extendBy(groupLookerWrite, mutableBoundsReturnRead);
                }
                children[elements] = newNode;
                elements++;
            }

            clearMerge(mutableBoundsReturn);
            groupLooker.wrap(array, 0, 0);
            for(int i=0; i<elements;i++){
                groupLooker.moveAbsolute(i);
                extendBy(mutableBoundsReturn, groupLooker.read());
            }
        }

        @Override
        public void build(BoundingBuilder builder) {
            V meta = builder.tree();
            if(builder.toVisit < precision) return;

            A groupLooker = builder.groupLooker;
            clearMerge(groupLooker.write());
            groupLooker.wrap(array, 0, 0);
            G read = groupLooker.read();
            for(int i=0; i<elements;i++){
                groupLooker.moveAbsolute(i);
                double childDist = read.getDistance();

                double restToSkip = builder.toSkip-childDist;
                if(restToSkip> precision){
                    builder.toSkip = restToSkip;
                }else{
                    builder.toSkip = 0;
                    double restToVisit = builder.toVisit-childDist;
                    if(restToVisit > -precision){
                        extendBy(builder.result, read);
                        builder.toVisit=restToVisit;
                    }else{
                        children[i].build(builder);
                        return;
                    }
                }
            }
        }

        @Override
        protected boolean calculateNext(NodeIterator state, double skip) {
            A looker = state.groupLooker;
            looker.wrap(array, elements, 0);
            for(int i=0;i<elements;i++){
                double childLen = looker.read().getDistance();
                if(childLen +precision > skip){
                    Node<?> child = children[i];
                    state.doneInCur=0;
                    state.grpstack.add(this);

                    state.idxstack.add(i);

                    return child.calculateNext(state, skip);
                }
                skip -= childLen;
                looker.moveRelative(1);
            }
            return false;
        }
    }

    protected V self(){
        return (V) this;
    }

    /**
     * wraps a node, either whole or a subset (this way, multiple undoPieces can use the same immutable tree, for when a piece is cut)
     */
    class Piece {
        final G bounds;
        final Node<?> root;
        final double offset; // length is contained in bounds
        Piece(Node<?> root, double offset, double distance) {
            this.root = root;
            this.offset = offset;

            BoundingBuilder boundingBuilder = new BoundingBuilder(offset, distance);
            root.build(boundingBuilder);
            bounds = boundingBuilder.result.read();
        }

        Piece(Iterable<? extends R> iterable){
            L elemLooker = createElementWriter();
            A groupLooker = createAggregateWriter();
            Iterator<? extends R> it = iterable.iterator();
            M mutableBounds = createMutableBounds();
            LeafNode first = new LeafNode(it, elemLooker, mutableBounds);
            Node tmpRoot = first;
            int depth = 0;
            while(it.hasNext()){
                depth++;
                tmpRoot = new GroupNode(it, elemLooker, groupLooker, mutableBounds, tmpRoot, depth);
            }
            root = tmpRoot;
            offset = 0;
            bounds = mutableBounds.read();
        }

    }
    class Undo{
        final int skip;
        final int removed; // first n undoPieces are removed, rest added
        final List<Piece> undoPieces;

        Undo(int skip, List<Piece> toRemoveList, List<Piece> toAddList) {
            this.skip = skip;
            undoPieces = new ArrayList<>(toRemoveList.size()+toAddList.size());
            this.removed = toRemoveList.size();
            undoPieces.addAll(toRemoveList);
            undoPieces.addAll(toAddList);
        }

        final void apply() {
            ListIterator<Piece> iterator = pieces.listIterator(skip);
            int i = 0;
            for (; i < removed; i++) iterator.remove();
            int length = undoPieces.size();
            for (; i < length; i++) iterator.add(undoPieces.get(i));
        }

        final void unapply() {
            ListIterator<Piece> iterator = Value.this.pieces.listIterator(skip);
            int i = removed;
            int length = undoPieces.size();
            for (; i < length; i++) iterator.remove();

            for (i = 0; i < removed; i++) iterator.add(undoPieces.get(i));
        }
    }

    public class BoundingBuilder {
        final M result;
        final L valueLooker;
        final A groupLooker;

        double toSkip;
        double toVisit;

        public BoundingBuilder(double skipDistance, double distance) {
            this.toSkip = skipDistance;
            this.toVisit = distance;
            result = createMutableBounds();
            valueLooker = createElementWriter();
            groupLooker = createAggregateWriter();
        }

        public V tree() {
            return (V) Value.this;
        }
    }


    private final LinkedList<Piece> pieces = new LinkedList<>();
    private final LinkedList<Undo> undos = new LinkedList<>();
    private final LinkedList<Undo> redos = new LinkedList<>();
    public void append(Iterable<? extends R> points){
        replace(Double.MAX_VALUE, 0, points);
    }
    public void insert(double from, Iterable<? extends R> points){
        replace(from, 0, points);
    }
    public void delete(double wrap, double distance){
        replace(wrap, distance, Collections.<R>emptyList());
    }
    public void replace(double from, double distance, Iterable<? extends R> points){
        List<Piece> toRemoveList = new ArrayList<>();
        List<Piece> toAddList = new ArrayList<>();

        int undoSkip = 0;
        if(from<Double.MAX_VALUE){
            double toSkip = from;
            double toRemove = distance;
            ListIterator<Piece> pit = pieces.listIterator();
            while(pit.hasNext()){
                Piece cur = pit.next();
                double plen = cur.bounds.getDistance();
                if(plen < toSkip  + precision){
                    undoSkip++;
                    toSkip-=plen;
                }else{
                    pit.previous(); // add cur to toRemoveList in the next while loop
                    if(toSkip > precision){
                        // add partial copy of first removed
                        toAddList.add(new Piece(cur.root, 0, toSkip));
                    }
                    break;
                }
            }
            toAddList.add(new Piece(points));
            while(pit.hasNext() && toRemove > precision){
                Piece cur = pit.next();
                double plen = cur.bounds.getDistance();
                toRemove -= plen - toSkip;
                toSkip = 0;
                toRemoveList.add(cur);
                if(toRemove < -precision){
                    double restToAdd = -toRemove;
                    toAddList.add(new Piece(cur.root, plen-restToAdd, restToAdd));
                }
            }
        }else{
            undoSkip = pieces.size();
            toAddList.add(new Piece(points));
        }

        Undo undo = new Undo(undoSkip, toRemoveList, toAddList);
        undo.apply();
        undos.add(undo);
        redos.clear();
    }

    @Override
    public Iterator<R> iterator() {
        return new Iterator<R>() {
            Iterator<Piece> pieceIt = pieces.iterator();
            Piece currentPiece = null;

            NodeIterator nodeIt = new NodeIterator();
            double curNodeDone = 0;

            final W next = Value.this.createMutableVal();
            final W cur = Value.this.createMutableVal();
            boolean hasNext = calcNext();

            protected boolean calcNext(){
                while(pieceIt.hasNext() && ! nodeIt.hasNext()){
                    currentPiece=pieceIt.next();
                    nodeIt.reset(currentPiece.root, currentPiece.offset, currentPiece.bounds.getDistance());
                }
                if( ! nodeIt.hasNext()) return false;
                copy(nodeIt.next(), next);
                return true;
            }
            @Override public boolean hasNext() {
                return hasNext;
            }

            @Override public R next() {
                copy(next.read(), cur);
                hasNext = calcNext();
                return cur.read();
            }

            @Override public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
    class NodeIterator implements Iterator<R>{
        protected ArrayList<GroupNode> grpstack = new ArrayList<>();
        protected ArrayList<Integer> idxstack = new ArrayList<>();
        protected LeafNode leaf;
        protected double remaining;
        final private W next = createMutableVal();
        final private W cur = createMutableVal();
        L valueLooker = createElementWriter();
        A groupLooker = createAggregateWriter();
        protected int doneInCur = 0;
        private boolean hasNext = false;

        void reset(Node root, double skip, double total){
            grpstack.ensureCapacity(root.depth());
            idxstack.ensureCapacity(root.depth());
            grpstack.clear();
            idxstack.clear();
            remaining = total;
            doneInCur=0;
            hasNext = root.calculateNext(this, skip);
        }

        @Override
        public boolean hasNext() {
            return hasNext && remaining>-precision;
        }

        @Override
        public R next() {
            copy(next.read(), cur);
            remaining-=cur.getDistance();
            hasNext = leaf.calculateNext(this, 0);
            return cur.read();
        }
        @Override public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        join(sb, "\n");
        return sb.toString();
    }
    public String join(String separator){
        StringBuilder sb = new StringBuilder();
        join(sb, separator);
        return sb.toString();
    }
    public void join(Appendable sb, String separator){
        Iterator<R> it = iterator();
        String sep = "";
        while(it.hasNext()){
            try {
                sb.append(sep);
            } catch (IOException e) {
                e.printStackTrace();
            }
            sep=separator;
            stringifyPoint(sb, it.next());
        }
    }

    /**
     * @return false if undo stack was empty
     */
    public boolean undo(){
        Undo undo = undos.pollLast();
        if(undo==null) return false;

        undo.unapply();
        redos.push(undo);
        return true;
    }
    public boolean canUndo(){
        return ! undos.isEmpty();
    }
    public boolean canRedo(){
        return ! redos.isEmpty();
    }
    /**
     * @return false if undo stack was empty
     */
    public boolean redo(){
        Undo redo = redos.poll();
        if(redo==null) return false;

        redo.apply();
        undos.add(redo);
        return true;
    }

    protected final double precision;

    protected Value(double precision) {
        this.precision = precision;
    }

    interface Reading <
            R extends Reading<R>
            > {
        R read();

        double getDistance();
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

        double getDistance();
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
    abstract L createElementWriter();
    abstract A createAggregateWriter();
    abstract M createMutableBounds();
    abstract W createMutableVal();

    String stringifyPoint(R point){
        StringBuilder sb = new StringBuilder();
        stringifyPoint(sb, point);
        return sb.toString();
    }
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
