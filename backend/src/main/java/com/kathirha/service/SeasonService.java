package com.kathirha.service;

import com.kathirha.domain.Season;
import com.kathirha.repository.SeasonRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SeasonService {

    private final SeasonRepository seasons;

    public SeasonService(SeasonRepository seasons) {
        this.seasons = seasons;
    }

    public Optional<Season> current() {
        return seasons.findFirstByActiveTrueOrderByStartDateDesc();
    }

    public Long currentSeasonId() {
        return current().map(Season::getId).orElse(null);
    }
}
