package edu.washington.escience.myria.expression;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import edu.washington.escience.myria.Schema;
import edu.washington.escience.myria.Type;

/**
 * An ExpressionOperator with one child.
 */
public abstract class UnaryExpression extends ExpressionOperator {
  /***/
  private static final long serialVersionUID = 1L;

  /**
   * This is not really unused, it's used automagically by Jackson deserialization.
   */
  protected UnaryExpression() {
    operand = null;
  }

  /**
   * The child expression.
   */
  @JsonProperty
  private final ExpressionOperator operand;

  /**
   * @param operand the operand.
   */
  protected UnaryExpression(final ExpressionOperator operand) {
    this.operand = operand;
  }

  /**
   * @return the child expression.
   */
  public final ExpressionOperator getOperand() {
    return operand;
  }

  /**
   * Returns the function call unary string: functionName + '(' + child + ")". E.g, for {@link SqrtExpression},
   * <code>functionName</code> is <code>"Math.sqrt"</code>.
   * 
   * @param functionName the string representation of the Java function name.
   * @param schema the input schema
   * @return the Java string for this operator.
   */
  protected final String getFunctionCallUnaryString(final String functionName, final Schema schema) {
    return new StringBuilder(functionName).append('(').append(operand.getJavaString(schema)).append(')').toString();
  }

  /**
   * Returns the function call unary string: child + functionName. E.g, for {@link ToUpperCaseExpression},
   * <code>functionName</code> is <code>".toUpperCase()"</code>.
   * 
   * @param functionName the string representation of the Java function name.
   * @param schema the input schema
   * @return the Java string for this operator.
   */
  protected final String getDotFunctionCallUnaryString(final String functionName, final Schema schema) {
    return new StringBuilder(operand.getJavaString(schema)).append(functionName).toString();
  }

  /**
   * A function that could be used as the default hash code for a unary expression.
   * 
   * @return a hash of (getClass().getCanonicalName(), operand).
   */
  protected final int defaultHashCode() {
    return Objects.hash(getClass().getCanonicalName(), operand);
  }

  @Override
  public int hashCode() {
    return defaultHashCode();
  }

  @Override
  public boolean equals(final Object other) {
    if (other == null || !getClass().equals(other.getClass())) {
      return false;
    }
    UnaryExpression otherExp = (UnaryExpression) other;
    return Objects.equals(operand, otherExp.operand);
  }

  /**
   * A function that could be used as the default type checker for a unary expression where the operand must be numeric.
   * 
   * @param schema the schema of the input tuples.
   * @return the default numeric type, based on the type of the operand and Java type precedence.
   */
  protected Type checkAndReturnDefaultNumericType(final Schema schema) {
    Type operandType = getOperand().getOutputType(schema);
    ImmutableList<Type> validTypes = ImmutableList.of(Type.DOUBLE_TYPE, Type.FLOAT_TYPE, Type.LONG_TYPE, Type.INT_TYPE);
    Preconditions.checkArgument(validTypes.contains(operandType), "%s cannot handle operand [%s] of Type %s",
        getClass().getSimpleName(), getOperand(), operandType);
    return operandType;
  }

  /**
   * A function that could be used as the default type checker for a unary expression where the operand must be numeric.
   * 
   * @param schema the schema of the input tuples.
   */
  protected void checkBooleanType(final Schema schema) {
    Type operandType = getOperand().getOutputType(schema);
    Preconditions.checkArgument(operandType == Type.BOOLEAN_TYPE, "%s cannot handle operand [%s] of Type %s",
        getClass().getSimpleName(), getOperand(), operandType);
  }
}