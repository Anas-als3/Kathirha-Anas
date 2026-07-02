package com.kathirha.web;

import com.kathirha.service.DemoService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Public Judge Demo Mode endpoints — no auth required so judges can click "Instant Demo". */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final DemoService demo;

    public DemoController(DemoService demo) {
        this.demo = demo;
    }

    @PostMapping("/seed")
    public Map<String, Object> seed(@RequestParam(required = false) String phone) {
        return demo.seedScenario(phone);
    }

    @PostMapping("/reset")
    public Map<String, Object> reset(@RequestParam(required = false) String phone) {
        demo.reset(phone);
        return Map.of("status", "reset", "phone", phone == null ? DemoService.DEFAULT_DEMO_PHONE : phone);
    }
}
