package com.householdops.app.household;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.householdops.app.household.HouseholdDtos.CreateHouseholdRequest;
import com.householdops.app.household.HouseholdDtos.HouseholdResponse;
import com.householdops.app.household.HouseholdDtos.UpdateHouseholdRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/households")
@RequiredArgsConstructor
public class HouseholdController {

    private final HouseholdService householdService;

    @GetMapping
    public List<HouseholdResponse> findAll() {
        return householdService.findAll().stream().map(HouseholdResponse::from).toList();
    }

    @GetMapping("/{id}")
    public HouseholdResponse getById(@PathVariable UUID id) {
        return HouseholdResponse.from(householdService.getById(id));
    }

    @PostMapping
    public ResponseEntity<HouseholdResponse> create(@Valid @RequestBody CreateHouseholdRequest request) {
        Household household = householdService.create(request);
        return ResponseEntity.ok(HouseholdResponse.from(household));
    }

    @PatchMapping("/{id}")
    public HouseholdResponse update(@PathVariable UUID id, @RequestBody UpdateHouseholdRequest request) {
        return HouseholdResponse.from(householdService.update(id, request));
    }
}
