package za.jay.blocks;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

/**
 * A BlockView is a very simple View with a color value. That color is drawn as a rectangle centered
 * in the view that is either a 1/4 of the size of the View (in "unselected" mode) or fits the
 * entire view (in "selected" mode).
 */
public class BlockView extends View {

    private Paint mPaint;
    private Rect mDrawingRect;
    private Rect mInnerRect;

    private boolean mSelected;

    public BlockView(Context context) {
        super(context);
        init();
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);
        mDrawingRect = new Rect();
        mInnerRect = new Rect();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mDrawingRect.set(0, 0, w, h);
        resizeInnerRect(w, h);
    }

    private void resizeInnerRect(int w, int h) {
        int quarterW = (int) (w / 4.0f);
        int quarterH = (int) (h / 4.0f);
        mInnerRect.set(quarterW, quarterH, w - quarterW, h - quarterH);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mSelected) {
            canvas.drawRect(mDrawingRect, mPaint);
        } else {
            canvas.drawRect(mInnerRect, mPaint);
        }
    }

    public void setColor(int color) {
        mPaint.setColor(color);
        postInvalidate();
    }

    public int getColor() {
        return mPaint.getColor();
    }

    public void setSelected(boolean selected) {
        if (selected != mSelected) {
            mSelected = selected;
            postInvalidate();
        }
    }

    public boolean isSelected() {
        return mSelected;
    }

}
