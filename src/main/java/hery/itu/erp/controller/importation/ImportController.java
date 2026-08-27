package hery.itu.erp.controller.importation;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;

import hery.itu.erp.erpnext.ErpNextException;
import hery.itu.erp.erpnext.ErpNextValidationException;
import hery.itu.erp.service.importation.ImportService;

@Controller
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @GetMapping("/import")
    public String showImportPage() {
        return "import";
    }

    @PostMapping("/import/csv")
    @ResponseBody
    public ResponseEntity<?> importCsvFiles(
            @RequestParam("file1") MultipartFile file1,
            @RequestParam("file2") MultipartFile file2,
            @RequestParam("file3") MultipartFile file3) throws IOException {
        try {
            JsonNode result = importService.importCsvFiles(file1.getBytes(), file2.getBytes(), file3.getBytes());
            return ResponseEntity.ok(result);
        } catch (ErpNextValidationException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Import refusé : " + e.getErpNextMessage()));
        } catch (ErpNextException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "Erreur lors de l'importation : " + e.getErpNextMessage()));
        }
    }
}
