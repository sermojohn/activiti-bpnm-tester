package gr.iserm.java.activiti.vacation;


import gr.iserm.java.activiti.BpmnTestUtils;

public class VacationProcess {

    public static void main(String[] args) {
        BpmnTestUtils.runProcess("vacation/VacationRequest.bpmn20.xml");
    }

}
