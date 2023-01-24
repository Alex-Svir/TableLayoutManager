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
        int sz = mLineWidth / 2;
        outRect.set(sz,sz,sz,sz);
    }

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDrawOver(c, parent, state);
        RecyclerView.LayoutManager lm = parent.getLayoutManager();
        if (!(lm instanceof TableLayoutManager))
            return;
        TableLayoutManager tlm = (TableLayoutManager) lm;

        int w = parent.getWidth();
        int h = parent.getHeight();

        //  frame
        //  TODO    outer frame should be mLineWidth / 4, now it eats outer borders; inset parent RecyclerView by paddings?
        int d = mLineWidth / 2;
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
        while (y < endBorderBound) {
            c.drawLine(0, y, w, y, mPaint);
            y += tlm.mScrollArea.cellHeight;
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
        while (x < endBorderBound) {
            c.drawLine(x, 0, x, h, mPaint);
            x += tlm.mScrollArea.cellWidth;
        }

        border = tlm.mBorders[TableLayoutManager.RIGHT].sizes;
        x = endBorderBound;
        for (Integer sz : border) {
            c.drawLine(x, 0, x, h, mPaint);
            x += sz;
        }
    }
}
