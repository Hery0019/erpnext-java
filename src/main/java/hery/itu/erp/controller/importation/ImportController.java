package hery.itu.erp.controller.importation;

import hery.itu.erp.service.importation.ImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Controller
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @GetMapping("/import")
    public String showImportPage() {
        System.out.println("Import");
        return "import"; 
    }

    @PostMapping("import/csv")
    @ResponseBody
    public ResponseEntity<?> importCsvFiles(
            @RequestParam("file1") MultipartFile file1,
            @RequestParam("file2") MultipartFile file2,
            @RequestParam("file3") MultipartFile file3
    ) {
        try {
            byte[] file1Bytes = file1.getBytes();
            byte[] file2Bytes = file2.getBytes();
            byte[] file3Bytes = file3.getBytes();

            Map result = importService.importCsvFiles(file1Bytes, file2Bytes, file3Bytes);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors de l'importation : " + e.getMessage());
        }
    }
}

