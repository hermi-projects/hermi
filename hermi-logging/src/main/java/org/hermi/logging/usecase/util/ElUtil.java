package org.hermi.logging.usecase.util;

import jakarta.el.ExpressionFactory;
import jakarta.el.StandardELContext;
import jakarta.el.ValueExpression;
import org.aspectj.lang.reflect.MethodSignature;

public class ElUtil {
  private static final ExpressionFactory EL_FACTORY = ExpressionFactory.newInstance();

  public static String resolve(String expression, MethodSignature sig, Object[] args) {
    if (expression == null || expression.isBlank()) return null;
    StandardELContext context = new StandardELContext(EL_FACTORY);
    String[] paramNames = sig.getParameterNames();
    if (args != null) {
      for (int i = 0; i < args.length; i++) {
        ValueExpression ve = EL_FACTORY.createValueExpression(args[i], Object.class);
        context.getVariableMapper().setVariable("arg" + i, ve);

        if (paramNames != null && paramNames.length > i && paramNames[i] != null) {
          context.getVariableMapper().setVariable(paramNames[i], ve);
        }
      }
    }
    try {
      ValueExpression resultVe =
          EL_FACTORY.createValueExpression(context, expression, String.class);
      Object result = resultVe.getValue(context);
      return result != null ? result.toString() : "";
    } catch (Exception e) {
      return expression;
    }
  }
}
