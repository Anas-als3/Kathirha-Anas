package com.kathirha.web;

import com.kathirha.domain.Season;
import com.kathirha.service.SeasonService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/seasons")
public class SeasonController {

    private final SeasonService seasons;

    public SeasonController(SeasonService seasons) {
        this.seasons = seasons;
    }

    @GetMapping("/current")
    public Map<String, Object> current() {
        Map<String, Object> out = new LinkedHashMap<>();
        seasons.current().ifPresentOrElse(s -> {
            out.put("active", true);
            out.put("id", s.getId());
            out.put("name", s.getName());
            out.put("theme", s.getTheme());
            out.put("description", s.getDescription());
            out.put("startDate", s.getStartDate() == null ? null : s.getStartDate().toString());
            out.put("endDate", s.getEndDate() == null ? null : s.getEndDate().toString());
        }, () -> out.put("active", false));
        return out;
    }
}
