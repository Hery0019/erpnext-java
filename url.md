Le champ outstanding_amount indique le montant encore dû. Si ce montant est > 0, la facture est impayée.
/api/resource/Purchase%20Invoice?filters=[["supplier", "=", "Fournisseur XYZ"], ["outstanding_amount", ">", 0]]

Une facture est considérée comme payée si outstanding_amount == 0.
GET /api/resource/Purchase%20Invoice?filters=[["supplier", "=", "Fournisseur XYZ"], ["outstanding_amount", "=", 0]]
