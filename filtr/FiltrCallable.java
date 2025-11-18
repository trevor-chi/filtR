package filtr;

import java.util.List;

interface FiltrCallable {
  int arity();
  Object call(Interpreter interpreter, List<Object> arguments);
}
