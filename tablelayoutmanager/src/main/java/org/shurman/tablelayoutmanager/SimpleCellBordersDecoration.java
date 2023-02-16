package org.shurman.tablelayoutmanager;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SimpleCellBordersDecoration extends RecyclerView.ItemDecoration {
    private static final int DEFAULT_LINE_WIDTH = 3;

    private final Paint mPaint;
    private int mLineWidth;

    public SimpleCellBordersDecoration(int lineWidth, int color) {
        assert lineWidth >= 0 : "Line width must be non-negative";
        mPaint = new Paint();
        setLineWidth(lineWidth);
        mPaint.setColor(color);
    }

    public SimpleCellBordersDecoration(int lineWidth) { this(lineWidth, Color.BLACK); }

    public SimpleCellBordersDecoration() { this(DEFAULT_LINE_WIDTH); }

    public void setLineWidth(int lineWidth) {
        mLineWidth = lineWidth;
        if ((lineWidth % 2) == 1)
            ++mLineWidth;
        mPaint.setStrokeWidth(mLineWidth);
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        TableLayoutManager tlm = state.get(TableLayoutManager.STATE_RESID_TLM);
        int columns = tlm.mColumns;
        int rows = tlm.mRows;
        int idx = parent.getChildLayoutPosition(view);
        int sz = mLineWidth >> 1;
        outRect.set(
                idx % columns   ==  0                       ?   mLineWidth : sz,    //  left
                idx             <   columns                 ?   mLineWidth : sz,    //  top
                idx % columns   ==  (columns - 1)           ?   mLineWidth : sz,    //  right
                idx             >=  (columns * (rows - 1))  ?   mLineWidth : sz     //  bottom
        );
    }

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        TableLayoutManager tlm = state.get(TableLayoutManager.STATE_RESID_TLM);

        int w = parent.getWidth();
        int h = parent.getHeight();

        //  frame
        int d = mLineWidth >> 1;
        c.drawLine(0, d, w, d, mPaint);                         //  top
        c.drawLine(0, h - d, w, h - d, mPaint);     //  bottom
        c.drawLine(d,0, d, h, mPaint);                          //  left
        c.drawLine(w - d, 0, w - d, h, mPaint);     //  right

        //  borders
        List<Integer> border = tlm.mBorders[TableLayoutManager.TOP].sizes;
        int y = 0;
        for (Integer sz : border) {
            y += sz;
            c.drawLine(0, y, w, y, mPaint);
        }

        //  scroll area
        y += tlm.mScrollArea.cellHeight - tlm.mFrame.topOutFrameOffset;
        int endBorderBound = h - tlm.mBorders[TableLayoutManager.BOTTOM].pixels;
        if (!tlm.mScrollArea.empty()) {
            while (y < endBorderBound) {
                c.drawLine(0, y, w, y, mPaint);
                y += tlm.mScrollArea.cellHeight;
            }
        }

        border = tlm.mBorders[TableLayoutManager.BOTTOM].sizes;
        y = endBorderBound;
        for (Integer sz : border) {
            c.drawLine(0, y, w, y, mPaint);
            y += sz;
        }

        border = tlm.mBorders[TableLayoutManager.LEFT].sizes;
        int x = 0;
        for (Integer sz : border) {
            x += sz;
            c.drawLine(x, 0, x, h, mPaint);
        }

        //  scroll area
        x += tlm.mScrollArea.cellWidth - tlm.mFrame.leftOutFrameOffset;
        endBorderBound = w - tlm.mBorders[TableLayoutManager.RIGHT].pixels;
        if (!tlm.mScrollArea.empty()) {
            while (x < endBorderBound) {
                c.drawLine(x, 0, x, h, mPaint);
                x += tlm.mScrollArea.cellWidth;
            }
        }

        border = tlm.mBorders[TableLayoutManager.RIGHT].sizes;
        x = endBorderBound;
        for (Integer sz : border) {
            c.drawLine(x, 0, x, h, mPaint);
            x += sz;
        }
    }
}
