Bien sûr ! Voici un **exemple concret pour chaque étape** dans ERPNext, avec des données fictives :

---

### 🔹 1. **Créer un produit (Item)**

* **Item Code** : `PROD-001`
* **Item Name** : `Clavier Bluetooth`
* **Item Group** : `Accessoires Informatique`
* **UOM** : `Unit`
* **Purchase Item** : ✅ coché

---

### 🔹 2. **Demande de besoin (Material Request)**

* **Type** : `Purchase`
* **Item** : `Clavier Bluetooth`
* **Qty** : `20`
* **Schedule Date** : `2025-05-05`
* **Warehouse** : `Stock Entrée`

➡️ **Résultat** : demande créée pour acheter 20 claviers.

---

### 🔹 3. **Demande de devis (Request for Quotation)**

* **Fournisseurs** :

  * `INFO-MG SARL`
  * `TECHNOSHOP MADAGASCAR`

* **Article** : `Clavier Bluetooth`

* **Quantité demandée** : `20`

➡️ Le système peut envoyer l’e-mail automatiquement ou tu peux générer un PDF.

---

### 🔹 4. **Devis reçu (Supplier Quotation)**

* **Fournisseur** : `TECHNOSHOP MADAGASCAR`
* **Article** : `Clavier Bluetooth`
* **Prix unitaire** : `80 000 MGA`
* **Total** : `1 600 000 MGA`

➡️ Tu enregistres leur devis ici.

---

### 🔹 5. **Bon de commande (Purchase Order)**

* **Fournisseur** : `TECHNOSHOP MADAGASCAR`
* **Articles** : `Clavier Bluetooth`
* **Qty** : `20`
* **Prix unitaire** : `80 000 MGA`
* **Total** : `1 600 000 MGA`

➡️ Tu soumets le PO → le fournisseur va préparer la commande.

---

### 🔹 6. **Facture fournisseur (Purchase Invoice)**

* **Fournisseur** : `TECHNOSHOP MADAGASCAR`
* **Référence PO** : `PO-0001`
* **Date** : `2025-05-06`
* **Montant total** : `1 600 000 MGA`
* **TVA** : 20% → `320 000 MGA`
* **Montant TTC** : `1 920 000 MGA`

➡️ Tu peux marquer comme payé partiellement ou totalement selon le règlement.

---

### 🔹 7. **Voir statut des factures**

Exemple :

| Facture   | Fournisseur           | Total TTC | Statut         |
| --------- | --------------------- | --------- | -------------- |
| PINV-0001 | TECHNOSHOP MADAGASCAR | 1 920 000 | Partially Paid |
| PINV-0002 | INFO-MG SARL          | 3 200 000 | Unpaid         |

---
