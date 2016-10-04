/**
 *
 */
package edu.washington.escience.myria.expression.evaluate;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.codehaus.janino.ExpressionEvaluator;

import com.google.common.base.Preconditions;

import edu.washington.escience.myria.DbException;
import edu.washington.escience.myria.MyriaConstants;
import edu.washington.escience.myria.Schema;
import edu.washington.escience.myria.Type;
import edu.washington.escience.myria.column.Column;
import edu.washington.escience.myria.column.builder.ColumnBuilder;
import edu.washington.escience.myria.column.builder.ColumnFactory;
import edu.washington.escience.myria.column.builder.WritableColumn;
import edu.washington.escience.myria.expression.Expression;
import edu.washington.escience.myria.expression.ExpressionOperator;
import edu.washington.escience.myria.expression.PyUDFExpression;
import edu.washington.escience.myria.expression.StateExpression;
import edu.washington.escience.myria.expression.VariableExpression;
import edu.washington.escience.myria.functions.PythonFunctionRegistrar;
import edu.washington.escience.myria.functions.PythonWorker;
import edu.washington.escience.myria.operator.Apply;
import edu.washington.escience.myria.operator.StatefulApply;
import edu.washington.escience.myria.storage.AppendableTable;
import edu.washington.escience.myria.storage.ReadableTable;
import edu.washington.escience.myria.storage.TupleBatch;

/**
 * An Expression evaluator for Python UDFs. Used in {@link Apply} and {@link StatefulApply}.
 */
public class PythonUDFEvaluator extends GenericEvaluator {

  /** logger for this class. */
  private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(PythonUDFEvaluator.class);

  private final PythonFunctionRegistrar pyFunction;

  private final static int PYTHON_EXCEPTION = -3;
  private final static int NULL_LENGTH = -5;

  private PythonWorker pyWorker;
  private boolean needsState = false;
  private final boolean[] isStateColumn;
  private int tupleSize = -1;
  private int[] columnIdxs = null;
  private final boolean btuples = false;

  private final Type outputType;

  /**
   * Default constructor.
   *
   * @param expression the expression for the evaluator
   * @param parameters parameters that are passed to the expression
   * @param pyFuncReg python function registrar to get the python function.
   */
  public PythonUDFEvaluator(final Expression expression, final ExpressionOperatorParameter parameters,
      final PythonFunctionRegistrar pyFuncReg) {
    super(expression, parameters);

    if (pyFuncReg != null) {
      pyFunction = pyFuncReg;
    } else {
      pyFunction = null;
    }

    if (parameters.getStateSchema() != null) {
      needsState = true;
    }

    PyUDFExpression op = (PyUDFExpression) expression.getRootExpressionOperator();
    outputType = op.getOutput();

    List<ExpressionOperator> childops = op.getChildren();
    tupleSize = childops.size();
    LOGGER.info("tuple size? " + tupleSize);
    columnIdxs = new int[tupleSize];
    isStateColumn = new boolean[tupleSize];
    Arrays.fill(columnIdxs, -1);
    Arrays.fill(isStateColumn, false);

  }

