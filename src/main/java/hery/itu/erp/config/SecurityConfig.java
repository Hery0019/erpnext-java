package hery.itu.erp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import hery.itu.erp.security.ErpNextAuthenticationProvider;

/**
 * Toutes les pages exigent un utilisateur authentifié auprès d'ERPNext, sauf la page de login
 * et les ressources statiques. La protection CSRF est active : les formulaires Thymeleaf
 * ({@code th:action}) reçoivent le jeton automatiquement ; les appels fetch() doivent l'envoyer
 * dans l'en-tête indiqué par la balise {@code <meta name="_csrf_header">}.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ErpNextAuthenticationProvider provider) throws Exception {
        http
            .authenticationProvider(provider)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/error", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/employes", false)
                .failureUrl("/?error"))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?logout")
                .invalidateHttpSession(true));
        return http.build();
    }
}
