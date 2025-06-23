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

import org.springframework.web.bind.annotation.RequestParam;

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

    public List<String> generateSalary(SalaryStructAss salaryStructAss, LocalDate startDate, LocalDate endDate) throws Exception {
        List<String> generatedSlips = new ArrayList<>();

        BigDecimal base = salaryStructAss.getBase();
        System.out.println("Base fournie par l'employee " + salaryStructAss.getEmployee() + " : " + base);
        if (base == null) {
            Double lastBase = getLastSalaryBase(salaryStructAss.getEmployee());
            if (lastBase != null) {
                base = BigDecimal.valueOf(lastBase);
                System.out.println("Base null donc recherche du dernier base");
                System.out.println("Dernier base trouvée pour l'employé " + salaryStructAss.getEmployee() + " : " + base);
            } else {
                throw new Exception("Base introuvable pour l'employé : " + salaryStructAss.getEmployee());
            }
        }


        LocalDate current = startDate.withDayOfMonth(1);
        LocalDate limit = endDate.withDayOfMonth(1);

        while (!current.isAfter(limit)) {
            LocalDate slipStart = current;
            LocalDate slipEnd = current.with(TemporalAdjusters.lastDayOfMonth());

            SalaryStructAss slipAss = new SalaryStructAss();
            slipAss.setEmployee(salaryStructAss.getEmployee());
            slipAss.setSalary_structure(salaryStructAss.getSalary_structure());
            slipAss.setCompany(salaryStructAss.getCompany());
            slipAss.setCurrency(salaryStructAss.getCurrency());
            slipAss.setBase(base);
            System.out.println("Base dans l'objet slipAss : " + slipAss.getBase());
            slipAss.setFrom_date(slipStart.toString());
            slipAss.setTo_date(slipEnd.toString());
            slipAss.setPosting_date(slipEnd.toString());

            Map<String, Object> result = createAssignmentAndSlip(slipAss);
            generatedSlips.add(result.get("slip").toString());

            current = current.plusMonths(1);
        }

        return generatedSlips;
    }

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
     *
     * @param employee le code employé
     * @param salaryComponent le nom du Salary Component (ex: "Basic")
     * @param condition "inf" ou "sup"
     * @param montant le montant seuil
     * @return Liste des montants trouvés
     */
    public List<SalaryFilterDTO> getSalaryComponentValues(String employee, String salaryComponent, String condition, double montant) throws Exception {
        String filters = "[[\"employee\",\"=\",\"" + employee + "\"]]";
        String fields = "[\"name\",\"employee\",\"posting_date\",\"salary_structure_assignment\",\"earnings\",\"deductions\"]";

        String url = baseUrl + "/api/resource/Salary Slip"
            + "?fields=" + URLEncoder.encode(fields, StandardCharsets.UTF_8)
            + "&filters=" + URLEncoder.encode(filters, StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Cookie", loginService.getSessionCookie());

        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );


        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Erreur lors de la récupération des Salary Slips : " + response.getBody());
        }

        JsonNode result = objectMapper.readTree(response.getBody()).path("data");
        List<SalaryFilterDTO> matchingResults = new ArrayList<>();

        for (JsonNode slip : result) {
            String slipName = slip.path("name").asText();
            String postingDate = slip.path("posting_date").asText();
            ArrayNode earnings = (ArrayNode) slip.path("earnings");

            for (JsonNode earning : earnings) {
                String comp = earning.path("salary_component").asText();
                double amount = earning.path("amount").asDouble();
                if (salaryComponent.equalsIgnoreCase(comp)) {
                    boolean matches = "inf".equals(condition) ? amount < montant : amount > montant;
                    if (matches) {
                        matchingResults.add(new SalaryFilterDTO(
                                slipName,
                                employee,
                                comp,
                                amount,
                                postingDate
                        ));
                    }
                }
            }
        }

        return matchingResults;
    }

    /**
     * Récupère le nom du Salary Structure Assignment pour un employé à une date donnée.
     * @param employee le code employé (ex: EMP001)
     * @param postingDate la date du Salary Slip (ex: 2025-06-23)
     * @return le nom du Salary Structure Assignment actif à cette date
     */
    public String getSalaryStructureAssignmentByEmployeeAndDate(SalaryFilterDTO salaryFilterDTO) throws Exception {
        String url = baseUrl + "/api/resource/Salary Structure Assignment" +
                "?fields=[\"name\",\"from_date\"]" +
                "&filters=" + URLEncoder.encode(
                    "[[\"employee\",\"=\",\"" + salaryFilterDTO.getEmployee() + "\"],[\"from_date\",\"<=\",\"" + salaryFilterDTO.getPostingDate() + "\"]]",
                    StandardCharsets.UTF_8
                ) +
                "&order_by=from_date desc" +
                "&limit_page_length=1";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Cookie", loginService.getSessionCookie());

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Erreur lors de la récupération du Salary Structure Assignment : " + response.getBody());
        }

        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
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
     * @param employees        la liste des employés concernés
     * @param salaryComponent  le nom du Salary Component à cibler
     * @param condition        "inf" ou "sup" (inférieur ou supérieur à un montant)
     * @param montant          le montant seuil à comparer
     * @param pourcentage      le pourcentage à appliquer (ajout ou retrait)
     * @throws Exception en cas d’erreur ou d’assignation manquante
     */
    public void applyModification(List<String> employees, String salaryComponent, String condition, double montant, double pourcentage) throws Exception {
        for (String employee : employees) {
            List<SalaryFilterDTO> matchingResults = getSalaryComponentValues(employee, salaryComponent, condition, montant);

            for (SalaryFilterDTO result : matchingResults) {
                // 🔍 Récupère le nom de l'Assignment actif à cette date
                String assignmentName = getSalaryStructureAssignmentByEmployeeAndDate(result);

                // 📦 Récupère l’objet complet SalaryStructAss
                SalaryStructAss salaryStructAss = getAssignmentById(assignmentName);

                if (salaryStructAss == null || salaryStructAss.getBase() == null) {
                    continue; // passe si aucune base trouvée
                }

                BigDecimal base = salaryStructAss.getBase();
                BigDecimal percentage = BigDecimal.valueOf(pourcentage).divide(BigDecimal.valueOf(100));

                BigDecimal adjustment;
                if ("inf".equals(condition)) {
                    adjustment = base.subtract(base.multiply(percentage));
                } else if ("sup".equals(condition)) {
                    adjustment = base.add(base.multiply(percentage));
                } else {
                    continue; // condition invalide
                }

                salaryStructAss.setBase(adjustment);
                updateAssignment(salaryStructAss); // ✏️ Mets à jour (via cancel + create + submit)
            }
        }
    }

}
