// Script pour afficher un formulaire de modification de prix et envoyer la requête AJAX

document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('.btn-modifier-prix').forEach(function(btn) {
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            const itemCode = btn.getAttribute('data-itemcode');
            const devisId = btn.getAttribute('data-devisid');
            const prixUnitaire = btn.getAttribute('data-prixunitaire');
            const entrepot = btn.getAttribute('data-entrepot');

            // Afficher un prompt pour le nouveau prix
            const newPrice = prompt('Entrer le nouveau prix pour l\'item ' + itemCode + ':', prixUnitaire);
            if(newPrice !== null && newPrice !== '' && !isNaN(newPrice)) {
                fetch(`/fournisseurs/devis/${devisId}/items/${itemCode}/updatePrice?newPrice=${newPrice}&entrepot=${encodeURIComponent(entrepot)}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    }
                })
                .then(response => response.text())
                .then(msg => {
                    alert(msg);
                    window.location.reload();
                })
                .catch(err => alert('Erreur lors de la mise à jour du prix : ' + err));
            }
        });
    });
});
