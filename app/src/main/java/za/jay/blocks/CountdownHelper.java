package za.jay.blocks;

import android.os.Handler;
import android.os.SystemClock;

/**
 * Created by jamie on 2014/02/15.
 */
public class CountdownHelper {

    private static final long TICK_DELAY = 1000; // 1 sec

    private long mCountdown;
    private final Handler mHandler;

    private boolean mPlaying;
    private long mLastTickTime;

    private CountdownListener mListener;

    /** Create a countdown with a given length in milliseconds */
    public CountdownHelper(long length) {
        mCountdown = length;
        mHandler = new Handler();
    }

    /** Start or resume the countdown */
    public void play() {
        if (!mPlaying) {
            mLastTickTime = SystemClock.elapsedRealtime();
            mPlaying = true;
            mHandler.post(mTickRunnable);
        }
    }

    /** Pause the countdown if it was playing */
    public void pause() {
        if (mPlaying) {
            mHandler.removeCallbacks(mTickRunnable);
            mPlaying = false;
        }
    }

    /** Pause the countdown for a certain amount of time */
    public void pause(long duration) {
        if (mPlaying) {
            mHandler.removeCallbacks(mTickRunnable);
            mPlaying = false;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    play();
                }
            }, duration);
        }
    }

    /** Returns true if the countdown is started and unpaused */
    public boolean isPlaying() {
        return mPlaying;
    }

    /** Set the callback for the countdown */
    public void setCountdownListener(CountdownListener listener) {
        mListener = listener;
    }

    private void tick() {
        if (!mPlaying) {
            return;
        }

        // Calculate time since last update
        long tickTime = SystemClock.elapsedRealtime();
        long tickLength = tickTime - mLastTickTime;
        // Subtract that time from countdown
        mCountdown -= tickLength;
        // Clamp countdown to >= 0
        if (mCountdown < 0) {
            mCountdown = 0;
        }

        // Notify the listener of the change in countdown
        if (mListener != null) {
            mListener.onCountdownTick(mCountdown);
        }

        // Either post next update or we're finished
        if (mCountdown > 0) {
            mLastTickTime = tickTime;
            mHandler.postDelayed(mTickRunnable, TICK_DELAY);
        } else {
            mHandler.removeCallbacks(mTickRunnable);
            mPlaying = false;
        }
    }

    private final Runnable mTickRunnable = new Runnable() {
        @Override
        public void run() {
            tick();
        }
    };

    /** Callback interface for countdown update */
    public interface CountdownListener {
        /**
         * Callback for each update of the countdown timer
         * @param remaining the amount of time remaining in the countdown in milliseconds
         */
        void onCountdownTick(long remaining);
    }
}
