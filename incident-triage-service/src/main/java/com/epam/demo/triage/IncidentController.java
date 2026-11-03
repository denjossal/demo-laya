package com.epam.demo.triage;

import com.epam.demo.triage.dto.Incident;
import com.epam.demo.triage.dto.TriageResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IncidentController {

    private final IncidentTriageService incidentTriageService;

    public IncidentController(IncidentTriageService incidentTriageService) {
        this.incidentTriageService = incidentTriageService;
    }

    @PostMapping("/incidents")
    public TriageResult triage(@RequestBody Incident incident) {
        return incidentTriageService.triage(incident.text());
    }
}
