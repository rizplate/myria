package edu.washington.escience.myriad.column;

import java.nio.FloatBuffer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import com.google.common.base.Preconditions;

import edu.washington.escience.myriad.TupleBatch;

/**
 * A column of Float values.
 * 
 * @author dhalperi
 * 
 */
public final class FloatColumn implements Column {
  /** Internal representation of the column data. */
  private final FloatBuffer data;

  /** Constructs an empty column that can hold up to TupleBatch.BATCH_SIZE elements. */
  public FloatColumn() {
    this.data = FloatBuffer.allocate(TupleBatch.BATCH_SIZE);
  }

  @Override
  public Object get(final int row) {
    return Float.valueOf(getFloat(row));
  }

  /**
   * Returns the element at the specified row in this column.
   * 
   * @param row row of element to return.
   * @return the element at the specified row in this column.
   */
  public float getFloat(final int row) {
    Preconditions.checkElementIndex(row, data.position());
    return data.get(row);
  }

  @Override
  public void getIntoJdbc(final int row, final PreparedStatement statement, final int jdbcIndex)
      throws SQLException {
    statement.setFloat(jdbcIndex, getFloat(row));
  }

  @Override
  public void getIntoSQLite(final int row, final SQLiteStatement statement, final int sqliteIndex)
      throws SQLiteException {
    statement.bind(sqliteIndex, getFloat(row));
  }

  @Override
  public void put(final Object value) {
    putFloat((Float) value);
  }

  /**
   * Inserts the specified element at end of this column.
   * 
   * @param value element to be inserted.
   */
  public void putFloat(final float value) {
    Preconditions.checkElementIndex(data.position(), data.capacity());
    data.put(value);
  }

  @Override
  public void putFromJdbc(final ResultSet resultSet, final int jdbcIndex) throws SQLException {
    putFloat(resultSet.getFloat(jdbcIndex));
  }

  @Override
  public void putFromSQLite(final SQLiteStatement statement, final int index)
      throws SQLiteException {
    throw new UnsupportedOperationException("SQLite does not support Float columns.");
  }

  @Override
  public int size() {
    return data.position();
  }
}