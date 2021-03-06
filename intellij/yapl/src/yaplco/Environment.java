package yaplco;

import java.util.Hashtable;

public class Environment {
    private static Object UNSET = new Object();

    public final Environment outer;
    private Hashtable<String, Object> locals = new Hashtable<>();

    public Environment(Environment outer) {
        this.outer = outer;
    }

    public Environment() { this(null); }

    public void define(String name, Primitive function) {
        define(name, (Object) function);
    }

    public <T, R> void defun(String name, Class<T> arg0Type, PrimitiveCoroutine1<T> function) {
        defun(name, (scheduler, evaluator, requester, args) ->
            evaluator.eval(scheduler, args.current, requester, arg0 ->
                function.accept(scheduler, evaluator, requester, (T) arg0)));
    }

    public <T, R> void defun(String name, Class<T> arg0Type, Class<R> arg1Type, PrimitiveCoroutine2<T, R> function) {
        defun(name, (scheduler, evaluator, requester, args) ->
            evaluator.eval(scheduler, args.current, requester, arg0 ->
                evaluator.eval(scheduler, args.next.current, requester, arg1 ->
                    function.accept(scheduler, evaluator, requester, (T) arg0, (R) arg1))));
    }

    public <T, R> void defun(String name, PrimitiveCoroutine function) {
        define(name, new Primitive() {
            @Override
            public CoRoutine newCo(Scheduler scheduler, Evaluator evaluator) {
                return new CoRoutineImpl() {
                    @Override
                    public void resume(CoRoutine requester, Object signal) {
                        scheduler.resume((CoRoutineImpl) (r, args) -> {
                            Pair signalAsPair = (Pair) args;
                            function.accept(scheduler, evaluator, r, signalAsPair);
                        }, requester, null);
                    }
                };
            }
        });
    }

    public void define(String name, Object value) {
        if(value == null)
            value = UNSET;

        locals.put(name, value);
    }

    public void set(String name, Object value) {
        locals.put(name, value);
    }

    public Object get(String name) {
        Object value = locals.get(name);
        if(value != null && value != UNSET)
            return value;
        return outer != null ? outer.get(name) : null;
    }
}
