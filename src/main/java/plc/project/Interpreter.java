package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        for (Ast.Global global : ast.getGlobals()) {
            visit(global);
        }
        for (Ast.Function function : ast.getFunctions()) {
            visit(function);
        }
        scope = new Scope(scope);
        return scope.lookupFunction("main", 0).invoke(Collections.emptyList());
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        if (ast.getValue().isPresent())
            scope.defineVariable(ast.getName(), ast.getMutable(),  visit(ast.getValue().get()));
        else
            scope.defineVariable(ast.getName(), false, Environment.NIL);

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        Scope declareScope = scope;
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            Scope invokeScope = scope;
            scope = new Scope(declareScope);
            for (int i = 0; i < ast.getParameters().size(); i++) {
                scope.defineVariable(ast.getParameters().get(i), true, args.get(i));
            }
            try {
                ast.getStatements().forEach(this::visit);
            }
            catch (Return returnValue) {
                return returnValue.value;
            }
            finally {
                scope = invokeScope;
            }
            return Environment.NIL;
        });

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        if (ast.getValue().isPresent())
            scope.defineVariable(ast.getName(), true, visit(ast.getValue().get()));
        else
            scope.defineVariable(ast.getName(), true, Environment.NIL);

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        if (ast.getReceiver().getClass() != Ast.Expression.Access.class)
            throw new RuntimeException("Assignment to non-access expression");
        try {
            scope = new Scope(scope);
            Ast.Expression.Access receiverAccess = (Ast.Expression.Access) ast.getReceiver();
            Environment.Variable receiverVariable = scope.lookupVariable(receiverAccess.getName());

            if (!receiverVariable.getMutable()) {
                throw new RuntimeException("Assignment to an immutable variable");
            }
            if (receiverAccess.getOffset().isPresent()) {
                Environment.PlcObject list = receiverVariable.getValue();
                Environment.PlcObject indexObj = visit(receiverAccess.getOffset().get());
                Environment.PlcObject valueToAssign = visit(ast.getValue());

                if (!(list.getValue() instanceof List) || !(indexObj.getValue() instanceof BigInteger)) {
                    throw new RuntimeException("Invalid index access or value assignment");
                }
                List<Object> listValue = (List<Object>) list.getValue();
                int idx = ((BigInteger) indexObj.getValue()).intValue();
                if (idx < 0 || idx >= listValue.size()) {
                    throw new RuntimeException("Index out of bounds");
                }
                //Environment.PlcObject tmp = listValue.get(idx);
                listValue.set(idx, valueToAssign.getValue());
            } else {
                receiverVariable.setValue(visit(ast.getValue()));
            }
        } finally {
            scope = scope.getParent();
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        boolean condition = requireType(Boolean.class, visit(ast.getCondition()));
        try {
            scope = new Scope(scope);
            if (condition) {
                for (Ast.Statement statement : ast.getThenStatements()) {
                    visit(statement);
                }
            }
            else {
                for (Ast.Statement statement : ast.getElseStatements()) {
                    visit(statement);
                }
            }
        } finally {
            scope = scope.getParent();
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        Environment.PlcObject condition = visit(ast.getCondition());
        boolean matched = false;
        Ast.Statement.Case defaultStatement = null;
        for (Ast.Statement.Case caseStatement : ast.getCases()) {
            if (caseStatement.getValue().isEmpty()){
                defaultStatement = caseStatement;
                continue;
            }
            Environment.PlcObject c = visit(caseStatement.getValue().get());
            if (condition.getValue().equals(c.getValue())) {
                visit(caseStatement);
                matched = true;
                break;
            }
        }
        // TODO handle DEFAULT case statement
        if (!matched && defaultStatement != null)
            visit(defaultStatement);
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        for (Ast.Statement statement : ast.getStatements()) {
            visit(statement);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Statement statement : ast.getStatements()) {
                    visit(statement);
                }
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new Return(visit(ast.getValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        return ast.getLiteral() == null ? Environment.NIL : Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        String op = ast.getOperator();
        if (op.equals("&&")){
            if (requireType(Boolean.class, visit(ast.getLeft())) == requireType(Boolean.class, visit(ast.getRight())))
                return visit(ast.getLeft());
            else
                return Environment.create(false);
        } else if (op.equals("||")) {
            if (requireType(Boolean.class, visit(ast.getLeft())))
                return visit(ast.getLeft());
            else if (requireType(Boolean.class, visit(ast.getRight())))
                return visit(ast.getRight());
            else
                return Environment.create(false);
        } else if (op.equals("==")) {
            if (visit(ast.getLeft()).getValue().equals(visit(ast.getRight()).getValue()))
                return Environment.create(true);
            else
                return Environment.create(false);
        } else if (op.equals("!=")) {
            if (visit(ast.getLeft()).getValue().equals(visit(ast.getRight()).getValue()))
                return Environment.create(false);
            else
                return Environment.create(true);
        } else if (op.equals("+")) {
            if (visit(ast.getLeft()).getValue() instanceof String
                    && visit(ast.getRight()).getValue() instanceof String)
                return Environment.create(visit(ast.getLeft()).getValue().toString()
                        + visit(ast.getRight()).getValue().toString());
            else if ((visit(ast.getLeft()).getValue() instanceof BigInteger)
                    && (visit(ast.getRight()).getValue() instanceof BigInteger))
                return Environment.create(BigInteger.class.cast(visit(ast.getLeft()).getValue())
                        .add(BigInteger.class.cast(visit(ast.getRight()).getValue())));
            else if (visit(ast.getLeft()).getValue() instanceof BigDecimal
                    && visit(ast.getRight()).getValue() instanceof BigDecimal)
                return Environment.create(BigDecimal.class.cast(visit(ast.getLeft()).getValue())
                        .add(BigDecimal.class.cast(visit(ast.getRight()).getValue())));
            else
                throw new RuntimeException("Wrong Operands for +");
        } else if (op.equals("-") || op.equals("*")) {
            if ((visit(ast.getLeft()).getValue() instanceof BigDecimal ||
                    visit(ast.getLeft()).getValue() instanceof BigInteger)
                    && visit(ast.getLeft()).getValue().getClass() == visit(ast.getRight()).getValue().getClass())
            {
                if (visit(ast.getLeft()).getValue() instanceof BigInteger) {
                    if (op.equals("*"))
                        return Environment.create(BigInteger.class.cast(visit(ast.getLeft()).getValue())
                                .multiply(BigInteger.class.cast(visit(ast.getRight()).getValue())));
                    else
                        return Environment.create(BigInteger.class.cast(visit(ast.getLeft()).getValue())
                                .subtract(BigInteger.class.cast(visit(ast.getRight()).getValue())));
                }
                else if (visit(ast.getLeft()).getValue() instanceof BigDecimal) {
                    if (op.equals("*"))
                        return Environment.create(BigDecimal.class.cast(visit(ast.getLeft()).getValue())
                                .multiply(BigDecimal.class.cast(visit(ast.getRight()).getValue())));
                    else
                        return Environment.create(BigDecimal.class.cast(visit(ast.getLeft()).getValue())
                                .subtract(BigDecimal.class.cast(visit(ast.getRight()).getValue())));
                } else
                    throw new RuntimeException("Tried to " + op + " but failed");
            }else
                throw new RuntimeException("Tried to " + op + " but failed");
        } else if (op.equals("/")) {
            if ((visit(ast.getLeft()).getValue() instanceof BigDecimal ||
                    visit(ast.getLeft()).getValue() instanceof BigInteger)
                    && visit(ast.getLeft()).getValue().getClass() == visit(ast.getRight()).getValue().getClass())
            {
                if ((visit(ast.getRight()).getValue().equals(BigDecimal.ZERO))
                        || (visit(ast.getRight()).getValue().equals(BigInteger.ZERO)))
                    throw new RuntimeException("Dividing by 0");

                if (visit(ast.getLeft()).getValue().getClass() == BigDecimal.class)
                    return Environment.create(BigDecimal.class.cast(visit(ast.getLeft()).getValue()).divide(BigDecimal.class.cast(visit(ast.getRight()).getValue()), RoundingMode.HALF_EVEN));
                else
                    return Environment.create(BigInteger.class.cast(visit(ast.getLeft()).getValue()).divide(BigInteger.class.cast(visit(ast.getRight()).getValue())));
            }
            else
                throw new RuntimeException("Tried to / but types are wrong");
        } else if (op.equals("<") || op.equals(">")) {
            if (visit(ast.getLeft()).getValue() instanceof Comparable &&
                    visit(ast.getLeft()).getValue().getClass() == visit(ast.getRight()).getValue().getClass())
            {
                int comparison;
                Comparable<Object> left = (Comparable<Object>) visit(ast.getLeft()).getValue();
                Comparable<Object> right = (Comparable<Object>) visit(ast.getRight()).getValue();
                comparison = left.compareTo(right);
                if (op.equals("<"))
                    return Environment.create(comparison < 0);
                else
                    return Environment.create(comparison > 0);
            }
        }
        throw new RuntimeException("Wrong Operator");
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        if (ast.getOffset().isEmpty())
            return scope.lookupVariable(ast.getName()).getValue();
        Environment.PlcObject indexObject = visit(ast.getOffset().get());
        Environment.PlcObject listObject = scope.lookupVariable(ast.getName()).getValue();
        if (!(indexObject.getValue() instanceof BigInteger)){
            throw new RuntimeException("Index not an integer");
        }
        if (!(listObject.getValue() instanceof List<?>)) {
            throw new RuntimeException("Variable not a list");
        }
        BigInteger index = (BigInteger) indexObject.getValue();
        List<Environment.PlcObject> plcList = (List<Environment.PlcObject>) listObject.getValue();
        int size = plcList.size();
        if (index.compareTo(BigInteger.ZERO) >= 0 && index.compareTo(BigInteger.valueOf(size)) < 0) {
            return Environment.create(plcList.get(index.intValue()));
        } else {
            throw new RuntimeException("Index out of bounds");
        }

    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        try {
            scope = new Scope(scope);
            List<Environment.PlcObject> args = new ArrayList<Environment.PlcObject>();
            for (int i = 0; i < ast.getArguments().size(); i++) {
                args.add(visit(ast.getArguments().get(i)));
            }
                return scope.lookupFunction(ast.getName(), args.size()).invoke(args);
        } finally {
            scope = scope.getParent();
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        List<Ast.Expression> list = ast.getValues();
        List<Object> plcList = new ArrayList<>();;
        for (Ast.Expression expression : list){
            plcList.add(visit(expression).getValue());
        }
        return Environment.create(plcList);
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
