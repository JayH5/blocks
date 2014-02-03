package za.jay.blocks;

import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created by jamie on 2014/02/01.
 */
public class GridModel {

    private final int[] mColours;
    private final int mWidth;
    private final int mHeight;

    private Random mRandom;
    private int[] mGrid;

    private GridChangeListener mListener;

    public GridModel(int width, int height, int[] colours) {
        mWidth = width;
        mHeight = height;
        mColours = colours;

        mRandom = new Random(SystemClock.elapsedRealtime());
        mGrid = new int[width * height];

        fillGrid();
    }

    /** Fills the grid with randomly chosen colours */
    private void fillGrid() {
        for (int i = 0; i < mWidth * mHeight; i++) {
            mGrid[i] = randomColour();
        }
    }

    /** Chooses a random value from the set of predefined colours */
    private int randomColour() {
        return mColours[mRandom.nextInt(mColours.length)];
    }

    /**
     * Get the color of a block at a given index.
     * @param index
     * @return
     */
    public int getBlock(int index) {
        return mGrid[index];
    }

    /** Removes a given block, "trickling down" the above blocks */
    private void removeBlock(int index) {
        for (int i = index; i >= mWidth; i -= mWidth) {
            setBlock(i, mGrid[i - mWidth]);
        }

        setBlock(index % mWidth, randomColour());
    }

    /** Internal method to update blocks that notifies the listener if set */
    private void setBlock(int index, int value) {
        mGrid[index] = value;

        if (mListener != null) {
            mListener.onBlockChange(index, value);
        }
    }

    /**
     * Removes all blocks of the same color as {@code color}.
     * @param color The color of the blocks to remove
     * @return The number of blocks removed
     */
    public int removeColor(int color) {
        // Find all of blocks of same color
        List<Integer> sameColor = new ArrayList<Integer>();
        for (int i = 0; i < mGrid.length; i++) {
            if (mGrid[i] == color) {
                sameColor.add(i);
            }
        }

        // Remove all those blocks (works in vertically descending order)
        for (Integer index : sameColor) {
            removeBlock(index);
        }

        return sameColor.size();
    }

    /**
     * Removes the list of block indices from the grid.
     * @param path A list if block indices
     * @return The number of blocks removes ({@code path.size()})
     */
    public int removePath(List<Integer> path) {
        // First ensure path is sorted as the order the blocks are removed is important
        Collections.sort(path);
        for (Integer index : path) {
            removeBlock(index);
        }

        return path.size();
    }

    public void setGridChangeListener(GridChangeListener listener) {
        mListener = listener;
    }

    public interface GridChangeListener {
        /**
         * This method is called when the color of a block changes.
         * @param index The index of the block in the grid
         * @param newColor The new color assigned to the block
         */
        void onBlockChange(int index, int newColor);
    }
}
