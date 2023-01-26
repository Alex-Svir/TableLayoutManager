package org.shurman.tablelayoutmanager;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class TableLayoutManager extends RecyclerView.LayoutManager {
    private final Scroller scrollHor = new Scroller();
    private final Scroller scrollVert = new Scroller();

    public TableLayoutManager(int columns, int rows, int topBorder, int leftBorder, int rightBorder, int bottomBorder) {
        reset(columns, rows, topBorder, leftBorder, rightBorder, bottomBorder);
    }

    public TableLayoutManager() { this(0, 0, 0, 0, 0, 0); }

    public void reset(int columns, int rows, int topBorder, int leftBorder, int rightBorder, int bottomBorder) {
        setTableDimensions(columns, rows, topBorder, leftBorder, rightBorder, bottomBorder);
        //remeasureTable()?
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        l("onLayoutChildren");
        if (state.didStructureChange()) {
            remeasureTable(recycler, state);
        }
        layoutTable(recycler, state);
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int scrolled = scrollHor.scroll(dx);
        if (scrolled != 0) {
            layoutTable(recycler, state);
        }
        return scrolled;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int scrolled = scrollVert.scroll(dy);
        if (scrolled != 0) {
            layoutTable(recycler, state);
        }
        return scrolled;
    }

    @Override
    public boolean canScrollHorizontally() { return scrollHor.isScrollable(); }

    @Override
    public boolean canScrollVertically() { return scrollVert.isScrollable(); }

    @Override
    public void onAdapterChanged(@Nullable RecyclerView.Adapter oldAdapter, @Nullable RecyclerView.Adapter newAdapter) {
        super.onAdapterChanged(oldAdapter, newAdapter);
        l("oldAdapter " + (oldAdapter == null ? "-" : "+") + "=> newAdapter " + (newAdapter == null ? "-" : "+"));
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

//=========================================================================================================================
//*************************************************************************************************************************
//=========================================================================================================================
//*************************************************************************************************************************
//=========================================================================================================================

    private int mRows, mColumns, mCount;
    /*private*/ final FixedBorder[] mBorders = new FixedBorder[]
                    {new FixedBorder(), new FixedBorder(), new FixedBorder(), new FixedBorder()};
    /*private*/ final ScrollArea mScrollArea = new ScrollArea();
    /*private*/ final InnerFrame mFrame = new InnerFrame();

    private void setTableDimensions(int columns, int rows, int topBorder, int leftBorder, int rightBorder, int bottomBorder) {
        assert (columns >= 0 && rows >= 0
                && topBorder >= 0 && leftBorder >= 0 && rightBorder >= 0 && bottomBorder >= 0
                && topBorder + bottomBorder <= rows && leftBorder + rightBorder <= columns)
                : "Illegal Table Layout initialization values";
        mCount = columns * rows;
        mColumns = columns;
        mRows = rows;
        mBorders[TOP].set(topBorder);
        mBorders[LEFT].set(leftBorder);
        mBorders[RIGHT].set(rightBorder);
        mBorders[BOTTOM].set(bottomBorder);
        mScrollArea.set(mBorders[TOP].levels,
                        mBorders[LEFT].levels,
                mColumns - mBorders[RIGHT].levels - 1,
                mRows - mBorders[BOTTOM].levels - 1);
    }

    private void remeasureTable(RecyclerView.Recycler recycler, RecyclerView.State state) {
        l("Remeasuring table");
        if (mBorders[TOP].levels > 0 && mBorders[LEFT].levels > 0)
            measureRegion(recycler, state,
                    0,
                    mBorders[LEFT].levels,
                    mBorders[TOP].levels,
                    (m, c, r) -> {if (m > mBorders[LEFT].sizes.get(c)) mBorders[LEFT].sizes.set(c, m);},
                    (m, c, r) -> {if (m > mBorders[TOP].sizes.get(r)) mBorders[TOP].sizes.set(r, m);});
        if (mBorders[TOP].levels > 0 && mBorders[RIGHT].levels > 0)
            measureRegion(recycler, state,
                    mColumns - mBorders[RIGHT].levels,
                    mBorders[RIGHT].levels,
                    mBorders[TOP].levels,
                    (m, c, r) -> {if (m > mBorders[RIGHT].sizes.get(c)) mBorders[RIGHT].sizes.set(c, m);},
                    (m, c, r) -> {if (m > mBorders[TOP].sizes.get(r)) mBorders[TOP].sizes.set(r, m);});
        if (mBorders[BOTTOM].levels > 0 && mBorders[LEFT].levels > 0)
            measureRegion(recycler, state,
                    mColumns * (mRows - mBorders[BOTTOM].levels),
                    mBorders[LEFT].levels,
                    mBorders[BOTTOM].levels,
                    (m, c, r) -> {if (m > mBorders[LEFT].sizes.get(c)) mBorders[LEFT].sizes.set(c, m);},
                    (m, c, r) -> {if (m > mBorders[BOTTOM].sizes.get(r)) mBorders[BOTTOM].sizes.set(r, m);});
        if (mBorders[BOTTOM].levels > 0 && mBorders[RIGHT].levels > 0)
            measureRegion(recycler, state,
                    mColumns * (mRows - mBorders[BOTTOM].levels) + (mColumns - mBorders[RIGHT].levels),
                    mBorders[RIGHT].levels,
                    mBorders[BOTTOM].levels,
                    (m, c, r) -> {if (m > mBorders[RIGHT].sizes.get(c)) mBorders[RIGHT].sizes.set(c, m);},
                    (m, c, r) -> {if (m > mBorders[BOTTOM].sizes.get(r)) mBorders[BOTTOM].sizes.set(r, m);});

        if (mScrollArea.empty()) {
            mFrame.widthPx = 0;
            mFrame.heightPx = 0;
            scrollHor.reset(0);
            scrollVert.reset(0);
            return;
        }

        if (mBorders[TOP].levels > 0)
            measureRegion(recycler, state,
                    mBorders[LEFT].levels,
                    mScrollArea.columns,
                    mBorders[TOP].levels,
                    (m, c, r) -> {if (m > mScrollArea.cellWidth) mScrollArea.cellWidth = m;},///////////////
                    (m, c, r) -> {if (m > mBorders[TOP].sizes.get(r)) mBorders[TOP].sizes.set(r, m);});
        if (mBorders[LEFT].levels > 0)
            measureRegion(recycler, state,
                    mColumns * mBorders[TOP].levels,
                    mBorders[LEFT].levels,
                    mScrollArea.rows,
                    (m, c, r) -> {if (m > mBorders[LEFT].sizes.get(c)) mBorders[LEFT].sizes.set(c, m);},
                    (m, c, r) -> {if (m > mScrollArea.cellHeight) mScrollArea.cellHeight = m;});
        if (mBorders[RIGHT].levels > 0)
            measureRegion(recycler, state,
                    mColumns * (mBorders[TOP].levels + 1) - mBorders[RIGHT].levels,
                    mBorders[RIGHT].levels,
                    mScrollArea.rows,
                    (m, c, r) -> {if (m > mBorders[RIGHT].sizes.get(c)) mBorders[RIGHT].sizes.set(c, m);},
                    (m, c, r) -> {if (m > mScrollArea.cellHeight) mScrollArea.cellHeight = m;});
        if (mBorders[BOTTOM].levels > 0)
            measureRegion(recycler, state,
                    mColumns * (mRows - mBorders[BOTTOM].levels) + mBorders[LEFT].levels,
                    mScrollArea.columns,
                    mBorders[BOTTOM].levels,
                    (m, c, r) -> {if (m > mScrollArea.cellWidth) mScrollArea.cellWidth = m;},
                    (m, c, r) -> {if (m > mBorders[BOTTOM].sizes.get(r)) mBorders[BOTTOM].sizes.set(r, m);});

        IntStream.range(0, 4).forEach(i -> mBorders[i].calcPixels());
        //todo squeeze/adapt

        measureRegion(recycler, state,
                mColumns * mBorders[TOP].levels + mBorders[LEFT].levels,
                mScrollArea.columns,
                mScrollArea.rows,
                (m, c, r) -> {if (mScrollArea.cellWidth < m) mScrollArea.cellWidth = m;},
                (m, c, r) -> {if (mScrollArea.cellHeight < m) mScrollArea.cellHeight = m;});

        mFrame.widthPx = getWidth() - mBorders[LEFT].pixels - mBorders[RIGHT].pixels;
        mFrame.heightPx = getHeight() - mBorders[TOP].pixels - mBorders[BOTTOM].pixels;
        scrollHor.reset(mScrollArea.width() - mFrame.widthPx);
        scrollVert.reset(mScrollArea.height() - mFrame.heightPx);
    }

    private void layoutTable(RecyclerView.Recycler recycler, RecyclerView.State state) {
        detachAndScrapAttachedViews(recycler);
        if (!mScrollArea.empty()) {
            mFrame.locate();
            layoutRegion(recycler, state,
                    mFrame.topRow * mColumns + mFrame.leftCol,
                    mFrame.rightCol - mFrame.leftCol + 1,
                    mFrame.bottomRow - mFrame.topRow + 1,
                    mBorders[TOP].pixels - mFrame.topOutFrameOffset,
                    mBorders[LEFT].pixels - mFrame.leftOutFrameOffset,
                    (c, r) -> mScrollArea.cellHeight,
                    (c, r) -> mScrollArea.cellWidth);
            layoutRegion(recycler, state,
                    mFrame.topRow * mColumns,
                    mBorders[LEFT].levels,
                    mFrame.bottomRow - mFrame.topRow + 1,
                    mBorders[TOP].pixels - mFrame.topOutFrameOffset,
                    0,
                    (c, r) -> mScrollArea.cellHeight,
                    (c, r) -> mBorders[LEFT].sizes.get(c));
            layoutRegion(recycler, state,
                    (mFrame.topRow + 1) * mColumns - mBorders[RIGHT].levels,
                    mBorders[RIGHT].levels,
                    mFrame.bottomRow - mFrame.topRow + 1,
                    mBorders[TOP].pixels - mFrame.topOutFrameOffset,
                    getWidth() - mBorders[RIGHT].pixels,
                    (c, r) -> mScrollArea.cellHeight,
                    (c, r) -> mBorders[RIGHT].sizes.get(c));
            layoutRegion(recycler, state,
                    mFrame.leftCol,
                    mFrame.rightCol - mFrame.leftCol + 1,
                    mBorders[TOP].levels,
                    0,
                    mBorders[LEFT].pixels - mFrame.leftOutFrameOffset,
                    (c, r) -> mBorders[TOP].sizes.get(r),
                    (c, r) -> mScrollArea.cellWidth);
            layoutRegion(recycler, state,
                    (mRows - mBorders[BOTTOM].levels) * mColumns + mFrame.leftCol,
                    mFrame.rightCol - mFrame.leftCol + 1,
                    mBorders[BOTTOM].levels,
                    getHeight() - mBorders[BOTTOM].pixels,
                    mBorders[LEFT].pixels - mFrame.leftOutFrameOffset,
                    (c, r) -> mBorders[BOTTOM].sizes.get(r),
                    (c, r) -> mScrollArea.cellWidth);
        }
        layoutRegion(recycler, state,
                0,
                mBorders[LEFT].levels,
                mBorders[TOP].levels,
                0,
                0,
                (c, r) -> mBorders[TOP].sizes.get(r),
                (c, r) -> mBorders[LEFT].sizes.get(c));
        layoutRegion(recycler, state,
                mColumns - mBorders[RIGHT].levels,
                mBorders[RIGHT].levels,
                mBorders[TOP].levels,
                0,
                getWidth() - mBorders[RIGHT].pixels,
                (c, r) -> mBorders[TOP].sizes.get(r),
                (c, r) -> mBorders[RIGHT].sizes.get(c));
        layoutRegion(recycler, state,
                (mRows - mBorders[BOTTOM].levels) * mColumns,
                mBorders[LEFT].levels,
                mBorders[BOTTOM].levels,
                getHeight() - mBorders[BOTTOM].pixels,
                0,
                (c, r) -> mBorders[BOTTOM].sizes.get(r),
                (c, r) -> mBorders[LEFT].sizes.get(c));
        layoutRegion(recycler, state,
                (mRows - mBorders[BOTTOM].levels + 1) * mColumns - mBorders[RIGHT].levels,
                mBorders[RIGHT].levels,
                mBorders[BOTTOM].levels,
                getHeight() - mBorders[BOTTOM].pixels,
                getWidth() - mBorders[RIGHT].pixels,
                (c, r) -> mBorders[BOTTOM].sizes.get(r),
                (c, r) -> mBorders[RIGHT].sizes.get(c));
        recycler.clear();
    }

    private void measureRegion(RecyclerView.Recycler recycler, RecyclerView.State state,
                               int ptr, int width, int height,
                               MeasureRegistrar widthReg,
                               MeasureRegistrar heightReg) {
        int count = state.getItemCount();
        int interRowsInc = mColumns - width;
        for (int row = 0; row < height; ++row, ptr += interRowsInc) {
            for (int col = 0; col < width; ++col, ++ptr) {
                if (ptr >= count) return;
                final View view = recycler.getViewForPosition(ptr);
                measureChildWithMargins(view, 0, 0);
                //widthReg.register(view.getMeasuredWidth(), col, row);
                widthReg.register(getDecoratedMeasuredWidth(view), col, row);
                //heightReg.register(view.getMeasuredHeight(), col, row);
                heightReg.register(getDecoratedMeasuredHeight(view), col, row);
            }
        }
    }

    private void layoutRegion(RecyclerView.Recycler recycler, RecyclerView.State state,
                              int ptr, int width, int height,
                              int topBoundary, int leftBoundary,
                              ContraMeasurer bottomFinder, ContraMeasurer rightFinder) {
        int count = state.getItemCount();
        int interRowsInc = mColumns - width;
        int top = topBoundary;
        for (int row = 0; row < height; ++row, ptr += interRowsInc) {
            int left = leftBoundary;
            int bottom = top + bottomFinder.diff(0, row);
            for (int col = 0; col < width; ++col, ++ptr) {
                if (ptr >= count) return;
                final View view = recycler.getViewForPosition(ptr);
                addView(view);
                measureChildWithMargins(view, 0, 0);
                int right = left + rightFinder.diff(col, row);
                layoutDecoratedWithMargins(view, left, top, right, bottom);
                left = right;
            }
            top = bottom;
        }
    }


//--------------------------------------------------------------------------------------------------
    /*private*/ static final int TOP = 0;
    /*private*/ static final int LEFT = 1;
    /*private*/ static final int RIGHT = 2;
    /*private*/ static final int BOTTOM = 3;

    /*private*/ static class FixedBorder {
        private int levels;
        /*private*/ int pixels;
        /*private*/ final List<Integer> sizes;
        private FixedBorder() {
            levels = 0;
            pixels = 0;
            sizes = new ArrayList<>();
        }
        private void set(int levels) {
            this.levels = levels;
            sizes.clear();
            IntStream.range(0, levels).forEach(i -> sizes.add(0));
        }
        private void calcPixels() { pixels = sizes.stream().mapToInt(i -> i).sum(); }
    }

    /*private*/ static class ScrollArea {
        int rows, columns;
        int topRow, bottomRow, leftCol, rightCol;
        int cellWidth, cellHeight;
        private ScrollArea() {
            rows = 0; columns = 0;
            topRow = 0; bottomRow = 0; leftCol = 0; rightCol = 0;
            cellWidth = 0; cellHeight = 0;
        }
        private void set(int topRow, int leftCol, int rightCol, int bottomRow) {
            this.topRow = topRow;
            this.leftCol = leftCol;
            this.rightCol = rightCol;
            this.bottomRow = bottomRow;
            rows = bottomRow - topRow + 1;
            columns = rightCol - leftCol + 1;
        }
        private boolean empty() {return rows == 0 || columns == 0;}
        private int width() { return columns * cellWidth; }
        private int height() { return rows * cellHeight; }
    }

    /*private*/ class InnerFrame {
        private int widthPx, heightPx;
        /*private*/ int topRow, bottomRow, leftCol, rightCol;
        int topOutFrameOffset, leftOutFrameOffset;

        private void locate() {
            int horScrollPosition = TableLayoutManager.this.scrollHor.getPosition();
            int vertScrollPosition = TableLayoutManager.this.scrollVert.getPosition();
            leftCol = horScrollPosition / TableLayoutManager.this.mScrollArea.cellWidth
                            + TableLayoutManager.this.mBorders[LEFT].levels;
            leftOutFrameOffset = horScrollPosition % TableLayoutManager.this.mScrollArea.cellWidth;
            rightCol = (horScrollPosition + widthPx) / TableLayoutManager.this.mScrollArea.cellWidth
                            + TableLayoutManager.this.mBorders[LEFT].levels;
            if (0 == (horScrollPosition + widthPx) % TableLayoutManager.this.mScrollArea.cellWidth)
                    --rightCol;
            topRow = vertScrollPosition / TableLayoutManager.this.mScrollArea.cellHeight
                            + TableLayoutManager.this.mBorders[TOP].levels;
            topOutFrameOffset = vertScrollPosition % TableLayoutManager.this.mScrollArea.cellHeight;
            bottomRow = (vertScrollPosition + heightPx) / TableLayoutManager.this.mScrollArea.cellHeight
                            + TableLayoutManager.this.mBorders[TOP].levels;
            if (0 == (vertScrollPosition + heightPx) % TableLayoutManager.this.mScrollArea.cellHeight)
                    --bottomRow;
        }
    }

    private static class Scroller {
        private final int minScroll;
        private int maxScroll;
        private int currentScroll;
        private boolean scrollable;

        public Scroller() {
            minScroll = 0;
            reset(0);
        }
        public void reset(int range) {
            maxScroll = range;
            currentScroll = minScroll;                    //      TODO        savedInstanceState
            scrollable = maxScroll > minScroll;
        }
        public int scroll(int d) {
            int previousScroll = currentScroll;
            currentScroll += d;
            if (currentScroll < minScroll) currentScroll = minScroll;
            else if (currentScroll > maxScroll) currentScroll = maxScroll;
            return currentScroll - previousScroll;
        }
        public int getPosition() { return currentScroll; }
        public boolean isScrollable() { return scrollable; }
    }

    private interface MeasureRegistrar {
        void register(int measure, int column, int row);
    }

    private interface ContraMeasurer {
        int diff(int column, int row);
    }
//==================================================================================================
    private static void l(String text) { Log.d("LOG_TAG::", text); }
}
