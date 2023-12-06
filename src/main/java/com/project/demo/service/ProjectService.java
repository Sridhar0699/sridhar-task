package com.project.demo.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.project.demo.model.Project;
import com.project.demo.model.Task;
import com.project.demo.pojo.ProjectPojo;
import com.project.demo.pojo.ResponseHandler;
import com.project.demo.pojo.SubProjects;
import com.project.demo.pojo.TaskPojo;
import com.project.demo.repository.ProjectRepository;
import com.project.demo.repository.TaskRepository;

@Service
public class ProjectService implements ProjectServiceInter{

  @Autowired
  private ProjectRepository projectRepository;
  @Autowired
  private TaskRepository repository;

  public ResponseHandler getResponse(String message, int status, int errorCode, Object o)  {
    ResponseHandler handler = new ResponseHandler();
    Map<String, Object> response = handler.getResponse();
    response.put("data", o);
    handler.setErrorCode(errorCode);
    handler.setMessage(message);
    handler.setStatusCode(status);

    return handler;
  }

  public ResponseHandler createProjectpojo(ProjectPojo pojo) {

    Project project = projectRepository.findByName(pojo.getName());
    ResponseHandler responseHandler = new ResponseHandler();
    Project pro = new Project();
    if (project == null) {
      try {

       
        BeanUtils.copyProperties(pojo, pro);
        List<SubProjects> subProjects =null;
        if( pojo.getSubProjects()!=null)
        {
        	subProjects=pojo.getSubProjects();
        for (SubProjects subPro : subProjects) {
        	
          List<TaskPojo> taskPojos = subPro.getTask();
          List<Task> taskList = new ArrayList<>();
          for (TaskPojo taskPojo : taskPojos) {
            Task task = new Task();
            BeanUtils.copyProperties(taskPojo, task);
            task.setProject(pro);
            taskList.add(task);

          }
          pro.setTasks(taskList);
        }
        }
        else
        {
        	pro.setTasks(null);
        }
        project = projectRepository.save(pro);
        responseHandler = getResponse("Project created successfully", 0, 0, project);
        BeanUtils.copyProperties(project, pojo);

      } catch (Exception e) {
        responseHandler = getResponse("Something went wrong", 1, 1, project);
      }

    } else {

      responseHandler = getResponse("Project Already exist", 1, 1, project);

    }
   

    return responseHandler;
  }

  public ResponseHandler getProjectByName(String pName) {
    
    Project findAll = projectRepository.findByName(pName);
    ResponseHandler handler=new ResponseHandler();
    try {
    	ProjectPojo pPojo = new ProjectPojo();
        BeanUtils.copyProperties(findAll, pPojo);
        List<SubProjects> list = new ArrayList<SubProjects>();
        List<Task> task = findAll.getTasks();
        for (Task tasks : task) {
          SubProjects childProjects = new SubProjects();
          BeanUtils.copyProperties(tasks, childProjects);
          childProjects.setProjectId(findAll.getId());
          childProjects.setCheckType("Task");
          list.add(childProjects);

        }

        extracted(pPojo, list);
        pPojo.setSubProjects(list);
        handler=getResponse("Project by name fetched successfully", 0, 0, pPojo);
		
	} catch (Exception e) {
		 handler=getResponse("Project by name Not found", 1, 1, new ProjectPojo());
	}
    
    return handler;
  }

  public void extracted(ProjectPojo pPojo, List<SubProjects> list) {
    List<Project> findChild = projectRepository.findByProjectId(pPojo.getId());
    
    if (findChild.size() != 0) {
      for (Project p : findChild) {
        SubProjects childProjects = new SubProjects();
        BeanUtils.copyProperties(p, childProjects);
        childProjects.setProjectId(p.getProjectId());
        childProjects.setCheckType("Child Project ");
        list.add(childProjects);
//        List<Task> listOfTasks = p.getTasks();
//        List<TaskPojo> taskPojos = new ArrayList<TaskPojo>();
//        List<SubProjects> task3=new ArrayList<SubProjects>();
//        
//        
       
//        
//        listOfTasks.stream().forEach(task2 -> {
//          TaskPojo pojo = new TaskPojo();
//          task2.setProject(p);
//          BeanUtils.copyProperties(task2, pojo);
//          childProjects.setTask(taskPojos);
////          childProjects.add(taskPojos);
//
//    });
//     childProjects.setTask(taskPojos);
      }
    }
  }

