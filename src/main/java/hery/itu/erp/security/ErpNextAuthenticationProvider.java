package hery.itu.erp.security;

import java.util.List;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import hery.itu.erp.service.login.LoginService;

/**
 * Délègue l'authentification à ERPNext ({@code /api/method/login}) et conserve le cookie de session
 * obtenu dans le principal, donc dans la HttpSession de l'utilisateur — jamais dans un singleton.
 */
@Component
public class ErpNextAuthenticationProvider implements AuthenticationProvider {

    private final LoginService loginService;

    public ErpNextAuthenticationProvider(LoginService loginService) {
        this.loginService = loginService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials() == null ? "" : authentication.getCredentials().toString();

        String sidCookie = loginService.loginToErpNext(username, password)
                .orElseThrow(() -> new BadCredentialsException("Identifiants ERPNext invalides"));

        ErpNextUser user = new ErpNextUser(username, sidCookie);
        return UsernamePasswordAuthenticationToken.authenticated(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
