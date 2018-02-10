package de.ulf_schreiber.fastbike.boundingpiecechain.value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

public abstract class Tree<
        V extends CoordinateDistance<R,W,G,M>,
        R extends CoordinateDistance.Reading<R>,
        W extends CoordinateDistance.Writing<R,W> & CoordinateDistance.Reading<R>,
        G extends CoordinateDistance.Grouping<R,G>,
        M extends CoordinateDistance.Merging<R,G,M> & CoordinateDistance.Grouping<R,G>,
        B,
        L extends Value.Editor<L,R,W,B>,
        A extends Value.Editor<A,G,M,B>,
        T extends Tree<V,R,W,G,M,B,L,A,T>
    > implements Iterable<R>{
    final V meta;

    public Tree(V meta) {
        this.meta = meta;
    }

    int blocksize = 16;

    void stringifyPoint(Appendable sw, R point){
        meta.stringifyPoint(sw,point);
    }

    boolean samePoint(R a, R b){
        return meta.sameAs(a,b);
    }


    abstract class Node<
//            V extends CoordinateDistance<R,W,G,M>,
//            R extends CoordinateDistance.Reading<R>,
//            W extends CoordinateDistance.Writing<R,W> & CoordinateDistance.Reading<R>,
//            G extends CoordinateDistance.Grouping<R,G>,
//            M extends CoordinateDistance.Merging<R,G,M> & CoordinateDistance.Grouping<R,G>,
//            B,
//            L extends Value.Editor<L,R,W,B>,
//            A extends Value.Editor<A,G,M,B>,
//            N extends Node<V,R,W,G,M,B,L,A,N>
            N extends Node<N>
        > {

        B array;
        int elements = 0;
        abstract void build(T.BoundingBuilder builder);
        abstract int depth();

        protected abstract void calculateNext(NodeIterator state, double skip);
    }
    abstract class LeafNode
//            <
//            V extends CoordinateDistance<R,W,G,M>,
//            R extends CoordinateDistance.Reading<R>,
//            W extends CoordinateDistance.Writing<R,W> & CoordinateDistance.Reading<R>,
//            G extends CoordinateDistance.Grouping<R,G>,
//            M extends CoordinateDistance.Merging<R,G,M> & CoordinateDistance.Grouping<R,G>,
//            B,
//            L extends Value.Editor<L,R,W,B>,
//            A extends Value.Editor<A,G,M,B>
//    >
            extends Node<LeafNode> {


        LeafNode(T tree, Iterator<R> it, L elemLooker, M mutableBoundsReturn) {
            final int blocksize = tree.blocksize;
            final V meta = tree.meta;
            array = elemLooker.createBuffer(blocksize);
            elemLooker.wrap(array, blocksize,0);
            while(it.hasNext() && elements < blocksize){
                R next = it.next();
                meta.copy(next, elemLooker.write());
                meta.extendBy(mutableBoundsReturn, next);
                elemLooker.moveRelative(1);
                elements++;
            }
        }

        @Override int depth() { return 0; }
        @Override
        public void build(T.BoundingBuilder builder) {
            V meta = builder.tree().meta;
            final double precision = meta.precision;
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
                    meta.extendBy(builder.result, looker.read());
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
        protected void calculateNext(NodeIterator state, double skip) {
            state.leaf=this; // for next iteration
            L looker = state.valueLooker;
            looker.wrap(array, elements, 0).moveAbsolute(state.doneInCur);
            while(skip>meta.precision){
                state.doneInCur++;
                skip -= looker.moveRelative(1).read().getDistance();
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
                        return;
                    }
                    idx--;
                }
                if(idx<0){
                    // EOT
                    state.next=null;
                    return;
                }
            }
            state.next = looker.moveRelative(1).read();
            state.doneInCur++;
        }
    }
