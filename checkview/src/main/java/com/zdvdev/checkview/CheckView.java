package com.zdvdev.checkview;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static android.util.TypedValue.applyDimension;

public class CheckView extends View {
    public static final int FLAG_STATE_PLUS = 0;
    public static final int FLAG_STATE_CHECK = 1;

    private static final long ANIMATION_DURATION_MS = 300L;

    private static final int DEFAULT_STROKE_WIDTH_DP = 4;
    private static final int DEFAULT_PADDING_DP = 12;
    private static final int DEFAULT_COLOR = Color.BLACK;

    // Arcs that define the set of all points between which the two lines are drawn
    // Names (top, bottom, etc) are from the reference point of the "plus" configuration.
    private Path firstPath;
    private Path secondPath;
    private Path thirdPath;
    private Path fourPath;

    // Pre-compute arc lengths when layout changes
    private float firstPathLength;
    private float secondPathLength;
    private float thirdPathLength;
    private float fourPathLength;

    private Paint paint;
    private int color = DEFAULT_COLOR;
    private float strokeWidth = DEFAULT_STROKE_WIDTH_DP;
    private PathMeasure pathMeasure;

    private float[] fromXY;
    private float[] toXY;

    /**
     * Internal state flag for the drawn appearance, plus or check.
     * The default starting position is "plus". This represents the real configuration, whereas
     * {@code percent} holds the frame-by-frame position when animating between
     * the states.
     */
    private int state = FLAG_STATE_PLUS;

    /**
     * The percent value upon the arcs that line endpoints should be found
     * when drawing.
     */
    private float percent = 1f;
    private boolean paddingDefined;
    OnClickListener onClickListener;
    boolean autoToggleEnabled;
    int padding;

    public CheckView(Context context) {
        super(context);
    }

    public CheckView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CheckView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        int[] attributes = new int[]{android.R.attr.padding, android.R.attr.paddingLeft, android.R.attr.paddingTop,
              android.R.attr.paddingBottom, android.R.attr.paddingRight};

        TypedArray arr = context.obtainStyledAttributes(attrs, attributes);
        for (int i = 0, length = attributes.length; i < length; i++) {
            paddingDefined |= arr.hasValue(i);
        }

