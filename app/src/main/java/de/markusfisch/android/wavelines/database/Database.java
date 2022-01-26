package de.markusfisch.android.wavelines.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

public class Database {
	public static final int VERSION = 8;
	public static final String THEMES = "themes";
	public static final String THEMES_ID = "_id";
	public static final String THEMES_COUPLED = "coupled";
	public static final String THEMES_UNIFORM = "uniform";
	public static final String THEMES_SHUFFLE = "shuffle";
	public static final String THEMES_LINES = "lines";
	public static final String THEMES_WAVES = "waves";
	public static final String THEMES_AMPLITUDE = "amplitude";
	public static final String THEMES_OSCILLATION = "oscillation";
	public static final String THEMES_SHIFT = "shift";
	public static final String THEMES_SPEED = "speed";
	public static final String THEMES_GROWTH = "growth";
	public static final String THEMES_ROTATION = "rotation";
	public static final String THEMES_COLORS = "colors";
	public static final String THEMES_STROKE_WIDTHS = "stroke_width";

	private SQLiteDatabase db;

	public static int getInt(Cursor cursor, String column) {
		int index = cursor.getColumnIndex(column);
		if (index < 0) {
			return 0;
		}
		return cursor.getInt(index);
	}

	public static long getLong(Cursor cursor, String column) {
		int index = cursor.getColumnIndex(column);
		if (index < 0) {
			return 0L;
		}
		return cursor.getLong(index);
	}

	public static float getFloat(Cursor cursor, String column) {
		int index = cursor.getColumnIndex(column);
		if (index < 0) {
			return 0f;
		}
		return cursor.getFloat(index);
	}

	public static byte[] getBlob(Cursor cursor, String column) {
		int index = cursor.getColumnIndex(column);
		if (index < 0) {
			return null;
		}
		return cursor.getBlob(index);
	}

	public void open(Context context) {
		OpenHelper helper = new OpenHelper(context);
		db = helper.getWritableDatabase();
	}

	public Cursor queryThemes() {
		return db.rawQuery(
				"SELECT " +
						THEMES_ID + "," +
						THEMES_COUPLED + "," +
						THEMES_UNIFORM + "," +
						THEMES_SHUFFLE + "," +
						THEMES_LINES + "," +
						THEMES_WAVES + "," +
						THEMES_AMPLITUDE + "," +
						THEMES_OSCILLATION + "," +
						THEMES_SHIFT + "," +
						THEMES_SPEED + "," +
						THEMES_GROWTH + "," +
						THEMES_ROTATION + "," +
						THEMES_COLORS + "," +
						THEMES_STROKE_WIDTHS +
						" FROM " + THEMES +
						" ORDER BY " + THEMES_ID,
				null);
	}

	public long getFirstThemeId() {
		Cursor cursor = db.rawQuery(
				"SELECT " +
						THEMES_ID +
						" FROM " + THEMES +
						" ORDER BY " + THEMES_ID +
						" LIMIT 1",
				null);

		long themeId = -1;
		if (cursor == null) {
			return themeId;
		}

		if (cursor.moveToFirst()) {
			themeId = cursor.getLong(0);
		}
		cursor.close();
		return themeId;
	}

	public Theme getTheme(long id) {
		Cursor cursor = db.rawQuery(
				"SELECT " +
						THEMES_ID + "," +
						THEMES_COUPLED + "," +
						THEMES_UNIFORM + "," +
						THEMES_SHUFFLE + "," +
						THEMES_LINES + "," +
						THEMES_WAVES + "," +
						THEMES_AMPLITUDE + "," +
						THEMES_OSCILLATION + "," +
						THEMES_SHIFT + "," +
						THEMES_SPEED + "," +
						THEMES_GROWTH + "," +
						THEMES_ROTATION + "," +
						THEMES_COLORS + "," +
						THEMES_STROKE_WIDTHS +
						" FROM " + THEMES +
						" WHERE " + THEMES_ID + "= ?",
				new String[]{String.valueOf(id)});

		if (cursor == null) {
			return null;
		}

		Theme theme = null;
		if (cursor.moveToFirst()) {
			theme = themeFromCursor(cursor);
		}
		cursor.close();
		return theme;
	}

	public long insertTheme(Theme theme) {
		return insertTheme(db, theme);
	}

