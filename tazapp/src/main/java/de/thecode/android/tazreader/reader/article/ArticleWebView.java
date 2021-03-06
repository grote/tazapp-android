package de.thecode.android.tazreader.reader.article;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.webkit.WebView;

import de.thecode.android.tazreader.data.TazSettings;

import timber.log.Timber;

public class ArticleWebView extends WebView {

    Context mContext;
    boolean isScroll;

    boolean mScrolling;

    int scrollCheckDelay = 100;

    public ArticleWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public ArticleWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ArticleWebView(Context context) {
        super(context);
        init(context);
    }

    @SuppressLint("NewApi")
    private void init(Context context) {
        mContext = context;
        setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    private void setIsScroll() {
        isScroll = TazSettings.getInstance(mContext)
                              .getPrefBoolean(TazSettings.PREFKEY.ISSCROLL, false);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        Timber.d("l: %s, t: %s, oldl: %s, oldt: %s", l, t, oldl, oldt);
        checkY = t;
        checkX = l;
        super.onScrollChanged(l, t, oldl, oldt);
        if (!isAlreadyChecking) {
            mScrolling = true;
            mCallback.onScrollStarted(this);
            isAlreadyChecking = true;
            this.postDelayed(scrollStopCheckerTask, scrollCheckDelay);
        }
    }

    public void smoothScrollToY(int y) {
        float density = getResources().getDisplayMetrics().density;
        ObjectAnimator scrollAnimation = ObjectAnimator.ofInt(this, "scrollY", (int) (Math.round(y * density)));
        scrollAnimation.setDuration(500);
        scrollAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        scrollAnimation.start();
    }

    boolean  isAlreadyChecking     = false;
    int      lastCheckedY          = 0;
    int      lastCheckedX          = 0;
    int      checkY                = 0;
    int      checkX                = 0;
    Runnable scrollStopCheckerTask = new Runnable() {

        @Override
        public void run() {
            if (checkY != lastCheckedY || checkX != lastCheckedX) {
                lastCheckedX = checkX;
                lastCheckedY = checkY;
                ArticleWebView.this.postDelayed(scrollStopCheckerTask, scrollCheckDelay);
            } else {
                mScrolling = false;
                if (mCallback != null) {
                    mCallback.onScrollFinished(ArticleWebView.this);
                }
                isAlreadyChecking = false;
            }

        }
    };

    public void loadUrl(String url) {
        Timber.i("url: %s", url);
        gestureDetector = new GestureDetector(mContext, simpleOnGestureListener);
        setIsScroll();
        super.loadUrl(url);
    }


    @Override
    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String failUrl) {
        Timber.i("baseUrl: %s, data: %s, mimeType: %s, encoding: %s, failUrl: %s", baseUrl, data, mimeType, encoding, failUrl);
        gestureDetector = new GestureDetector(mContext, simpleOnGestureListener);
        setIsScroll();
        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, failUrl);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!mScrolling && gestureDetector != null) {
            return gestureDetector.onTouchEvent(ev) || super.onTouchEvent(ev);
        } else return super.onTouchEvent(ev);

    }

    public int getContentWidth() {
        return computeHorizontalScrollRange();
    }


    private GestureDetector gestureDetector;

    private ArticleWebViewCallback mCallback;

    public void setArticleWebViewCallback(ArticleWebViewCallback listener) {
        mCallback = listener;
    }

    public interface ArticleWebViewCallback {

        public void onScrollStarted(ArticleWebView view);

        public void onScrollFinished(ArticleWebView view);

        public void onSwipeRight(ArticleWebView view, MotionEvent e1, MotionEvent e2);

        public void onSwipeLeft(ArticleWebView view, MotionEvent e1, MotionEvent e2);

        public void onSwipeBottom(ArticleWebView view, MotionEvent e1, MotionEvent e2);

        public void onSwipeTop(ArticleWebView view, MotionEvent e1, MotionEvent e2);

        public boolean onDoubleTap(MotionEvent e);
    }

    GestureDetector.SimpleOnGestureListener simpleOnGestureListener = new SimpleOnGestureListener() {

        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (mCallback != null) return mCallback.onDoubleTap(e);
            return super.onDoubleTap(e);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Timber.d("e1: %s, e2: %s, velocityX: %s, velocityY: %s", e1, e2, velocityX, velocityY);
            boolean result = false;

            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            if (mCallback != null) mCallback.onSwipeRight(ArticleWebView.this, e1, e2);
                        } else {
                            if (mCallback != null) mCallback.onSwipeLeft(ArticleWebView.this, e1, e2);
                        }
                    }
                    result = true;
                } else if (!isScroll && Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        if (mCallback != null) mCallback.onSwipeBottom(ArticleWebView.this, e1, e2);
                    } else {
                        if (mCallback != null) mCallback.onSwipeTop(ArticleWebView.this, e1, e2);
                    }
                    result = true;
                }

            } catch (Exception exception) {
                exception.printStackTrace();
            }

            return result;
        }
    };
}
