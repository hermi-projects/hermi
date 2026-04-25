package org.hermi.shell;

public interface Mapper<C, R, I, O> {

  I convertContext(C context);

  R convertResult(O output);
}
