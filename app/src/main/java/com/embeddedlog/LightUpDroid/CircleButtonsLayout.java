package com.embeddedlog.LightUpDroid;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

public class CircleButtonsLayout extends FrameLayout {
    private Context mContext;
    private int mCircleTimerViewId;
    private int mLeftButtonId;
    private int mRightButtonId;
    private int mStopButtonId;
    private int mLabelId;
    private int mLabelTextId;
    private float mLeftButtonPadding;
    private float mRightButtonPadding;
    private float mStrokeSize;
    private float mDiamOffset;
    private CircleTimerView mCtv;
    private ImageButton mLeft, mRight;
    private TextView mStop;
    private FrameLayout mLabel;
    private TextView mLabelText;

    public CircleButtonsLayout(Context context) {
        this(context, null);
        mContext = context;
    }

    public CircleButtonsLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public void setCircleTimerViewIds(int circleTimerViewId, int leftButtonId, int rightButtonId,
            int stopButtonId, int leftButtonPaddingDimenId, int rightButtonPaddingDimenId,
            int labelId, int labelTextId) {
        mCircleTimerViewId = circleTimerViewId;
        mLeftButtonId = leftButtonId;
        mRightButtonId = rightButtonId;
        mStopButtonId = stopButtonId;
        mLabelId = labelId;
        mLabelTextId = labelTextId;
        mLeftButtonPadding = mContext.getResources().getDimension(leftButtonPaddingDimenId);
        mRightButtonPadding = mContext.getResources().getDimension(rightButtonPaddingDimenId);

        float dotStrokeSize = mContext.getResources().getDimension(R.dimen.circletimer_dot_size);
        float markerStrokeSize =
                mContext.getResources().getDimension(R.dimen.circletimer_marker_size);
        mStrokeSize = mContext.getResources().getDimension(R.dimen.circletimer_circle_size);
        mDiamOffset = Utils.calculateRadiusOffset(mStrokeSize, dotStrokeSize, markerStrokeSize) * 2;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        remeasureViews();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    protected void remeasureViews() {
        if (mCtv == null) {
            mCtv = (CircleTimerView) findViewById(mCircleTimerViewId);
            if (mCtv == null) {
                return;
            }
            mLeft = (ImageButton) findViewById(mLeftButtonId);
            mRight = (ImageButton) findViewById(mRightButtonId);
            mStop = (TextView) findViewById(mStopButtonId);
            mLabel = (FrameLayout) findViewById(mLabelId);
            mLabelText = (TextView) findViewById(mLabelTextId);
        }

        int frameWidth = mCtv.getMeasuredWidth();
        int frameHeight = mCtv.getMeasuredHeight();
        int minBound = Math.min(frameWidth, frameHeight);
        int circleDiam = (int) (minBound - mDiamOffset);

        MarginLayoutParams stopParams = (MarginLayoutParams) mStop.getLayoutParams();
        stopParams.bottomMargin = circleDiam/6;
        if (minBound == frameWidth) {
            stopParams.bottomMargin += (frameHeight-frameWidth)/2;
        }

        if (mLabel != null) {
            MarginLayoutParams labelParams = (MarginLayoutParams) mLabel.getLayoutParams();
            labelParams.topMargin = circleDiam/6;
            if (minBound == frameWidth) {
                labelParams.topMargin += (frameHeight-frameWidth)/2;
            }

            int r = circleDiam / 2;

            int y = frameHeight / 2 - labelParams.topMargin;
            double w = 2 * Math.sqrt((r + y) * (r - y));

            mLabelText.setMaxWidth((int) w);
        }

        int sideMarginOffset = (int) ((frameWidth - circleDiam - mStrokeSize) / 2)
                - (int) mContext.getResources().getDimension(R.dimen.timer_button_extra_offset);
        int leftMarginOffset = Math.max(0, sideMarginOffset - (int) mLeftButtonPadding);
        int rightMarginOffset = Math.max(0, sideMarginOffset - (int) mRightButtonPadding);
        int bottomMarginOffset = (frameHeight - minBound) / 2;
        MarginLayoutParams leftParams = (MarginLayoutParams) mLeft.getLayoutParams();
        leftParams.leftMargin = leftMarginOffset;
        leftParams.bottomMargin = bottomMarginOffset;
        MarginLayoutParams rightParams = (MarginLayoutParams) mRight.getLayoutParams();
        rightParams.rightMargin = rightMarginOffset;
        rightParams.bottomMargin = bottomMarginOffset;
    }
}
