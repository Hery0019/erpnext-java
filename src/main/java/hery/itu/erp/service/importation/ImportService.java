package hery.itu.erp.service.importation;

import java.util.Base64;

import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.JsonNode;

import hery.itu.erp.erpnext.ErpNextClient;

@Service
public class ImportService {

    /** Méthode whitelistée côté ERPNext : util/salaireImport.py. */
    private static final String IMPORT_METHOD = "erpnext.api.salaireImport.import_csv_files";

    private final ErpNextClient client;

    public ImportService(ErpNextClient client) {
        this.client = client;
    }

    /** Envoie les trois CSV (base64) à la méthode d'import et renvoie sa réponse brute. */
    public JsonNode importCsvFiles(byte[] file1Bytes, byte[] file2Bytes, byte[] file3Bytes) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("file1", Base64.getEncoder().encodeToString(file1Bytes));
        body.add("file2", Base64.getEncoder().encodeToString(file2Bytes));
        body.add("file3", Base64.getEncoder().encodeToString(file3Bytes));
        return client.callMethodForm(IMPORT_METHOD, body);
    }
}
