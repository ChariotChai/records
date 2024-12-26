import graphql.execution.instrumentation.NoOpInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.language.Directive;

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

        // 收集所有叶子节点路径
        List<String> leafPaths = new ArrayList<>();
        collectLeafPaths(operationDefinition.getSelectionSet(), new ArrayList<>(), variables, leafPaths);

        // 打印所有叶子节点路径
        System.out.println("Leaf DataPaths: " + leafPaths);

        return super.beginExecuteOperation(parameters);
    }

    private void collectLeafPaths(SelectionSet selectionSet, List<String> parentPath, Map<String, Object> variables, List<String> paths) {
        if (selectionSet == null) return;

        for (Selection<?> selection : selectionSet.getSelections()) {
            if (selection instanceof Field) {
                Field field = (Field) selection;

                // 检查指令是否允许解析此字段
                if (!shouldIncludeField(field, variables)) {
                    continue; // 跳过字段
                }

                // 构造当前路径
                List<String> currentPath = new ArrayList<>(parentPath);
                currentPath.add(field.getName());

                // 如果是叶子节点，记录路径
                if (field.getSelectionSet() == null || field.getSelectionSet().getSelections().isEmpty()) {
                    paths.add(String.join("/", currentPath));
                } else {
                    // 递归解析子字段
                    collectLeafPaths(field.getSelectionSet(), currentPath, variables, paths);
                }
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
