package com.l7tech.external.assertions.odata.server.producer.jdbc;

import com.l7tech.external.assertions.odata.server.producer.jdbc.JdbcModel.JdbcColumn;
import com.l7tech.external.assertions.odata.server.producer.jdbc.SqlStatement.SqlParameter;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmProperty;
import org.odata4j.exceptions.BadRequestException;
import org.odata4j.expression.AddExpression;
import org.odata4j.expression.AggregateAllFunction;
import org.odata4j.expression.AggregateAnyFunction;
import org.odata4j.expression.AndExpression;
import org.odata4j.expression.BinaryLiteral;
import org.odata4j.expression.BoolParenExpression;
import org.odata4j.expression.BooleanLiteral;
import org.odata4j.expression.ByteLiteral;
import org.odata4j.expression.CastExpression;
import org.odata4j.expression.CeilingMethodCallExpression;
import org.odata4j.expression.ConcatMethodCallExpression;
import org.odata4j.expression.DateTimeLiteral;
import org.odata4j.expression.DateTimeOffsetLiteral;
import org.odata4j.expression.DayMethodCallExpression;
import org.odata4j.expression.DecimalLiteral;
import org.odata4j.expression.DivExpression;
import org.odata4j.expression.DoubleLiteral;
import org.odata4j.expression.EndsWithMethodCallExpression;
import org.odata4j.expression.EntitySimpleProperty;
import org.odata4j.expression.EqExpression;
import org.odata4j.expression.ExpressionVisitor;
import org.odata4j.expression.FloorMethodCallExpression;
import org.odata4j.expression.GeExpression;
import org.odata4j.expression.GtExpression;
import org.odata4j.expression.GuidLiteral;
import org.odata4j.expression.HourMethodCallExpression;
import org.odata4j.expression.IndexOfMethodCallExpression;
import org.odata4j.expression.Int64Literal;
import org.odata4j.expression.IntegralLiteral;
import org.odata4j.expression.IsofExpression;
import org.odata4j.expression.LeExpression;
import org.odata4j.expression.LengthMethodCallExpression;
import org.odata4j.expression.LtExpression;
import org.odata4j.expression.MinuteMethodCallExpression;
import org.odata4j.expression.ModExpression;
import org.odata4j.expression.MonthMethodCallExpression;
import org.odata4j.expression.MulExpression;
import org.odata4j.expression.NeExpression;
import org.odata4j.expression.NegateExpression;
import org.odata4j.expression.NotExpression;
import org.odata4j.expression.NullLiteral;
import org.odata4j.expression.OrExpression;
import org.odata4j.expression.OrderByExpression;
import org.odata4j.expression.OrderByExpression.Direction;
import org.odata4j.expression.ParenExpression;
import org.odata4j.expression.ReplaceMethodCallExpression;
import org.odata4j.expression.RoundMethodCallExpression;
import org.odata4j.expression.SByteLiteral;
import org.odata4j.expression.SecondMethodCallExpression;
import org.odata4j.expression.SingleLiteral;
import org.odata4j.expression.StartsWithMethodCallExpression;
import org.odata4j.expression.StringLiteral;
import org.odata4j.expression.SubExpression;
import org.odata4j.expression.SubstringMethodCallExpression;
import org.odata4j.expression.SubstringOfMethodCallExpression;
import org.odata4j.expression.TimeLiteral;
import org.odata4j.expression.ToLowerMethodCallExpression;
import org.odata4j.expression.ToUpperMethodCallExpression;
import org.odata4j.expression.TrimMethodCallExpression;
import org.odata4j.expression.YearMethodCallExpression;

public class GenerateWhereClause implements ExpressionVisitor {

  private final StringBuilder sb = new StringBuilder();
  private final List<SqlParameter> params = new ArrayList<SqlParameter>();

  private final EdmEntitySet entitySet;
  private final JdbcMetadataMapping mapping;

  private Stack<String> nextBetween = new Stack<String>();
  private final Stack<String> nextBeforeDescend = new Stack<>();
  private final Stack<String> nextAfterDescend = new Stack<>();

  private static final Logger logger = Logger.getLogger(GenerateWhereClause.class.getName());

  public GenerateWhereClause(EdmEntitySet entitySet, JdbcMetadataMapping mapping) {
    this.entitySet = entitySet;
    this.mapping = mapping;
  }

  public void append(StringBuilder sql, List<SqlParameter> params) {
    sql.append(" WHERE ");
    sql.append(sb);
    params.addAll(this.params);
  }

  @Override
  public void beforeDescend() {
    if (!nextBeforeDescend.isEmpty())
      sb.append(nextBeforeDescend.pop());
  }

  @Override
  public void afterDescend() {
    if (!nextAfterDescend.isEmpty())
      sb.append(nextAfterDescend.pop());
  }

  @Override
  public void betweenDescend() {
    if (!nextBetween.isEmpty()) {
      sb.append(nextBetween.pop());
    }
  }

