package com.clockwork.scheduler.controller.admin;

import com.clockwork.scheduler.domain.Workflow;
import com.clockwork.scheduler.domain.WorkflowStep;
import com.clockwork.scheduler.repository.WorkflowRepository;
import com.clockwork.scheduler.repository.WorkflowStepRepository;
import com.clockwork.scheduler.workflow.StatementWorkflowService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/admin/workflows")
public class AdminWorkflowsController {

    private final WorkflowRepository workflowRepository;
    private final WorkflowStepRepository workflowStepRepository;
    private final StatementWorkflowService statementWorkflowService;

    public AdminWorkflowsController(WorkflowRepository workflowRepository,
                                    WorkflowStepRepository workflowStepRepository,
                                    StatementWorkflowService statementWorkflowService) {
        this.workflowRepository = workflowRepository;
        this.workflowStepRepository = workflowStepRepository;
        this.statementWorkflowService = statementWorkflowService;
    }

    @GetMapping
    public String listWorkflows(Model model) {
        List<Workflow> workflows = workflowRepository.findAll();
        Map<UUID, List<WorkflowStep>> stepsByWorkflow = new HashMap<>();
        for (Workflow wf : workflows) {
            stepsByWorkflow.put(wf.getId(), workflowStepRepository.findByWorkflowIdOrderByStepOrderAsc(wf.getId()));
        }

        model.addAttribute("workflows", workflows);
        model.addAttribute("stepsByWorkflow", stepsByWorkflow);
        return "admin/workflows";
    }

    @PostMapping("/create")
    public String createWorkflow(@RequestParam(defaultValue = "Kenyan SME Statement Run") String name,
                                 @RequestParam(defaultValue = "CUST-NAIROBI-01") String customerId,
                                 @RequestParam(defaultValue = "finance@sme.co.ke") String email,
                                 @RequestParam(defaultValue = "+254712345678") String phone,
                                 RedirectAttributes redirectAttributes) {
        Workflow wf = statementWorkflowService.createDefaultStatementWorkflow(customerId, email, phone);
        wf.setName(name);
        workflowRepository.save(wf);

        redirectAttributes.addFlashAttribute("successMessage", "Workflow created: " + wf.getName());
        return "redirect:/admin/workflows";
    }

    @PostMapping("/{id}/start")
    public String startWorkflow(@PathVariable UUID id,
                                @RequestParam(required = false) String simulateFailureAtStep,
                                RedirectAttributes redirectAttributes) {
        try {
            Workflow updated = statementWorkflowService.executeWorkflow(id, simulateFailureAtStep);
            redirectAttributes.addFlashAttribute("successMessage", "Workflow completed with state: " + updated.getCurrentState());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Workflow execution halted: " + e.getMessage());
        }
        return "redirect:/admin/workflows";
    }

    @PostMapping("/{id}/retry")
    public String retryWorkflow(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            Workflow updated = statementWorkflowService.retryWorkflow(id);
            redirectAttributes.addFlashAttribute("successMessage", "Workflow retried and reached state: " + updated.getCurrentState());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Workflow retry failed: " + e.getMessage());
        }
        return "redirect:/admin/workflows";
    }
}
