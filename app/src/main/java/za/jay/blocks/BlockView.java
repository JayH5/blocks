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

    public static enum PathDirection {
        LEFT, RIGHT, UP, DOWN
    }

    private Paint mPaint;
    private RectF mInnerRect;

    private Rect mSrcPathRect;
    private Rect mDestPathRect;

    private boolean mSelected;

    public BlockView(Context context) {
        super(context);
        init();
    }

    private void init() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        mInnerRect = new RectF();
        mSrcPathRect = new Rect();
        mDestPathRect = new Rect();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
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
        canvas.drawOval(mInnerRect, mPaint);
        if (mSelected) {
            canvas.drawRect(mSrcPathRect, mPaint);
            canvas.drawRect(mDestPathRect, mPaint);
        }
    }

    public void setColor(int color) {
        mPaint.setColor(color);
        postInvalidate();
    }

    public int getColor() {
        return mPaint.getColor();
    }

    /** Select the dot with a source incoming path (may be null) */
    public void select(PathDirection src) {
        if (!mSelected) {
            shapePathRectangle(src, mSrcPathRect);
            mSelected = true;
            postInvalidate();
        }
    }

    /** Deselect the dot, removing any paths */
    public void deselect() {
        if (mSelected) {
            mSrcPathRect.setEmpty();
            mDestPathRect.setEmpty();
            mSelected = false;
            postInvalidate();
        }
    }

    /** Add a link to the next selected dot */
    public void connectNext(PathDirection dest) {
        if (mSelected) {
            shapePathRectangle(dest, mDestPathRect);
            postInvalidate();
        }
    }

    /** Remove the link to the next dot */
    public void disconnectNext() {
        if (mSelected) {
            mDestPathRect.setEmpty();
            postInvalidate();
        }
    }

    /** Adjust 'rect' so that it is the path going in direction 'dir' */
    private void shapePathRectangle(PathDirection dir, Rect rect) {
        if (dir == null) {
            return;
        }

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int centerX = (int) (width / 2.0f);
        int centerY = (int) (height / 2.0f);

        int halfPathWidth;
        switch (dir) {
            case LEFT:
                halfPathWidth = (int) (height / 20.0f);
                rect.set(0, centerY - halfPathWidth, centerX, centerY + halfPathWidth);
                break;
            case RIGHT:
                halfPathWidth = (int) (height / 20.0f);
                rect.set(centerX, centerY - halfPathWidth, width, centerY + halfPathWidth);
                break;
            case UP:
                halfPathWidth = (int) (width / 20.0f);
                rect.set(centerX - halfPathWidth, 0, centerX + halfPathWidth, centerY);
                break;
            case DOWN:
                halfPathWidth = (int) (width / 20.0f);
                rect.set(centerX - halfPathWidth, centerY, centerX + halfPathWidth, height);
                break;
        }
    }

    public boolean isSelected() {
        return mSelected;
    }

}
