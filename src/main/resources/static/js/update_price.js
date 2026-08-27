// Modification du prix d'un item de devis (appel AJAX POST, protégé par le jeton CSRF de la page)

document.addEventListener('DOMContentLoaded', function () {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

    document.querySelectorAll('.btn-modifier-prix').forEach(function (btn) {
        btn.addEventListener('click', function (e) {
            e.preventDefault();
            const itemCode = btn.getAttribute('data-itemcode');
            const devisId = btn.getAttribute('data-devisid');
            const prixUnitaire = btn.getAttribute('data-prixunitaire');
            const entrepot = btn.getAttribute('data-entrepot');

            const newPrice = prompt("Entrer le nouveau prix pour l'item " + itemCode + " :", prixUnitaire);
            if (newPrice !== null && newPrice !== '' && !isNaN(newPrice)) {
                const headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
                if (csrfToken) {
                    headers[csrfHeader] = csrfToken;
                }
                const url = '/fournisseurs/devis/' + encodeURIComponent(devisId)
                    + '/items/' + encodeURIComponent(itemCode)
                    + '/updatePrice?newPrice=' + encodeURIComponent(newPrice)
                    + '&entrepot=' + encodeURIComponent(entrepot);
                fetch(url, { method: 'POST', headers: headers })
                    .then(response => response.text().then(msg => ({ ok: response.ok, msg: msg })))
                    .then(result => {
                        alert(result.msg);
                        if (result.ok) {
                            window.location.reload();
                        }
                    })
                    .catch(err => alert('Erreur lors de la mise à jour du prix : ' + err));
            }
        });
    });
});
