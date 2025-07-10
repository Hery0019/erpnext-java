package hery.itu.erp.service.util;

import org.springframework.stereotype.Service;

@Service
public class StringConvertService {
    
    /**
     * Convertit une chaîne au format "HR-EMP-00001" en "1"
     * @param input la chaîne à convertir (ex: "HR-EMP-00001")
     * @return le nombre extrait sous forme de chaîne (ex: "1")
     * @throws IllegalArgumentException si le format n'est pas respecté
     */
    public String convertEmployeeId(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("L'input ne peut pas être null ou vide");
        }
        
        // Découper la chaîne en utilisant le tiret comme séparateur
        String[] parts = input.split("-");
        
        // Vérifier qu'on a bien 3 parties et que la dernière commence par des zéros
        if (parts.length != 3 || !parts[2].matches("0+\\d+")) {
            throw new IllegalArgumentException("Format d'ID employé invalide. Format attendu: HR-EMP-00001");
        }
        
        try {
            // Supprimer les zéros non significatifs et convertir en String
            String numberStr = parts[2].replaceFirst("^0+", "");
            return numberStr.isEmpty() ? "0" : numberStr; // Cas où l'input est "HR-EMP-00000"
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La partie numérique de l'ID employé est invalide", e);
        }
    }
}