  /**
   * Initializes the python evaluator.
   */
  private void initEvaluator() throws DbException {
    ExpressionOperator op = getExpression().getRootExpressionOperator();

    String pyFunc = ((PyUDFExpression) op).getName();
    LOGGER.info("name of python function " + pyFunc);
    try {
      if (pyFunction != null) {
        LOGGER.info("pyFunction is  not null!!");
        String pyCodeString = pyFunction.getUDF(pyFunc);
        if (pyCodeString == null) {
          LOGGER.info("no python UDF with name " + pyFunc);
          throw new DbException("No Python UDf with given name registered.");
        } else {
          // tuple size is
          LOGGER.info("tuple size is: " + tupleSize);
          LOGGER.info("does this eval need state? " + needsState);
          if (pyWorker != null) {
            pyWorker.sendCodePickle(pyCodeString, tupleSize, outputType, 0);
            LOGGER.info("sent code pickle?");
          }
        }
      } else {
        LOGGER.info("pyFunction is null!");

      }

      List<ExpressionOperator> childops = op.getChildren();
      if (childops != null) {
        LOGGER.info("number of child ops " + childops.size());
        for (int i = 0; i < childops.size(); i++) {
          LOGGER.info("op id " + i);
          if (childops.get(i).getClass().equals(StateExpression.class)) {
            isStateColumn[i] = true;
            columnIdxs[i] = ((StateExpression) childops.get(i)).getColumnIdx();

          } else {
            columnIdxs[i] = ((VariableExpression) childops.get(i)).getColumnIdx();
          }
        }
      } else {
        LOGGER.info("child ops is null ");
      }

    } catch (Exception e) {
      LOGGER.info(e.getMessage());
      throw new DbException(e);
    }
  }

  /**
   * Creates an {@link ExpressionEvaluator} from the {@link #javaExpression}. This does not really compile the
   * expression and is thus faster.
   */
  @Override
  public void compile() {
    // LOGGER.info("this should be called when compiling!");
    /* Do nothing! */
  }

  @Override
  public void eval(final ReadableTable tb, final int rowIdx, final WritableColumn result, final ReadableTable state)
      throws DbException, IOException {
    // LOGGER.info("eval called");

    Object obj = evaluatePython(tb, rowIdx, state);
    if (obj == null) {
      throw new DbException("Python process returned null!");
    }
    try {
      switch (outputType) {
        case DOUBLE_TYPE:
          result.appendDouble((Double) obj);
          break;
        case BYTES_TYPE:
          result.appendByteBuffer(ByteBuffer.wrap((byte[]) obj));
          break;
        case FLOAT_TYPE:
          result.appendFloat((float) obj);
          break;
        case INT_TYPE:
          result.appendInt((int) obj);
          break;
        case LONG_TYPE:
          result.appendLong((long) obj);
          break;
        default:
          LOGGER.info("Type{} not supported as python Output." + outputType.toString());
          break;
      }
    } catch (Exception e) {
      throw new DbException(e);
    }
  }

  // /**
  // *
  // * @param tb - tuple batch to evaluate
  // * @param rowIdx - row index for evaluation
  // * @param result - result column
  // * @param state - state for aggregator functions
  // * @throws DbException
  // * @throws IOException
  // */
  //// public void evalUpdatePyExpression(final ReadableTable tb, final int rowIdx, final AppendableTable result,
  // final ReadableTable state) throws DbException, IOException {
  // pyWorker.sendNumTuples(1);
  //
  // Object obj = evaluatePython(tb, rowIdx, state);
  // int resultcol = -1;
  // for (int i = 0; i < tupleSize; i++) {
  // if (isStateColumn[i]) {
  // resultcol = columnIdxs[i];
  // }
  // break;
  // }
  //
  // // LOGGER.info("trying to update state on column: " + resultcol);
  // try {
  // switch (outputType) {
  // case DOUBLE_TYPE:
  // result.putDouble(resultcol, (Double) obj);
  // break;
  // case BYTES_TYPE:
  // // LOGGER.info("updating state!");
  // result.putByteBuffer(resultcol, (ByteBuffer.wrap((byte[]) obj)));
  // break;
  // case FLOAT_TYPE:
  // result.putFloat(resultcol, (float) obj);
  // break;
  // case INT_TYPE:
  // result.putInt(resultcol, (int) obj);
  // break;
  // case LONG_TYPE:
  // result.putLong(resultcol, (long) obj);
  // break;
  //
  // default:
  // LOGGER.info("type not supported as Python Output");
  // break;
  // }
  // } catch (Exception e) {
  // throw new DbException(e);
  // }
  // }

