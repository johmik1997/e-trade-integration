package et.medco.etrade.web;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

import et.medco.etrade.domain.Registration;
import et.medco.etrade.service.RegistrationService;

@RestController
@RequestMapping("/api/v1/registrations")
public class RegistrationController {

    private final RegistrationService service;

    RegistrationController(RegistrationService service) {
        this.service = service;
    }

    @GetMapping("/{tin}")
    public ResponseEntity<Registration> byTin(
            @PathVariable String tin,
            @RequestParam(required = false) String lang) {
        Registration registration = service.lookup(tin, lang);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate())
                .body(registration);
    }
}
