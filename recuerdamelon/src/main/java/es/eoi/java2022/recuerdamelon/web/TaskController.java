package es.eoi.java2022.recuerdamelon.web;

import es.eoi.java2022.recuerdamelon.data.entity.Community;
import es.eoi.java2022.recuerdamelon.data.entity.User;
import es.eoi.java2022.recuerdamelon.dto.CommunityDTO;
import es.eoi.java2022.recuerdamelon.dto.HorarioDTO;
import es.eoi.java2022.recuerdamelon.dto.TaskDTO;
import es.eoi.java2022.recuerdamelon.service.CommunityService;
import es.eoi.java2022.recuerdamelon.service.TaskService;
import es.eoi.java2022.recuerdamelon.service.TaskTypeService;
import es.eoi.java2022.recuerdamelon.service.UserService;
import es.eoi.java2022.recuerdamelon.utils.DateUtil;
import es.eoi.java2022.recuerdamelon.utils.TaskHorario;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.context.request.WebRequest;

import java.util.*;

@Controller
public class TaskController {

    private final TaskService taskService;

    private  final TaskTypeService taskTypeService;
    private final CommunityService communityService;

    private  final UserService userService;

    public TaskController(TaskService taskService, TaskTypeService taskTypeService, CommunityService communityService, UserService userService) {
        this.taskService = taskService;
        this.taskTypeService = taskTypeService;
        this.communityService = communityService;
        this.userService = userService;
    }

//********************************************    CRUD     *******************************************//
    //             ---------------------------GET Methods-----------------------------         //
    //# READ...
//    @GetMapping("/tasks")
////    @PostAuthorize("hasRole('ROLE_ADMIN') or #model[tasks].ownerId == authentication.principal.id")
//    public String findAll(@RequestParam("page") Optional<Integer> page,
//                          @RequestParam("size") Optional<Integer> size, Model model) {
//        // Convierte parámetros page y size a pageable
//        Pageable pageable = PageRequest.of(page.orElse(1) - 1, size.orElse(10));
//        model.addAttribute("tasks", taskService.findAll(pageable));
//        return "tasks";
//    }

    @GetMapping("/tasks")
    @PostAuthorize("#model[task].userId == authentication.principal.id")
    public String findByUser( ModelMap model) {
        final User user = ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        model.addAttribute("task", this.taskService.findByUser(user));
        return "tasks";
    }

    //# UPDATE % CREATE...
    @GetMapping("/task/{id}/edit")//get de update -create&update-//
    @PostAuthorize("hasRole('ROLE_ADMIN') or #model[task].userId == authentication.principal.id")
    public String update (@PathVariable("id") Integer id, ModelMap model) {
        model.addAttribute("task", this.taskService.findById(id));
        return "task/edit";
    }



      //#CREATE USER TASK
      @GetMapping("/task/create")
      public String getTask(WebRequest request, Model model) {
          TaskDTO taskDTO = new TaskDTO();
          model.addAttribute("task", taskDTO);
          return "task";
      }

   /* @Transactional
    @PostMapping("/task/create")*/
//    public String saveTask(TaskDTO taskDTO, Model model, Errors errors, RedirectAttributes redirectAttributes) {
//        List<Task> allTask = taskService.findAll(Pageable.unpaged());
//        if (allTask.contains(taskService.findByTitle(taskDTO.getTitle()))) {
//            redirectAttributes.addFlashAttribute("errortitle", true);
//            return "redirect:/task"; /*return "redirect:/task/create";*/
//        }
//        else {

//            this.taskRepository.save(taskService.save(taskDTO));
//            return "task"; /*return "redirect:/task/create";*/
//        }
//    }



    //             ---------------------------POST Methods-----------------------------         //

    @Transactional //post transaccional del get de update//
    @PostMapping(value = {"/task/{id}/edit"})
    @PostAuthorize("hasRole('ROLE_ADMIN') or #model[task].userId == authentication.principal.id")
    public String save(TaskDTO dto) {
        return String.format("redirect:/tasks/%s",
                this.taskService.save(dto).getId());
    }

    //# DELETE
    @PostMapping("/task/delete")
    // @PreAuthorize("hasRole('ROLE_ADMIN') or #model[task].userId == authentication.principal.id") //es una `LISTA` de tareas, como asegurar que toda la lista es nuestra?
    public Object deleteById(@RequestParam Integer[] taskid, SessionStatus status, ModelMap model) {
        try {
            for (int i = 0; i < taskid.length; i++) {
                this.taskService.deleteById(taskid[i]);
            }
        } catch (DataIntegrityViolationException e) {
            System.out.println("ERROR AL BORRAR TAREAS" + e.getMessage());
        }
        status.setComplete();//Restablecemos atributos de session tras eliminar y...
        return "redirect:/tasks";//...redirigimos a "/tasks"
    }
        //Horarios en tasck Business
@GetMapping("/business/horario")
    public  String getHorario(WebRequest request, Model model){
    HorarioDTO horarioDTO = new HorarioDTO();
    TaskDTO taskDTO = new TaskDTO();
    CommunityDTO communityDTO = new CommunityDTO();
    final User user = ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal());

    List<Community> equipos = userService.findCommunitiesByUserId(user.getId());
//    horarioDTO.setEquipo(equipos);
    model.addAttribute("task",taskDTO);
    model.addAttribute("horarios",horarioDTO);
    model.addAttribute("equipos", equipos);
    return "NewtasckBusiness";
}
@Transactional
@PostMapping("/business/horario")
public String saveHorario(TaskDTO taskDTO, HorarioDTO horarioDTO){
    final User user = ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    List<String > start = new ArrayList<>();
    for (String date: TaskHorario.starTime(horarioDTO)) {
      start.add(date);
        System.out.println("date = " + date);
    }
    List<String > end = new ArrayList<>();
    for (String date: TaskHorario.endTime(horarioDTO)) {
        end.add(date);
        System.out.println("date = " + date);
    }

    Map<String,String> map = new HashMap<>();
    map = TaskHorario.map(TaskHorario.starTime(horarioDTO), TaskHorario.endTime(horarioDTO));
    for (String descripcion:TaskHorario.horarios(horarioDTO)) {
        for (var entry : map.entrySet()){
                taskDTO.setHorario(true);
                taskDTO.setDescription(descripcion);
                taskDTO.setStartDate( DateUtil.dateToString1(DateUtil.stringToDate1(entry.getKey())));
                taskDTO.setEndDate(DateUtil.dateToString1(DateUtil.stringToDate1( entry.getValue())));
                taskDTO.setDeleted(false);
                taskDTO.setTaskType(taskTypeService.findByName("business"));
                this.taskService.save(taskDTO);
            }
        }

 return "redirect:/tasks";
}
}