  public ResponseHandler getProjectById(int projectId) {

    Project findById = projectRepository.findById(projectId);
    ResponseHandler responseHandler = new ResponseHandler();
   
    if (findById != null) {
      try {

        ProjectPojo pojo = new ProjectPojo();
        BeanUtils.copyProperties(findById, pojo);
        List<SubProjects> objects = new ArrayList<SubProjects>();
        List<Task> tasks = findById.getTasks();
        for (Task t : tasks) {
          SubProjects childProjects = new SubProjects();
          BeanUtils.copyProperties(t, childProjects);
          childProjects.setCheckType("Parent-Task");
          objects.add(childProjects);
        }
        pojo.setSubProjects(objects);
        responseHandler = getResponse("Project fetched -successfully", 0, 0, pojo);

      } catch (Exception e) {
        responseHandler = getResponse("Something went wrong", 1, 1, new ProjectPojo());
      }

    } else {

      responseHandler = getResponse("Invalid Project id", 1, 1, new ProjectPojo());
    }

    return responseHandler;

  }

  public ResponseHandler getAllParentChildTasks() {

    ResponseHandler responseHandler = new ResponseHandler();
    List<Project> projects = projectRepository.findAll();

    List<ProjectPojo> listProject = new ArrayList<>();
    Map<Integer, ProjectPojo> mapParent = new HashMap<>();
    try {

      for (Project project : projects) {

        if (project.getProjectId() == 0) {

          ProjectPojo parentPojo = new ProjectPojo();

          BeanUtils.copyProperties(project, parentPojo);

          List<TaskPojo> tasksFound = getTasksByProjectId(project);
          List<SubProjects> listOfSub = new ArrayList<>();
          for (TaskPojo task : tasksFound) {
            SubProjects pro = new SubProjects();
            BeanUtils.copyProperties(task, pro);
            pro.setCheckType("Parent-Task");
            listOfSub.add(pro);
          }

          parentPojo.setSubProjects(listOfSub);

          mapParent.put(project.getId(), parentPojo);

        }
      }

      List<SubProjects> listOFChild = new ArrayList<>();

      List<TaskPojo> listOfTask = new ArrayList<>();

      for (Project project : projects) {
        if (project.getProjectId() != 0) {


        	SubProjects childProject = new SubProjects();
          childProject.setProjectId(project.getProjectId());
          List<TaskPojo> tasksFound = getTasksByProjectId(project);
          System.out.println(tasksFound);
          listOfTask.addAll(tasksFound);
          childProject.setTask(tasksFound);
          BeanUtils.copyProperties(project, childProject);
          childProject.setCheckType("Child Project");
          
          ProjectPojo parentPojo = mapParent.get(project.getProjectId());

          if (parentPojo != null) {
            List<SubProjects> childProjects = parentPojo.getSubProjects();
            if (childProjects == null) {
              childProjects = new ArrayList<>();

              parentPojo.setSubProjects(childProjects);

            }

            childProjects.add(childProject);

            listOFChild.add(childProject);

          }
        }
      }

      for (Map.Entry<Integer, ProjectPojo> pp : mapParent.entrySet()) {
        ProjectPojo value = pp.getValue();
        List<SubProjects> listOfChild = new ArrayList<>();

        List<SubProjects> subProjects = value.getSubProjects();
        listOfChild.addAll(subProjects);

        for (SubProjects cp : listOFChild) {

          if (value.getId() == cp.getProjectId()) {

            boolean isChildProjectExists = listOfChild.stream().anyMatch(obj -> obj instanceof SubProjects
                && ((SubProjects) obj).getProjectId() == cp.getProjectId());

            if (!isChildProjectExists) {
              SubProjects childP = new SubProjects();
              BeanUtils.copyProperties(cp, childP);
              listOfChild.add(childP);
            }

          }
        }

        value.setSubProjects(listOfChild);
        listProject.add(value);
         responseHandler = getResponse("Project with childs and tasks fetched successfully", 0, 0, listProject);

      }

    } catch (Exception e) {
      // TODO: handle exception
      responseHandler = getResponse("Something went wrong while fecthing projetcs", 1, 1, new ProjectPojo());
    }

    return responseHandler;
  }

