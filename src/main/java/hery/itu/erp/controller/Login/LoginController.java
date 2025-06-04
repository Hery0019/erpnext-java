package hery.itu.erp.controller.login;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import hery.itu.erp.service.login.LoginService;


@Controller
public class LoginController {
    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password) {
        if (loginService.loginToErpNext(username, password)==true) {
            return "redirect:/employes";
        }
        return "error"; // Redirige vers la page d'erreur
    }
}