package hery.itu.erp.service.importation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import hery.itu.erp.service.login.LoginService;

@Service
public class ImportService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final LoginService loginService;

    public ImportService(LoginService loginService) {
        this.loginService = loginService;
    }


    public Map importCsvFiles(byte[] file1Bytes, byte[] file2Bytes, byte[] file3Bytes)throws Exception {
        String url = "http://erpnext.localhost:8000/api/method/erpnext.api.salaireImport.import_csv_files";
    
        String content1 = Base64.getEncoder().encodeToString(file1Bytes);
        String content2 = Base64.getEncoder().encodeToString(file2Bytes);
        String content3 = Base64.getEncoder().encodeToString(file3Bytes);
    
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("file1", content1);
        body.add("file2", content2);
        body.add("file3", content3);
    
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Cookie", loginService.getSessionCookie());
    
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
    
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
    
        System.out.println("Réponse: " + response.getBody());
        return response.getBody();
    }
    
    
}

