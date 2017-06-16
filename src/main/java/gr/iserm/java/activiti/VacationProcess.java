package gr.iserm.java.activiti;


public class VacationProcess {

    public static void main(String[] args) {
        BpmnTestUtils.runProcess("VacationRequest.bpmn20.xml");
    }

}
