function editPrice(button) {
    const parent = button.parentElement;
    const currentDisplay = button.innerHTML;
    const devisId = button.getAttribute('data-devis-id');
    const itemCode = button.getAttribute('data-item-code');
    const currentPrice = button.getAttribute('data-current-price');
    
    // Créer l'input
    const input = document.createElement('input');
    input.type = 'number';
    input.step = '0.01';
    input.value = currentPrice;
    input.className = 'price-input';

    // Créer le bouton de sauvegarde
    const saveButton = document.createElement('button');
    saveButton.innerHTML = '✓';
    saveButton.className = 'save-price';
    saveButton.onclick = () => savePrice(devisId, itemCode, input.value, parent, currentDisplay);

    // Créer le bouton d'annulation
    const cancelButton = document.createElement('button');
    cancelButton.innerHTML = '✕';
    cancelButton.className = 'cancel-edit';
    cancelButton.onclick = () => cancelEdit(parent, currentDisplay);

    // Remplacer le contenu
    parent.innerHTML = '';
    parent.appendChild(input);
    parent.appendChild(saveButton);
    parent.appendChild(cancelButton);
    
    input.focus();
}

function savePrice(devisId, itemCode, newPrice, parent, oldDisplay) {
    fetch(`/fournisseurs/devis/${devisId}/items/${itemCode}/updatePrice?newPrice=${newPrice}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => {
        if (response.ok) {
            // Recharger la page pour afficher les nouvelles valeurs
            window.location.reload();
        } else {
            throw new Error('Erreur lors de la mise à jour du prix');
        }
    })
    .catch(error => {
        console.error('Erreur:', error);
        cancelEdit(parent, oldDisplay);
        alert('Erreur lors de la mise à jour du prix');
    });
}

function cancelEdit(parent, oldDisplay) {
    parent.innerHTML = oldDisplay;
}
