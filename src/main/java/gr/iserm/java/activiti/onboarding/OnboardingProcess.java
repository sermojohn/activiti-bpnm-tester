package gr.iserm.java.activiti.onboarding;

import static gr.iserm.java.activiti.BpmnTestUtils.runProcess;

public class OnboardingProcess {

    static final String BPMN = "onboarding/onboarding.bpmn20.xml";

    public static void main(String[] args) throws Exception {
        runProcess(BPMN);
    }
}