        if (!paddingDefined) {
            int padding = (int) applyDimension(COMPLEX_UNIT_DIP, DEFAULT_PADDING_DP, getResources().getDisplayMetrics());
            setPadding(padding, padding, padding, padding);
        }
        arr.recycle();

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CheckView, 0, 0);
        color = a.getColor(R.styleable.CheckView_cvlineColor, DEFAULT_COLOR);
        strokeWidth = a.getDimension(R.styleable.CheckView_cvstrokeWidth, -1);
        if (strokeWidth == -1) {
            strokeWidth = applyDimension(COMPLEX_UNIT_DIP, DEFAULT_STROKE_WIDTH_DP, getResources().getDisplayMetrics());
        }
        autoToggleEnabled = a.getBoolean(R.styleable.CheckView_cvautoToggle, true);

        super.setOnClickListener(onClickListenerDelegate);
        a.recycle();
    }

    public void setAutoToggle(boolean enable) {
        autoToggleEnabled = enable;
    }

    final OnClickListener onClickListenerDelegate = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (autoToggleEnabled) toggle();
            if (onClickListener != null) onClickListener.onClick(v);
        }
    };

    @Override
    public void setOnClickListener(OnClickListener l) {
        onClickListener = l;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.translate(padding, padding);

        //TODO this could be moved as a class attribute
        float percentFromState = state == FLAG_STATE_PLUS ? percent : 1 - percent;

        setPointFromPercent(firstPath, firstPathLength, percentFromState, fromXY);
        setPointFromPercent(secondPath, secondPathLength, percentFromState, toXY);

        canvas.drawLine(fromXY[0], fromXY[1], toXY[0], toXY[1], paint);

        setPointFromPercent(thirdPath, thirdPathLength, percentFromState, fromXY);
        setPointFromPercent(fourPath, fourPathLength, percentFromState, toXY);

        canvas.drawLine(fromXY[0], fromXY[1], toXY[0], toXY[1], paint);

        canvas.restore();
    }

    /**
     * Given some path and its length, find the point ([x,y]) on that path at
     * the given percentage of length. Store the result in {@code points}.
     */
    private void setPointFromPercent(Path path, float length, float percent, float[] points) {
        pathMeasure.setPath(path, false);
        pathMeasure.getPosTan(length * percent, points, null);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed) {
            measurePaths();
            invalidate();
        }
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
        measurePaths();
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void setPaddingRelative(int start, int top, int end, int bottom) {
        super.setPaddingRelative(start, top, end, bottom);
        measurePaths();
    }


    /**
     * Perform measurements and pre-calculations. This should be called any time
     * the view measurements or visuals are changed, such as with a call to {@link #setPadding(int, int, int, int)}
     * or an operating system callback like {@link #onLayout(boolean, int, int, int, int)}.
     */
    private void measurePaths() {
        int maxSize;
        float middle;

        maxSize = Math.min(getWidth(), getHeight());
        padding = Math.max(
              Math.max(getPaddingBottom(), getPaddingTop()),
              Math.max(getPaddingRight(), getPaddingLeft()));
        maxSize -= padding * 2;
        middle = maxSize / 2f;

        pathMeasure = new PathMeasure();

        PointF p1a = new PointF(middle, 0);
        PointF p1b = getCheckRightPoint(maxSize);

        firstPath = new Path();
        firstPath.moveTo(p1a.x, p1a.y);
        firstPath.lineTo(p1b.x, p1b.y);
        pathMeasure.setPath(firstPath, false);
        firstPathLength = pathMeasure.getLength();

        PointF p2a = new PointF(middle, maxSize);
        PointF p2b = getCheckMiddlePoint(maxSize);

        secondPath = new Path();
        secondPath.moveTo(p2a.x, p2a.y);
        secondPath.lineTo(p2b.x, p2b.y);
        pathMeasure.setPath(secondPath, false);
        secondPathLength = pathMeasure.getLength();

        PointF p3a = new PointF(0, middle);
        PointF p3b = getCheckLeftPoint(maxSize);

        thirdPath = new Path();
        thirdPath.moveTo(p3a.x, p3a.y);
        thirdPath.lineTo(p3b.x, p3b.y);
        pathMeasure.setPath(thirdPath, false);
        thirdPathLength = pathMeasure.getLength();

        PointF p4a = new PointF(maxSize, middle);

        fourPath = new Path();
        fourPath.moveTo(p4a.x, p4a.y);
        fourPath.lineTo(p2b.x, p2b.y);
        pathMeasure.setPath(fourPath, false);
        fourPathLength = pathMeasure.getLength();

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.SQUARE);
        paint.setStrokeWidth(strokeWidth);

        fromXY = new float[]{0f, 0f};
        toXY = new float[]{0f, 0f};
    }

    private PointF getCheckLeftPoint(int maxSize) {
        return new PointF(1, maxSize / 2);
    }

    private PointF getCheckMiddlePoint(int maxSize) {
        return new PointF(5 * maxSize /  16f, 13 * maxSize / 16f);
    }

    private PointF getCheckRightPoint(int maxSize) {
        return new PointF(maxSize, maxSize / 8f);
    }

    public void setColor(int argb) {
        color = argb;
        if (paint == null) {
            paint = new Paint();
        }
        paint.setColor(argb);
        invalidate();
    }

    /**
     * Transition to check status
     */
    public void check() {
        check(ANIMATION_DURATION_MS);
    }

    /**
     * Transition to check status over the given animation duration
     */
    public void check(long animationDurationMS) {
        if (state == FLAG_STATE_CHECK) {
            return;
        }
        toggle(animationDurationMS);
    }

    /**
     * Transition to "+"
     */
    public void plus() {
        plus(ANIMATION_DURATION_MS);
    }

    /**
     * Transition to "+" over the given animation duration
     */
    public void plus(long animationDurationMS) {
        if (state == FLAG_STATE_PLUS) {
            return;
        }
        toggle(animationDurationMS);
    }

    /**
     * Tell this view to switch states from check to plus, or back, using the default animation duration.
     *
     * @return an integer flag that represents the new state after toggling.
     * This will be either {@link #FLAG_STATE_PLUS} or {@link #FLAG_STATE_CHECK}
     */
    public int toggle() {
        return toggle(ANIMATION_DURATION_MS);
    }

    /**
     * Tell this view to switch states from check to plus, or back.
     *
     * @param animationDurationMS duration in milliseconds for the toggle animation
     * @return an integer flag that represents the new state after toggling.
     * This will be either {@link #FLAG_STATE_PLUS} or {@link #FLAG_STATE_CHECK}
     */
    public int toggle(long animationDurationMS) {
        state = state == FLAG_STATE_PLUS ? FLAG_STATE_CHECK : FLAG_STATE_PLUS;
        // invert percent, because state was just flipped
        percent = 1 - percent;
        ValueAnimator animator = ValueAnimator.ofFloat(percent, 1);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(animationDurationMS);
        animator.addUpdateListener(animationListener);

        animator.start();
        return state;
    }

    private final AnimatorUpdateListener animationListener = new AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            setPercent(animation.getAnimatedFraction());
        }
    };

    void setPercent(float percent) {
        this.percent = percent;
        invalidate();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable parcelable = super.onSaveInstanceState();
        if (parcelable == null) {
            parcelable = new Bundle();
        }

        CheckViewState savedState = new CheckViewState(parcelable);
        savedState.flagState = state;
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof CheckViewState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        CheckViewState ss = (CheckViewState) state;
        this.state = ss.flagState;
        if (this.state != FLAG_STATE_PLUS && this.state != FLAG_STATE_CHECK) {
            this.state = FLAG_STATE_PLUS;
        }

        super.onRestoreInstanceState(ss.getSuperState());
    }

    static class CheckViewState extends BaseSavedState {
        int flagState;

        CheckViewState(Parcelable superState) {
            super(superState);
        }

        CheckViewState(Parcel in) {
            super(in);
            flagState = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(flagState);
        }

        public static final Creator<CheckViewState> CREATOR = new Creator<CheckViewState>() {
            @Override
            public CheckViewState createFromParcel(Parcel in) {
                return new CheckViewState(in);
            }

            @Override
            public CheckViewState[] newArray(int size) {
                return new CheckViewState[size];
            }
        };
    }
}