package com.embeddedlog.LightUpDroid.widget;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.embeddedlog.LightUpDroid.R;


public class ActionableToastBar extends LinearLayout {
    private boolean mHidden = false;
    private Animator mShowAnimation;
    private Animator mHideAnimation;
    private final int mBottomMarginSizeInConversation;


    private ImageView mActionDescriptionIcon;

    private View mActionButton;

    private View mActionIcon;

    private TextView mActionDescriptionView;

    private TextView mActionText;


    public ActionableToastBar(Context context) {
        this(context, null);
    }

    public ActionableToastBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ActionableToastBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mBottomMarginSizeInConversation = context.getResources().getDimensionPixelSize(
                R.dimen.toast_bar_bottom_margin_in_conversation);
        LayoutInflater.from(context).inflate(R.layout.actionable_toast_row, this, true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mActionDescriptionIcon = (ImageView) findViewById(R.id.description_icon);
        mActionDescriptionView = (TextView) findViewById(R.id.description_text);
        mActionButton = findViewById(R.id.action_button);
        mActionIcon = findViewById(R.id.action_icon);
        mActionText = (TextView) findViewById(R.id.action_text);
    }

    public void setConversationMode(boolean isInConversationMode) {
        final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
        params.bottomMargin = isInConversationMode ? mBottomMarginSizeInConversation : 0;
        setLayoutParams(params);
    }

    public void show(final ActionClickedListener listener, int descriptionIconResourceId,
            CharSequence descriptionText, boolean showActionIcon, int actionTextResource,
            boolean replaceVisibleToast) {

        if (!mHidden && !replaceVisibleToast) {
            return;
        }

        mActionButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View widget) {
                if (listener != null) {
                    listener.onActionClicked();
                }
                hide(true);
            }
        });


        if (descriptionIconResourceId == 0) {
            mActionDescriptionIcon.setVisibility(GONE);
        } else {
            mActionDescriptionIcon.setVisibility(VISIBLE);
            mActionDescriptionIcon.setImageResource(descriptionIconResourceId);
        }

        mActionDescriptionView.setText(descriptionText);
        mActionIcon.setVisibility(showActionIcon ? VISIBLE : GONE);
        mActionText.setText(actionTextResource);

        mHidden = false;
        getShowAnimation().start();
    }

    public void hide(boolean animate) {

        if (!mHidden && !getShowAnimation().isRunning()) {
            mHidden = true;
            if (getVisibility() == View.VISIBLE) {
                mActionDescriptionView.setText("");
                mActionButton.setOnClickListener(null);

                if (animate) {
                    getHideAnimation().start();
                } else {
                    setAlpha(0);
                    setVisibility(View.GONE);
                }
            }
        }
    }

    private Animator getShowAnimation() {
        if (mShowAnimation == null) {
            mShowAnimation = AnimatorInflater.loadAnimator(getContext(),
                    R.animator.fade_in);
            mShowAnimation.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    setVisibility(View.VISIBLE);
                }
                @Override
                public void onAnimationEnd(Animator animation) {

                    setVisibility(View.VISIBLE);
                }
                @Override
                public void onAnimationCancel(Animator animation) {
                }
                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
            mShowAnimation.setTarget(this);
        }
        return mShowAnimation;
    }

    private Animator getHideAnimation() {
        if (mHideAnimation == null) {
            mHideAnimation = AnimatorInflater.loadAnimator(getContext(),
                    R.animator.fade_out);
            mHideAnimation.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }
                @Override
                public void onAnimationRepeat(Animator animation) {
                }
                @Override
                public void onAnimationEnd(Animator animation) {
                    setVisibility(View.GONE);
                }
                @Override
                public void onAnimationCancel(Animator animation) {
                }
            });
            mHideAnimation.setTarget(this);
        }
        return mHideAnimation;
    }

    public boolean isEventInToastBar(MotionEvent event) {
        if (!isShown()) {
            return false;
        }
        int[] xy = new int[2];
        float x = event.getX();
        float y = event.getY();
        getLocationOnScreen(xy);
        return (x > xy[0] && x < (xy[0] + getWidth()) && y > xy[1] && y < xy[1] + getHeight());
    }

    public interface ActionClickedListener {
        public void onActionClicked();
    }
}
