package com.mahal.controller;

import com.mahal.sync.SupabaseSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/diag")
public class DiagnosticController {

    @Autowired
    private SupabaseSyncService supabaseSyncService;

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();

        boolean configured = supabaseSyncService.isConfigured();
        String url = supabaseSyncService.getSupabaseUrl();
        String key = supabaseSyncService.getSupabaseApiKey();

        status.put("supabase_configured", configured);
        status.put("supabase_url", url);
        status.put("supabase_key_present", key != null && !key.isEmpty());
        if (key != null && key.length() > 10) {
            status.put("supabase_key_preview", key.substring(0, 5) + "..." + key.substring(key.length() - 5));
        }

        return status;
    }
}
