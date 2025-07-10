import frappe
from datetime import datetime
import csv
import io
import base64
from dateutil.parser import parse

@frappe.whitelist()
def import_csv_files(file1=None, file2=None, file3=None):
    if not (file1 and file2 and file3):
        frappe.throw("3 fichiers requis.")


    try:
        decoded1 = base64.b64decode(file1)
        stream = io.StringIO(decoded1.decode("utf-8"))
        reader = csv.DictReader(stream)
    except Exception as e:
        frappe.log_error(f"Erreur de décodage CSV : {e}", "Erreur CSV")
        frappe.throw("Fichier CSV invalide.")
        frappe.db.roll

    try:
        first_row = next(reader)
        frappe.log_error(f"Header détecté : {first_row}", "DEBUG CSV HEADER")
    except StopIteration:
        frappe.throw("Le fichier CSV est vide.")

    required_fields = ["Nom", "Prenom", "genre", "Date embauche", "date naissance", "company"]
    if not all(field in first_row and first_row[field].strip() for field in required_fields):
        frappe.throw("La première ligne du CSV doit contenir tous les champs requis fichier 1.")

    company_name = first_row.get("company").strip()

    try:
        if not frappe.db.exists("Company", company_name):
            company = frappe.new_doc("Company")
            company.company_name = company_name
            company.abbr = company_name[:2].upper()
            company.default_currency = "USD"
            company.country = "France"
            company.insert(ignore_permissions=True)

            holiday_list = frappe.new_doc("Holiday List")
            holiday_list.holiday_list_name = f"Holiday List {company_name} 2025"
            holiday_list.from_date = datetime(2025, 1, 1)
            holiday_list.to_date = datetime(2025, 12, 31)
            holiday_list.is_default = 0
            holiday_list.append("holidays", {
                "description": "Nouvel An",
                "holiday_date": datetime(2025, 1, 1)
            })
            holiday_list.append("holidays", {
                "description": "Fête de l'indépendance",
                "holiday_date": datetime(2025, 6, 26)
            })
            holiday_list.insert(ignore_permissions=True)

            company.default_holiday_list = holiday_list.name
            company.save(ignore_permissions=True)
            # frappe.db.commit()
    except Exception as e:
        frappe.db.rollback()
        frappe.log_error(f"Erreur création société : {e}", "Erreur société")
        frappe.throw("Erreur lors de la création de l'entreprise.")

    # Traiter toutes les lignes
    all_rows = [first_row] + list(reader)
    ligne_num = 1
    nb_employes = 0

    for row in all_rows:
        ligne_num += 1
        try:
            row = normalize_row(row)
            nom = row.get("nom", "").strip()
            prenom = row.get("prenom", "").strip()
            genre_csv = row.get("genre", "").strip().upper()
            date_embauche_str = row.get("date embauche", "").strip()
            date_naissance_str = row.get("date naissance", "").strip()

            print(f"Ligne {ligne_num} : tentative création employé : {prenom} {nom}, genre={genre_csv}, embauche={date_embauche_str}, naissance={date_naissance_str}", "DEBUG employé")

            if not (nom and prenom and genre_csv and date_embauche_str and date_naissance_str):
                frappe.db.rollback()
                frappe.throw(f"Ligne {ligne_num} incomplète : champs requis manquants.")

            if genre_csv == "MASCULIN":
                genre = "Male"
            elif genre_csv == "FEMININ":
                genre = "Female"
            else:
                genre = "Other"

            try:
                date_embauche = parse_date(date_embauche_str,ligne_num)
                date_naissance = parse_date(date_naissance_str,ligne_num)

            except Exception as e:
                frappe.db.rollback()
                frappe.throw(f"Ligne {ligne_num} : erreur format date : {e}")

            employee = frappe.new_doc("Employee")
            employee.first_name = prenom
            employee.last_name = nom
            employee.gender = genre
            employee.date_of_birth = date_naissance
            employee.date_of_joining = date_embauche
            employee.company = company_name
            employee.insert(ignore_permissions=True)
            # frappe.db.commit()
            nb_employes += 1

        except Exception as e:
            frappe.db.rollback()
            frappe.log_error(f"Ligne {ligne_num} : erreur import employee: {e}", "Erreur import")
            frappe.throw(f"Ligne {ligne_num} : erreur import employee: {e}", "Erreur import{str(e)}")  # stoppe l'import immédiatement

    try:
        decoded1 = base64.b64decode(file2)
        stream2 = io.StringIO(decoded1.decode("utf-8"))
        reader2 = csv.DictReader(stream2)
    except Exception as e:
        frappe.db.rollback()
        frappe.log_error(f"Erreur de décodage CSV : {e}", "Erreur CSV")
        frappe.throw("Fichier CSV 2 invalide.")

    try:
        first_row = next(reader2)
        frappe.log_error(f"Header détecté : {first_row}", "DEBUG CSV HEADER")
    except StopIteration:
        frappe.db.rollback()
        frappe.throw("Le fichier CSV 2 est vide.")    

    required_fields = ["salary structure", "name", "Abbr", "type", "valeur", "company"]
    if not all(field in first_row and first_row[field].strip() for field in required_fields):
        frappe.db.rollback()
        frappe.throw("La première ligne du CSV doit contenir tous les champs requis fichier 2.")        

    all_rows2 = [first_row] + list(reader2)
    ligne_num_2 = 1
    nb_salary_component = 0

    for row in all_rows2:
        try:
            row = normalize_row(row)
            name = row.get("name", "").strip()

            if not frappe.db.exists("Salary Component", name):
                print(
                    f"Ligne {ligne_num_2} : Création du composant salarial : name={name}",
                    "DEBUG Salary Component"
                )

                abbr = row.get("abbr", "").strip()
                type_composant_csv = row.get("type", "").strip()
                if type_composant_csv == "earning":
                    type_composant = "Earning"
                elif type_composant_csv == "deduction":
                    type_composant = "Deduction"    
                valeur = row.get("valeur", "").strip()
                company_name = row.get("company", "").strip()
                company_abbr = frappe.db.get_value("Company", company_name, "abbr")
                frappe.log_error(row, "DEBUG row")
                frappe.log_error(f"Avant insert : name={name}, abbr={abbr}", "DEBUG AVANT INSERT")

                doc = frappe.get_doc({
                    "doctype": "Salary Component",
                    "salary_component": name,
                    "salary_component_abbr": abbr,
                    "type": type_composant,
                    "amount_based_on_formula": 1,
                    "condition": "True",
                    "formula": valeur,
                    "company": company_name,
                    "is_tax_applicable": 0,
                    "depends_on_payment_days":0,
                    "doctype": "Salary Component",
                    "accounts": [{
                        "company": company_name,
                        "account": "Cash - "+company_abbr
                    }]                    
                })

                doc.insert()

                nb_salary_component += 1

        except Exception as e:
            frappe.db.rollback()
            frappe.log_error(
                f"Ligne {ligne_num_2} : Erreur {str(e)} avec données : {row}",
                "Erreur Salary Component"
            )
            frappe.throw(
                f"Ligne {ligne_num_2} : Erreur {str(e)} avec données : {row}",
                "Erreur insertion Salary Component"
            )
        ligne_num_2 += 1

    try:
        decoded3 = base64.b64decode(file2)
        stream3 = io.StringIO(decoded3.decode("utf-8"))
        reader3 = csv.DictReader(stream3)
    except Exception as e:
        frappe.db.rollback()
        frappe.log_error(f"Erreur de décodage CSV : {e}", "Erreur CSV")
        frappe.throw("Fichier CSV 2 invalide.")

    try:
        first_row = next(reader3)
        frappe.log_error(f"Header détecté : {first_row}", "DEBUG CSV HEADER")
    except StopIteration:
        frappe.db.rollback()
        frappe.throw("Le fichier CSV 2 est vide.")

    all_rows = [first_row] + list(reader3)
    ligne_num = 1
    nb_salary_structure = 0

    salary_structures_to_submit = []

    for row in all_rows:
        ligne_num += 1
        try:
            row = normalize_row(row)
            salary_structure = row.get("salary structure", "").strip()
            type_csv = row.get("type")
            component_name = row.get("name")
            component_abbr = row.get("abbr")
            valeur = row.get("valeur")
            company = row.get("company", "").strip()

            structure_exists = frappe.db.exists("Salary Structure", salary_structure)
            print(f"Creation de salary struct : {salary_structure}")
            if not structure_exists:
                doc = frappe.get_doc({
                    "doctype": "Salary Structure",
                    "name": salary_structure,
                    "company": company,
                    "is_active": "Yes",
                    "currency": "USD",
                    "mode_of_payment": "Cash",
                    "payroll_frequency": "Monthly",
                })

                if type_csv == "earning":
                    doc.append("earnings", {
                        "salary_component": component_name,
                        "salary_component_abbr": component_abbr,
                        "amount_based_on_formula": 1,
                        "formula": valeur
                    })
                elif type_csv == "deduction":
                    doc.append("deductions", {
                        "salary_component": component_name,
                        "salary_component_abbr": component_abbr,
                        "amount_based_on_formula": 1,
                        "formula": valeur
                    })

                doc.insert(ignore_permissions=True)
                nb_salary_structure += 1
                salary_structures_to_submit.append(doc.name)

            else:
                salary_struct = frappe.get_doc("Salary Structure", salary_structure)
                print(f"ajout de component {component_name} dans salary struct : {salary_structure}")

                if type_csv == "earning":
                    if not any(e.salary_component == component_name for e in salary_struct.earnings):
                        salary_struct.append("earnings", {
                            "salary_component": component_name,
                            "salary_component_abbr": component_abbr,
                            "amount_based_on_formula": 1,
                            "formula": valeur
                        })
                elif type_csv == "deduction":
                    if not any(d.salary_component == component_name for d in salary_struct.deductions):
                        salary_struct.append("deductions", {
                            "salary_component": component_name,
                            "salary_component_abbr": component_abbr,
                            "amount_based_on_formula": 1,
                            "formula": valeur
                        })

                salary_struct.save(ignore_permissions=True)
                salary_structures_to_submit.append(salary_struct.name)

            # frappe.db.commit()

        except Exception as e:
            frappe.db.rollback()
            frappe.throw(
                f"Ligne {ligne_num} : Erreur {str(e)} avec données : {row}",
                "Erreur Salary Structure"
            )

    # Après la boucle, soumettre tous les Salary Structure
    for name in set(salary_structures_to_submit):  # `set` pour éviter les doublons
        try:
            doc = frappe.get_doc("Salary Structure", name)
            if doc.docstatus == 0:
                doc.submit()
        except Exception as e:
            frappe.db.rollback()
            frappe.throw(f"Erreur lors du submit de {name} : {e}", "Erreur Submit Salary Structure")



        # fichierrrrrr 3
    try:
        decoded4 = base64.b64decode(file3)
        stream4 = io.StringIO(decoded4.decode("utf-8"))
        reader4 = csv.DictReader(stream4)
    except Exception as e:
        frappe.db.rollback()
        frappe.log_error(f"Erreur de décodage CSV : {e}", "Erreur CSV")
        frappe.throw("Fichier CSV 3 invalide.")

    try:
        first_row = next(reader4)
        frappe.log_error(f"Header détecté : {first_row}", "DEBUG CSV HEADER")
    except StopIteration:
        frappe.db.rollback()
        frappe.throw("Le fichier CSV est vide.")


    all_rows = [first_row] + list(reader4)
    ligne_num = 1
    nb_salary_structure_assignment = 0

    for row in all_rows:
        ligne_num += 1
        try:
            row = normalize_row(row)
            from_date_CSV = row.get("mois", "").strip()
            ref = row.get("ref employe")
            employee = f"HR-EMP-{str(ref).zfill(5)}"
            base = row.get("salaire base")
            salary_structure = row.get("salaire")

            try:
                from_date = parse_date(from_date_CSV, ligne_num)
            except Exception as e:
                frappe.db.rollback()
                frappe.throw(f"Ligne {ligne_num} : erreur format date : {e}")

            print(f"[{ligne_num}] ➤ Création Salary Structure Assignment pour {employee}")

            # Création Salary Structure Assignment
            doc = frappe.get_doc({
                "doctype": "Salary Structure Assignment",
                "employee": employee,
                "company": company_name,
                "salary_structure": salary_structure,
                "from_date": from_date,
                "base": base
            })
            doc.insert(ignore_permissions=True)
            doc.submit()

            nb_salary_structure_assignment += 1
            # frappe.db.commit()

            print(f"[{ligne_num}] ✔️ Assignment créé - Création Salary Slip...")

            # Création Salary Slip
            try:
                doc2 = frappe.get_doc({
                    "doctype": "Salary Slip",
                    "employee": employee,
                    "company": company_name,
                    "posting_date": from_date,
                    "payroll_frequency": "Monthly",
                    "salary_structure": salary_structure,
                    "mode_of_payment": "Cash"
                })
                doc2.insert(ignore_permissions=True)
                doc2.submit()

                # frappe.db.commit()
                print(f"[{ligne_num}] ✅ Salary Slip créé pour {employee}")

            except Exception as slip_error:
                frappe.db.rollback()
                frappe.throw(
                    f"Ligne {ligne_num} : Erreur Salary Slip : {str(slip_error)} avec données : {row}",
                    "Erreur Salary Slip"
                )
                print(f"[{ligne_num}] ❌ Erreur création Salary Slip : {slip_error}")

        except Exception as e:
            frappe.db.rollback()
            frappe.throw(
                f"Ligne {ligne_num} : Erreur globale : {str(e)} avec données : {row}",
                "Erreur Salary Structure"
            )
            print(f"[{ligne_num}] ❌ Erreur globale : {e}")
            
    frappe.db.commit()

    return {
        "status": "success",
        "nbEmploye": f"{nb_employes} employés importés avec succès.",
        "nbSalaryComponent": f"{nb_salary_component} Salary component inseres",
        "nbSalaryStruct": f"{nb_salary_structure} Salary Struct inseres",
        "nbSalaryAssignment": f"{nb_salary_structure_assignment} Salary Assignment inseres",
        "entreprise": company_name
    }

@frappe.whitelist()
def normalize_row(row):
    return {k.strip().lower(): v.strip() for k, v in row.items()}

@frappe.whitelist()
def parse_date(date_str, ligne_num):
    """
    Tente d'interpréter date_str en date, 
    en tolérant slash, tiret, chiffres à 1 ou 2 chiffres, etc.
    Lève frappe.ValidationError si l'analyse échoue.
    """
    date_str = (date_str or "").strip()
    if not date_str:
        frappe.throw(f"Ligne {ligne_num} : champ date vide.")
    try:
        # parse() est très permissif :
        #    "8/06/2024", "8-6-2024", "2024/6/8", "2024-06-08", etc. fonctionnent tous.
        dt = parse(date_str, dayfirst=True)  # dayfirst=True pour jj/mm/aaaa
        return dt.date()
    except Exception:
        frappe.throw(
            f"Ligne {ligne_num} : format de date invalide (« {date_str} »).\n"
            f"Exemples acceptés : 08/06/2024, 8/6/2024, 08-06-2024, 2024-06-08, etc."
        )