	public void updateTheme(long id, Theme theme) {
		db.update(
				THEMES,
				getThemeContentValues(theme),
				THEMES_ID + "= ?",
				new String[]{String.valueOf(id)});
	}

	public void deleteTheme(long id) {
		db.delete(
				THEMES,
				THEMES_ID + "= ?",
				new String[]{String.valueOf(id)});
	}

	public static Theme themeFromCursor(Cursor cursor) {
		return new Theme(
				getInt(cursor, THEMES_COUPLED) > 0,
				getInt(cursor, THEMES_UNIFORM) > 0,
				getInt(cursor, THEMES_SHUFFLE) > 0,
				getInt(cursor, THEMES_LINES),
				getInt(cursor, THEMES_WAVES),
				getFloat(cursor, THEMES_AMPLITUDE),
				getFloat(cursor, THEMES_OSCILLATION),
				getFloat(cursor, THEMES_SHIFT),
				getFloat(cursor, THEMES_SPEED),
				getFloat(cursor, THEMES_GROWTH),
				getInt(cursor, THEMES_ROTATION),
				intArrayFromCursor(cursor, THEMES_COLORS),
				intArrayFromCursor(cursor, THEMES_STROKE_WIDTHS));
	}

	private static int[] intArrayFromCursor(Cursor cursor, String column) {
		byte[] bytes = getBlob(cursor, column);
		if (bytes == null) {
			return null;
		}
		IntBuffer ib = ByteBuffer
				.wrap(bytes)
				.order(ByteOrder.nativeOrder())
				.asIntBuffer();
		int[] colors = new int[ib.remaining()];
		ib.get(colors);
		return colors;
	}

	private static long insertTheme(SQLiteDatabase db, Theme theme) {
		return db.insert(
				THEMES,
				null,
				getThemeContentValues(theme));
	}

	private static ContentValues getThemeContentValues(Theme theme) {
		ContentValues cv = new ContentValues();
		cv.put(THEMES_COUPLED, theme.coupled);
		cv.put(THEMES_UNIFORM, theme.uniform);
		cv.put(THEMES_SHUFFLE, theme.shuffle);
		cv.put(THEMES_LINES, theme.lines);
		cv.put(THEMES_WAVES, theme.waves);
		cv.put(THEMES_AMPLITUDE, theme.amplitude);
		cv.put(THEMES_OSCILLATION, theme.oscillation);
		cv.put(THEMES_SHIFT, theme.shift);
		cv.put(THEMES_SPEED, theme.speed);
		cv.put(THEMES_GROWTH, theme.growth);
		cv.put(THEMES_ROTATION, theme.rotation);
		cv.put(THEMES_COLORS, getByteBuffer(theme.colors));
		cv.put(THEMES_STROKE_WIDTHS, getByteBuffer(theme.strokeWidths));
		return cv;
	}

	private static byte[] getByteBuffer(int[] array) {
		ByteBuffer bb = ByteBuffer.allocate(array.length << 2);
		bb.order(ByteOrder.nativeOrder());
		IntBuffer ib = bb.asIntBuffer();
		ib.put(array);
		return bb.array();
	}

	private static void insertDefaultThemes(SQLiteDatabase db) {
		insertTheme(db, new Theme(true, false, false, 24, 3, .02f, 1f, 0f, .01f, 0f, 0, new int[]{
				0xff0060a0,
				0xff00b0f0,
				0xff0080c0,
				0xff00a0e0,
				0xff0070b0,
				0xff0090d0
		}, null));
		insertTheme(db, new Theme(false, false, false, 4, 2, .04f, 1f, 0f, .01f, 0f, 0, new int[]{
				0xff00b06c,
				0xff007ac6,
				0xffe86f13,
				0xffcf6310
		}, null));
		insertTheme(db, new Theme(false, true, false, 2, 1, .1f, .5f, 0f, .1f, .002f, 350, new int[]{
				0xffbd8119,
				0xfff7aa21
		}, null));
		insertTheme(db, new Theme(true, false, false, 4, 2, .02f, 1f, 0f, .01f, 0f, 32, new int[]{
				0xff8c2fb5,
				0xffb33ce8,
				0xff58299f,
				0xff602daf
		}, null));
		insertTheme(db, new Theme(true, false, false, 3, 1, .08f, 0.3f, 0f, .18f, .001f, 48, new int[]{
				0xff134dca,
				0xff1658e7,
				0xff1143b1
		}, null));
		insertTheme(db, new Theme(true, true, false, 12, 1, .04f, 0.7f, .18f, .1f, 0f, 22, new int[]{
				0xff333333,
				0xff6fa397,
				0xffdeda90,
				0xff9f6ca6,
				0xffeda1f7,
				0xff6fa397,
				0xffdeda90,
				0xff9f6ca6,
				0xffeda1f7,
				0xff6fa397,
				0xffdeda90,
				0xff9f6ca6,
		}, new int[]{
				1, 1, 1, 1, 1, 1,
				1, 1, 1, 1, 1, 1,
		}));
	}