  public void evalBatch(final List<TupleBatch> ltb, final AppendableTable result, final ReadableTable state)
      throws DbException, IOException {

    // LOGGER.info("evalbatch called!!");
    if (pyWorker == null) {
      pyWorker = new PythonWorker();
      initEvaluator();
    }
    // this could be a problem -- this is finding the first state column -- could there be multple state
    // columns ?
    int resultcol = -1;
    for (int i = 0; i < tupleSize; i++) {
      if (isStateColumn[i]) {
        resultcol = columnIdxs[i];
      }
      break;
    }
    try {

      DataOutputStream dOut = pyWorker.getDataOutputStream();
      int numTuples = 0;
      for (int j = 0; j < ltb.size(); j++) {
        numTuples += ltb.get(j).numTuples();
      }
      pyWorker.sendNumTuples(numTuples);
      // LOGGER.info("number of tuples for stateful agg: " + numTuples);
      for (int tbIdx = 0; tbIdx < ltb.size(); tbIdx++) {
        TupleBatch tb = ltb.get(tbIdx);
        for (int tup = 0; tup < tb.numTuples(); tup++) {
          for (int col = 0; col < tupleSize; col++) {
            // LOGGER.info("is this a state column? " + isStateColumn[col]);
            writeToStream(tb, tup, columnIdxs[col], dOut);
          }
        }
      }

      // LOGGER.info("wrote all the tuples back!");
      // read result back
      Object obj = readFromStream();
      LOGGER.info("trying to update state on column: " + resultcol);

      switch (outputType) {
        case DOUBLE_TYPE:
          result.putDouble(resultcol, (Double) obj);
          break;
        case BYTES_TYPE:
          result.putByteBuffer(resultcol, (ByteBuffer.wrap((byte[]) obj)));
          break;
        case FLOAT_TYPE:
          result.putFloat(resultcol, (float) obj);
          break;
        case INT_TYPE:
          result.putInt(resultcol, (int) obj);
          break;
        case LONG_TYPE:
          result.putLong(resultcol, (long) obj);
          break;

        default:
          LOGGER.info("type not supported as Python Output");
          break;
      }

    } catch (Exception e) {

      throw new DbException(e);
    }
  }

  /**
   *
   * @param tb
   * @param rowIdx
   * @param state
   * @return object result
   * @throws DbException
   * @throws IOException
   */
  private Object evaluatePython(final ReadableTable tb, final int rowIdx, final ReadableTable state) throws DbException,
      IOException {
    // LOGGER.info("eval called!");

    if (pyWorker == null) {
      pyWorker = new PythonWorker();
      initEvaluator();
    }
    try {

      DataOutputStream dOut = pyWorker.getDataOutputStream();

      pyWorker.sendNumTuples(1);
      // LOGGER.info("number of tuples to be written: " + 1);
      for (int i = 0; i < tupleSize; i++) {
        if (isStateColumn[i]) {
          writeToStream(state, rowIdx, columnIdxs[i], dOut);
        } else {
          writeToStream(tb, rowIdx, columnIdxs[i], dOut);
        }
      }
      // read response back
      Object result = readFromStream();
      return result;

    } catch (DbException e) {
      LOGGER.info("Error writing to python stream" + e.getMessage());
      throw new DbException(e);
    }
  }

  @Override
  public Column<?> evaluateColumn(final TupleBatch tb) throws DbException, IOException {

    Type type = getOutputType();
    ColumnBuilder<?> ret = ColumnFactory.allocateColumn(type);
    for (int row = 0; row < tb.numTuples(); ++row) {
      eval(tb, row, ret, null);
    }
    return ret.build();
  }

