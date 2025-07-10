package hery.itu.erp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String showLoginPage() {
        System.out.println("hello");
        return "login"; // This should match the name of your HTML file without the extension
    }
}