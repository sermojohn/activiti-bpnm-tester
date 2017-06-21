package gr.iserm.java.activiti.myprocess;

import gr.iserm.java.activiti.BpmnTestUtils;

public class MyProcess {

    public static void main(String[] args) {
        BpmnTestUtils.runProcess("myprocess/MyProcess.bpmn20.xml");
    }

}
