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
        if (ast.getFunctions().stream().anyMatch(f -> f.getName().equals("main") && f.getParameters().isEmpty())) {
            return visit(new Ast.Expression.Function("main", List.of()));
        }
        throw new RuntimeException("Main function not found");
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
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            try {
                scope = new Scope(scope);
                for (int i = 0; i < ast.getParameters().size(); i++) {
                    scope.defineVariable(ast.getParameters().get(i),true, args.get(i)); // TODO: is mutability true??
                }
                for (Ast.Statement statement : ast.getStatements()) {
                    visit(statement);
                }
            } catch (Return r) {
                return r.value;
            } finally {
                scope = scope.getParent();
            }
            return Environment.NIL;
        }
        );

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
            // TODO List indexing doesn't work i tink
            Ast.Expression.Access temp = Ast.Expression.Access.class.cast(ast.getReceiver());
            if (scope.lookupVariable(temp.getName()).getMutable())
                scope.lookupVariable(temp.getName()).setValue(visit(ast.getValue()));
            else
                throw new RuntimeException("Assignment to an immutable");
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
                return Environment.create(Boolean.FALSE);
        } else if (op.equals("||")) {
            if (requireType(Boolean.class, visit(ast.getLeft())) == Boolean.TRUE)
                return visit(ast.getLeft());
            else if (requireType(Boolean.class, visit(ast.getRight())) == Boolean.TRUE)
                return visit(ast.getRight());
            else
                return Environment.create(Boolean.FALSE);
        } else if (op.equals("==")) {
            if (visit(ast.getLeft()).getValue().equals(visit(ast.getRight()).getValue()))
                return Environment.create(Boolean.TRUE);
            else
                return Environment.create(Boolean.FALSE);
        } else if (op.equals("!=")) {
            if (visit(ast.getLeft()).getValue().equals(visit(ast.getRight()).getValue()))
                return Environment.create(Boolean.FALSE);
            else
                return Environment.create(Boolean.TRUE);
        } else if (op.equals("+")) {
            if (visit(ast.getLeft()).getValue().getClass() == String.class && visit(ast.getRight()).getValue().getClass() == String.class)
                return Environment.create(visit(ast.getLeft()).getValue().toString() + visit(ast.getRight()).getValue().toString());
            else if (visit(ast.getLeft()).getValue().getClass() == BigInteger.class && visit(ast.getLeft()).getValue().getClass() == visit(ast.getRight()).getValue().getClass())
                return Environment.create(BigInteger.class.cast(visit(ast.getLeft()).getValue()).add(BigInteger.class.cast(visit(ast.getRight()).getValue())));
            else if (visit(ast.getLeft()).getValue().getClass() == BigDecimal.class && visit(ast.getLeft()).getValue().getClass() == visit(ast.getRight()).getValue().getClass())
                return Environment.create(BigDecimal.class.cast(visit(ast.getLeft()).getValue()).add(BigDecimal.class.cast(visit(ast.getRight()).getValue())));
            else
                throw new RuntimeException("Wrong Operands for +");
        } else if (op.equals("-") || op.equals("*")) {
            if ((visit(ast.getLeft()).getValue().getClass() == BigDecimal.class ||
                    visit(ast.getLeft()).getValue().getClass() == BigInteger.class)
                    && visit(ast.getLeft()).getValue().getClass() == visit(ast.getRight()).getValue().getClass())
            {
                if (visit(ast.getLeft()).getValue().getClass() == BigInteger.class) {
                    if (op.equals("*"))
                        return Environment.create(BigInteger.class.cast(visit(ast.getLeft()).getValue()).multiply(BigInteger.class.cast(visit(ast.getRight()).getValue())));
                    else
                        return Environment.create(BigInteger.class.cast(visit(ast.getLeft()).getValue()).subtract(BigInteger.class.cast(visit(ast.getRight()).getValue())));
                }
                else if (visit(ast.getLeft()).getValue().getClass() == BigDecimal.class) {
                    if (op.equals("*"))
                        return Environment.create(BigDecimal.class.cast(visit(ast.getLeft()).getValue()).multiply(BigDecimal.class.cast(visit(ast.getRight()).getValue())));
                    else
                        return Environment.create(BigDecimal.class.cast(visit(ast.getLeft()).getValue()).subtract(BigDecimal.class.cast(visit(ast.getRight()).getValue())));
                } else
                    throw new RuntimeException("Tried to " + op + " but failed");
            }else
                throw new RuntimeException("Tried to " + op + " but failed");
        } else if (op.equals("/")) {
            if ((visit(ast.getLeft()).getValue().getClass() == BigDecimal.class ||
                    visit(ast.getLeft()).getValue().getClass() == BigInteger.class)
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
                switch (op) {
                    case "<":
                        if (comparison < 0)
                            return Environment.create(Boolean.TRUE);
                        else
                            return Environment.create(Boolean.FALSE);
                    case ">":
                        if (comparison > 0)
                            return Environment.create(Boolean.TRUE);
                        else
                            return Environment.create(Boolean.FALSE);
                }
            }
        }
        throw new RuntimeException("Wrong Operator");
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        return scope.lookupVariable(ast.getName()).getValue();
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
        List<Environment.PlcObject> list = ast.getValues()
                .stream()
                .map(this::visit)
                .collect(Collectors.toList());
        return Environment.create(list);
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
