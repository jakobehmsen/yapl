package yaplco;

import java.io.*;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static yaplco.Pair.list;

public class Main {
    public static void main(String[] args) {
        Environment env = new Environment();

        env.defun("get", String.class, (s, evaluator, requester, name) ->
            s.respond(requester, evaluator.environment.get(name)));

        env.define("quote", (scheduler1, evaluator) -> (CoRoutineImpl) (requester, signal) ->
            scheduler1.respond(requester, signal));

        env.defun("request", (s, evaluator, requester, args1) ->
            s.resume(requester, requester, null));

        env.defun("resume", Object.class, (s, evaluator, requester, signal) ->
            s.resume(requester, requester, signal));

        env.defun("+", int.class, int.class, (s, evaluator, requester, lhs, rhs) ->
            s.respond(requester, lhs + rhs));

        env.defun("let-list-eval", new PrimitiveCoroutine() {
            @Override
            public void accept(Scheduler scheduler, Evaluator evaluator, CoRoutine requester, Pair args) {
                Pair listToEvaluate = (Pair) args.current;
                Pair names = (Pair)args.next.current;
                Object body = args.next.next.current;

                // CoRoutineImpl
                evaluator.eval(scheduler, listToEvaluate /*How to handle requests properly?*/, requester, list -> {
                    evaluator.environment = new Environment(evaluator.environment);

                    evaluateAndBindNext(scheduler, evaluator, requester, (Pair)list, names, body);
                });

                /*evaluator.eval(scheduler, listToEvaluate, new CoRoutineImpl() {
                    @Override
                    public void resume(CoRoutine requester, Object signal) {
                        evaluator.environment = new Environment(evaluator.environment);

                        evaluateAndBindNext(scheduler, evaluator, requester, (Pair)signal, names, body);
                    }
                });*/
            }

            private void evaluateAndBindNext(Scheduler scheduler, Evaluator evaluator, CoRoutine requester, Pair list, Pair names, Object body) {
                if(list != null && names != null) {
                    Object itemToEvaluate = list.current;
                    String name = (String)names.current;

                    evaluator.eval(scheduler, itemToEvaluate, requester, item -> {
                        evaluator.environment.define(name, item);

                        evaluateAndBindNext(scheduler, evaluator, requester, list.next, names.next, body);
                    });
                } else {
                    CoRoutine bodyForEvaluation = evaluator.eval(scheduler, body);
                    evaluator.eval(scheduler, requester, bodyForEvaluation, result -> {
                        scheduler.respond(requester, result);
                    });
                }
            }
        });

        env.define("myFunc", list("let-list-eval", list("request"), list("a", "b"), list(
            list("+", list("get", "a"), list("get", "b"))
        )));

        /*
        How should arguments provided?
        As a list?

        Are provided one at a time?
        And thus requested one at a time?

        // let-list-eval maps each item of a list to its evaluated part and declares and binds locals to the evaluated items
        (define myFunc '(let-list-eval (resume) (x y z) (

        )))

        */

        //requester.respond(lhs + rhs));
        /*env.defun("+", int.class, int.class, (e, r, lhs, rhs) -> r);
        env.defun("-", int.class, int.class, (co, lhs, rhs) -> e.popFrame(lhs - rhs));
        env.defun("/", int.class, int.class, (co, lhs, rhs) -> e.popFrame(lhs / rhs));
        env.defun("*", int.class, int.class, (co, lhs, rhs) -> e.popFrame(lhs * rhs));*/

        Scheduler scheduler = new Scheduler();

        Evaluator ev = new Evaluator(env);

        //CoRoutine evaluation = ev.eval(scheduler, list("+", list("+", 1, 2), 2));
        CoRoutine evaluation = ev.eval(scheduler, list("myFunc", 1, 2));
        scheduler.resume(new CoRoutineImpl() {
            @Override
            public void resume(CoRoutine requester, Object signal) {
                new String();
            }

            @Override
            public void resumeResponse(CoRoutine requester, Object signal) {
                System.out.println("Response: " + signal);
            }

            @Override
            public void resumeError(CoRoutine requester, Object signal) {
                System.err.println("Error: " + signal);
            }

            @Override
            public void resumeOther(CoRoutine requester, Object signal) {
                System.out.println("Other: " + signal);
            }
        }, evaluation, null);

        if(1 != 2)
            return;

        // Define primitives

        /*
        env.define("quote", (e, a) ->
            e.popFrame(a.current));

        env.define("locals", (e, a) ->
            e.popFrame(e.getLocals()));

        env.defun("first", Pair.class, (e, l) ->
            e.popFrame(l.current));

        env.define("rest", (e, a) ->
            e.popFrame(((Pair) a.current).next));

        env.defun("get", String.class, (e, name) -> {
            Object value = e.getEnvironment().get(name);
            e.popFrame(value);
        });

        env.defun("store", String.class, Object.class, (e, name, value) -> {
            e.getEnvironment().store(name, value);
            e.popFrame(value);
        });

        env.defun("define", String.class, Object.class, (e, name, value) -> {
            e.getEnvironment().define(name, value);
            e.popFrame(value);
        });

        env.defun("apply", Object.class, Object.class, (e, item, locals) ->
            e.apply(item, (Pair) locals));

        env.defun("fun", Object.class, (e, body) ->
            e.popFrame((BiConsumer<Evaluator, Pair>) (e2, l) -> {
                e2.pushFrame(result -> e2.popFrame(result), l);
                e2.evaluate(body);
            }));
        */


        // A scan function should probably rather be a closure which juggles the user and it as a supplier
        /*
        (define tokens (scan theInput))
        Somehow, scan should signal outwards its extracted tokens

        A response handler simply handles signal provided from a caller?

        //var coscan = (co (scan theInput))
        var coscan = (co scan)

        loop
            //var signal = (resume coscan)
            var signal = (resume coscan theInput) // Initiates coroutine
            // Here, an implicit provider of next (a continuation) is stored for additional next calls
            // This continuation should simply be a singleton arg?
            var numbers = []
            match signal
                case (next, :item):
                    if digit(item)
                        var digits = [item]
                        loop
                            match signal = (resume coscan)
                                case (next, :item):
                                    if digit(item)
                                        digits.append(item)
                                    else
                                        numbers.append(digits)
                                        break
                    if ws(item)
                        continue


            match signal, next, digit // next and digit can looked up and invoked


        Then, a regular function call look something like:

        (resume (co func) arg1, arg2, ..., argn)

        So, a function is basically a continuation factory




        handle
            (scan theInput)
        with // Should be given the code to run
            loop
                var signal = resume // Jumps to execution handle code
                // Here, an implicit provider of next (a continuation) is stored for additional next calls
                // This continuation should simply be a singleton arg?
                var numbers = []
                match signal
                    case (next, :item):
                        if digit(item)
                            var digits = [item]
                            loop
                                match signal = resume
                                    case (next, :item):
                                        if digit(item)
                                            digits.append(item)
                                        else
                                            numbers.append(digits)
                                            break
                        if ws(item)
                            continue


                match signal, next, digit // next and digit can looked up and invoked

        */

        /*
        env.defun("scan", InputStream.class, (e, input) ->
            e.popFrame((BiConsumer<Evaluator, Pair>) (e2, l) -> {
                e2.pushFrame(result -> e2.popFrame(result), l);
                e2.evaluate(body);
            }));
        */

        env.define("parse", new Primitive() {
            @Override
            public CoRoutine newCo(Scheduler scheduler, Evaluator evaluator) {
                return new CoRoutineImpl() {
                    int index;
                    Reader reader;
                    StringBuffer buffer;
                    boolean inited;

                    @Override
                    public void resume(CoRoutine requester, Object signal) {
                        if (!inited) {
                            evaluator.eval(scheduler, ((Pair) signal).current, requester, arg0 -> {
                                InputStream inputStream = (InputStream) arg0;
                                reader = new BufferedReader(new InputStreamReader(inputStream));
                                buffer = new StringBuffer();

                                inited = true;

                                scheduler.resumeResponse(this, requester, null);
                            });
                        } else {
                            ignore();
                            if (!atEnd()) {
                                Object next = next();
                                scheduler.resumeResponse(this, requester, Pair.list("next", next));
                            } else
                                scheduler.resumeResponse(this, requester, Pair.list("atEnd"));
                        }
                    }

                    Object next() {
                        if (peek() == '(') {
                            consume();
                            ignore();

                            Object next = nextList();
                            if (peek() == ')') {
                                consume();

                                return next;
                            } else {
                                // ERROR
                            }
                        } else if (Character.isDigit(peek())) {
                            StringBuffer digits = new StringBuffer();
                            digits.append(peek());
                            consume();

                            while (Character.isDigit(peek())) {
                                digits.append(peek());
                                consume();
                            }

                            return Integer.parseInt(digits.toString());
                        } else if (peek() == '"') {
                            consume();

                            StringBuffer chars = new StringBuffer();

                            while (true) {
                                if (peek() == '"') {
                                    consume();
                                    break;
                                }
                                // Check character escapes

                                chars.append(peek());
                                consume();
                            }

                            return chars.toString();
                        } else if (isIdentifierPart(peek())) {
                            StringBuffer parts = new StringBuffer();
                            parts.append(peek());
                            consume();

                            while (isIdentifierPart(peek())) {
                                parts.append(peek());
                                consume();
                            }

                            return parts.toString();
                        }

                        return null;
                    }

                    boolean isIdentifierPart(char ch) {
                        //return Character.isJavaIdentifierPart(ch);
                        return !Character.isWhitespace(ch) && ch != ')';
                    }

                    Pair nextList() {
                        ArrayList<Object> items = new ArrayList<>();

                        while (!atEnd()) {
                            Object next = next();
                            if (next != null) {
                                items.add(next);
                                ignore();
                            } else
                                break;
                        }

                        return Pair.list(items);
                    }

                    void consume() {
                        index++;
                    }

                    boolean atEnd() {
                        ensureBuffered();

                        return index >= buffer.length();
                    }

                    void ensureBuffered() {
                        if (index >= buffer.length()) {
                            try {
                                CharBuffer readChars = CharBuffer.allocate(1024);
                                int readCharCount = reader.read(readChars);
                                if (readCharCount > 0) {
                                    readChars.position(0);
                                    buffer.append(readChars, 0, readCharCount);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    char peek() {
                        ensureBuffered();

                        return !atEnd() ? buffer.charAt(index) : '\0';
                    }

                    void ignore() {
                        while (!atEnd() && Character.isWhitespace(peek()))
                            consume();
                    }
                };
            }
        });

        // co wraps an expression such that variable co-routines are supported during request/response logic
        // Can this primitive be implemented in a custom way
        env.define("co", new Primitive() {
            @Override
            public CoRoutine newCo(Scheduler scheduler, Evaluator evaluator) {
                return new CoRoutineImpl() {
                    @Override
                    public void resume(CoRoutine requester, Object signal) {
                        Object item = ((Pair)signal).current;
                        CoRoutine signalAsCo = evaluator.eval(scheduler, item);

                        CoRoutine co = new CoRoutineImpl() {
                            CoRoutine current = signalAsCo;
                            CoRoutine currentRequester;
                            boolean handlingRequest;

                            @Override
                            public void resume(CoRoutine requester, Object signal) {
                                if(!handlingRequest) {
                                    currentRequester = requester;
                                    handlingRequest = true;
                                    scheduler.resume(this, current, signal);
                                    current = null;
                                } else {
                                    current = requester;
                                    handlingRequest = false;
                                    scheduler.resume(this, currentRequester, signal);
                                    currentRequester = null;
                                }
                            }
                        };

                        scheduler.respond(requester, co);
                    }

                    @Override
                    public void resumeError(CoRoutine requester, Object signal) {

                    }
                };
            }
        });

        // A list co routine that doesn't return the same routine multiple times
        env.define("aList", new Primitive() {
            @Override
            public CoRoutine newCo(Scheduler scheduler, Evaluator evaluator) {
                return new CoRoutineImpl() {
                    private List<Object> list = Arrays.asList(1, 2, 3, 4, 5);

                    @Override
                    public void resume(CoRoutine requester, Object signal) {
                        provide(requester, 0, list);
                    }

                    private void provide(CoRoutine requester, int index, List<Object> list) {
                        if(index < list.size()) {
                            Object next = list.get(index);
                            scheduler.resumeOther(new CoCaller(scheduler, requester) {
                                @Override
                                public void resumeResponse(CoRoutine requester, Object signal) {
                                    provide(requester, index + 1, list);
                                }
                            }, requester, list("next", next));
                        } else {
                            scheduler.resumeOther(requester, "end");
                        }
                    }
                };
            }
        });

        Object aListCode = list(
            list("resume", list("quote", list("next", 0))),
            list("resume", list("quote", list("next", 1))),
            list("resume", list("quote", list("next", 2)))
        );

        // An example of using co with an expression for which new coroutine instances are created for each response
        CoRoutine coListCode = new Evaluator(env).eval(scheduler, list("co", list("aList")));

        scheduler.respond(new CoCaller(scheduler, null) {
            CoRoutine coList;

            @Override
            public void resumeResponse(CoRoutine requester, Object signal) {
                scheduler.respond(new CoCaller(scheduler, requester) {
                    @Override
                    public void resumeResponse(CoRoutine requester, Object signal) {
                        coList = (CoRoutine)signal;

                        /*scheduler.respond(new CoCaller(scheduler, requester) {
                            @Override
                            public void resumeResponse(CoRoutine requester, Object signal) {
                                handleNext(requester, signal);
                            }
                        }, coList, null);*/

                        scheduler.respond(new CoRoutineImpl() {
                            @Override
                            public void resume(CoRoutine requester, Object signal) {
                                signal.toString();
                                handleNext(requester, signal);
                            }

                            @Override
                            public void resumeOther(CoRoutine requester, Object signal) {
                                signal.toString();
                            }
                        }, coList, null);
                    }
                }, coListCode, null);
            }

            private void handleNext(CoRoutine requester, Object signal) {
                if(signal instanceof Pair && ((Pair)signal).current.equals("next")) {
                    System.out.println("Next in list: " + ((Pair)signal).next.current);

                    scheduler.respond(new CoRoutineImpl() {
                        @Override
                        public void resume(CoRoutine requester, Object signal) {
                            handleNext(requester, signal);
                        }
                    }, coList, null);
                } else if(signal.equals("end")) {
                    System.out.println("No more in list.");
                }
            }
        }, "start");

        /*env.defun("print", String.class, (e, str) -> {
            System.out.print(str);
            e.popFrame(null);
        });

        env.define("test", (e, a) ->
            e.popFrame("Test"));*/

        /*Pair program = Pair.list(
            "print", "A string\n"
        );*/

        /*Pair program = Pair.list(
            Pair.list("print", "A string\n"),
            Pair.list("test"),
            Pair.list("add", 1, 3)
        );*/

        /*
        Pair program = Pair.list(
            Pair.list("define", "add2", Pair.list(Pair.list("lhs"), Pair.list("add", Pair.list("get", "lhs"), 2))),
            Pair.list("add2", 1)
        );
        */

        Evaluator evaluator2 = new Evaluator(env);

        //String src = "( 11 (234 2)43 43)";
        //String src = "(  \"A string\" 1 2 33 (quote some stuff) )";
        String src = "(+ 1 2) (+  344343 66  )";
        InputStream srcInputStream = new ByteArrayInputStream(src.getBytes());

        //InputStream srcInputStream = System.in;
        CoRoutine coParse = new Evaluator(env).eval(scheduler, list("parse", srcInputStream));

        scheduler.respond(
            new CoCaller(scheduler, null) {
                CoRoutineImpl parseHandler = this;
                CoRoutine parseRequester;

                @Override
                public void resumeResponse(CoRoutine requester, Object signal) {
                    if (signal != null && signal instanceof Pair) {
                        Pair signalAsPair = (Pair) signal;

                        if (signalAsPair.current.equals("next")) {
                            Object next = signalAsPair.next.current;
                            System.out.println("Next to evaluate: " + next);

                            Object program = next;
                            CoRoutine coProgram = evaluator2.eval(scheduler, program);

                            scheduler.resume(new CoCaller(scheduler, requester) {
                                @Override
                                public void resumeResponse(CoRoutine requester, Object signal) {
                                    System.out.println("=> " + signal);
                                    System.out.flush();

                                    // Parse next
                                    System.out.print(">> ");
                                    scheduler.resume(parseHandler, coParse, null);
                                }

                                @Override
                                public void resumeError(CoRoutine requester, Object signal) {
                                    System.err.println("Error: " + signal);
                                    System.out.flush();
                                }

                                @Override
                                public void resumeOther(CoRoutine requester, Object signal) {
                                    super.resumeOther(requester, signal);
                                }
                            }, coProgram, null);
                        } else if (signalAsPair.current.equals("atEnd")) {
                            scheduler.respond(parseRequester, "atEnd");
                        }
                    } else {
                        if(signal != null && signal.equals("Start")) {
                            parseRequester = requester;
                            System.out.println("Yapl repl:");
                            scheduler.resume(this, coParse, null);
                        } else {
                            // Parse next
                            System.out.print(">> ");
                            scheduler.resume(this, coParse, null);
                        }
                    }
                }
            }, "Start"
        );

        Pair program = list(
            /*
            Pair.list(
                "define", "add2",

                Pair.list("head", Pair.list("args"))
                Pair.list("head", Pair.list("rest", Pair.list("args")))

                Pair.list(Pair.list("lhs"), Pair.list("add", Pair.list("get", "lhs"), 2))
            ),
            */
            //Pair.list("first", Pair.list("quote", Pair.list("First", "Second")))

            /*list("define", "myFunc", list("fun", list("quote", list("*", 3, 7)))),
            list("myFunc")*/
            list("+", 1, 2),
            list("+", 1, 4)
        );

        CoRoutine coProgram = evaluator2.eval(scheduler, program);
        scheduler.resume(new CoCaller(scheduler, null) {
            @Override
            public void resumeResponse(CoRoutine requester, Object signal) {
                System.out.println("Response: " + signal);
            }

            @Override
            public void resumeError(CoRoutine requester, Object signal) {
                System.err.println("Error: " + signal);
            }
        }, coProgram, null);

    }
}
