package edu.washington.escience.myriad;

import java.io.IOException;
import java.io.Serializable;
import java.util.NoSuchElementException;

import edu.washington.escience.myriad.operator.Operator;
import edu.washington.escience.myriad.table._TupleBatch;

/**
 * Query is a wrapper class to manage the execution of queries. It takes a query plan in the form of a high level
 * DbIterator (built by initiating the constructors of query plans) and runs it as a part of a specified transaction.
 * 
 * @author Sam Madden
 */

public class Query implements Serializable {

  private static final long serialVersionUID = 1L;

  private transient Operator op;
  private transient boolean started = false;

  public Query() {
  }

  public Query(final Operator root) {
    op = root;
  }

  /** Close the iterator */
  public final void close() throws IOException {
    op.close();
    started = false;
  }

  public final void execute() throws IOException, DbException {
    final Schema td = this.getOutputSchema();

    String names = "";
    for (int i = 0; i < td.numFields(); i++) {
      names += td.getFieldName(i) + "\t";
    }
    System.out.println(names);
    for (int i = 0; i < names.length() + td.numFields() * 4; i++) {
      System.out.print("-");
    }
    System.out.println("");

    this.start();
    int cnt = 0;
    while (this.hasNext()) {
      final _TupleBatch tup = this.next();
      System.out.println(tup);
      cnt += tup.numOutputTuples();
    }
    System.out.println("\n " + cnt + " rows.");
    this.close();
  }

  public final Schema getOutputSchema() {
    return this.op.getSchema();
  }

  public final Operator getPhysicalPlan() {
    return this.op;
  }

  /** @return true if there are more tuples remaining. */
  public final boolean hasNext() throws DbException {
    return op.hasNext();
  }

  /**
   * Returns the next tuple, or throws NoSuchElementException if the iterator is closed.
   * 
   * @return The next tuple in the iterator
   * @throws DbException If there is an error in the database system
   * @throws NoSuchElementException If the iterator has finished iterating
   */
  public final _TupleBatch next() throws DbException, NoSuchElementException {
    if (!started) {
      throw new DbException("Database not started.");
    }

    return op.next();
  }

  public final void setPhysicalPlan(final Operator pp) {
    this.op = pp;
  }

  public final void start() throws IOException, DbException {
    op.open();

    started = true;
  }
}