//    abstract static class GroupNode<
//            V extends CoordinateDistance<R,W,G,M>,
//            R extends CoordinateDistance.Reading<R>,
//            W extends CoordinateDistance.Writing<R,W> & CoordinateDistance.Reading<R>,
//            G extends CoordinateDistance.Grouping<R,G>,
//            M extends CoordinateDistance.Merging<R,G,M> & CoordinateDistance.Grouping<R,G>,
//            B,
//            L extends Value.Editor<L,R,W,B>,
//            A extends Value.Editor<A,G,M,B>
//        > extends Node<V,R,W,G,M,B,L,A,GroupNode> {
    abstract class GroupNode extends Node<GroupNode> {


        Node<?>[] children;

        int depth;

        @Override
        int depth() {
            return depth;
        }

        GroupNode(T tree, Iterator<R> it, L elemLooker, A groupLooker, M mutableBoundsReturn, Node<?> firstChild, int depth){
            children = new Node[tree.blocksize];
            array = groupLooker.createBuffer(tree.blocksize);
            final V meta = tree.meta;
            this.depth = depth;

            groupLooker.wrap(array, 0, 0);

            M groupLookerWrite = groupLooker.write();
            G mutableBoundsReturnRead = mutableBoundsReturn.read();

            if(firstChild!=null) { // creating a new root
                children[0]=firstChild;
                meta.clearMerge(groupLookerWrite);
                meta.extendBy(groupLookerWrite, mutableBoundsReturnRead);
            }

            while(it.hasNext() && elements<tree.blocksize){
                Node<?> newNode;

                if(depth==1){
                    groupLooker.moveAbsolute(elements);
                    newNode = tree.createLeafNode(tree, it, elemLooker, groupLookerWrite);
                } else {
                    meta.clearMerge(mutableBoundsReturn);
                    newNode = tree.createGroupNodeNode(tree, it, elemLooker, groupLooker, mutableBoundsReturn, null, depth-1);
                    groupLooker.wrap(array, elements, 0);
                    meta.clearMerge(groupLookerWrite);
                    meta.extendBy(groupLookerWrite, mutableBoundsReturnRead);
                }
                children[elements] = newNode;
                elements++;
            }

            meta.clearMerge(mutableBoundsReturn);
            groupLooker.wrap(array, 0, 0);
            for(int i=0; i<elements;i++){
                groupLooker.moveAbsolute(i);
                meta.extendBy(mutableBoundsReturn, groupLooker.read());
            }
        }

        @Override
        public void build(T.BoundingBuilder builder) {
            V meta = builder.tree().meta;
            final double precision = meta.precision;
            if(builder.toVisit < precision) return;

            A groupLooker = builder.groupLooker;
            meta.clearMerge(groupLooker.write());
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
                        meta.extendBy(builder.result, read);
                        builder.toVisit=restToVisit;
                    }else{
                        children[i].build(builder);
                        return;
                    }
                }
            }
        }

    @Override
    protected void calculateNext(NodeIterator state, double skip) {
        A looker = state.groupLooker;
        looker.wrap(array, elements, 0);
        for(int i=0;i<elements;i++){
            double childLen = looker.read().getDistance();
            if(childLen +meta.precision > skip){
                Node<?> child = children[i];
                state.doneInCur=0;
                state.grpstack.add(this);

                state.idxstack.add(i);

                child.calculateNext(state, skip);
                return;
            }
            skip -= childLen;
            looker.moveRelative(1);
        }
    }
}
    class Segment {
        final Node<?> root;
        final G bounds;

        Segment(Node<?> root, G bounds) {
            this.root = root;
            this.bounds = bounds;
        }
    }
    abstract LeafNode createLeafNode(T tree, Iterator<R> it, L looker, M bounds);
    abstract GroupNode createGroupNodeNode(T tree, Iterator<R> it, L looker, A groupLooker, M bounds, Node firstChild, int depth);

    Segment makeTree(Iterable<R> iterable){
        L elemLooker = meta.<L,B>createElementWriter();
        A groupLooker = meta.<A,B>createAggregateWriter();
        Iterator<R> it = iterable.iterator();
        M mutableBounds = meta.createMutableBounds();
        LeafNode first = createLeafNode(self(), it, elemLooker, mutableBounds);
        Node root = first;
        int depth = 0;
        while(it.hasNext()){
            depth++;
            root = createGroupNodeNode(self(), it, elemLooker, groupLooker, mutableBounds, root, depth);
        }

        return new Segment(root, mutableBounds.read());
    }

    protected T self(){
        return (T) this;
    }

    /**
     * wraps a node, either whole or a subset (this way, multiple pieces can use the same immutable tree, for when a piece is cut)
     */
    class Piece {
        G bounds;
        final Node<?> root;
        final double offset; // length is contained in bounds
        Piece(Node<?> root, double offset, double distance, T tree) {
            this.root = root;
            this.offset = offset;

            T.BoundingBuilder boundingBuilder = tree.new BoundingBuilder(offset, distance);
            root.build(boundingBuilder);
            bounds = boundingBuilder.result.read();
        }
    }
    class Undo<
            V extends CoordinateDistance<R,W,G,M>,
            R extends CoordinateDistance.Reading<R>,
            W extends CoordinateDistance.Writing<R,W> & CoordinateDistance.Reading<R>,
            G extends CoordinateDistance.Grouping<R,G>,
            M extends CoordinateDistance.Merging<R,G,M> & CoordinateDistance.Grouping<R,G>,
            B,
            L extends Value.Editor<L,R,W,B>,
            A extends Value.Editor<A,G,M,B>
            > {
        int wrap;
        int removed; // first n pieces are removed, rest added
        Piece[] pieces;

        final void apply(T tree) {
            ListIterator<Piece> iterator = tree.pieces.listIterator(wrap);
            int i = 0;
            for (; i < removed; i++) iterator.remove();
            int length = pieces.length;
            for (; i < length; i++) iterator.add(pieces[i]);
        }

        final void unapply(T tree) {
            ListIterator<Piece> iterator = tree.pieces.listIterator(wrap);
            int i = removed;
            int length = pieces.length;
            for (; i < length; i++) iterator.remove();

            for (i = 0; i < removed; i++) iterator.add(pieces[i]);
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
            result = meta.createMutableBounds();
            valueLooker = meta.<L,B>createElementWriter();
            groupLooker = meta.<A,B>createAggregateWriter();
        }

        public T tree() {
            return (T) Tree.this;
        }
    }


    protected final LinkedList<Piece> pieces = new LinkedList<>();
    protected final LinkedList<Undo> undos = new LinkedList<>();
    public void append(Iterable<? extends R> points){
        replace(Double.MAX_VALUE, 0, points);
    }
    public void insert(double wrap, Iterable<? extends R> points){
        replace(wrap, 0, points);
    }
    public void delete(double wrap, double distance){
        replace(wrap, distance, Collections.<R>emptyList());
    }
    public void replace(double wrap, double distance, Iterable<? extends R> points){

    }


