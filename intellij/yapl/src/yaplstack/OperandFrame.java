package yaplstack;

import java.util.Stack;

public class OperandFrame {
    public final OperandFrame outer;
    private Stack<Object> stack = new Stack<>();

    public OperandFrame() {
        this(null);
    }

    public OperandFrame(OperandFrame outer) {
        this.outer = outer;
    }

    public void push(Object obj) {
        stack.push(obj);
    }

    public Object pop() {
        return stack.pop();
    }

    public void dup() {
        stack.push(stack.peek());
    }

    public void dupx1() {
        stack.add(stack.size() - 2, stack.peek());
    }

    public void pushTo(OperandFrame operandFrame, int pushCount) {
        for(int i = 0; i < pushCount; i++)
            operandFrame.push(pop());
    }

    public void swap() {
        Object tmp = stack.pop();
        stack.add(stack.size() - 1, tmp);
    }
}
