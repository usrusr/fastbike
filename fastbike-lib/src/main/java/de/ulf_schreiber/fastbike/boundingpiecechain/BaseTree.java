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
 * @param <B> backing type for compact memory representation (e.g. double[])
 * @param <L> editor for reading/writing elements in B
 * @param <M> editor for reading/writing groups in B
 */
public abstract class BaseTree<
        V extends BaseTree<V,R,W,G,M,B,L,A>,
        R extends BaseTree.Reading<R>,
        W extends BaseTree.Writing<R,W> & BaseTree.Reading<R>,
        G extends BaseTree.Grouping<R,G>,
        M extends BaseTree.Merging<R,G,M> & BaseTree.Grouping<R,G>,
        B,
        L extends BaseTree.Editor<L,R,W,B>,
        A extends BaseTree.Editor<A,G,M,B>
    > implements Iterable<R>{



    protected final int blocksize;


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
            array = elemLooker.createBuffer();
            elemLooker.wrap(array, 0);
            while(it.hasNext() && elements < blocksize){
                R next = it.next();
                copy(next, elemLooker.write());
                extendBy(mutableBoundsReturn, next);
                elements++;
                if(elements<blocksize) elemLooker.moveRelative(1);
            }
        }

        @Override int depth() { return 0; }
        @Override
        public void build(BoundingBuilder builder) {

            if(builder.toVisit < precision) return;

            L looker = builder.valueLooker;
            looker.wrap(array, 0);

            for(int i=0;i<elements;i++){
                double dist = looker.read().getDistance();

                double restToSkip = builder.toSkip-dist;
                if(restToSkip> precision){
                    builder.toSkip = restToSkip;
                    copy(looker.read(), builder.lastVisited);
                }else{

                    if(builder.toSkip>precision){
                        interpolate(builder.lastVisited.read(), looker.read(),builder.toSkip / dist, builder.lastVisited);
                        builder.lastVisited.write().setDistance(0d); // virtual start node interpolation
                        extendBy(builder.result, builder.lastVisited.read());
                        dist -= builder.toSkip;
                    }
                    builder.toSkip = 0;
                    if(dist > builder.toVisit + precision){
                        interpolate(builder.lastVisited.read(), looker.read(), builder.toVisit / dist, builder.lastVisited);
                        builder.toVisit = 0;
                        extendBy(builder.result, builder.lastVisited.read());
                        return;
                    }else {
                        copy(looker.read(), builder.lastVisited);
                        builder.lastVisited.setDistance(dist);  // if cut by skip dist isn't looker.read.dist
                        builder.toVisit -= dist;
                        extendBy(builder.result, builder.lastVisited.read());
                    }
                }
                looker.moveRelative(1);
            }
        }

        @Override
        protected boolean calculateNext(NodeIterator state, final double skipIn) {
            return state.calculateNext(this, skipIn);
        }
    }

    class GroupNode extends Node<GroupNode> {


        Node<?>[] children;

        int depth;

        @Override
        int depth() {
            return depth;
        }

        /**
         *
         * @param it
         * @param elemLooker
         * @param groupLooker
         * @param mutableBoundsReturn contains bounds of firstChild if firstChild is not null on call, bounds of created after call
         * @param firstChild
         * @param depth
         */
        GroupNode(Iterator<? extends R> it, L elemLooker, A groupLooker, M mutableBoundsReturn, Node<?> firstChild, int depth){
            children = new Node[blocksize];
            array = groupLooker.createBuffer();
            this.depth = depth;

            groupLooker.wrap(array, 0);

            if(firstChild!=null) { // creating a new root
                children[0]=firstChild;
                clearMerge(groupLooker.write());
                extendBy(groupLooker.write(), mutableBoundsReturn.read());
                elements++;
            }

            while(it.hasNext() && elements<blocksize){
                Node<?> newNode;

                if(depth==1){
                    groupLooker.moveAbsolute(elements);
                    clearMerge(groupLooker.write()); // need to clear, because we need to start with NaN bounds instead of 0d
                    newNode = new LeafNode(it, elemLooker, groupLooker.write());
                } else {
                    clearMerge(mutableBoundsReturn);
                    newNode = new GroupNode(it, elemLooker, groupLooker, mutableBoundsReturn, null, depth-1);
                    groupLooker.wrap(array, 0);
                    groupLooker.moveAbsolute(elements);
                    clearMerge(groupLooker.write()); // need to clear, because we need to start with NaN bounds instead of 0d
                    extendBy(groupLooker.write(), mutableBoundsReturn.read());
                }
                children[elements] = newNode;
                elements++;
            }

            clearMerge(mutableBoundsReturn);
            groupLooker.wrap(array, 0);
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
            groupLooker.wrap(array, 0);
//            clearMerge(builder.result);
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
            return state.calculateNext(this, skip);
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
        private Piece(Node<?> rootIn, double offsetIn, double distance) {

            // try to identify a descendant node that is big enough to satisfy offsetIn and distance
            if(GroupNode.class.isInstance(rootIn)){
                A looker = createAggregateWriter();
                GroupNode cur = GroupNode.class.cast(rootIn);
                while(true){
                    looker.wrap(cur.array, 0);
                    double thisLevelOffset = offsetIn;

                    Node<?> fittingChild = null;
                    for(int i = 0;i<cur.elements;i++){
                        looker.moveAbsolute(i);
                        double childDistance = looker.read().getDistance();
                        if(thisLevelOffset + precision > childDistance){
                            thisLevelOffset-=childDistance;
                        }else{
                            if(childDistance + precision > thisLevelOffset+distance){
                                fittingChild=cur.children[i];
                            }else{
                                // cur is the most narrow we have
                            }
                            break;
                        }
                    }

                    if( ! GroupNode.class.isInstance(fittingChild)){
                        break;
                    }
                    cur = GroupNode.class.cast(fittingChild);
                    offsetIn = thisLevelOffset;
                }
                rootIn = cur;
            }


            this.offset = offsetIn;
            this.root = rootIn;

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
            for (; i < removed; i++) {
                iterator.next();
                iterator.remove();
            }
            int length = undoPieces.size();
            for (; i < length; i++) iterator.add(undoPieces.get(i));
        }

        final void unapply() {
            ListIterator<Piece> iterator = BaseTree.this.pieces.listIterator(skip);
            int i = removed;
            int length = undoPieces.size();
            for (; i < length; i++) {
                iterator.next();
                iterator.remove();
            }

            for (i = 0; i < removed; i++) iterator.add(undoPieces.get(i));
        }
    }

    public class BoundingBuilder {
        final M result = createMutableBounds();
        final L valueLooker = createElementWriter();
        final A groupLooker = createAggregateWriter();
        final W lastVisited = createMutableVal();

        double toSkip;
        double toVisit;

        public BoundingBuilder(double skipDistance, double distance) {
            this.toSkip = skipDistance;
            this.toVisit = distance;
        }

        public V tree() {
            return (V) BaseTree.this;
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
    public void delete(double from, double distance){
        replace(from, distance, Collections.<R>emptyList());
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
                        toAddList.add(new Piece(cur.root, cur.offset, toSkip));
                    }
                    break;
                }
            }
            if(points.iterator().hasNext()) toAddList.add(new Piece(points));
            while(pit.hasNext() && toRemove > precision){
                Piece cur = pit.next();
                double plen = cur.bounds.getDistance();
                toRemove -= plen - toSkip;
                toSkip = 0;
                toRemoveList.add(cur);
                if(toRemove < -precision){
                    double restToAdd = -toRemove;
                    toAddList.add(new Piece(cur.root, cur.offset+plen-restToAdd, restToAdd));
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

            final W next = BaseTree.this.createMutableVal();
            final W cur = BaseTree.this.createMutableVal();
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

        private boolean calculateNext(GroupNode node, double skip) {
            A looker = groupLooker;
            looker.wrap(node.array, 0);
            for(int i=0;i<node.elements;i++){
                double childLen = looker.read().getDistance();
                if(childLen +precision > skip){
                    Node<?> child = node.children[i];
                    doneInCur=0;
                    grpstack.add(node);

                    idxstack.add(i);

                    return child.calculateNext(this, skip);
                }
                skip -= childLen;
                looker.moveRelative(1);
            }
            return false;
        }

        private boolean calculateNext(LeafNode leafNode, double skipIn) {
            if(remaining<precision) return false;
            double skip = skipIn;
            leaf=leafNode; // for next iteration
            L looker = valueLooker;
            if(doneInCur < leafNode.elements) {
                looker.wrap(leafNode.array, 0).moveAbsolute(doneInCur);

                R read = looker.read();
                while (skipIn > 0 && skip > read.getDistance()) {
                    skip -= read.getDistance();
                    if(looker.index<looker.size) {
                        looker.moveRelative(1);
                    }
                    doneInCur++;
                }
            }
            if(doneInCur>=leafNode.elements){
                int stackDepth = grpstack.size();
                int idx = stackDepth -1;
                while(idx>=0){ // zip all deeper levels to first element
                    GroupNode groupNode = grpstack.get(idx);
                    int cur = idxstack.get(idx);
                    cur++;
                    if (cur < groupNode.elements) {
                        idxstack.set(idx, cur);
                        Node<?> child = groupNode.children[cur];

                        while(GroupNode.class.isInstance(child)){
                            idx++;
                            GroupNode newParent = GroupNode.class.cast(child);
                            grpstack.set(idx, newParent);
                            idxstack.set(idx, 0);
                            child = newParent.children[0];
                        }

//                        idx++;
//                        while(idx < stackDepth){
//                            GroupNode newParent = GroupNode.class.cast(child);
//                            grpstack.set(idx, newParent);
//                            idxstack.set(idx, 0);
//                            child = newParent.children[0];
//                        }
                        doneInCur = 0;
                        LeafNode cast = LeafNode.class.cast(child);
                        return calculateNext(cast, skip);
                    }
                    idx--;
                }
                return false;
            }

            double readDistance = looker.read().getDistance();

            double remainingWithSkip = remaining + skip;
            if(remainingWithSkip < readDistance){
                interpolate(cur.read(), looker.read(), remainingWithSkip/readDistance, next.write());
                if(skip>0) {
                    next.write().setDistance(next.getDistance()-skip);
                }
                remaining = 0;
                doneInCur = Integer.MAX_VALUE;
                return true;
            }

            copy(looker.read(), next);
            if(skip>0) {
                next.write().setDistance(next.getDistance()-skip);
            }
//            remaining-=next.getDistance();
            if(looker.index<leafNode.elements-1) looker.moveRelative(1);
            doneInCur++;
            return true;
        }
    }

    protected void interpolate(R from, R to, double fraction, W result){

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        try {
            join(sb, "\n");
        } catch (IOException e) {
            e.printStackTrace();
            sb.append(e.getMessage());
        }
        return sb.toString();
    }
    public String join(String separator){
        StringBuilder sb = new StringBuilder();
        try {
            join(sb, separator);
        } catch (IOException e) {
            e.printStackTrace();
            sb.append(e.getMessage());
        }
        return sb.toString();
    }
    public void join(Appendable sb, String separator) throws IOException {
        Iterator<R> it = iterator();
        String sep = "";
        while(it.hasNext()){
            sb.append(sep);
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

    protected BaseTree(int blocksize, double precision) {
        this.blocksize = blocksize;
        this.precision = precision;
    }

    interface Reading <
            R extends Reading<R>
            > extends PublicRead {
        R read();

        double getDistance();
    }
    interface Writing<
            R extends Reading<R>,
            W extends Writing<R,W>
            > extends Reading<R> {
        W write();

        void setDistance(double distance);
    }
    interface Grouping<
            R extends Reading<R>,
            G extends Grouping<R,G>
            > extends PublicGroup {
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

    public interface PublicRead {

    }
    public interface PublicGroup {

    }


    boolean sameAs(R one, R other){
        return true;
    }
    void copy(R from, W to){}
    void extendBy(M toExtend, R point){}
    void extendBy(M toExtend, G aggregate){};

    W clearWrite(W toClear){
        return toClear;
    }
    M clearMerge(M toClear){
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

    final String stringifyPoint(R point){
        StringBuilder sb = new StringBuilder();
        try {
            stringifyPoint(sb, point);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return point.toString();
    }
    void stringifyPoint(Appendable sw, R point) throws IOException {
        if(point==null) {
            sw.append("null");
        }else{
            sw.append('[').append(point.toString()).append(']');
        }
    }
    protected void stringifyDouble(Appendable sw, double val) throws IOException {
        sw.append(""+val);
    }

    static abstract class Editor<L extends Editor<L,R,W,B>,R,W,B>  {
        protected final int weight;
        protected final int size;

        protected B buffer = null;
        protected int offset;
        protected int index;
        protected int actualIndex;

        protected final static int layerlen = 0;
        /**
         * @param size internal index steps per external index (as determined by implementors)
         * @param blocksize
         */
        protected Editor(int size, int blocksize) {
            this.weight = size;
            this.size = blocksize;
        }


        @SuppressWarnings("unchecked")
        private L asEditor(){
            return (L) this;
        }
        final public L wrap(B buffer, int offset) {
            this.buffer = buffer;
            this.offset=offset;

            this.index = 0;
            this.actualIndex = 0;
            return this.asEditor();
        }

        final public L moveRelative(int direction) {
            int next = index + direction;
            if(next < 0 || next >= size) {
                throw new IndexOutOfBoundsException();
            }
            index = next;
            actualIndex = offset + index*weight;
            return this.asEditor();
        }

        final public L moveAbsolute(int next) {
            if(next < 0 || next >= size) {
                throw new IndexOutOfBoundsException();
            }
            index = next;
            actualIndex = offset + index*weight;
            return this.asEditor();
        }

        public abstract B createBuffer();

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