  /**
   *
   * @return
   * @throws DbException
   */
  private Object readFromStream() throws DbException {
    // LOGGER.info("trying to read now");
    int type = 0;
    Object obj = null;
    DataInputStream dIn = pyWorker.getDataInputStream();

    try {
      // read length of incoming message
      type = dIn.readInt();
      if (type == PYTHON_EXCEPTION) {
        int excepLength = dIn.readInt();
        byte[] excp = new byte[excepLength];
        dIn.readFully(excp);
        throw new DbException(new String(excp));
      } else {
        // LOGGER.info("type read: " + type);
        if (type == MyriaConstants.PythonType.DOUBLE.getVal()) {
          obj = dIn.readDouble();
        } else if (type == MyriaConstants.PythonType.FLOAT.getVal()) {
          obj = dIn.readFloat();
        } else if (type == MyriaConstants.PythonType.INT.getVal()) {
          // LOGGER.info("trying to read int ");
          obj = dIn.readInt();
        } else if (type == MyriaConstants.PythonType.LONG.getVal()) {
          obj = dIn.readLong();
        } else if (type == MyriaConstants.PythonType.BYTES.getVal()) {
          int l = dIn.readInt();
          if (l > 0) {
            // LOGGER.info("length greater than zero!");
            obj = new byte[l];
            dIn.readFully((byte[]) obj);
          }
        }
      }

    } catch (Exception e) {
      LOGGER.info("Error reading from stream");
      throw new DbException(e);
    }
    return obj;
  }

  /**
   *
   * @param tb
   * @param row
   * @param columnIdx
   * @param dOut
   * @throws DbException
   */
  private void writeToStream(final ReadableTable tb, final int row, final int columnIdx, final DataOutputStream dOut)
      throws DbException {

    Preconditions.checkNotNull(tb, "tuple input cannot be null");
    Preconditions.checkNotNull(dOut, "Output stream for python process cannot be null");
    Schema tbsc = tb.getSchema();

    // LOGGER.info("tuple batch schema " + tbsc.toString());
    try {
      Type type = tbsc.getColumnType(columnIdx);
      // LOGGER.info("column index " + columnIdx + " columnType " + type.toString());
      switch (type) {
        case BOOLEAN_TYPE:
          LOGGER.info("BOOLEAN type not supported for python function ");
          break;
        case DOUBLE_TYPE:
          dOut.writeInt(MyriaConstants.PythonType.DOUBLE.getVal());
          dOut.writeInt(Double.SIZE / Byte.SIZE);

          dOut.writeDouble(tb.getDouble(columnIdx, row));
          break;
        case FLOAT_TYPE:
          dOut.writeInt(MyriaConstants.PythonType.FLOAT.getVal());
          dOut.writeInt(Float.SIZE / Byte.SIZE);
          dOut.writeFloat(tb.getFloat(columnIdx, row));
          break;
        case INT_TYPE:
          dOut.writeInt(MyriaConstants.PythonType.INT.getVal());
          dOut.writeInt(Integer.SIZE / Byte.SIZE);
          dOut.writeInt(tb.getInt(columnIdx, row));
          break;
        case LONG_TYPE:
          dOut.writeInt(MyriaConstants.PythonType.LONG.getVal());
          dOut.writeInt(Long.SIZE / Byte.SIZE);
          dOut.writeLong(tb.getLong(columnIdx, row));
          break;
        case STRING_TYPE:
          LOGGER.info("STRING type is not yet supported for python function ");
          break;
        case DATETIME_TYPE:
          LOGGER.info("date time not yet supported for python function ");
          break;
        case BYTES_TYPE:
          dOut.writeInt(MyriaConstants.PythonType.BYTES.getVal());
          ByteBuffer input = tb.getByteBuffer(columnIdx, row);
          if (input != null && input.hasArray()) {
            // LOGGER.info("input array buffer length" + input.array().length);
            dOut.writeInt(input.array().length);
            dOut.write(input.array());
          } else {
            // LOGGER.info("input arraybuffer length is null");
            dOut.writeInt(NULL_LENGTH);
          }
      }
      // LOGGER.info("flushing data");
      dOut.flush();

    } catch (Exception e) {
      // LOGGER.info(e.getMessage());
      throw new DbException(e);
    }
  }
}
