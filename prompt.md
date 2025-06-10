creer une fonction getMonthlySalarySummaryByYear qui prend une annee et un String company en parametres et qui boucle la fonction getSalaryReport pour chaque mois de l'annee et retourne la valeur total de chaque salary components par mois, cette fonction getMonthlySalarySummaryByYear utilise la fonction getFormattedSalaryComponentNames pour obtenir les noms formatés des Salary Components.
Metter le resultat dans un List<Map<String, Object>> 

public Map<String, Object> getSalaryReport(LocalDate fromDate, LocalDate toDate, String company) {
        String url = "http://erpnext.localhost:8000/api/method/frappe.desk.query_report.run";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cookie", loginService.getSessionCookie());

        Map<String, Object> filters = Map.of(
            "report_name", "Salary Register",
            "filters", Map.of(
                "from_date", fromDate.toString(),
                "to_date", toDate.toString(),
                "company", company
            )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(filters, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        return response.getBody();
    }

    public List<String> getFormattedSalaryComponentNames() {
        String url = "http://erpnext.localhost:8000/api/resource/Salary Component?fields=[\"name\"]";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", loginService.getSessionCookie());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
        );

        List<String> formattedNames = new ArrayList<>();

        if (response.getStatusCode() == HttpStatus.OK) {
            List<Map<String, String>> data = (List<Map<String, String>>) response.getBody().get("data");

            for (Map<String, String> item : data) {
                String originalName = item.get("name");
                String formatted = originalName.toLowerCase().replace(" ", "_");
                formattedNames.add(formatted);
            }
        }

        return formattedNames;
    }