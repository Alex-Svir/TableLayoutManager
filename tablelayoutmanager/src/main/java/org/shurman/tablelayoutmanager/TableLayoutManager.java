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
    static final int STATE_RESID_TLM = 101;

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
        state.put(STATE_RESID_TLM, this);
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

    /*private*/ int mRows, mColumns, mCount;
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
                mColumns - 1 - mBorders[RIGHT].levels,
                mRows - 1 - mBorders[BOTTOM].levels);
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
            mFrame.measure(0, 0);
            adjustMeasurements();
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

        measureRegion(recycler, state,
                mColumns * mBorders[TOP].levels + mBorders[LEFT].levels,
                mScrollArea.columns,
                mScrollArea.rows,
                (m, c, r) -> {if (mScrollArea.cellWidth < m) mScrollArea.cellWidth = m;},
                (m, c, r) -> {if (mScrollArea.cellHeight < m) mScrollArea.cellHeight = m;});

        mFrame.measure(getWidth() - mBorders[LEFT].pixels - mBorders[RIGHT].pixels,
                        getHeight() - mBorders[TOP].pixels - mBorders[BOTTOM].pixels);
        adjustMeasurements();
        scrollHor.reset(mScrollArea.width() - mFrame.widthPx);
        scrollVert.reset(mScrollArea.height() - mFrame.heightPx);
    }

    private void adjustMeasurements() {       //  TODO    TODO    POLICY
        if (mScrollArea.empty()) {
            int bordersSum = mBorders[LEFT].pixels + mBorders[RIGHT].pixels;
            int limit = getWidth();
            if (bordersSum > limit) {
                //  POLICY:     ignore OR squeeze to junction
                resizeBorders(mBorders[LEFT], mBorders[RIGHT], limit);
                l(">>>___Horizontal borders overlay");
            } else if (bordersSum < limit) {
                //  POLICY:     pump to junction OR tetris
                resizeBorders(mBorders[LEFT], mBorders[RIGHT], limit);
                l(">>>___Horizontal borders diastasis");
            }
            bordersSum = mBorders[TOP].pixels + mBorders[BOTTOM].pixels;
            limit = getHeight();
            if (bordersSum > limit) {
                //  POLICY:     ignore OR squeeze to junction
                resizeBorders(mBorders[TOP], mBorders[BOTTOM], limit);
                l(">>>___Vertical borders overlay");
            } else if (bordersSum < limit) {
                //  POLICY:     pump to junction OR tetris
                resizeBorders(mBorders[TOP], mBorders[BOTTOM], limit);
                l(">>>___Vertical borders diastasis");
            }
            return;
        }

        int minFrame = calcMinFrameWidth();
        if (mFrame.widthPx < 0) {
            //  POLICY:     ignore OR squeeze to minFrame OR junction??
            resizeBorders(mBorders[LEFT], mBorders[RIGHT], minFrame);
            l(">>>__>>Horizontal borders overlay");
        } else if (mFrame.widthPx == 0) {
            //  POLICY:     ignore OR squeeze to minFrame
            resizeBorders(mBorders[LEFT], mBorders[RIGHT], minFrame);
            l(">>>__>>Horizontal borders joint");
        } else {        //  frame.width > 0
            if (mFrame.widthPx < minFrame) {
                //  POLICY:     ignore OR squeeze to minFrame OR pump to junction????!!
                resizeBorders(mBorders[LEFT], mBorders[RIGHT], minFrame);
                l(">>>__>>Scrollable frame clipped horizontally");
            } else if (mFrame.widthPx > mScrollArea.width()) {
                //  POLICY:     pump borders OR scrollArea OR both OR tetris
                float k = (float) getWidth() / (float) (mBorders[LEFT].pixels + mScrollArea.width() + mBorders[RIGHT].pixels);
                int newScrollArea = resizeScrollAreaHorizontally(k);
                resizeBorders(mBorders[LEFT], mBorders[RIGHT], getWidth() - newScrollArea);
                l(">>>__>>Scrollable Area too small horizontally");
            }
        }
        minFrame = calcMinFrameHeight();
        if (mFrame.heightPx < 0) {
            //  POLICY:     ignore OR squeeze to minFrame OR junction??
            resizeBorders(mBorders[TOP], mBorders[BOTTOM], minFrame);
            l(">>>__>>Vertical borders overlay");
        } else if (mFrame.heightPx == 0) {
            //  POLICY:     ignore OR squeeze to minFrame
            resizeBorders(mBorders[TOP], mBorders[BOTTOM], minFrame);
            l(">>>__>>Vertical borders joint");
        } else {        //  frame.height > 0
            if (mFrame.heightPx < minFrame) {
                //  POLICY:     ignore OR squeeze to minFrame OR pump to junction????!!
                resizeBorders(mBorders[TOP], mBorders[BOTTOM], minFrame);
                l(">>>__>>Scrollable frame clipped vertically");
            } else if (mFrame.heightPx > mScrollArea.height()) {
                //  POLICY:     pump borders OR scrollArea OR both OR tetris
                float k = (float) getHeight() / (float) (mBorders[TOP].pixels + mScrollArea.height() + mBorders[BOTTOM].pixels);
                int newScrollArea = resizeScrollAreaVertically(k);
                resizeBorders(mBorders[TOP], mBorders[BOTTOM], getHeight() - newScrollArea);
                l(">>>__>>Scrollable Area too small vertically");
            }
        }
        mFrame.measure(getWidth() - mBorders[LEFT].pixels - mBorders[RIGHT].pixels,
                        getHeight() - mBorders[TOP].pixels - mBorders[BOTTOM].pixels);
    }
    private int calcMinFrameWidth() { return mScrollArea.columns > 1 ? mScrollArea.cellWidth * 2 : mScrollArea.cellWidth; }     //  TODO    POLICY?
    private int calcMinFrameHeight() { return mScrollArea.rows > 1 ? mScrollArea.cellHeight * 2 : mScrollArea.cellHeight; }     //  TODO    POLICY?

    private int resizeScrollAreaVertically(float k) {
        mScrollArea.cellHeight *= k;
        return mScrollArea.height();
    }
    private int resizeScrollAreaHorizontally(float k) {
        mScrollArea.cellWidth *= k;
        return mScrollArea.width();
    }
    private int resizeScrollAreaVertically(int newHeight) {
        int h = mScrollArea.height();
        if (h == 0) return 0;
        return resizeScrollAreaVertically((float) newHeight / (float) h);
    }
    private int resizeScrollAreaHorizontally(int newWidth) {
        int w = mScrollArea.width();
        if (w == 0) return 0;
        return resizeScrollAreaHorizontally((float) newWidth / (float) w);
    }
    private static void resizeBorders(FixedBorder startBorder, FixedBorder endBorder, int newSize) {
        int oldSize = startBorder.pixels + endBorder.pixels;
        if (oldSize == 0) return;
        float k = (float) newSize / (float) oldSize;
        int d = newSize - resizeBorder(startBorder, k) - resizeBorder(endBorder, k);
        if (d > 0) { l(">>>>>>>\tSpread pixels on borders: " + d);                //  TODO    TODO  STUB
            int base = d / (startBorder.levels + endBorder.levels);
            int extra = d % (startBorder.levels + endBorder.levels);
            fineTunePumpSTUB(endBorder, base, fineTunePumpSTUB(startBorder, base, extra));
        }
        else if (d < 0) { l(">>>>>>>>>\tPick pixels from borders: " + d);       //  TODO    TODO  STUB
            fineTuneSqueezeSTUB(startBorder, endBorder, Math.min(startBorder.pixels + endBorder.pixels, -d));
        }
    }
    private static int resizeBorder(FixedBorder border, float k) {
        int px = 0;
        for (int i = 0; i < border.levels; ++i) {
            int val = (int) (border.sizes.get(i) * k);
            px += val;
            border.sizes.set(i, val);
        }
        return border.pixels = px;
    }
    private static int fineTunePumpSTUB(FixedBorder border, int base, int extra) {      //  TODO    TODO    STUB
        int idx = 0;
        while (extra > 0 && idx < border.levels) {
            border.sizes.set(idx, border.sizes.get(idx) + base + 1);
            ++idx;
            --extra;
        }
        if (base > 0) {
            while (idx < border.levels) {
                border.sizes.set(idx, border.sizes.get(idx) + base);
                ++idx;
            }
        }
        return extra;
    }
    private static void fineTuneSqueezeSTUB(FixedBorder start, FixedBorder end, int n) { //  TODO    TODO   STUB
        while (n > 0) {
            for (int i = 0; i < start.levels; ++i) {
                int val = start.sizes.get(i);
                if (val > 0) {
                    start.sizes.set(i, val - 1);
                    if (--n == 0) return;
                }
            }
            for (int i = 0; i < end.levels; ++i) {
                int val = end.sizes.get(i);
                if (val > 0) {
                    end.sizes.set(i, val - 1);
                    if (--n == 0) return;
                }
            }
        }
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
        /*private*/ boolean empty() {return rows == 0 || columns == 0;}
        private int width() { return columns * cellWidth; }
        private int height() { return rows * cellHeight; }
    }

    /*private*/ class InnerFrame {
        private int widthPx, heightPx;
        /*private*/ int topRow, bottomRow, leftCol, rightCol;
        int topOutFrameOffset, leftOutFrameOffset;

        private void measure(int w, int h) {
            widthPx = w;
            heightPx = h;
        }

        private void locate() {
            //assert
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
            maxScroll = Math.max(range, 0);
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
