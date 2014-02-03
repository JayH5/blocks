package za.jay.blocks;

import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class GameFragment extends Fragment {

    private static final String TAG = "GameFragment";

    public static final String ARG_MODE = "extra_mode";
    public static final int MODE_TIMED = 1;
    public static final int MODE_MOVES = 2;
    public static final int MODE_ENDLESS = 3;

    private static final int WIDTH = 6;
    private static final int HEIGHT = 6;

    private int mMode;

    private GridModel mModel;
    private GridLayout mGrid;

    private int mScore;
    private int mMoves;

    private TextView mRemainingCount;
    private TextView mScoreCount;

    private List<Integer> mPath;
    private Rect mLastHitRect;
    private boolean mSquare;

    private ObjectAnimator mCountdownAnimator;

    public static GameFragment newInstance(int mode) {
        Bundle args = new Bundle();
        args.putInt(ARG_MODE, mode);

        GameFragment frag = new GameFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMode = getArguments().getInt(ARG_MODE);
        mModel = new GridModel(WIDTH, HEIGHT, getResources().getIntArray(R.array.block_colours));
        mModel.setGridChangeListener(mGridChangeListener);

        if (mMode == MODE_TIMED) {
            mCountdownAnimator = (ObjectAnimator)
                    AnimatorInflater.loadAnimator(getActivity(), R.animator.countdown);
            mCountdownAnimator.setTarget(this);
        }

        mPath = new ArrayList<Integer>();
        mLastHitRect = new Rect();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_game, container, false);

        initGridView(root);
        initStatsViews(root);

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mMode == MODE_TIMED) {
            mCountdownAnimator.start();
        }
    }

    /** Set up the playing area. */
    private void initGridView(View root) {
        mGrid = (GridLayout) root.findViewById(R.id.game_grid);
        mGrid.setOnTouchListener(mBlockTouchListener);

        Resources res = getResources();
        int gridWidth = res.getDisplayMetrics().widthPixels
                - res.getDimensionPixelSize(R.dimen.activity_horizontal_margin) * 2;
        int cellSize = (int) (gridWidth / 6.0f);
        Context context = getActivity();
        for (int i = 0; i < WIDTH * HEIGHT; i++) {
            // Create the block, assign a colour
            BlockView block = new BlockView(context);
            block.setColor(mModel.getBlock(i));

            // Set the grid layout params
            GridLayout.Spec rowSpec = GridLayout.spec(i / WIDTH);
            GridLayout.Spec colSpec = GridLayout.spec(i % WIDTH);
            GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(rowSpec, colSpec);
            layoutParams.width = cellSize;
            layoutParams.height = cellSize;

            // Add the block to the grid
            mGrid.addView(block, layoutParams);
        }
    }

    /** Set up the moves/time/score counters depending on the game mode */
    private void initStatsViews(View root) {
        mRemainingCount = (TextView) root.findViewById(R.id.game_remaining_counter);
        mScoreCount = (TextView) root.findViewById(R.id.game_score_counter);

        TextView remainingText = (TextView) root.findViewById(R.id.game_remaining_text);
        switch(mMode) {
            case MODE_MOVES:
                remainingText.setText(R.string.game_moves_left);
                mRemainingCount.setText(R.string.thirty);
                break;
            case MODE_TIMED:
                remainingText.setText(R.string.game_time);
                mRemainingCount.setText(R.string.sixty);
                break;
            case MODE_ENDLESS:
                remainingText.setText(R.string.game_moves);
                mRemainingCount.setText(R.string.zero);
                break;
        }
    }

    /** Add {@code points} to the score and update UI accordingly */
    private void increaseScore(int points) {
        mScore += points;
        mScoreCount.setText(String.valueOf(mScore));
    }

    /** Updates countdown value if in timed mode. Called by countdown animator. */
    public void setCountdown(int count) {
        if (count > 0) {
            mRemainingCount.setText(String.valueOf(count));
        } else {
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        }
    }

    /** Increment the moves count by 1. Update UI accordingly. */
    private void incrementMoves() {
        mMoves++;
        if (mMode == MODE_MOVES) {
            int remaining = 30 - mMoves;
            if (remaining > 0) {
                mRemainingCount.setText(String.valueOf(remaining));
            } else {
                getActivity().finish();
            }
        } else if (mMode == MODE_ENDLESS) {
            mRemainingCount.setText(String.valueOf(mMoves));
        }
    }

    private void beginPath(float x, float y) {
        int index = calculateGridIndex(x, y);
        if (index >= 0 && index < mGrid.getChildCount()) {
            BlockView block = (BlockView) mGrid.getChildAt(index);
            block.setSelected(true);
            mPath.add(index);
        }
    }

    private void movePath(float x, float y) {
        // Check if we're still over the same rectangle. If we still are, nothing to do
        if (mLastHitRect.contains((int) x, (int) y)) {
            return;
        }

        // Get the index in the ViewGroup and check that it's valid
        int index = calculateGridIndex(x, y);
        if (index < 0 || index >= mGrid.getChildCount()) {
            return;
        }

        // Get the actual block view and save its hit rect
        BlockView block = (BlockView) mGrid.getChildAt(index);
        block.getHitRect(mLastHitRect);

        // Check if already selected
        int pathSize = mPath.size();
        int lastIndex = mPath.get(pathSize - 1);
        BlockView lastBlock = (BlockView) mGrid.getChildAt(lastIndex);
        if (!block.isSelected()) {
            // If not, and new block is valid link, add to path
            if (block.getColor() == lastBlock.getColor() && isAdjacent(index, lastIndex)) {
                block.setSelected(true);
                mPath.add(index);
            }
        } else if (pathSize >= 2 && block != lastBlock) {
            // If so, either backtracking or making a closed path
            if (index == mPath.get(pathSize - 2)) {
                // Backtracking: deselect the last block and remove from path
                lastBlock.setSelected(false);
                mPath.remove(pathSize - 1);
            } else if (block.getColor() == lastBlock.getColor() && isAdjacent(index, lastIndex)) {
                mSquare = true;
            }
        }
    }

    /** Check if two indices are adjacent in the grid */
    private static boolean isAdjacent(int index1, int index2) {
        if (index1 > index2) {
            int temp = index1;
            index1 = index2;
            index2 = temp;
        }

        return index1 == index2 - 1 && index2 % WIDTH != 0
                || index1 == index2 - WIDTH;
    }

    /** Find the index of the child view in the block given a touch position */
    private int calculateGridIndex(float x, float y) {
        int col = (int) (WIDTH * x / mGrid.getWidth());
        int row = (int) (HEIGHT * y / mGrid.getHeight());
        return col + WIDTH * row;
    }

    private void endPath(float x, float y) {
        if (!mSquare) {
            int pathSize = mPath.size();
            if (pathSize >= 2) {
                // Have a path, remove the blocks
                removePath();
                incrementMoves();
            } else if (pathSize == 1) {
                resetPath();
            }
        } else {
            // Have a closed path, remove all blocks of same color
            removeColor();
            incrementMoves();
            mSquare = false;
        }
    }

    /** Remove all blocks in the path from the grid */
    private void removePath() {
        int removals = mModel.removePath(mPath);
        increaseScore(removals);
        resetPath();
    }

    /** Remove all blocks that are the same colour as blocks in the path */
    private void removeColor() {
        BlockView block = (BlockView) mGrid.getChildAt(mPath.get(0));
        int removals = mModel.removeColor(block.getColor());
        increaseScore(removals);
        resetPath();
    }

    /** Set all blocks in path back to unselected state, clear the path */
    private void resetPath() {
        for (Integer index : mPath) {
            BlockView block = (BlockView) mGrid.getChildAt(index);
            block.setSelected(false);
        }
        mPath.clear();
    }

    private final View.OnTouchListener mBlockTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                beginPath(event.getX(), event.getY());
            } else if (action == MotionEvent.ACTION_MOVE) {
                movePath(event.getX(), event.getY());
            } else if (action == MotionEvent.ACTION_UP) {
                endPath(event.getX(), event.getY());
            }

            return true;
        }
    };

    private final GridModel.GridChangeListener mGridChangeListener =
            new GridModel.GridChangeListener() {
        @Override
        public void onBlockChange(int index, int newColor) {
            BlockView block = (BlockView) mGrid.getChildAt(index);
            block.setColor(newColor);
        }
    };

}
