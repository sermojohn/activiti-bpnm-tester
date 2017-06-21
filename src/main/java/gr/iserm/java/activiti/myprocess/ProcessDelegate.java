package gr.iserm.java.activiti.myprocess;

import org.activiti.engine.delegate.DelegateExecution;

import java.util.Random;

public class ProcessDelegate extends DebugDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        super.execute(execution);
        execution.setVariable("ok", new Random().nextDouble() > 0.5d);
    }
}
