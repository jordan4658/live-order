package com.caipiao.live.order.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/health")
public class AppHealthController {

    @RequestMapping("/ok")
    public static String ok() {
        return "ok";
    }
}
