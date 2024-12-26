import graphql.execution.instrumentation.NoOpInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.language.Directive;
import graphql.execution.ResultPath;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomInstrumentation extends NoOpInstrumentation {

    @Override
    public InstrumentationContext<ExecutionResult> beginExecuteOperation(InstrumentationExecuteOperationParameters parameters) {
        // 获取 Document (AST) 和操作定义
        Document document = parameters.getExecutionInput().getDocument();
        OperationDefinition operationDefinition = (OperationDefinition) document.getDefinitions().get(0);

        // 获取变量值
        Map<String, Object> variables = parameters.getExecutionInput().getVariables();

        // 收集所有路径
        List<ResultPath> allPaths = new ArrayList<>();
        collectPaths(operationDefinition.getSelectionSet(), ResultPath.rootPath(), variables, allPaths);

        // 打印所有路径
        System.out.println("All DataPaths: " + allPaths);

        return super.beginExecuteOperation(parameters);
    }

    private void collectPaths(SelectionSet selectionSet, ResultPath parentPath, Map<String, Object> variables, List<ResultPath> paths) {
        if (selectionSet == null) return;

        for (Selection<?> selection : selectionSet.getSelections()) {
            if (selection instanceof Field) {
                Field field = (Field) selection;

                // 检查指令是否允许解析此字段
                if (!shouldIncludeField(field, variables)) {
                    continue; // 跳过字段
                }

                // 构造路径
                ResultPath currentPath = parentPath.segment(field.getName());
                paths.add(currentPath);

                // 递归解析子字段
                collectPaths(field.getSelectionSet(), currentPath, variables, paths);
            }
        }
    }

    private boolean shouldIncludeField(Field field, Map<String, Object> variables) {
        if (field.getDirectives() == null) {
            return true; // 如果没有指令，默认包含字段
        }

        for (Directive directive : field.getDirectives()) {
            switch (directive.getName()) {
                case "include":
                    return evaluateDirectiveCondition(directive, variables, true);
                case "skip":
                    return !evaluateDirectiveCondition(directive, variables, false);
            }
        }
        return true; // 如果没有匹配的指令，默认包含字段
    }

    private boolean evaluateDirectiveCondition(Directive directive, Map<String, Object> variables, boolean defaultValue) {
        // 获取指令的 "if" 参数
        return directive.getArguments().stream()
            .filter(arg -> arg.getName().equals("if"))
            .findFirst()
            .map(arg -> {
                Object value = arg.getValueWithVariables(variables);
                return value instanceof Boolean ? (Boolean) value : defaultValue;
            })
            .orElse(defaultValue); // 如果没有 "if" 参数，使用默认值
    }
}
