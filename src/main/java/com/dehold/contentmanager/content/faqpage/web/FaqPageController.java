package com.dehold.contentmanager.content.faqpage.web;

import com.dehold.contentmanager.content.faqpage.model.FaqItem;
import com.dehold.contentmanager.content.faqpage.model.FaqPage;
import com.dehold.contentmanager.content.faqpage.repository.FaqPageRepository;
import com.dehold.contentmanager.content.faqpage.web.dto.CreateFaqPageRequest;
import com.dehold.contentmanager.exception.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/faqpages")
public class FaqPageController {

    private final FaqPageRepository repository;

    public FaqPageController(FaqPageRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<FaqPage> createFaqPage(@RequestBody CreateFaqPageRequest request) {
    List<FaqItem> items = request.getFaqItems() == null ? List.of() : request.getFaqItems().stream()
        .map(i -> new FaqItem(i.getTitle(), i.getText()))
                .collect(Collectors.toList());
        FaqPage page = new FaqPage(null, request.getTitle(), request.getIntroduction(), items, null, null, request.getUserId());
        repository.createFaqPage(page);
        // repository.createFaqPage sets id and timestamps if null; fetch the saved
        FaqPage saved = repository.getFaqPage(page.getId()).orElseThrow(() -> EntityNotFoundException.of("FaqPage", page.getId().toString()));
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FaqPage> getFaqPage(@PathVariable UUID id) {
        FaqPage page = repository.getFaqPage(id).orElseThrow(() -> EntityNotFoundException.of("FaqPage", id.toString()));
        return ResponseEntity.ok(page);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<FaqPage>> getFaqPagesByUser(@PathVariable UUID userId) {
        List<FaqPage> pages = repository.getFaqPagesByUserId(userId);
        return ResponseEntity.ok(pages);
    }
}
