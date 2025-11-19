package com.dehold.contentmanager.content.generic.web;

import com.dehold.contentmanager.content.generic.model.GenericContentModel;
import com.dehold.contentmanager.content.generic.service.GenericContentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/content")
public class GenericContentController {

    private final GenericContentService genericContentService;

    public GenericContentController(GenericContentService genericContentService) {
        this.genericContentService = genericContentService;
    }

    @PostMapping
    public ResponseEntity<GenericContentModel> createContent(@RequestBody GenericContentModel content) {
        content.setId(UUID.randomUUID());
        content.setCreatedAt(Instant.now());
        content.setUpdatedAt(Instant.now());
        GenericContentModel created = genericContentService.create(content);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GenericContentModel> getContentById(@PathVariable UUID id) {
        GenericContentModel content = genericContentService.getById(id);
        return new ResponseEntity<>(content, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GenericContentModel> updateContent(@PathVariable UUID id, @RequestBody GenericContentModel content) {
        content.setId(id);
        content.setUpdatedAt(Instant.now());
        GenericContentModel updated = genericContentService.update(content);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContent(@PathVariable UUID id) {
        genericContentService.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
