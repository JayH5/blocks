package za.jay.blocks;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jamie on 2014/03/02.
 */
public class ScoresDatabase extends SQLiteOpenHelper {

    private static final String TAG = "ScoresDatabase";
    private static final boolean DEBUG = false;

    private static final int VERSION = 1;
    private static final String NAME = "scores";

    private static final String TABLE_NAME = "scores";
    private static final String KEY_SCORE = "score";
    private static final String KEY_TIMESTAMP = "timestamp";

    private static final DateFormat SQL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public ScoresDatabase(Context context) {
        super(context, NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + KEY_SCORE + " INTEGER, "
                + KEY_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void saveScoreAsync(int score) {
        new AsyncTask<Integer, Void, Void>() {
            @Override
            protected Void doInBackground(Integer... params) {
                saveScore(params[0]);
                return null;
            }
        }.execute(score);
    }

    public void saveScore(int score) {
        ContentValues values = new ContentValues();
        values.put(KEY_SCORE, score);
        getWritableDatabase().insert(TABLE_NAME, null, values);

        if (DEBUG) {
            Log.d(TAG, dump());
        }
    }

    /**
     * Get all the scores ever recorded.
     * @return a map of the scores indexed by date scored.
     */
    public Map<Date, Integer> getScores() {
        Cursor cursor = getReadableDatabase().query(TABLE_NAME,
                new String[] { KEY_SCORE, KEY_TIMESTAMP }, null, null, null, null, null);

        Map<Date, Integer> map = null;
        if (cursor != null) {
            map = new HashMap<Date, Integer>(cursor.getCount());
            if (cursor.moveToFirst()) {
                do {
                    int score = cursor.getInt(0);
                    String dateString = cursor.getString(1);
                    Date date = null;
                    try {
                        date = SQL_DATE_FORMAT.parse(dateString);
                    } catch (ParseException e) {
                        Log.e(TAG, "Failed to parse date!", e);
                    }
                    if (date != null) {
                        map.put(date, score);
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return map;
    }

    /**
     * Get the top ten scores.
     * @return a list of the top ten scores in descending order
     */
    public List<Integer> getTopTenScores() {
        Cursor cursor = getReadableDatabase().query(TABLE_NAME, new String[] { KEY_SCORE }, null,
                null, null, null, KEY_SCORE + " DESC", "10");

        List<Integer> list = null;
        if (cursor != null) {
            list = new ArrayList<Integer>(cursor.getCount());
            if (cursor.moveToFirst()) {
                do {
                    list.add(cursor.getInt(0));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return list;
    }

    /** For DEBUG */
    private String dump() {
        Map<Date, Integer> scores = getScores();
        StringBuilder sb = new StringBuilder();
        final int limit = 20;
        int pos = 0;
        for (Map.Entry<Date, Integer> score : scores.entrySet()) {
            sb.append(score.getKey().toString())
                    .append(" - ")
                    .append(score.getValue())
                    .append('\n');

            if (++pos == limit) {
                break;
            }
        }

        return sb.toString();
    }
}
