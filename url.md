Le champ outstanding_amount indique le montant encore dû. Si ce montant est > 0, la facture est impayée.
/api/resource/Purchase%20Invoice?filters=[["supplier", "=", "Fournisseur XYZ"], ["outstanding_amount", ">", 0]]

Une facture est considérée comme payée si outstanding_amount == 0.
GET /api/resource/Purchase%20Invoice?filters=[["supplier", "=", "Fournisseur XYZ"], ["outstanding_amount", "=", 0]]

Cela vous retourne tous les comptes utilisables en paid_from.
GET /api/resource/Account?filters=[["is_group","=","0"],["account_type","in",["Bank","Cash"]]]