  @Override
  public void visit(String type) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(OrderByExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(Direction direction) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(AddExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(AndExpression expr) {
    nextBetween.push(" AND ");
  }

  @Override
  public void visit(BooleanLiteral expr) {
    sb.append(expr.getValue() ? "TRUE" : "FALSE");
  }

  @Override
  public void visit(CastExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(ConcatMethodCallExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(DateTimeLiteral expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(DateTimeOffsetLiteral expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(DecimalLiteral expr) {
    sb.append("?");
    params.add(new SqlParameter(expr.getValue(), Types.DECIMAL));
  }

  @Override
  public void visit(DivExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(EndsWithMethodCallExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(EntitySimpleProperty expr) {
    EdmProperty edmProp = entitySet.getType().findProperty(expr.getPropertyName());
    if (edmProp == null) {
      logger.log(Level.SEVERE, "{0} is a non-existing field for {1}", new String[]{expr.getPropertyName(), entitySet.getName()});
      throw new BadRequestException("There was an invalid field in the $filter parameter");
    }
    JdbcColumn column = mapping.getMappedColumn(edmProp);
    sb.append(column.columnName);
  }

  @Override
  public void visit(EqExpression expr) {
    nextBetween.push(" = ");
  }

  @Override
  public void visit(GeExpression expr) {
    nextBetween.push(" >= ");
  }

  @Override
  public void visit(GtExpression expr) {
    nextBetween.push(" > ");
  }

  @Override
  public void visit(GuidLiteral expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(BinaryLiteral expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(ByteLiteral expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(SByteLiteral expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(IndexOfMethodCallExpression expr) {
    nextBeforeDescend.push(" INSTR(");
    nextBetween.push(",");
    nextAfterDescend.push(")");
  }

  @Override
  public void visit(SingleLiteral expr) {
    sb.append("?");
    params.add(new SqlParameter(expr.getValue(), Types.FLOAT));
  }

  @Override
  public void visit(DoubleLiteral expr) {
    sb.append("?");
    params.add(new SqlParameter(expr.getValue(), Types.DOUBLE));
  }

  @Override
  public void visit(IntegralLiteral expr) {
    sb.append("?");
    params.add(new SqlParameter(expr.getValue(), Types.INTEGER));
  }

  @Override
  public void visit(Int64Literal expr) {
    sb.append("?");
    params.add(new SqlParameter(expr.getValue(), Types.BIGINT));
  }

  @Override
  public void visit(IsofExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(LeExpression expr) {
    nextBetween.push(" <= ");
  }

  @Override
  public void visit(LengthMethodCallExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(LtExpression expr) {
    nextBetween.push(" < ");
  }

  @Override
  public void visit(ModExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(MulExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(NeExpression expr) {
    nextBetween.push(" <> ");
  }

  @Override
  public void visit(NegateExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(NotExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(NullLiteral expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(OrExpression expr) {
    nextBetween.push(" OR ");
  }

  @Override
  public void visit(ParenExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(BoolParenExpression expr) {
    nextBeforeDescend.push("(");
    nextAfterDescend.push(")");
    //counts instances of OR and AND expression
    VisitorCount counter = new VisitorCount();
    expr.visit(counter);
    for (int i = 0; i < counter.getCount(); i++) {
      nextAfterDescend.push("");
    }
  }

  @Override
  public void visit(ReplaceMethodCallExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(StartsWithMethodCallExpression expr) {
    //throw new UnsupportedOperationException();
    nextBetween.push(" LIKE ");
  }

  @Override
  public void visit(StringLiteral expr) {
    sb.append("?");
    if (sb.toString().endsWith(" LIKE ?")) {
      String value = expr.getValue();
      //escape dangerous characters
      value = value.replace("%", "\\%");
      value = value.replace("_", "\\_");
      if (!value.endsWith("%")) {
        value = value + "%";
      }
      params.add(new SqlParameter(value, Types.VARCHAR));
    } else {
      params.add(new SqlParameter(expr.getValue(), Types.VARCHAR));
    }
  }

  @Override
  public void visit(SubExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(SubstringMethodCallExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(SubstringOfMethodCallExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(TimeLiteral expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(ToLowerMethodCallExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(ToUpperMethodCallExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(TrimMethodCallExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(YearMethodCallExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(MonthMethodCallExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(DayMethodCallExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(HourMethodCallExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(MinuteMethodCallExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(SecondMethodCallExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(RoundMethodCallExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(FloorMethodCallExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(CeilingMethodCallExpression expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(AggregateAnyFunction expr) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visit(AggregateAllFunction expr) {
    throw new UnsupportedOperationException();
  }

  class VisitorCount implements ExpressionVisitor {
    int count = 0;

    public int getCount() {
      return count;
    }

    @Override
    public void beforeDescend() {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void afterDescend() {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void betweenDescend() {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(String type) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(OrderByExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(Direction direction) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(AddExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(AndExpression expr) {
      count++;
    }

    @Override
    public void visit(BooleanLiteral expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(CastExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(ConcatMethodCallExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(DateTimeLiteral expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(DateTimeOffsetLiteral expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(DecimalLiteral expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(DivExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(EndsWithMethodCallExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(EntitySimpleProperty expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(EqExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(GeExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(GtExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(GuidLiteral expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(BinaryLiteral expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(ByteLiteral expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(SByteLiteral expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(IndexOfMethodCallExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(SingleLiteral expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(DoubleLiteral expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(IntegralLiteral expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(Int64Literal expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(IsofExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(LeExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(LengthMethodCallExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(LtExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(ModExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(MulExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(NeExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(NegateExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(NotExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(NullLiteral expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(OrExpression expr) {
      count++;
    }

    @Override
    public void visit(ParenExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(BoolParenExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(ReplaceMethodCallExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(StartsWithMethodCallExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(StringLiteral expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(SubExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(SubstringMethodCallExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(SubstringOfMethodCallExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(TimeLiteral expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(ToLowerMethodCallExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(ToUpperMethodCallExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(TrimMethodCallExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(YearMethodCallExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(MonthMethodCallExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(DayMethodCallExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(HourMethodCallExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(MinuteMethodCallExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(SecondMethodCallExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(RoundMethodCallExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(FloorMethodCallExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(CeilingMethodCallExpression expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(AggregateAnyFunction expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void visit(AggregateAllFunction expr) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

  }

}