package za.jay.blocks;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

/**
 * A BlockView is a very simple View with a color value. That color is drawn as a rectangle centered
 * in the view that is either a 1/4 of the size of the View (in "unselected" mode) or fits the
 * entire view (in "selected" mode).
 */
public class BlockView extends View {

    private Paint mPaint;
    private Rect mDrawingRect;
    private RectF mInnerRect;

    private boolean mSelected;

    public BlockView(Context context) {
        super(context);
        init();
    }

    private void init() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        mDrawingRect = new Rect();
        mInnerRect = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mDrawingRect.set(0, 0, w, h);
        resizeInnerRect(w, h);
    }

    private void resizeInnerRect(int w, int h) {
        int centerX = (int) (w / 2.0f);
        int centerY = (int) (h / 2.0f);
        int fifthW = Math.round(w / 5.0f);
        int fifthH = Math.round(h / 5.0f);
        mInnerRect.set(centerX - fifthW, centerY - fifthH, centerX + fifthW, centerY + fifthH);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mSelected) {
            canvas.drawRect(mDrawingRect, mPaint);
        } else {
            canvas.drawOval(mInnerRect, mPaint);
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
