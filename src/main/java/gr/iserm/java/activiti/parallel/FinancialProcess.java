package gr.iserm.java.activiti.parallel;

import gr.iserm.java.activiti.BpmnTestUtils;

public class FinancialProcess {

    public static void main(String[] args) {
        BpmnTestUtils.runProcess("parallel/FinancialReportProcess.bpmn20.xml");
    }

}
