package plc.project;

import java.io.PrintWriter;
import java.util.List;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        print("public class Main {");
        indent++;
        if (!ast.getGlobals().isEmpty()) {
            newline(0);
            for (Ast.Global g : ast.getGlobals()) {
                newline(indent);
                visit(g);
            }
        }
        newline(--indent);

        // Java's main method
        newline(++indent);
        print("public static void main(String[] args) {");
        newline(++indent);
        print("System.exit(new Main().main());");
        newline(--indent);
        print("}");

        for (Ast.Function f : ast.getFunctions()) {
            newline(0);
            newline(indent);
            visit(f);
        }

        newline(--indent);
        newline(0);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        if (!ast.getMutable()) print("final ");

        print(ast.getVariable().getType().getJvmName());
        if (ast.getValue().isPresent() && ast.getValue().get() instanceof Ast.Expression.PlcList)
            print("[]");
        print(" ");
        print(ast.getVariable().getJvmName());
        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        print(ast.getFunction().getReturnType().getJvmName());
        print(" ");
        print(ast.getFunction().getJvmName());
        print("(");
        List<String> pNames = ast.getParameters();
        List<String> pTypeNames = ast.getParameterTypeNames();
        for (int i = 0; i < pNames.size(); i++) {
            print(Environment.getType(pTypeNames.get(i)).getJvmName(), " ", pNames.get(i));
            if (i == pNames.size()-1) continue; // gate
            print(", ");
        }
        print(") {");

        indent++;
        List<Ast.Statement> statements = ast.getStatements();
        for (Ast.Statement s : statements) {
            newline(indent);
            visit(s);
        }
        indent--;
        if (!statements.isEmpty()) newline(indent);

        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        print(ast.getVariable().getType().getJvmName(), " ");
        print(ast.getName());
        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        print(ast.getReceiver());
        print(" = ");
        visit(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if (");
        visit(ast.getCondition());
        print(") {");
        indent++;
        for (Ast.Statement s : ast.getThenStatements()) {
            newline(indent);
            visit(s);
        }
        newline(--indent);
        print("}");

        if (ast.getElseStatements().isEmpty()) return null; // gate

        print(" else {");
        indent++;
        for (Ast.Statement s : ast.getElseStatements()) {
            newline(indent);
            visit(s);
        }
        newline(--indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        print("switch (");
        visit(ast.getCondition());
        print(") {");
        indent++;
        for (Ast.Statement.Case sc : ast.getCases()) {
            newline(indent);
            visit(sc);
        }
        newline(--indent);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        if (ast.getValue().isEmpty())
            print("default:");
        else
        {
            print("case ");
            visit(ast.getValue().get());
            print(":");
        }

        indent++;
        for (Ast.Statement s : ast.getStatements()) {
            newline(indent);
            visit(s);
        }

        --indent;

        if (ast.getValue().isEmpty()) return null; // gate

        newline(indent+1);
        print("break;");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (");
        visit(ast.getCondition());
        print(") {");
        indent++;
        List<Ast.Statement> statements = ast.getStatements();
        for (Ast.Statement s : statements) {
            newline(indent);
            visit(s);
        }
        indent--;
        if (!statements.isEmpty()) newline(indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return ");
        visit(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) { // TODO use bigDecimal string for better precision. this is enough for now
        Object lit = ast.getLiteral();
        if (lit instanceof String)
            print("\"", lit, "\"");
        else if (lit instanceof Character)
            print("'", lit, "'");
        else if (ast.getType() == Environment.Type.NIL)
            print("null");
        else
            print(lit);
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(");
        visit(ast.getExpression());
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        if (ast.getOperator().equals("^")) {
            print("Math.pow(", ast.getLeft(), ", ",  ast.getRight(), ")");
            return null;
        }

        visit(ast.getLeft());
        print(" ", ast.getOperator(), " ");
        visit(ast.getRight());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        print(ast.getVariable().getJvmName());
        if (ast.getOffset().isPresent())
            print("[", ast.getOffset().get(), "]");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        print(ast.getFunction().getJvmName(), "(");
        for (int i = 0; i < ast.getArguments().size(); i++){
            Ast.Expression exp = ast.getArguments().get(i);
            visit(exp);
            if (i != ast.getArguments().size() - 1) {
                print(", ");
            }
        }
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) { // case where there's no values?
        List<Ast.Expression> values = ast.getValues();
        print("{");
        for (int i = 0; i < values.size(); i++) {
            Ast.Expression e = values.get(i);
            visit(e);
            if (i == values.size()-1) continue;
            print(", ");
        }
        print("}");
        return null;
    }

}