	private static void createThemes(SQLiteDatabase db) {
		db.execSQL("DROP TABLE IF EXISTS " + THEMES);
		db.execSQL("CREATE TABLE " + THEMES + " (" +
				THEMES_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
				THEMES_COUPLED + " INTEGER," +
				THEMES_UNIFORM + " INTEGER," +
				THEMES_SHUFFLE + " INTEGER," +
				THEMES_LINES + " INTEGER," +
				THEMES_WAVES + " INTEGER," +
				THEMES_AMPLITUDE + " DOUBLE," +
				THEMES_OSCILLATION + " DOUBLE," +
				THEMES_SHIFT + " DOUBLE," +
				THEMES_SPEED + " DOUBLE," +
				THEMES_GROWTH + " DOUBLE," +
				THEMES_ROTATION + " INTEGER," +
				THEMES_COLORS + " BLOB," +
				THEMES_STROKE_WIDTHS + " BLOB);");
	}

	private static void addShuffle(SQLiteDatabase db) {
		db.execSQL("ALTER TABLE " + THEMES +
				" ADD COLUMN " + THEMES_SHUFFLE + " INTEGER;");
	}

	private static void addRotation(SQLiteDatabase db) {
		db.execSQL("ALTER TABLE " + THEMES +
				" ADD COLUMN " + THEMES_ROTATION + " INTEGER;");
	}

	private static void addOscillation(SQLiteDatabase db) {
		db.execSQL("ALTER TABLE " + THEMES +
				" ADD COLUMN " + THEMES_OSCILLATION + " DOUBLE;");
		db.execSQL("UPDATE " + THEMES +
				" SET " + THEMES_OSCILLATION + " = 1");
	}

	private static void addShift(SQLiteDatabase db) {
		db.execSQL("ALTER TABLE " + THEMES +
				" ADD COLUMN " + THEMES_SHIFT + " DOUBLE;");
	}

	private static void addSpeed(SQLiteDatabase db) {
		db.execSQL("ALTER TABLE " + THEMES +
				" ADD COLUMN " + THEMES_SPEED + " DOUBLE;");
		db.execSQL("UPDATE " + THEMES +
				" SET " + THEMES_SPEED + " = .01");
	}

	private static void addGrowth(SQLiteDatabase db) {
		db.execSQL("ALTER TABLE " + THEMES +
				" ADD COLUMN " + THEMES_GROWTH + " DOUBLE;");
	}

	private static void addStrokeWidths(SQLiteDatabase db) {
		db.execSQL("ALTER TABLE " + THEMES +
				" ADD COLUMN " + THEMES_STROKE_WIDTHS + " BLOB;");
	}

	private static class OpenHelper extends SQLiteOpenHelper {
		private OpenHelper(Context context) {
			super(context, "themes.db", null, VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			createThemes(db);
			insertDefaultThemes(db);
		}

		@Override
		public void onDowngrade(
				SQLiteDatabase db,
				int oldVersion,
				int newVersion) {
		}

		@Override
		public void onUpgrade(
				SQLiteDatabase db,
				int oldVersion,
				int newVersion) {
			if (oldVersion < 2) {
				addShuffle(db);
			}
			if (oldVersion < 3) {
				addRotation(db);
			}
			if (oldVersion < 4) {
				addOscillation(db);
			}
			if (oldVersion < 5) {
				addShift(db);
			}
			if (oldVersion < 6) {
				addSpeed(db);
			}
			if (oldVersion < 7) {
				addGrowth(db);
			}
			if (oldVersion < 8) {
				addStrokeWidths(db);
			}
		}
	}
}
