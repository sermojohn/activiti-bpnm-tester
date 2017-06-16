package gr.iserm.java.activiti;

import com.google.common.collect.Collections2;
import com.google.common.collect.Streams;
import org.activiti.engine.*;
import org.activiti.engine.form.FormData;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.StartFormData;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.EnumFormType;
import org.activiti.engine.impl.form.LongFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;

import javax.xml.transform.stream.StreamSource;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.System.in;
import static java.lang.System.out;

public class BpmnTestUtils {

    public static void runProcess(String BPMN) {
        final ProcessEngine processEngine = getProcessEngine();

        ProcessDefinition processDefinition = getProcessDefinition(processEngine, BPMN);

        try(Scanner scanner = new Scanner(in)) {
            String processInstanceId = startProcess(processEngine.getRuntimeService(), processEngine.getFormService(), processDefinition, scanner);
            while (isProcessRunning(processEngine.getRuntimeService(), processInstanceId)) {
                for (Task task : getOutstandingTasks(processEngine.getTaskService(), processInstanceId)) {
                    executeTask(processEngine, processDefinition, processInstanceId, scanner, task);
                }
            }
        }
    }

    public static void executeTask(ProcessEngine processEngine, ProcessDefinition processDefinition, String processInstanceId, Scanner scanner, Task task) {
        logTask(task);
        FormData formData = processEngine.getFormService().getTaskFormData(task.getId());
        Map<String, Object> userInput = getUserInput(formData, scanner);
        processEngine.getTaskService().complete(task.getId(), userInput);
        printHistory(processDefinition, processEngine.getHistoryService(), processInstanceId);
    }

    public static List<Task> getOutstandingTasks(TaskService taskService, String processInstanceId) {
//        List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup(group).list();
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
//        out.println("Active outstanding tasks: [" + tasks.size() + "]");
        return tasks;
    }

    public static String startProcess(RuntimeService runtimeService, FormService formService, ProcessDefinition processDefinition, Scanner scanner) {
        StartFormData startFormData = formService.getStartFormData(processDefinition.getId());
        boolean startEventWithFormData = startFormData.getFormProperties().size() > 0;

        Map<String, Object> userInput = startEventWithFormData ? getUserInput(startFormData, scanner) : Collections.emptyMap();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinition.getKey(), userInput);

//        out.println("Process started with process instance id [" + processInstance.getProcessInstanceId() + "] key [" + processInstance.getProcessDefinitionKey() + "]");
        return processInstance.getId();
    }


    public static boolean isProcessRunning(RuntimeService runtimeService, String processInstanceId) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        return processInstance != null && !processInstance.isEnded();
    }

    public static ProcessDefinition getProcessDefinition(ProcessEngine processEngine, String bpmnResource) {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource(bpmnResource).deploy();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId()).singleResult();
        out.println("Found process definition [" + processDefinition.getName() + "] with id [" + processDefinition.getId() + "]");
        return processDefinition;
    }

    public static Map<String, Object> getUserInput(FormData formData, Scanner scanner) {
        try {
            Map<String, Object> variables = new HashMap<>();
            for (FormProperty formProperty : formData.getFormProperties()) {
                if (StringFormType.class.isInstance(formProperty.getType())) {
                    out.println(formProperty.getName() + "?");
                    String value = scanner.nextLine();
                    variables.put(formProperty.getId(), value);
                } else if (LongFormType.class.isInstance(formProperty.getType())) {
                    out.println(formProperty.getName() + "? (Must be a whole number)");
                    Long value = Long.valueOf(scanner.nextLine());
                    variables.put(formProperty.getId(), value);
                } else if (DateFormType.class.isInstance(formProperty.getType())) {
                    out.println(formProperty.getName() + "? (Must be a date m/d/yy)");
                    DateFormat dateFormat = new SimpleDateFormat("m/d/yy");
                    Date value = dateFormat.parse(scanner.nextLine());
                    variables.put(formProperty.getId(), value);
                } else if (EnumFormType.class.isInstance(formProperty.getType())) {
                    Map<String, String> values = (Map<String, String>) formProperty.getType().getInformation("values");

                    List<Map.Entry<String, String>> entryList = new ArrayList<>(values.entrySet());
                    Stream<String> objectStream = entryList.stream()
                            .map(Map.Entry::getValue);
                    String valueOptions = Streams.mapWithIndex(objectStream, (String e, long i) -> "(" + i + ")" + e)
                            .collect(Collectors.joining(", "));

                    out.println(formProperty.getName() + "? ["+valueOptions+"]");
                    Integer value = Integer.valueOf(scanner.nextLine());
                    variables.put(formProperty.getId(), entryList.get(value).getKey());
                } else {
                    out.println("<form type not supported>");
                }
            }
            return variables;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void logTask(Task task) {
        out.println("Processing Task [" + task.getName() + "]");
    }

    public static void printHistory(ProcessDefinition processDefinition, HistoryService historyService, String processInstanceId) {
        HistoricActivityInstance endActivity = null;
        List<HistoricActivityInstance> activities =
                historyService.createHistoricActivityInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .orderByHistoricActivityInstanceEndTime().asc()
                        .list();
        for (HistoricActivityInstance activity : activities) {
            if (activity.getActivityType() == "startEvent") {
                out.println("BEGIN " + processDefinition.getName() + " [" + processDefinition.getKey() + "] " + activity.getStartTime());
            }
            if (activity.getActivityType() == "endEvent") {
                // Handle edge case where end step happens so fast that the end step
                // and previous step(s) are sorted the same. So, cache the end step
                //and display it last to represent the logical sequence.
                endActivity = activity;
            } else {
                out.println("-- " + activity.getActivityName() + " [" + activity.getActivityId() + "] " + activity.getDurationInMillis() + " ms");
            }
        }
        if (endActivity != null) {
            out.println("-- " + endActivity.getActivityName() + " [" + endActivity.getActivityId() + "] " + endActivity.getDurationInMillis() + " ms");
            out.println("COMPLETE " + processDefinition.getName() + " [" + processDefinition.getKey() + "] " + endActivity.getEndTime());
        }
    }

    public static ProcessEngine getProcessEngine() {
        ProcessEngineConfiguration cfg = new StandaloneProcessEngineConfiguration()
                .setJdbcUrl("jdbc:h2:mem:activiti;DB_CLOSE_DELAY=1000")
                .setJdbcUsername("sa")
                .setJdbcPassword("")
                .setJdbcDriver("org.h2.Driver")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);


        ProcessEngine processEngine = cfg.buildProcessEngine();
        String pName = processEngine.getName();
        String ver = ProcessEngine.VERSION;
        //out.println("ProcessEngine [" + pName + "] Version: [" + ver + "]");
        return processEngine;
    }
    
}
