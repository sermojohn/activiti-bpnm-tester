package gr.iserm.java.activiti.myprocess;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

import java.util.Map;

public class DebugDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Map<String, Object> variables = execution.getVariables();
        System.out.println("DEBUG "+execution.getCurrentActivityName());
        variables.entrySet()
                .forEach(entry -> System.out.println(entry.getKey()+"\t\t"+entry.getValue()));
    }
}
