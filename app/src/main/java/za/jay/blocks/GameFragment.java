package za.jay.blocks;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class GameFragment extends Fragment implements View.OnTouchListener,
        GridModel.GridChangeListener {

    private static final String TAG = "GameFragment";

    private static final String ARG_MODE = "extra_mode";

    private static final int WIDTH = 6;
    private static final int HEIGHT = 6;

    private GameMode mGameMode;

    private GridModel mModel;
    private GridLayout mGrid;

    private int mScore;
    private int mMoves;

    private TextView mRemainingCount;
    private TextView mScoreCount;

    private List<Integer> mPath;
    private Rect mLastHitRect;
    private boolean mSquare;

    private CountdownHelper mCountdown;

    private GestureDetector mGestureDetector;

    private Button mPowerup1Button;
    private Button mPowerup2Button;
    private Button mPowerup3Button;
    private TextView mPowerupHint1;
    private TextView mPowerupHint2;
    private PowerUp mActivePowerUp;

    enum PowerUp {
        MORE_MOVES, SHRINKERS, EXPANDERS
    }

    public static GameFragment newInstance(GameMode mode) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_MODE, mode);

        GameFragment frag = new GameFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGameMode = (GameMode) getArguments().getSerializable(ARG_MODE);
        mModel = new GridModel(WIDTH, HEIGHT, getResources().getIntArray(R.array.block_colours));
        mModel.setGridChangeListener(this);

        if (mGameMode == GameMode.TIMED) {
            mCountdown = new CountdownHelper(60 * 1000); // 60 seconds
            mCountdown.setCountdownListener(new CountdownHelper.CountdownListener() {
                @Override
                public void onCountdownTick(long remaining) {
                    if (remaining == 0) {
                        endGame();
                    }
                    mRemainingCount.setText(String.valueOf(remaining / 1000));
                }
            });
        }

        mPath = new ArrayList<Integer>();
        mLastHitRect = new Rect();

        mGestureDetector = new GestureDetector(getActivity(),
                new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                int index = calculateGridIndex(e.getX(), e.getY());
                mModel.removeBlock(index);
                increaseScore(1);
                incrementMoves();
                return true;
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_game, container, false);

        initGridView(root);
        initStatsViews(root);
        initPowerups(root);

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mGameMode == GameMode.TIMED) {
            mCountdown.play();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGameMode == GameMode.TIMED) {
            mCountdown.pause();
        }
    }

    /** Set up the playing area. */
    private void initGridView(View root) {
        mGrid = (GridLayout) root.findViewById(R.id.game_grid);
        mGrid.setOnTouchListener(this);

        Resources res = getResources();
        int gridWidth = res.getDisplayMetrics().widthPixels
                - res.getDimensionPixelSize(R.dimen.grid_horizontal_margin) * 2;
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
        Typeface josefinSans =
                Typeface.createFromAsset(getActivity().getAssets(), "fonts/JosefinSans-Regular.ttf");

        mRemainingCount = (TextView) root.findViewById(R.id.game_remaining_counter);
        mRemainingCount.setTypeface(josefinSans);
        mScoreCount = (TextView) root.findViewById(R.id.game_score_counter);
        mScoreCount.setTypeface(josefinSans);

        TextView remainingText = (TextView) root.findViewById(R.id.game_remaining_text);
        remainingText.setTypeface(josefinSans);
        switch(mGameMode) {
            case MOVES:
                remainingText.setText(R.string.game_moves_left);
                mRemainingCount.setText(R.string.thirty);
                break;
            case TIMED:
                remainingText.setText(R.string.game_time);
                mRemainingCount.setText(R.string.sixty);
                break;
            case ENDLESS:
                remainingText.setText(R.string.game_moves);
                mRemainingCount.setText(R.string.zero);
                break;
        }

        TextView scoreText = (TextView) root.findViewById(R.id.game_score_text);
        scoreText.setTypeface(josefinSans);
    }

    private void initPowerups(View root) {
        mPowerup1Button = (Button) root.findViewById(R.id.btn_more_moves);
        if (mGameMode == GameMode.TIMED) {
            mPowerup1Button.setText(R.string.time_stop);
        } else if (mGameMode == GameMode.ENDLESS) {
            mPowerup1Button.setVisibility(View.GONE);
        }

        mPowerup2Button = (Button) root.findViewById(R.id.btn_shrinkers);
        mPowerup3Button = (Button) root.findViewById(R.id.btn_expanders);

        mPowerup1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPowerupDialog(PowerUp.MORE_MOVES);
            }
        });

        mPowerup2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPowerupDialog(PowerUp.SHRINKERS);
            }
        });

        mPowerup3Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPowerupDialog(PowerUp.EXPANDERS);
            }
        });

        mPowerupHint1 = (TextView) root.findViewById(R.id.powerup_hint1);
        mPowerupHint2 = (TextView) root.findViewById(R.id.powerup_hint2);
    }

    private void showPowerupDialog(PowerUp powerUp) {
        int title;
        int message;
        final Runnable action;
        switch (powerUp) {
            case MORE_MOVES:
                title = mGameMode == GameMode.TIMED ? R.string.time_stop : R.string.more_moves;
                message = mGameMode == GameMode.TIMED ?
                        R.string.time_stop_message : R.string.more_moves_message;
                action = new Runnable() {
                    @Override
                    public void run() {
                        toggleMoreMoves();
                    }
                };
                break;
            case SHRINKERS:
                title = R.string.shrinker;
                message = R.string.shrinker_message;
                action = new Runnable() {
                    @Override
                    public void run() {
                        toggleShrinkers();
                    }
                };
                break;
            case EXPANDERS:
                title = R.string.expander;
                message = R.string.expander_message;
                action = new Runnable() {
                    @Override
                    public void run() {
                        toggleExpanders();
                    }
                };
                break;
            default: // Stupid, stupid, stupid Java switches
                title = 0;
                message = 0;
                action = null;
                break;
        }

        /*AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        action.run();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        resumeCountdown();
                    }
                });
        pauseCountdown();
        builder.show();*/
        action.run();
    }

    private void toggleMoreMoves() {
        if (mGameMode == GameMode.TIMED) {
            mCountdown.pause(5000);
        } else {
            mMoves -= 5;
            mRemainingCount.setText(String.valueOf(30 - mMoves));
        }
        mPowerup1Button.setEnabled(false); // Disable button
    }

    private void toggleShrinkers() {
        if (mActivePowerUp == PowerUp.EXPANDERS) {
            toggleExpanders();
        }

        if (mActivePowerUp != PowerUp.SHRINKERS) { // Activate
            mPowerup2Button.setTypeface(null, Typeface.BOLD);

            mPowerupHint1.setText(R.string.shrinker_hint1);
            mPowerupHint2.setText(R.string.shrinker_hint2);

            mPowerupHint1.setVisibility(View.VISIBLE);
            mPowerupHint2.setVisibility(View.VISIBLE);

            mActivePowerUp = PowerUp.SHRINKERS;
        } else { // Deactivate
            mPowerup2Button.setTypeface(null, Typeface.NORMAL);

            mPowerupHint1.setVisibility(View.INVISIBLE);
            mPowerupHint2.setVisibility(View.INVISIBLE);
            mActivePowerUp = null;
        }
    }

    private void toggleExpanders() {
        if (mActivePowerUp == PowerUp.SHRINKERS) {
            toggleShrinkers();
        }

        if (mActivePowerUp != PowerUp.EXPANDERS) { // Activate
            mPowerup3Button.setTypeface(null, Typeface.BOLD);

            mPowerupHint1.setText(R.string.expander_hint1);
            mPowerupHint1.setVisibility(View.VISIBLE);

            mActivePowerUp = PowerUp.EXPANDERS;
        } else { // Deactivate
            mPowerup3Button.setTypeface(null, Typeface.NORMAL);

            mPowerupHint1.setVisibility(View.INVISIBLE);
            mActivePowerUp = null;
        }
    }

    private void endGame() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.end_game_dialog_title)
                .setMessage(getString(R.string.end_game_dialog_message, mScore))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity().finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_info)
                .setCancelable(false)
                .show();
    }

    /** Add {@code points} to the score and update UI accordingly */
    private void increaseScore(int points) {
        mScore += points;
        mScoreCount.setText(String.valueOf(mScore));
    }

    /** Increment the moves count by 1. Update UI accordingly. */
    private void incrementMoves() {
        mMoves++;
        if (mGameMode == GameMode.MOVES) {
            int remaining = 30 - mMoves;
            if (remaining > 0) {
                mRemainingCount.setText(String.valueOf(remaining));
            } else {
                endGame();
            }
        } else if (mGameMode == GameMode.ENDLESS) {
            mRemainingCount.setText(String.valueOf(mMoves));
        }
    }

    private boolean isPowerup(float x, float y) {
        if (mActivePowerUp == PowerUp.SHRINKERS) {
            int index = calculateGridIndex(x, y);
            mModel.removeBlock(index);
            incrementMoves();
            increaseScore(1);
            toggleShrinkers();
            return true;
        } else if (mActivePowerUp == PowerUp.EXPANDERS) {
            int index = calculateGridIndex(x, y);
            int score = mModel.removeColor(mModel.getBlock(index));
            increaseScore(score);
            incrementMoves();
            toggleExpanders();
            mPowerup3Button.setEnabled(false);
            return true;
        }
        return false;
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
        if (mPath.isEmpty()) {
            return;
        }

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
        if (mPath.isEmpty()) {
            return;
        }

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

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        final int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            if (!isPowerup(event.getX(), event.getY())) {
                beginPath(event.getX(), event.getY());
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            movePath(event.getX(), event.getY());
        } else if (action == MotionEvent.ACTION_UP) {
            endPath(event.getX(), event.getY());
        }

        return true;
    }

    @Override
    public void onBlockChange(int index, int newColor) {
        BlockView block = (BlockView) mGrid.getChildAt(index);
        block.setColor(newColor);
    }

}
