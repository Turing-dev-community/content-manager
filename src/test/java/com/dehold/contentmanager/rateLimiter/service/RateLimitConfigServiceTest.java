package com.dehold.contentmanager.rateLimiter.service;

import com.dehold.contentmanager.ContentManagerApplicationTests;
import com.dehold.contentmanager.ratelimiter.config.RateLimitConfig;
import com.dehold.contentmanager.ratelimiter.repository.RateLimitConfigRepository;
import com.dehold.contentmanager.ratelimiter.service.RateLimitConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RateLimitConfigServiceTest extends ContentManagerApplicationTests {

    private RateLimitConfigRepository repo;
    private RateLimitConfigService service;

    @BeforeEach
    void setup() {
        repo = mock(RateLimitConfigRepository.class);

        // before constructor runs, mock repo.findAll()
        when(repo.findAll()).thenReturn(new ArrayList<>());

        service = new RateLimitConfigService(repo);
    }

    private RateLimitConfig cfg(String pattern) {
        RateLimitConfig c = new RateLimitConfig();
        c.setId(UUID.randomUUID());
        c.setPathPattern(pattern);
        c.setCapacity(10);
        c.setRefillTokens(1);
        c.setRefillIntervalMillis(1000);
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return c;
    }

    // ---------------------------------------------------------
    // reloadCache() sorts patterns by length (descending)
    // ---------------------------------------------------------
    @Test
    void reloadCache_shouldSortByPatternLengthDescending() {
        List<RateLimitConfig> list = new ArrayList<>();
        list.add(cfg("/api/a"));
        list.add(cfg("/api/abc/longer"));
        list.add(cfg("/api/ab"));

        when(repo.findAll()).thenReturn(list);

        service.reloadCache();

        List<RateLimitConfig> cached = service.findAll();

        assertEquals("/api/abc/longer", cached.get(0).getPathPattern());
        assertEquals("/api/ab", cached.get(1).getPathPattern());
        assertEquals("/api/a", cached.get(2).getPathPattern());
    }

    // ---------------------------------------------------------
    // create(): sets ID + timestamps + saves + reloads cache
    // ---------------------------------------------------------
    @Test
    void create_shouldSetIdAndTimestamps_andPersistAndReloadCache() {
        when(repo.findAll()).thenReturn(new ArrayList<>());

        RateLimitConfig newCfg = new RateLimitConfig();
        newCfg.setPathPattern("/x");
        newCfg.setCapacity(5);
        newCfg.setRefillTokens(1);
        newCfg.setRefillIntervalMillis(1500);

        service.create(newCfg);

        ArgumentCaptor<RateLimitConfig> captor = ArgumentCaptor.forClass(RateLimitConfig.class);
        verify(repo).insert(captor.capture());
        RateLimitConfig saved = captor.getValue();

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        verify(repo, times(2)).findAll(); // initial + reloadCache
    }


    // ---------------------------------------------------------
    // delete(): calls repo.delete + reloadCache
    // ---------------------------------------------------------
    @Test
    void delete_shouldCallRepoDelete_andReloadCache() {
        UUID id = UUID.randomUUID();

        service.delete(id);

        verify(repo).delete(id);
        verify(repo, times(2)).findAll(); // initial + reload
    }

    // ---------------------------------------------------------
    // findBestMatchForPath() tests
    // ---------------------------------------------------------
    @Test
    void findBestMatch_exactMatchShouldWin() {
        RateLimitConfig a = cfg("/api/users");
        RateLimitConfig b = cfg("/api/users/profile");
        when(repo.findAll()).thenReturn(new ArrayList<>(List.of(a, b)));

        service.reloadCache();

        Optional<RateLimitConfig> res = service.findBestMatchForPath("/api/users");
        assertTrue(res.isPresent());
        assertEquals("/api/users", res.get().getPathPattern());
    }

    @Test
    void findBestMatch_longestPatternShouldWin() {
        RateLimitConfig broad = cfg("/api/users");
        RateLimitConfig specific = cfg("/api/users/profile");

        // Order reversed intentionally (to test sorting)
        when(repo.findAll()).thenReturn(new ArrayList<>(List.of(broad, specific)));

        service.reloadCache();

        Optional<RateLimitConfig> res = service.findBestMatchForPath("/api/users/profile");
        assertTrue(res.isPresent());
        assertEquals("/api/users/profile", res.get().getPathPattern());
    }

    @Test
    void findBestMatch_prefixMatchShouldWork() {
        RateLimitConfig c = cfg("/api/blogs");
        when(repo.findAll()).thenReturn(new ArrayList<>(List.of(c)));
        service.reloadCache();

        Optional<RateLimitConfig> res = service.findBestMatchForPath("/api/blogs/new");
        assertTrue(res.isPresent());
        assertEquals("/api/blogs", res.get().getPathPattern());
    }

    @Test
    void findBestMatch_noMatchShouldReturnEmpty() {
        when(repo.findAll()).thenReturn(new ArrayList<>(List.of(cfg("/api/a"))));
        service.reloadCache();

        Optional<RateLimitConfig> res = service.findBestMatchForPath("/xxx");
        assertTrue(res.isEmpty());
    }

    @Test
    void findBestMatch_ambiguousButLongestWins() {
        RateLimitConfig c1 = cfg("/api/u");
        RateLimitConfig c2 = cfg("/api/users");
        when(repo.findAll()).thenReturn(new ArrayList<>(List.of(c1, c2)));
        service.reloadCache();

        Optional<RateLimitConfig> res = service.findBestMatchForPath("/api/users/xyz");

        assertTrue(res.isPresent());
        assertEquals("/api/users", res.get().getPathPattern());
    }

    @Test
    void findById_shouldDelegateToRepo() {
        UUID id = UUID.randomUUID();
        RateLimitConfig cfg = cfg("/path");
        when(repo.findById(id)).thenReturn(Optional.of(cfg));

        Optional<RateLimitConfig> result = service.findById(id);
        assertTrue(result.isPresent());
        verify(repo).findById(id);
    }

    @Test
    void findByPathExact_shouldDelegateToRepo() {
        RateLimitConfig cfg = cfg("/aaa");
        when(repo.findByPathPattern("/aaa")).thenReturn(Optional.of(cfg));

        Optional<RateLimitConfig> result = service.findByPathExact("/aaa");
        assertTrue(result.isPresent());
        verify(repo).findByPathPattern("/aaa");
    }
}
