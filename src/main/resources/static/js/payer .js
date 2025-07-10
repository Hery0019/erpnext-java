function openPaymentModal() {
    document.getElementById('paymentModal').style.display = 'block';
}

function closePaymentModal() {
    document.getElementById('paymentModal').style.display = 'none';
}

function confirmPayment(factureNom) {
    const amount = document.getElementById('paymentAmount').value;
    if (!amount || amount <= 0) {
        alert('Veuillez entrer un montant valide');
        return;
    }
    
    console.log('Paiement en cours...', factureNom, amount);
    fetch(`/factures/${factureNom}/payer?amount=${amount}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => {
        console.log('Response:', response);
        return response.json();
    })
    .then(data => {
        console.log('Data:', data);
        if (data.message) {
            alert(data.message);
            window.location.reload();
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Erreur lors du paiement: ' + error.message);
    })
    .finally(() => {
        closePaymentModal();
    });
}

// Fermer le modal si on clique en dehors
window.onclick = function(event) {
    const modal = document.getElementById('paymentModal');
    if (event.target == modal) {
        closePaymentModal();
    }
}