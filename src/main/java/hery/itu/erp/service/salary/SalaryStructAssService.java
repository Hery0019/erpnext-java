package hery.itu.erp.service.salary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import hery.itu.erp.model.salary.SalaryFilterDTO;
import hery.itu.erp.model.salary.SalaryStructAss;
import hery.itu.erp.service.login.LoginService;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class SalaryStructAssService {

    private final RestTemplate restTemplate;
    private final LoginService loginService;
    private final String baseUrl = "http://erpnext.localhost:8000";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SalaryStructAssService(RestTemplateBuilder builder, LoginService loginService) {
        this.restTemplate = builder.build();
        this.loginService = loginService;
    }

    public String createSalaryStructureAssignmentAndSubmit(SalaryStructAss salaryStructAss) throws Exception {
        String cookie = loginService.getSessionCookie();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, cookie);

        Map<String, Object> data = new HashMap<>();
        data.put("employee", salaryStructAss.getEmployee());
        data.put("salary_structure", salaryStructAss.getSalary_structure());
        data.put("company", salaryStructAss.getCompany());
        data.put("currency", salaryStructAss.getCurrency());
        data.put("base", salaryStructAss.getBase().toString());
        data.put("from_date", salaryStructAss.getFrom_date());
        if (salaryStructAss.getTo_date() != null) {
            data.put("to_date", salaryStructAss.getTo_date());
        }

        String payload = objectMapper.writeValueAsString(data);
        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/resource/Salary Structure Assignment",
                HttpMethod.POST,
                request,
                String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Erreur lors de la création : " + response.getBody());
        }

        String name = objectMapper.readTree(response.getBody()).path("data").path("name").asText();
        if (name == null || name.isEmpty()) {
            throw new Exception("Impossible de récupérer le name de l'Assignment");
        }

        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> check = restTemplate.exchange(
                    baseUrl + "/api/resource/Salary Structure Assignment/" + name,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            if (check.getStatusCode().is2xxSuccessful()) break;
            Thread.sleep(300);
        }

        ResponseEntity<String> submitResponse = restTemplate.exchange(
                baseUrl + "/api/resource/Salary Structure Assignment/" + name + "?run_method=submit",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class);

        if (!submitResponse.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Erreur lors de la soumission : " + submitResponse.getBody());
        }

        return name;
    }

    public String createSalarySlipAndSubmit(SalaryStructAss salaryStructAss) throws Exception {
        String cookie = loginService.getSessionCookie();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, cookie);

        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> check = restTemplate.exchange(
                    baseUrl + "/api/resource/Salary Structure Assignment?filters=" +
                            "[[\"Salary Structure Assignment\",\"employee\",\"=\",\"" + salaryStructAss.getEmployee() + "\"]]",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);
            if (check.getBody().contains("data")) break;
            Thread.sleep(300);
        }

        // ✅ Générer suffix incrémental
        String prefix = "Sal Slip/" + salaryStructAss.getEmployee() + "/";
        int suffix = getNextSlipSuffix(salaryStructAss.getEmployee());
        String slipName = String.format("%s%05d", prefix, suffix);

        Map<String, Object> slipData = new HashMap<>();
        slipData.put("name", slipName);
        slipData.put("employee", salaryStructAss.getEmployee());
        slipData.put("salary_structure", salaryStructAss.getSalary_structure());
        slipData.put("company", salaryStructAss.getCompany());
        slipData.put("start_date", salaryStructAss.getFrom_date());
        slipData.put("end_date", salaryStructAss.getTo_date() != null ? salaryStructAss.getTo_date() : salaryStructAss.getFrom_date());
        slipData.put("posting_date", salaryStructAss.getPosting_date());
        slipData.put("payroll_frequency", "Monthly");
        slipData.put("base", salaryStructAss.getBase());

        String slipJson = objectMapper.writeValueAsString(slipData);
        HttpEntity<String> slipRequest = new HttpEntity<>(slipJson, headers);

        ResponseEntity<String> slipResponse = restTemplate.exchange(
                baseUrl + "/api/resource/Salary Slip",
                HttpMethod.POST,
                slipRequest,
                String.class);

        if (!slipResponse.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Erreur lors de la création du Salary Slip : " + slipResponse.getBody());
        }

        ResponseEntity<String> slipSubmitResponse = restTemplate.exchange(
                baseUrl + "/api/resource/Salary Slip/" + slipName + "?run_method=submit",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class);

        if (!slipSubmitResponse.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Erreur lors de la soumission du Salary Slip : " + slipSubmitResponse.getBody());
        }

        return slipName;
    }

    private int getNextSlipSuffix(String employee) throws Exception {
        String cookie = loginService.getSessionCookie();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);

        String url = baseUrl + "/api/resource/Salary Slip?fields=[\"name\"]"
                + "&filters=[[\"Salary Slip\",\"employee\",\"=\",\"" + employee + "\"]]"
                + "&order_by=creation desc&limit=1";

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        String lastName = null;
        if (response.getStatusCode().is2xxSuccessful()) {
            var arr = objectMapper.readTree(response.getBody()).path("data");
            if (arr.isArray() && arr.size() > 0) {
                lastName = arr.get(0).path("name").asText();
            }
        }

        if (lastName != null && lastName.contains("/")) {
            String[] parts = lastName.split("/");
            try {
                return Integer.parseInt(parts[2]) + 1;
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        return 1;
    }

    public Map<String, Object> createAssignmentAndSlip(SalaryStructAss salaryStructAss) throws Exception {
        if (salaryStructAss.getBase() == null) {
            Double lastBase = getLastSalaryBase(salaryStructAss.getEmployee());
            if (lastBase != null) {
                salaryStructAss.setBase(BigDecimal.valueOf(lastBase));
            } else {
                throw new Exception("Base introuvable et non fournie !");
            }
        }

        String assignmentName = createSalaryStructureAssignmentAndSubmit(salaryStructAss);
        String slipName = createSalarySlipAndSubmit(salaryStructAss);

        Map<String, Object> result = new HashMap<>();
        result.put("assignment", assignmentName);
        result.put("slip", slipName);
        return result;
    }

    /**
     * Vérifie s'il existe déjà un Salary Slip pour l'employé et la période donnée.
     *
     * @param employee employé concerné
     * @param startDate date de début du slip
     * @param endDate date de fin du slip
     * @return true s'il existe déjà un slip pour cette période
     * @throws Exception en cas d'erreur d'appel
     */
    private boolean salarySlipExists(String employee, String startDate, String endDate) throws Exception {
        String cookie = loginService.getSessionCookie();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, cookie);

        String url = baseUrl + "/api/resource/Salary Slip?filters=" +
                "[[\"Salary Slip\",\"employee\",\"=\",\"" + employee + "\"]," +
                "[\"Salary Slip\",\"start_date\",\"=\",\"" + startDate + "\"]," +
                "[\"Salary Slip\",\"end_date\",\"=\",\"" + endDate + "\"]]";

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        var arr = objectMapper.readTree(response.getBody()).path("data");
        return arr.isArray() && arr.size() > 0;
    }

    public boolean salaryAssignmentExists(String employeeId, String fromDate) {
        String url = "http://erpnext.localhost:8000/api/resource/Salary Structure Assignment?fields=[\"name\"]&filters=[[\"employee\",\"=\",\"" + employeeId + "\"],[\"from_date\",\"=\",\"" + fromDate + "\"]]";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", loginService.getSessionCookie());
    
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
            return data != null && !data.isEmpty();
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification de Salary Assignment : " + e.getMessage());
            return false;
        }
    }
    
    public List<String> generateSalary(SalaryStructAss salaryStructAss, LocalDate startDate, LocalDate endDate, String ecraser, String moyenne) throws Exception {
        List<String> generatedSlips = new ArrayList<>();
        BigDecimal base = salaryStructAss.getBase();
    
        System.out.println("Base fournie par l'employé " + salaryStructAss.getEmployee() + " : " + base);
    
        if (base == null && "oui".equalsIgnoreCase(moyenne)) {
            base = getMoyenneTotalBaseOfAllEmployees();
            System.out.println("Base moyenne utilisée : " + base);
        }
    
        if (base == null) {
            Double lastBase = getLastSalaryBase(salaryStructAss.getEmployee());
            if (lastBase != null) {
                base = BigDecimal.valueOf(lastBase);
                System.out.println("Base récupérée depuis la dernière assignment : " + base);
            } else {
                throw new Exception("Base introuvable pour l'employé : " + salaryStructAss.getEmployee());
            }
        }
    
        LocalDate current = startDate.withDayOfMonth(1);
        LocalDate limit = endDate.withDayOfMonth(1);
    
        while (!current.isAfter(limit)) {
            LocalDate slipStart = current;
            LocalDate slipEnd = current.with(TemporalAdjusters.lastDayOfMonth());
    
            boolean slipExists = salarySlipExists(salaryStructAss.getEmployee(), slipStart.toString(), slipEnd.toString());
            boolean assignmentExists = salaryAssignmentExists(salaryStructAss.getEmployee(), slipStart.toString());
    
            SalaryStructAss slipAss = new SalaryStructAss();
            slipAss.setEmployee(salaryStructAss.getEmployee());
            slipAss.setSalary_structure(salaryStructAss.getSalary_structure());
            slipAss.setCompany(salaryStructAss.getCompany());
            slipAss.setCurrency(salaryStructAss.getCurrency());
            slipAss.setBase(base);
            slipAss.setFrom_date(slipStart.toString());
            slipAss.setTo_date(slipEnd.toString());
            slipAss.setPosting_date(slipEnd.toString());
    
            try {
                if (slipExists && !"oui".equalsIgnoreCase(ecraser)) {
                    System.out.println("Slip déjà existant pour " + slipStart.getMonth() + "/" + slipStart.getYear() + " -> ignoré");
                } else {
                    if (assignmentExists && "oui".equalsIgnoreCase(ecraser)) {
                        slipAss.setName(getAssignmentNameBySalaryStructure(salaryStructAss.getSalary_structure()));
                        Map<String, Object> result = updateAssignment(slipAss);
                        generatedSlips.add(result.get("slip").toString());
                        System.out.println("Slip mis à jour pour : " + slipStart);
                    } else if (!assignmentExists) {
                        Map<String, Object> result = createAssignmentAndSlip(slipAss);
                        generatedSlips.add(result.get("slip").toString());
                        System.out.println("Slip créé pour : " + slipStart);
                    } else {
                        System.out.println("Assignment déjà existant et écrasement non autorisé -> ignoré pour " + slipStart);
                    }
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de la génération du slip pour " + slipStart + " : " + e.getMessage());
            }
    
            current = current.plusMonths(1);
        }
    
        return generatedSlips;
    }
    
    

    // public List<String> generateSalary(SalaryStructAss salaryStructAss, LocalDate startDate, LocalDate endDate, String ecraser, String moyenne) throws Exception {
    //     List<String> generatedSlips = new ArrayList<>();
    
    //     BigDecimal base = salaryStructAss.getBase();
    //     SalaryStructAss slipAss = new SalaryStructAss();

    //     System.out.println("Base fournie par l'employé " + salaryStructAss.getEmployee() + " : " + base);

    //     if (base == null) {
    //         if (moyenne.equals("oui")) {
    //             base = getMoyenneBase();
    //             System.out.println("Base moyenne : " + base);
    //         }
    //     }
    //     if (base == null) {
    //         Double lastBase = getLastSalaryBase(salaryStructAss.getEmployee());
    //         if (lastBase != null) {
    //             base = BigDecimal.valueOf(lastBase);
    //             System.out.println("Base null, recherche du dernier base : " + base);
    //         } else {
    //             throw new Exception("Base introuvable pour l'employé : " + salaryStructAss.getEmployee());
    //         }
    //     }
    
    //     LocalDate current = startDate.withDayOfMonth(1);
    //     LocalDate limit = endDate.withDayOfMonth(1);
    
    //     while (!current.isAfter(limit)) {
    //         LocalDate slipStart = current;
    //         LocalDate slipEnd = current.with(TemporalAdjusters.lastDayOfMonth());
    
    //         // ✅ Vérifier s'il y a déjà un Salary Slip pour ce mois pour cet employé
    //         if (salarySlipExists(salaryStructAss.getEmployee(), slipStart.toString(), slipEnd.toString())) {
    //             if (!ecraser.equals("oui"))  { // donc ne pas ecraser
    //                 System.out.println("Ecraser est :" + ecraser);
    //                 System.out.println("Salary Slip déjà existant pour le mois : " + slipStart.getMonth() + " " + slipStart.getYear() + " -> skip");
    //                 current = current.plusMonths(1);
    //                 continue;
    //             }

    //             if (ecraser.equals("oui")) {
                    
    //             }
    //         }
    
    //         slipAss.setEmployee(salaryStructAss.getEmployee());
    //         slipAss.setSalary_structure(salaryStructAss.getSalary_structure());
    //         slipAss.setCompany(salaryStructAss.getCompany());
    //         slipAss.setCurrency(salaryStructAss.getCurrency());
    //         slipAss.setBase(base);
    //         slipAss.setFrom_date(slipStart.toString());
    //         slipAss.setTo_date(slipEnd.toString());
    //         slipAss.setPosting_date(slipEnd.toString());
    
    //         Map<String, Object> result = createAssignmentAndSlip(slipAss);
    //         generatedSlips.add(result.get("slip").toString());
    
    //         current = current.plusMonths(1);
    //     }
    
    //     return generatedSlips;
    // }

    private Double getLastSalaryBase(String employee) throws Exception {
        String cookie = loginService.getSessionCookie();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, cookie);
    
        // ✅ Lire le dernier Salary Structure Assignment soumis
        String url = baseUrl + "/api/resource/Salary Structure Assignment?filters="
                + "[[\"Salary Structure Assignment\",\"employee\",\"=\",\"" + employee + "\"],"
                + "[\"Salary Structure Assignment\",\"docstatus\",\"=\",1]]"
                + "&fields=[\"name\",\"base\",\"from_date\"]"
                + "&order_by=from_date desc&limit=1";
    
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    
        var arr = objectMapper.readTree(response.getBody()).path("data");
        if (arr.isArray() && arr.size() > 0) {
            return arr.get(0).path("base").asDouble();
        }
    
        return null; // pas trouvé
    }

    /**
     * Annule un Salary Slip par son name (via run_method=cancel).
     * @param slipName le nom du Salary Slip (ex: Sal Slip/EMP001/00001)
     */
    public void cancelSalarySlip(String slipName) throws Exception {
        String cookie = loginService.getSessionCookie();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, cookie);

        ResponseEntity<String> cancelResponse = restTemplate.exchange(
                baseUrl + "/api/resource/Salary Slip/" + slipName + "?run_method=cancel",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class);

        if (!cancelResponse.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Erreur lors de l'annulation du Salary Slip : " + cancelResponse.getBody());
        }

        System.out.println("Salary Slip " + slipName + " annulé avec succès !");
    }


    /**
     * Annule un Salary Structure Assignment par son name (via run_method=cancel).
     * @param assignmentName le nom du Salary Structure Assignment (ex: HR-SSA-2025-00001)
     */
    public void cancelSalaryStructureAssignment(String assignmentName) throws Exception {
        String cookie = loginService.getSessionCookie();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, cookie);

        ResponseEntity<String> cancelResponse = restTemplate.exchange(
                baseUrl + "/api/resource/Salary Structure Assignment/" + assignmentName + "?run_method=cancel",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class);

        if (!cancelResponse.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Erreur lors de l'annulation du Salary Structure Assignment : " + cancelResponse.getBody());
        }

        System.out.println("Salary Structure Assignment " + assignmentName + " annulé avec succès !");
    }


    public List<SalaryStructAss> getAllSalaryStructureAssignments() throws Exception {
        String cookie = loginService.getSessionCookie();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, cookie);

        // ✅ 1) Construire filters JSON + fields JSON
        String fields = "[\"name\",\"employee\",\"posting_date\",\"salary_structure_assignment\",\"earnings\",\"deductions\"]";

        // ✅ 2) Encoder pour l'URL
        String url = baseUrl + "/api/resource/Salary Slip"
                + "?fields=" + URLEncoder.encode(fields, StandardCharsets.UTF_8);
        // ✅ 3) Appel REST avec headers + cookie session
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );


        var arr = objectMapper.readTree(response.getBody()).path("data");
        List<SalaryStructAss> assignments = new ArrayList<>();
        for (JsonNode assignment : arr) {
            SalaryStructAss salaryStructAss = new SalaryStructAss();
            salaryStructAss.setName(assignment.path("name").asText());
            salaryStructAss.setEmployee(assignment.path("employee_name").asText());
            salaryStructAss.setSalary_structure(assignment.path("salary_structure").asText());
            salaryStructAss.setCompany(assignment.path("company").asText());
            salaryStructAss.setCurrency(assignment.path("currency").asText());
            salaryStructAss.setBase(new BigDecimal(assignment.path("base").asDouble()));
            salaryStructAss.setFrom_date(assignment.path("from_date").asText());
            assignments.add(salaryStructAss);
        }
        return assignments;
    }

    /**
     * Soumettre un Salary Structure Assignment par son name.
     */
    public void submitAssignment(String name) throws Exception {
        String cookie = loginService.getSessionCookie();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, cookie);

        String url = baseUrl + "/api/resource/Salary Structure Assignment/" + name + "?run_method=submit";

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Erreur lors de la soumission du Salary Structure Assignment : " + response.getBody());
        }
    }


    /**
     * Soumettre un Salary Slip par son name.
     */
    public void submitSalarySlip(String slipName) throws Exception {
        String cookie = loginService.getSessionCookie();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, cookie);

        String url = baseUrl + "/api/resource/Salary Slip/" + slipName + "?run_method=submit";

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Erreur lors de la soumission du Salary Slip : " + response.getBody());
        }
    }

    
    /**
     * Amend un Salary Structure Assignment en utilisant run_method=amend.
     *
     * @param assignmentName le nom du Salary Structure Assignment à amender
     * @return le nom du nouveau Salary Structure Assignment créé par l'amend
     * @throws Exception en cas d'erreur
     */
    public String amendSalaryStructureAssignment(String assignmentName) throws Exception {
        String cookie = loginService.getSessionCookie();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, cookie);

        String url = baseUrl + "/api/resource/Salary Structure Assignment/" + assignmentName + "?run_method=amend";

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Erreur lors de l'amend du Salary Structure Assignment : " + response.getBody());
        }

        // Extraire le nom du nouvel amendement créé
        String newName = objectMapper.readTree(response.getBody()).path("message").asText();
        System.out.println("Salary Structure Assignment amendé, nouveau : " + newName);

        return newName;
    }

    /**
     * Amend un Salary Slip en utilisant run_method=amend.
     *
     * @param slipName le nom du Salary Slip à amender
     * @return le nom du nouveau Salary Slip créé par l'amend
     * @throws Exception en cas d'erreur
     */
    public String amendSalarySlip(String slipName) throws Exception {
        String cookie = loginService.getSessionCookie();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, cookie);

        String url = baseUrl + "/api/resource/Salary Slip/" + slipName + "?run_method=amend";

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Erreur lors de l'amend du Salary Slip : " + response.getBody());
        }

        // Extraire le nom du nouvel amendement créé
        String newName = objectMapper.readTree(response.getBody()).path("message").asText();
        System.out.println("Salary Slip amendé, nouveau : " + newName);

        return newName;
    }


    // ✅ Récupère un Salary Structure Assignment par son id
    public SalaryStructAss getAssignmentById(String id) throws Exception {
        String cookie = loginService.getSessionCookie();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, cookie);

        String url = baseUrl + "/api/resource/Salary Structure Assignment/" + id;

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Erreur lors de la récupération : " + response.getBody());
        }

        return objectMapper.readValue(
            objectMapper.readTree(response.getBody()).path("data").toString(),
            SalaryStructAss.class
        );
    }

  
    /**
     * Met à jour complètement un Salary Structure Assignment :
     * - Annule si nécessaire
     * - Supprime le Salary Slip lié
     * - Supprime l'ancien SSA
     * - Recrée SSA + Salary Slip mis à jour
     *
     * @param updatedAss l'objet mis à jour
     * @return Map contenant {"assignment": nouveau SSA name, "slip": nouveau Salary Slip name}
     * @throws Exception en cas d'erreur
     */
    public Map<String, Object> updateAssignment(SalaryStructAss updatedAss) throws Exception {
        String cookie = loginService.getSessionCookie();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, cookie);

        // 1️⃣ Vérifier docstatus de l'ancien SSA
        String urlGet = baseUrl + "/api/resource/Salary Structure Assignment/" + updatedAss.getName();
        ResponseEntity<String> getResponse = restTemplate.exchange(urlGet, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        if (!getResponse.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Erreur lors de la récupération du SSA : " + getResponse.getBody());
        }
        
        

        int docstatus = objectMapper.readTree(getResponse.getBody()).path("data").path("docstatus").asInt();

        // 2️⃣ Chercher le Salary Slip lié pour cet employé et SSA
        String slipQueryUrl = baseUrl + "/api/resource/Salary Slip?filters=" +
                "[[\"Salary Slip\",\"salary_structure\",\"=\",\"" + updatedAss.getSalary_structure() + "\"]," +
                "[\"Salary Slip\",\"employee\",\"=\",\"" + updatedAss.getEmployee() + "\"]]" +
                "&fields=[\"name\"]&limit=1";

        ResponseEntity<String> slipResponse = restTemplate.exchange(slipQueryUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        String slipName = null;
        var slipArr = objectMapper.readTree(slipResponse.getBody()).path("data");
        if (slipArr.isArray() && slipArr.size() > 0) {
            slipName = slipArr.get(0).path("name").asText();
        }

        // 3️⃣ Si soumis → annuler SSA
        if (docstatus == 1) {
            cancelSalaryStructureAssignment(updatedAss.getName());
        }

        // 4️⃣ Si Salary Slip existe → annuler et supprimer
        if (slipName != null) {
            cancelSalarySlip(slipName);

            String slipDeleteUrl = baseUrl + "/api/resource/Salary Slip/" + slipName;
            ResponseEntity<String> slipDeleteResponse = restTemplate.exchange(
                    slipDeleteUrl,
                    HttpMethod.DELETE,
                    new HttpEntity<>(headers),
                    String.class
            );
            if (!slipDeleteResponse.getStatusCode().is2xxSuccessful()) {
                throw new Exception("Erreur lors de la suppression du Salary Slip : " + slipDeleteResponse.getBody());
            }
            System.out.println("Salary Slip " + slipName + " annulé et supprimé avec succès !");
        }

        // 5️⃣ Supprimer l'ancien SSA maintenant annulé ou brouillon
        deleteAssignment(updatedAss.getName());

        // 6️⃣ Créer nouveau SSA + Salary Slip mis à jour
        Map<String, Object> result = createAssignmentAndSlip(updatedAss);

        System.out.println("✅ Nouveau SSA et Salary Slip créés : " + result);
        return result;
    }


    /**
     * Supprime un Salary Structure Assignment :
     * - Si soumis, l'annule d'abord puis supprime
     * - Sinon supprime directement.
     *
     * @param assignmentName le nom du SSA à supprimer (ex : HR-SSA-2025-00004)
     * @throws Exception en cas d'erreur
     */
    public void deleteAssignment(String assignmentName) throws Exception {
        String cookie = loginService.getSessionCookie();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, cookie);

        // 1️⃣ Vérifier docstatus
        String urlGet = baseUrl + "/api/resource/Salary Structure Assignment/" + assignmentName;
        ResponseEntity<String> getResponse = restTemplate.exchange(urlGet, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        if (!getResponse.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Erreur lors de la récupération : " + getResponse.getBody());
        }

        int docstatus = objectMapper.readTree(getResponse.getBody()).path("data").path("docstatus").asInt();

        if (docstatus == 1) {
            // 2️⃣ Annuler avant suppression si soumis
            cancelSalaryStructureAssignment(assignmentName);
        }

        // 3️⃣ Supprimer
        String urlDelete = baseUrl + "/api/resource/Salary Structure Assignment/" + assignmentName;

        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                urlDelete,
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                String.class
        );

        if (!deleteResponse.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Erreur lors de la suppression : " + deleteResponse.getBody());
        }

        System.out.println("Salary Structure Assignment " + assignmentName + " supprimé avec succès !");
    }

    /**
     * Récupère toutes les valeurs d'un Salary Component pour un employé,
     * filtrées par condition (inférieur ou supérieur) et un montant donné.
     * Utilise get_list + get pour contourner les restrictions ERPNext.
     * Ajout de System.out.println pour debug complet.
     */
    public List<SalaryFilterDTO> getSalaryComponentValues(String employee, String salaryComponent, String condition, double montant) throws Exception {

        System.out.println("=== Début getSalaryComponentValues ===");
        System.out.println("Paramètres : employee = " + employee + ", salaryComponent = " + salaryComponent + ", condition = " + condition + ", montant = " + montant);

        // ---------------------------
        // 1) get_list : récupère les Salary Slip autorisés
        // ---------------------------
        String filters = "[[\"employee\",\"=\",\"" + employee + "\"]]";
        String fields = "[\"name\",\"employee\",\"posting_date\"]";

        Map<String, Object> payload = new HashMap<>();
        payload.put("doctype", "Salary Slip");
        payload.put("filters", objectMapper.readTree(filters));
        payload.put("fields", objectMapper.readTree(fields));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, loginService.getSessionCookie());

        ResponseEntity<String> listResponse = restTemplate.exchange(
            baseUrl + "/api/method/frappe.client.get_list",
            HttpMethod.POST,
            new HttpEntity<>(payload, headers),
            String.class
        );

        if (!listResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Erreur lors du get_list : " + listResponse.getBody());
        }

        JsonNode slips = objectMapper.readTree(listResponse.getBody()).path("message");
        System.out.println("Nombre de slips trouvés : " + slips.size());

        List<SalaryFilterDTO> matchingResults = new ArrayList<>();

        // ---------------------------
        // 2) get pour chaque slip pour avoir tous les champs
        // ---------------------------
        for (JsonNode slip : slips) {
            String slipName = slip.path("name").asText();
            System.out.println("Récupération du slip : " + slipName);

            // Appel GET pour ce slip
            ResponseEntity<String> slipResponse = restTemplate.exchange(
                baseUrl + "/api/resource/Salary Slip/" + slipName,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
            );

            if (!slipResponse.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Erreur get Salary Slip " + slipName + " : " + slipResponse.getBody());
            }

            JsonNode slipDetails = objectMapper.readTree(slipResponse.getBody()).path("data");

            String postingDate = slipDetails.path("posting_date").asText();
            System.out.println(" - Posting Date : " + postingDate);

            ArrayNode earnings = (ArrayNode) slipDetails.path("earnings");
            ArrayNode deductions = (ArrayNode) slipDetails.path("deductions");

            // Vérifie earnings
            for (JsonNode earning : earnings) {
                String comp = earning.path("salary_component").asText();
                double amount = earning.path("amount").asDouble();
                System.out.println("   Checking earning: component = " + comp + ", amount = " + amount);
                if (salaryComponent.equalsIgnoreCase(comp)) {
                    boolean matches = "inf".equals(condition) ? amount < montant : amount > montant;
                    System.out.println("     -> Comparaison : amount = " + amount + " | montant = " + montant + " | condition = " + condition + " | matches ? " + matches);
                    if (matches) {
                        SalaryFilterDTO dto = new SalaryFilterDTO(
                            slipName,
                            employee,
                            comp,
                            amount,
                            postingDate
                        );
                        System.out.println("     => Ajouté : " + dto);
                        matchingResults.add(dto);
                    }
                }
            }

            // Vérifie deductions aussi
            for (JsonNode deduction : deductions) {
                String comp = deduction.path("salary_component").asText();
                double amount = deduction.path("amount").asDouble();
                System.out.println("   Checking deduction: component = " + comp + ", amount = " + amount);
                if (salaryComponent.equalsIgnoreCase(comp)) {
                    boolean matches = "inf".equals(condition) ? amount < montant : amount > montant;
                    System.out.println("     -> Matches condition ? " + matches);
                    if (matches) {
                        SalaryFilterDTO dto = new SalaryFilterDTO(
                            slipName,
                            employee,
                            comp,
                            amount,
                            postingDate
                        );
                        System.out.println("     => Ajouté : " + dto);
                        matchingResults.add(dto);
                    }
                }
            }
        }

        System.out.println("=== Fin getSalaryComponentValues : Total résultats = " + matchingResults.size() + " ===");
        return matchingResults;
    }

    public BigDecimal getMoyenneTotalBaseOfAllEmployees() throws Exception {
        String cookie = loginService.getSessionCookie();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, cookie);
    
        String url = baseUrl + "/api/resource/Salary Structure Assignment?fields=[\"base\"]"
                + "&filters=[[\"Salary Structure Assignment\",\"docstatus\",\"=\",1]]"
                + "&limit_page_length=1000"; 
    
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Erreur lors de la récupération des bases : " + response.getBody());
        }
    
        BigDecimal total = BigDecimal.ZERO;
        var array = objectMapper.readTree(response.getBody()).path("data");
    
        int i = 0;
        for (JsonNode node : array) {
            BigDecimal base = node.has("base") ? new BigDecimal(node.get("base").asDouble()) : BigDecimal.ZERO;
            total = total.add(base);
            i++;
        }
    
        return total.divide(new BigDecimal(i));
    }
    

    /**
     * Récupère le nom du Salary Structure Assignment pour un employé à une date donnée.
     * Version robuste : utilise POST sur get_list.
     */
    public String getSalaryStructureAssignmentByEmployeeAndDate(SalaryFilterDTO salaryFilterDTO) throws Exception {
        // ✅ Construire le payload JSON correct
        Map<String, Object> payload = new HashMap<>();
        payload.put("doctype", "Salary Structure Assignment");
        payload.put("fields", Arrays.asList("name", "from_date"));
        payload.put("filters", Arrays.asList(
            Arrays.asList("employee", "=", salaryFilterDTO.getEmployee()),
            Arrays.asList("from_date", "<=", salaryFilterDTO.getPostingDate())
        ));
        payload.put("order_by", "from_date desc");
        payload.put("limit_page_length", 1);

        // ✅ Préparer les headers avec le cookie de session ERPNext
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Cookie", loginService.getSessionCookie());

        // ✅ Appeler POST sur frappe.client.get_list
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/api/method/frappe.client.get_list",
            HttpMethod.POST,
            new HttpEntity<>(payload, headers),
            String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Erreur get_list Salary Structure Assignment : " + response.getBody());
        }

        // ✅ Lire la réponse JSON (champ 'message' pour get_list)
        JsonNode data = objectMapper.readTree(response.getBody()).path("message");

        System.out.println("==> Résultat get_list SSA pour " + salaryFilterDTO.getEmployee() + " à la date " + salaryFilterDTO.getPostingDate() + ": " + data);

        if (data.isArray() && data.size() > 0) {
            return data.get(0).path("name").asText();
        } else {
            throw new RuntimeException("Aucun Salary Structure Assignment trouvé pour l'employé à cette date.");
        }
    }


   /**
     * Applique une modification sur la base salariale d’un Salary Structure Assignment
     * en fonction d’un Salary Component, d’une condition et d’un pourcentage.
     *
     * @param employees       la liste des employés concernés
     * @param salaryComponent le nom du Salary Component à cibler
     * @param condition       "inf" ou "sup" (inférieur ou supérieur à un montant)
     * @param montant         le montant seuil à comparer
     * @param then            "ajouter" ou "retirer" pour appliquer la modification
     * @param pourcentage     le pourcentage à appliquer
     * @throws Exception en cas d’erreur ou d’assignation manquante
     */
    public void applyModification(List<String> employees, String salaryComponent, String condition, String then, double montant, double pourcentage) throws Exception {
        System.out.println("=== APPLY MODIFICATION ===");
        System.out.println("Target Salary Component: " + salaryComponent);
        System.out.println("Condition: " + condition);
        System.out.println("Action (then): " + then);
        System.out.println("Seuil montant: " + montant);
        System.out.println("Pourcentage: " + pourcentage + "%");
        System.out.println("-------------------------------");

        for (String employee : employees) {
            System.out.println(">> Employee: " + employee);

            // Récupère tous les Salary Slip components qui matchent pour cet employé
            List<SalaryFilterDTO> matchingResults = getSalaryComponentValues(employee, salaryComponent, condition, montant);
            System.out.println("  Found " + matchingResults.size() + " matching slips for employee " + employee);

            for (SalaryFilterDTO result : matchingResults) {
                System.out.println("   -> Slip: " + result.getSlipName() +
                        ", Component: " + result.getSalaryComponent() +
                        ", Amount: " + result.getAmount() +
                        ", Posting Date: " + result.getPostingDate());

                // Cherche l'Assignment actif à cette date
                String assignmentName = getSalaryStructureAssignmentByEmployeeAndDate(result);
                System.out.println("   => Matching Assignment: " + assignmentName);

                SalaryStructAss salaryStructAss = getAssignmentById(assignmentName);
                if (salaryStructAss == null) {
                    System.out.println("   [!] WARNING: Salary Structure Assignment not found, skipping.");
                    continue;
                }

                BigDecimal base = salaryStructAss.getBase();
                if (base == null) {
                    System.out.println("   [!] WARNING: Base salary is null, skipping.");
                    continue;
                }

                System.out.println("      Original base: " + base);

                BigDecimal percentage = BigDecimal.valueOf(pourcentage).divide(BigDecimal.valueOf(100));
                BigDecimal adjustment;
                if ("retirer".equals(then)) {
                    adjustment = base.subtract(base.multiply(percentage));
                    System.out.println("      Action: RETIRER -> New base = " + adjustment);
                } else if ("ajouter".equals(then)) {
                    adjustment = base.add(base.multiply(percentage));
                    System.out.println("      Action: AJOUTER -> New base = " + adjustment);
                } else {
                    System.out.println("   [!] Invalid 'then' condition: " + then + ", skipping.");
                    continue;
                }

                salaryStructAss.setBase(adjustment);
                System.out.println("   [✓] Updating Assignment with new base...");

                updateAssignment(salaryStructAss);

                System.out.println("   [✓] Update DONE for Assignment: " + assignmentName);
            }

            System.out.println("-------------------------------");
        }

        System.out.println("=== APPLY MODIFICATION DONE ===");
    }

    public String getAssignmentNameBySalaryStructure(String salaryStructure) throws Exception {
        String cookie = loginService.getSessionCookie();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, cookie);
    
        String url = baseUrl + "/api/resource/Salary Structure Assignment?fields=[\"name\"]"
                + "&filters=[[\"Salary Structure Assignment\",\"salary_structure\",\"=\",\"" + salaryStructure + "\"]]"
                + "&order_by=creation desc&limit=1";
    
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Erreur lors de la récupération : " + response.getBody());
        }
    
        JsonNode dataArray = objectMapper.readTree(response.getBody()).path("data");
    
        if (dataArray.isArray() && dataArray.size() > 0) {
            return dataArray.get(0).path("name").asText();
        }
    
        throw new Exception("Aucun Salary Structure Assignment trouvé pour le Salary Structure : " + salaryStructure);
    }
    


}
