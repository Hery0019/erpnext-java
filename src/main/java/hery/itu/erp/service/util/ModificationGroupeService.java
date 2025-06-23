package hery.itu.erp.service.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import hery.itu.erp.service.login.LoginService;

import java.util.List;

@Service
public class ModificationGroupeService {
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private LoginService loginService;
    

    public boolean filtreStylee() {
        return false;
    }
   

}
