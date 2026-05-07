package com.ipl.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class FallbackController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String root() {
        return "forward:/index.html";
    }

    @GetMapping(value = "/index.html", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String index() throws Exception {
        Resource resource = new ClassPathResource("static/index.html");
        return new String(resource.getInputStream().readAllBytes());
    }

    @GetMapping(value = "/{path:[^\\.]*}")
    public String fallback() {
        return "forward:/index.html";
    }
}