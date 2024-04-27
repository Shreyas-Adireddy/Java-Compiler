package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", List.of(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        for (Ast.Global global : ast.getGlobals()) {
            visit(global);
        }
        for (Ast.Function function : ast.getFunctions()) {
            visit(function);
        }
        if (scope.lookupFunction("main", 0).getReturnType().equals(Environment.Type.INTEGER)) {
            return null;
        }
        throw new RuntimeException("Main method must return integer");
    }

    @Override
    public Void visit(Ast.Global ast) {
        Optional<Ast.Expression> value = ast.getValue();
        if (value.isPresent()){
            if (value.get() instanceof Ast.Expression.PlcList) {
                ((Ast.Expression.PlcList) value.get()).setType(Environment.getType(ast.getTypeName()));
            }
            visit(value.get());
            requireAssignable(Environment.getType(ast.getTypeName()), value.get().getType());
        }
        Environment.Variable variable = scope.defineVariable(ast.getName(),
                        ast.getName(),
                        Environment.getType(ast.getTypeName()),
                        ast.getMutable(),
                        Environment.NIL);
        ast.setVariable(variable);
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        List<Environment.Type> argsTypes = new ArrayList<>();
        for (String string : ast.getParameterTypeNames())
            argsTypes.add(Environment.getType(string));

        Optional<String> returnType = ast.getReturnTypeName();
        Environment.Function func = null;
        func = returnType.map(s -> scope.defineFunction(ast.getName(),
                ast.getName(),
                argsTypes,
                Environment.getType(s),
                args -> Environment.NIL)).orElseGet(() -> scope.defineFunction(ast.getName(),
                ast.getName(),
                argsTypes,
                Environment.getType("Nil"),
                args -> Environment.NIL));

        ast.setFunction(func);
        scope = new Scope(scope);
        for (int i = 0; i < argsTypes.size(); i++) {
            scope.defineVariable(ast.getParameters().get(i),
                    ast.getParameters().get(i),
                    Environment.getType(ast.getParameterTypeNames().get(i)),
                    true,
                    Environment.NIL);
        }
        function = ast;
        for (Ast.Statement statement: ast.getStatements())
            visit(statement);
        function = null;
        scope = scope.getParent();
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        if (ast.getExpression() instanceof Ast.Expression.Function) {
            visit(ast.getExpression());
            return null;
        }
        throw new RuntimeException("Expected Function");
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        Environment.Type type = null;
        if (ast.getTypeName().isPresent()){
            type = Environment.getType(ast.getTypeName().get());
            if (ast.getValue().isPresent()){
                visit(ast.getValue().get());
                requireAssignable(type, ast.getValue().get().getType());
            }
        } else {
            if (ast.getValue().isEmpty())
                throw new RuntimeException("Variable Value missing");
            visit(ast.getValue().get());
            type = ast.getValue().get().getType();
        }
        Environment.Variable variable = scope.defineVariable(ast.getName(), ast.getName(), type, true, Environment.NIL);
        ast.setVariable(variable);
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        if (ast.getReceiver()instanceof Ast.Expression.Access){
            visit(ast.getReceiver());
            visit(ast.getValue());
            requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());
            return null;
        }
        throw new RuntimeException("Not able to assign");
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        visit(ast.getCondition());
        if (!ast.getCondition().getType().equals(Environment.Type.BOOLEAN)) {
            throw new RuntimeException("Condition not a boolean.");
        }
        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("THEN block empty");
        }
        scope = new Scope(scope);
        ast.getThenStatements().forEach(this::visit);
        scope = scope.getParent();

        scope = new Scope(scope);
        ast.getElseStatements().forEach(this::visit);
        scope = scope.getParent();

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        visit(ast.getCondition());

        List<Ast.Statement.Case> cases = ast.getCases();
        for (int i = 0; i < cases.size(); i++) {
            if (cases.get(i).getValue().isPresent()) {
                if (i == cases.size() - 1) {
                    throw new RuntimeException("Default case cannot have condition");
                }
                visit(cases.get(i).getValue().get());
                if (cases.get(i).getValue().get().getType().equals(ast.getCondition().getType())) {

                } else {
                    throw new RuntimeException("Condition and case type must match in a switch statement.");
                }

            }
            visit(cases.get(i));
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        scope = new Scope(scope);
        ast.getStatements().forEach(this::visit);
        scope = scope.getParent();
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());
        if (!ast.getCondition().getType().equals(Environment.Type.BOOLEAN))
            throw new RuntimeException("Invalid condition in WHILE statement.");

        scope = new Scope(scope);
        ast.getStatements().forEach(this::visit);
        scope = scope.getParent();

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        visit(ast.getValue());
        requireAssignable(Environment.getType(function.getReturnTypeName().get()), ast.getValue().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if (ast.getLiteral() instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
        }
        else if (ast.getLiteral() instanceof String) {
            ast.setType(Environment.Type.STRING);
        }
        else if (ast.getLiteral() instanceof BigInteger integer) {
            if (integer.toByteArray().length <= 4){
                ast.setType(Environment.Type.INTEGER);
            }else{
                throw new RuntimeException("Too big integer");
            }
        } else if (ast.getLiteral() instanceof BigDecimal decimal) {
            if ((decimal.doubleValue() > Double.MAX_VALUE) || (decimal.doubleValue() < Double.MIN_VALUE)) {
                throw new RuntimeException("Too big decimal");
            } else {
                ast.setType(Environment.Type.DECIMAL);
            }
        } else {
            ast.setType(Environment.Type.NIL);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        if (!(ast.getExpression() instanceof Ast.Expression.Binary))
            throw new RuntimeException("The grouped expression is not binary.");

        visit(ast.getExpression());
        ast.setType(ast.getExpression().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        String op = (ast.getOperator());
        Ast.Expression left = ast.getLeft(), right = ast.getRight();
        visit(left);
        visit(right);
        if (op.equals("&&") || op.equals("||")){
            if (left.getType().equals(Environment.Type.BOOLEAN) &&
                    right.getType().equals(Environment.Type.BOOLEAN)) {
                ast.setType(Environment.Type.BOOLEAN);
            } else {
                throw new RuntimeException("Left or Right not a boolean");
            }
        } else if (op.equals("<") || op.equals(">") || op.equals("==") || op.equals("!=")) {
            requireAssignable(Environment.Type.COMPARABLE, left.getType());
            requireAssignable(Environment.Type.COMPARABLE, right.getType());
            if (left.getType().equals(right.getType()))
                ast.setType(Environment.Type.BOOLEAN);
            else
                throw new RuntimeException("Left or Right types don't match");
        } else if (op.equals("+")) {
            if (left.getType().equals(Environment.Type.STRING)
                    || right.getType().equals(Environment.Type.STRING)){
                ast.setType(Environment.Type.STRING);
            }
            else if (left.getType().equals(Environment.Type.INTEGER)
                    && left.getType().equals(right.getType())){
                ast.setType(Environment.Type.INTEGER);
            }
            else if (left.getType().equals(Environment.Type.DECIMAL)
                    && left.getType().equals(right.getType())){
                ast.setType(Environment.Type.DECIMAL);
            }
            else {
                throw new RuntimeException("+ is supported for those types");
            }
        } else if (op.equals("-") || op.equals("/") || op.equals("*")) {
            if (left.getType().equals(Environment.Type.INTEGER)
                    && left.getType().equals(right.getType())){
                ast.setType(Environment.Type.INTEGER);
            }
            else if (left.getType().equals(Environment.Type.DECIMAL)
                    && left.getType().equals(right.getType())){
                ast.setType(Environment.Type.DECIMAL);
            }
            else {
                throw new RuntimeException("+ is supported for those types");
            }
        } else if (op.equals("^")) {
            if (left.getType().equals(Environment.Type.INTEGER)
                    && left.getType().equals(right.getType())){
                ast.setType(Environment.Type.INTEGER);
            } else {
                throw new RuntimeException("+ is supported for those types");
            }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        if (ast.getOffset().isPresent()){
            Ast.Expression offset = ast.getOffset().get();
            visit(offset);
            if (!(offset.getType().equals(Environment.Type.INTEGER))){
                throw new RuntimeException("Offset must be an integer");
            }
        }
        ast.setVariable(scope.lookupVariable(ast.getName()));
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        ast.setFunction(scope.lookupFunction(ast.getName(), ast.getArguments().size()));
        List<Ast.Expression> args = ast.getArguments();
        List<Environment.Type> types = ast.getFunction().getParameterTypes();
        for (int i = 0; i < args.size(); i++) {
            Ast.Expression arg = args.get(i);
            Environment.Type param = types.get(i);

            visit(arg);
            requireAssignable(param, arg.getType());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        List<Ast.Expression> list = ast.getValues();
        list.forEach(expr -> {
            visit(expr);
            requireAssignable(ast.getType(), expr.getType());
        });
        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (target.equals(type) || target.equals(Environment.Type.ANY))
            return;
        if (target.equals(Environment.Type.COMPARABLE)
                && (type.getName().equals("Integer")
                || type.getName().equals("Decimal")
                || type.getName().equals("Character")
                || type.getName().equals("String"))) {
                return;
        }
        throw new RuntimeException("Types don't match");
    }

}