//
//    /** @return new root or self (will always return self if called with parent) */
//    Node<R,T,?> extendTree(GroupNode<R,T> group, GroupNode<R,T> parent, Iterator<Step> iterator, R looker){
//        GroupNode<R, T> cur = group;
//        while(iterator.hasNext()) {
//            if (cur.len == this.blocksize) {
//                if(parent==null){
//                    GroupNode<R, T> newParent = new GroupNode<>(this);
//                    newParent.add(cur);
//                    return extendTree(newParent, null, iterator, looker);
//                }else {
//                    // continue in parent call
//                    return group;
//                }
//            }
//            if(cur.containsLeaves){
//                LeafNode<R, T> newChild = new LeafNode<>(this, iterator, looker);
//
//                cur.add(newChild);
//            }else{
//                GroupNode<R, T> newChild = new GroupNode<>(this);
//                extendTree(newChild, cur, iterator, looker);
//                cur.add(newChild);
//            }
//        }
//        return group;
//    }
//

    @Override
    public Iterator<R> iterator() {
        return new Iterator<R>() {
            R next = calcNext();
            Iterator<Piece> pieceIt = pieces.iterator();
            Piece currentPiece = null;
            double currentPieceDistLeft = 0;

            NodeIterator nodeIt = new NodeIterator();
            double curNodeDone = 0;

            protected R calcNext(){
                while(pieceIt.hasNext() && ! nodeIt.hasNext()){
                    currentPiece=pieceIt.next();
                    currentPieceDistLeft=currentPiece.bounds.getDistance();
                    nodeIt.reset(currentPiece.root, currentPiece.offset, currentPiece.bounds.getDistance());
                }
                if( ! nodeIt.hasNext()) return null;
                return nodeIt.next();
            }
            @Override public boolean hasNext() {
                return next!=null;
            }

            @Override public R next() {
                R ret = this.next;
                next = calcNext();
                return ret;
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
        protected R next;
        L valueLooker = meta.<L,B>createElementWriter();
        A groupLooker = meta.<A,B>createAggregateWriter();
        protected int doneInCur = 0;

        void reset(Node root, double skip, double total){
            grpstack.ensureCapacity(root.depth());
            idxstack.ensureCapacity(root.depth());
            grpstack.clear();
            idxstack.clear();
            remaining = total;
            root.calculateNext(this, skip);
        }

        @Override
        public boolean hasNext() {
            return next!=null && remaining>-meta.precision;
        }

        @Override
        public R next() {
            R ret = next;
            remaining-=ret.getDistance();
            leaf.calculateNext(this, 0);
            return ret;
        }
        @Override public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