  public List<TaskPojo> getTasksByProjectId(Project project) {
    List<TaskPojo> taskPojos = new ArrayList<>();

    try {
      List<Task> findByProject = repository.findByProject(project);

      if (!findByProject.isEmpty()) {
        for (Task task : findByProject) {
          TaskPojo taskPojo = new TaskPojo();
          BeanUtils.copyProperties(task, taskPojo);
          taskPojo.setProjectId(project.getId());
          taskPojo.setCheckType("Parent-Task");
          taskPojos.add(taskPojo);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return taskPojos;
  }
  
  public ResponseHandler getAllParentProject() {
    
    List<Project> projects = projectRepository.findByProjectId(0);
    String message="Parent-project";
    ResponseHandler handler = getProjectDetails(projects,message);
    return handler;
  }

  public ResponseHandler getProjectDetails(List<Project> projects,String message) {
    
    ResponseHandler responseHandler = new ResponseHandler();
    List<ProjectPojo> listProject = new ArrayList<>();
    Map<Integer, ProjectPojo> mapParent = new HashMap<>();
    try {
      for (Project project : projects) {
        

          ProjectPojo parentPojo = new ProjectPojo();

          BeanUtils.copyProperties(project, parentPojo);

          mapParent.put(project.getId(), parentPojo);
          listProject.add(parentPojo);

        
      }
      
      responseHandler = getResponse( "Projects fetched succesfully", 0, 0, listProject);

      
    }
    catch (Exception e) {
      // TODO: handle exception
      message="Something went wrong";
      responseHandler = getResponse(message, 1, 1, new ProjectPojo());
    }

    return responseHandler;
  }

  public ResponseHandler getAllChildProjects() {
	ResponseHandler responseHandler = new ResponseHandler();
    List<Project> projects = projectRepository.findByProjectIdNot(0);
    String message="Child Project";
     responseHandler = getProjectDetails(projects,message);
    return responseHandler;
  }

  public ResponseHandler createTasksBasedOnProjectId(TaskPojo pojo, int projectId) {
    Project pId = projectRepository.findById(projectId);
    ResponseHandler handler=new ResponseHandler();
    try {
      Task tasks = new Task();
      BeanUtils.copyProperties(pojo, tasks);
      tasks.setProject(pId);
      Task save = repository.save(tasks);
      BeanUtils.copyProperties(save, pojo);
      pojo.setCheckType("Task");
      handler=getResponse("Task created successfully", 0, 0, pojo);

    } catch (Exception e) {
      System.out.println(e);
      
      handler = getResponse("Something went wrong", 1, 1, new TaskPojo());
    }
    return handler;

  }
  
  public ResponseHandler getAll(){
	  List<Project> findAll = projectRepository.findAll();
	 ResponseHandler handler = getProjectDetails(findAll,"");
	  return handler;
  }
  
  public ResponseHandler getParentTasks() {
	  List<Project> projects = projectRepository.findByProjectId(0);
	    List<TaskPojo> parentTasks = new ArrayList<>();

	    try {
	        for (Project project : projects) {
	            List<TaskPojo> tasks = getTasksByProjectId(project);

	            for (TaskPojo task : tasks) {
	                if ("Parent-Task".equals(task.getCheckType())) {
	                    parentTasks.add(task);
	                }
	            }
	        }

	        ResponseHandler responseHandler = getResponse("Parent tasks fetched successfully", 0, 0, parentTasks);
	        return responseHandler;
	    } catch (Exception e) {
	        ResponseHandler responseHandler = getResponse("Something went wrong", 1, 1, new ArrayList<>());
	        return responseHandler;
	    }
	}
  
  public ResponseHandler getParentTasksByProjectId(int projectId) {
	    Project project = projectRepository.findById(projectId);

	    if (project == null || project.getProjectId() != 0) {
	        return getResponse("Invalid project or not a parent project", 1, 1, new ArrayList<>());
	    }

	    List<TaskPojo> parentTasks = new ArrayList<>();

	    try {
	        List<TaskPojo> tasks = getTasksByProjectId(project);

	        for (TaskPojo task : tasks) {
	            if ("Parent-Task".equals(task.getCheckType())) {
	            	task.setCheckType("Task for "+project.getName());

	                parentTasks.add(task);
	            }
	        }

	        ResponseHandler responseHandler = getResponse("Parent tasks fetched successfully", 0, 0, parentTasks);
	        return responseHandler;
	    } catch (Exception e) {
	        ResponseHandler responseHandler = getResponse("Something went wrong", 1, 1, new ArrayList<>());
	        return responseHandler;
	    }
	}
  
  public ResponseHandler getChildTasksByProjectId(int projectId) {
	    Project project = projectRepository.findById(projectId);

	    if (project == null || project.getProjectId() == 0) {
	        return getResponse("Invalid project or not a Child project", 1, 1, new ArrayList<>());
	    }

	    List<TaskPojo> parentTasks = new ArrayList<>();

	    try {
	        List<TaskPojo> tasks = getTasksByProjectId(project);

	        for (TaskPojo task : tasks) {
	            if ("Parent-Task".equals(task.getCheckType())) {
	            	task.setCheckType("Task for "+project.getName());
	                parentTasks.add(task);
	            }
	        }

	        ResponseHandler responseHandler = getResponse("Child tasks fetched successfully", 0, 0, parentTasks);
	        return responseHandler;
	    } catch (Exception e) {
	        ResponseHandler responseHandler = getResponse("Something went wrong", 1, 1, new ArrayList<>());
	        return responseHandler;
	    }
	}
  
  public ResponseHandler updateProject( ProjectPojo projectPojo) {
	  ResponseHandler handler=new ResponseHandler();
	    try {
	       
	        Project existingProject = projectRepository.findByName(projectPojo.getName());

	        if (existingProject != null) {
	            existingProject.setName(projectPojo.getName());
	            existingProject.setType(projectPojo.getType());
	            existingProject.setDescription(projectPojo.getDescription());
	            existingProject.setStatus(projectPojo.getStatus());
	            existingProject.setStartDate(projectPojo.getStartDate());
	            existingProject.setEndDate(projectPojo.getEndDate());
	            existingProject.setHours(projectPojo.getHours());
	            existingProject.setLead(projectPojo.getLead());
	           
	            existingProject = projectRepository.save(existingProject);
	            
	            handler = getResponse("Project updated successfully", 0,0, existingProject);
	        }
	        else {
	            handler = getResponse("Project not found", 1,1, null);
	        }
	    } 
	    
	    catch (Exception e)
	    {
	        e.printStackTrace();
	    }
	    return handler;
	}
  
  public ResponseHandler update(TaskPojo pojo) {
	 ResponseHandler handler=new ResponseHandler();
	 try {
		
		Task findByName = repository.findByName(pojo.getName());
		if(findByName!=null) {
			findByName.setType(pojo.getType());
			findByName.setDescription(pojo.getDescription());
			findByName.setHours(pojo.getHours());
			findByName.setStartDate(pojo.getStartDate());
			findByName.setEndDate(pojo.getEndDate());
			findByName.setStatus(pojo.getStatus());
			findByName.setAssignee(pojo.getAssignee());
			
			Task save = repository.save(findByName);
			handler=getResponse("Updated", 0, 0, save);
		}
		else {
			handler=getResponse("Error", 1, 1, null);
		}
			
	}
		 catch (Exception e) {
		e.printStackTrace();
	}
	 
		
	 
	 return handler;
 }
  
  public ResponseHandler getParentTasksByName(String name) {
	    Task project = repository.findByName(name);

	    if (project == null ) {
	        return getResponse("Invalid project or not a parent project", 1, 1, new ArrayList<>());
	    }

	  
	    try {
               TaskPojo pojo=new TaskPojo();
               BeanUtils.copyProperties(project, pojo);

	        ResponseHandler responseHandler = getResponse("Parent tasks fetched successfully", 0, 0, pojo);
	        return responseHandler;
	    } catch (Exception e) {
	        ResponseHandler responseHandler = getResponse("Something went wrong", 1, 1, null);
	        return responseHandler;
	    }
	}

}