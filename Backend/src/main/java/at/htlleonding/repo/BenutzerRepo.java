package at.htlleonding.repo;

import static at.htlleonding.repo.RepositoryMapper.toUser;
import static at.htlleonding.repo.RepositoryMapper.toUserDto;
import static at.htlleonding.repo.RepositorySupport.hashPassword;
import static at.htlleonding.repo.RepositorySupport.isBlank;
import static at.htlleonding.repo.RepositorySupport.isSuperadmin;
import static at.htlleonding.repo.RepositorySupport.validateDepartmentAccess;
import static at.htlleonding.repo.RepositorySupport.validateUserPermissions;

import at.htlleonding.DTOs.*;
import at.htlleonding.DTOs.BenutzerRequestDTO;
import at.htlleonding.DTOs.LoginRequestDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.io.*;
import java.text.Normalizer;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;

@ApplicationScoped
public class BenutzerRepo extends MongoRepository {

    private final Object userImportWriteLock = new Object();

    @Inject
    AuditRepo auditRepo;

    @Inject
    BenutzerZugriff benutzerZugriff;

    public List<BenutzerDTO> getUsers() {
        List<BenutzerDTO> benutzer = getDatabase()
            .getCollection("benutzer")
            .find(new Document("geloescht", new Document("$ne", true)))
            .sort(new Document("nachname", 1))
            .map(RepositoryMapper::toUser)
            .map(RepositoryMapper::toUserDto)
            .into(new ArrayList<>());

        if (benutzer.isEmpty()) {
            throw new NotFoundException("Keine Benutzer in der Datenbank gefunden.");
        }

        return benutzer;
    }

    public BenutzerDTO loginWithTestAccount(LoginRequestDTO request) {
        if (request == null || request.email() == null || request.passwort() == null) {
            throw new BadRequestException("E-Mail und Passwort sind erforderlich.");
        }

        Document dokument = getDatabase()
            .getCollection("benutzer")
            .find(new Document("email", request.email().trim().toLowerCase()))
            .first();

        if (
            dokument == null ||
            !hashPassword(request.passwort()).equals(dokument.getString("passwortHash"))
        ) {
            throw new BadRequestException("E-Mail oder Passwort ist falsch.");
        }

        if (!dokument.getBoolean("aktiv", true)) {
            throw new BadRequestException("Dieses Benutzerkonto ist deaktiviert.");
        }

        return toUserDto(toUser(dokument));
    }

    public BenutzerDTO loginWithEntra(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Das Entra-Token enthält keine E-Mail-Adresse.");
        }

        Document dokument = benutzerZugriff.findUserByEntraEmail(email);

        if (dokument == null) {
            throw new NotFoundException(
                "Für dieses Microsoft-Konto wurde noch kein Mitarbeiterprofil angelegt."
            );
        }
        if (!dokument.getBoolean("aktiv", true)) {
            throw new BadRequestException("Dieses Benutzerkonto ist deaktiviert.");
        }
        return toUserDto(toUser(dokument));
    }

    public List<BenutzerDTO> getManagedUsers(String adminId) {
        BenutzerDTO admin = benutzerZugriff.requireAdmin(adminId);
        if (isSuperadmin(admin)) {
            return getUsers();
        }
        return getDatabase()
            .getCollection("benutzer")
            .find(
                new Document("abteilungId", admin.abteilungId()).append(
                    "geloescht",
                    new Document("$ne", true)
                )
            )
            .sort(new Document("nachname", 1))
            .map(RepositoryMapper::toUser)
            .map(RepositoryMapper::toUserDto)
            .into(new ArrayList<>());
    }

    public BenutzerDTO createUser(String adminId, BenutzerRequestDTO request) {
        BenutzerDTO admin = benutzerZugriff.requireAdmin(adminId);
        validateUserRequest(request);
        validateUserPermissions(admin, request);
        validatePreferredRoom(request);
        synchronized (userImportWriteLock) {
            String email = request.email().trim().toLowerCase(Locale.ROOT);
            Document bestehend = getDatabase()
                .getCollection("benutzer")
                .find(new Document("email", email))
                .first();
            if (bestehend != null && !bestehend.getBoolean("geloescht", false)) {
                throw new BadRequestException("Diese E-Mail-Adresse wird bereits verwendet.");
            }
            if (bestehend != null) {
                // Ältere Versionen haben gelöschte Konten als ausgeblendeten Datensatz behalten.
                // Vor dem erneuten Anlegen wird dieser Altbestand entfernt, damit der eindeutige
                // E-Mail-Index nicht kollidiert.
                getDatabase()
                    .getCollection("benutzer")
                    .deleteOne(new Document("_id", bestehend.getString("_id")));
            }
            String id = "USER-" + System.currentTimeMillis();
            Document dokument = buildUserDocument(id, request).append(
                "passwortHash",
                hashPassword("holter123")
            );
            getDatabase().getCollection("benutzer").insertOne(dokument);
            auditRepo.recordAuditEntry(adminId, "BENUTZER_ANGELEGT", request.email());
            return toUserDto(toUser(dokument));
        }
    }

    public BenutzerDTO updateUser(String adminId, String id, BenutzerRequestDTO request) {
        BenutzerDTO admin = benutzerZugriff.requireAdmin(adminId);
        validateUserRequest(request);
        Document alt = getDatabase()
            .getCollection("benutzer")
            .find(new Document("_id", id))
            .first();
        if (alt == null) throw new NotFoundException("Benutzer wurde nicht gefunden.");
        validateDepartmentAccess(admin, alt.getString("abteilungId"));
        validateDepartmentAccess(admin, request.abteilungId());
        validatePreferredRoom(request);
        if (alt.getBoolean("geloescht", false)) throw new BadRequestException(
            "Gelöschte Benutzer können nicht bearbeitet werden."
        );
        if (adminId.equals(id) && !request.aktiv()) throw new BadRequestException(
            "Das eigene Konto kann nicht deaktiviert werden."
        );
        if (
            !isSuperadmin(admin) && !alt.getString("rolle").equals(request.rolle())
        ) throw new BadRequestException("Nur Superadmins duerfen Adminrollen aendern.");
        Document dokument = buildUserDocument(id, request).append(
            "passwortHash",
            alt.getString("passwortHash")
        );
        if (alt.containsKey("entraEmail")) dokument.put("entraEmail", alt.getString("entraEmail"));
        getDatabase().getCollection("benutzer").replaceOne(new Document("_id", id), dokument);
        if (!request.aktiv()) deleteUserReservations(adminId, id);
        auditRepo.recordAuditEntry(adminId, "BENUTZER_GEAENDERT", request.email());
        return toUserDto(toUser(dokument));
    }

    public void deleteUser(String adminId, String id) {
        benutzerZugriff.requireSuperadmin(adminId);
        Document benutzer = getDatabase()
            .getCollection("benutzer")
            .find(new Document("_id", id))
            .first();
        if (benutzer == null) throw new NotFoundException("Benutzer wurde nicht gefunden.");
        if (!"USER".equals(benutzer.getString("rolle"))) {
            throw new BadRequestException(
                "Admin-Konten werden über die Rollenverwaltung geändert und nicht als Mitarbeiter gelöscht."
            );
        }
        deleteUserReservations(adminId, id);
        getDatabase().getCollection("benutzer").deleteOne(new Document("_id", id));
        auditRepo.recordAuditEntry(adminId, "BENUTZER_GELOESCHT", benutzer.getString("email"));
    }

    private void validateUserRequest(BenutzerRequestDTO request) {
        if (
            request == null ||
            isBlank(request.vorname()) ||
            isBlank(request.nachname()) ||
            isBlank(request.email()) ||
            isBlank(request.rolle()) ||
            isBlank(request.abteilungId()) ||
            isBlank(request.abteilungName())
        ) {
            throw new BadRequestException("Bitte alle Benutzerdaten ausfüllen.");
        }
    }

    private Document buildUserDocument(String id, BenutzerRequestDTO request) {
        return new Document("_id", id)
            .append("vorname", request.vorname().trim())
            .append("nachname", request.nachname().trim())
            .append("email", request.email().trim().toLowerCase())
            .append("rolle", request.rolle())
            .append("abteilungId", request.abteilungId())
            .append("abteilungName", request.abteilungName())
            .append("bevorzugterRaumId", request.bevorzugterRaumId())
            .append("aktiv", request.aktiv());
    }

    private void deleteUserReservations(String adminId, String benutzerId) {
        long anzahl = getDatabase()
            .getCollection("reservierungen")
            .deleteMany(new Document("benutzerId", benutzerId))
            .getDeletedCount();
        auditRepo.recordAuditEntry(
            adminId,
            "BENUTZER_RESERVIERUNGEN_GELOESCHT",
            benutzerId + ": " + anzahl
        );
    }

    public MitarbeiterImportErgebnisDTO validateUserImport(String adminId, InputStream datei) {
        return processUserImport(adminId, datei, false);
    }

    public MitarbeiterImportErgebnisDTO importUsers(String adminId, InputStream datei) {
        synchronized (userImportWriteLock) {
            return processUserImport(adminId, datei, true);
        }
    }

    public byte[] createUserImportTemplate(String adminId) {
        BenutzerDTO admin = benutzerZugriff.requireAdmin(adminId);
        Document abteilungsFilter = isSuperadmin(admin)
            ? new Document()
            : new Document("_id", admin.abteilungId());
        List<Document> abteilungen = getDatabase()
            .getCollection("abteilungen")
            .find(abteilungsFilter)
            .sort(new Document("name", 1))
            .into(new ArrayList<>());
        Document raumFilter = isSuperadmin(admin)
            ? new Document()
            : new Document("abteilungId", admin.abteilungId());
        List<Document> raeume = getDatabase()
            .getCollection("raeume")
            .find(raumFilter)
            .sort(new Document("name", 1))
            .into(new ArrayList<>());
        Map<String, String> standorte = getDatabase()
            .getCollection("standorte")
            .find()
            .into(new ArrayList<>())
            .stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    d -> d.getString("_id"),
                    d -> d.getString("name"),
                    (a, b) -> a
                )
            );

        try (
            Workbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream ausgabe = new ByteArrayOutputStream()
        ) {
            Sheet mitarbeiter = workbook.createSheet("Mitarbeiter");
            Sheet hinweise = workbook.createSheet("Hinweise und Listen");
            CellStyle kopf = workbook.createCellStyle();
            kopf.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
            kopf.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            kopf.setBorderBottom(BorderStyle.THIN);
            Font kopfSchrift = workbook.createFont();
            kopfSchrift.setBold(true);
            kopfSchrift.setColor(IndexedColors.WHITE.getIndex());
            kopf.setFont(kopfSchrift);

            String[] spalten = {
                "Vorname",
                "Nachname",
                "E-Mail",
                "Abteilung",
                "Bevorzugter Raum",
                "Aktiv",
            };
            Row kopfzeile = mitarbeiter.createRow(0);
            for (int index = 0; index < spalten.length; index++) {
                Cell zelle = kopfzeile.createCell(index);
                zelle.setCellValue(spalten[index]);
                zelle.setCellStyle(kopf);
            }
            mitarbeiter.createFreezePane(0, 1);
            mitarbeiter.setAutoFilter(new CellRangeAddress(0, 0, 0, spalten.length - 1));
            int[] breiten = { 18, 22, 36, 24, 42, 12 };
            for (int index = 0; index < breiten.length; index++) mitarbeiter.setColumnWidth(
                index,
                breiten[index] * 256
            );

            DataValidationHelper validationHelper = mitarbeiter.getDataValidationHelper();
            DataValidation aktivValidation = validationHelper.createValidation(
                validationHelper.createExplicitListConstraint(new String[] { "Ja", "Nein" }),
                new org.apache.poi.ss.util.CellRangeAddressList(1, 1000, 5, 5)
            );
            aktivValidation.setShowErrorBox(true);
            aktivValidation.createErrorBox("Ungültiger Wert", "Bitte Ja oder Nein auswählen.");
            mitarbeiter.addValidationData(aktivValidation);

            Row titel = hinweise.createRow(0);
            titel.createCell(0).setCellValue("Mitarbeiterimport");
            titel.getCell(0).setCellStyle(kopf);
            hinweise
                .createRow(2)
                .createCell(0)
                .setCellValue(
                    "Eine Zeile entspricht einem Mitarbeiter. Vorname, Nachname, E-Mail und Abteilung sind Pflichtfelder."
                );
            hinweise
                .createRow(3)
                .createCell(0)
                .setCellValue(
                    "Bevorzugter Raum ist optional. Verwenden Sie dafür exakt einen Eintrag aus der Liste unten."
                );
            hinweise
                .createRow(4)
                .createCell(0)
                .setCellValue(
                    "Aktiv ist optional und standardmäßig Ja. Adminrollen werden aus Sicherheitsgründen nicht über Excel vergeben."
                );
            Row listenKopf = hinweise.createRow(6);
            listenKopf.createCell(0).setCellValue("Gültige Abteilungen");
            listenKopf.createCell(1).setCellValue("Gültige bevorzugte Räume");
            listenKopf.getCell(0).setCellStyle(kopf);
            listenKopf.getCell(1).setCellStyle(kopf);
            for (int index = 0; index < abteilungen.size(); index++) {
                hinweise
                    .createRow(index + 7)
                    .createCell(0)
                    .setCellValue(abteilungen.get(index).getString("name"));
            }
            for (int index = 0; index < raeume.size(); index++) {
                Row zeile = hinweise.getRow(index + 7);
                if (zeile == null) zeile = hinweise.createRow(index + 7);
                Document raum = raeume.get(index);
                String standort = standorte.getOrDefault(
                    raum.getString("standortId"),
                    raum.getString("standortId")
                );
                zeile.createCell(1).setCellValue(standort + " · " + raum.getString("name"));
            }
            if (!abteilungen.isEmpty()) {
                Name abteilungsListe = workbook.createName();
                abteilungsListe.setNameName("GueltigeAbteilungen");
                abteilungsListe.setRefersToFormula(
                    "'Hinweise und Listen'!$A$8:$A$" + (7 + abteilungen.size())
                );
                DataValidation validation = validationHelper.createValidation(
                    validationHelper.createFormulaListConstraint("GueltigeAbteilungen"),
                    new org.apache.poi.ss.util.CellRangeAddressList(1, 1000, 3, 3)
                );
                validation.setShowErrorBox(true);
                validation.createErrorBox(
                    "Unbekannte Abteilung",
                    "Bitte eine Abteilung aus der Liste auswählen."
                );
                mitarbeiter.addValidationData(validation);
            }
            if (!raeume.isEmpty()) {
                Name raumListe = workbook.createName();
                raumListe.setNameName("GueltigeRaeume");
                raumListe.setRefersToFormula(
                    "'Hinweise und Listen'!$B$8:$B$" + (7 + raeume.size())
                );
                DataValidation validation = validationHelper.createValidation(
                    validationHelper.createFormulaListConstraint("GueltigeRaeume"),
                    new org.apache.poi.ss.util.CellRangeAddressList(1, 1000, 4, 4)
                );
                validation.setShowErrorBox(true);
                validation.setEmptyCellAllowed(true);
                validation.createErrorBox(
                    "Unbekannter Raum",
                    "Bitte einen Raum aus der Liste auswählen oder das Feld leer lassen."
                );
                mitarbeiter.addValidationData(validation);
            }
            hinweise.setColumnWidth(0, 34 * 256);
            hinweise.setColumnWidth(1, 52 * 256);
            workbook.write(ausgabe);
            return ausgabe.toByteArray();
        } catch (IOException exception) {
            throw new BadRequestException("Die Excel-Vorlage konnte nicht erstellt werden.");
        }
    }

    private MitarbeiterImportErgebnisDTO processUserImport(
        String adminId,
        InputStream datei,
        boolean speichern
    ) {
        BenutzerDTO admin = benutzerZugriff.requireAdmin(adminId);
        List<MitarbeiterImportFehlerDTO> fehler = new ArrayList<>();
        List<MitarbeiterImportZeileDTO> vorschau = new ArrayList<>();
        List<BenutzerRequestDTO> gueltigeAnfragen = new ArrayList<>();
        Set<String> dateiEmails = new HashSet<>();
        Set<String> vorhandeneEmails = new HashSet<>();
        getDatabase()
            .getCollection("benutzer")
            .find()
            .projection(new Document("email", 1).append("geloescht", 1))
            .forEach(dokument -> {
                String email = dokument.getString("email");
                if (!dokument.getBoolean("geloescht", false) && !isBlank(email)) {
                    vorhandeneEmails.add(email.trim().toLowerCase(Locale.ROOT));
                }
            });

        Map<String, Document> abteilungen = new HashMap<>();
        getDatabase()
            .getCollection("abteilungen")
            .find()
            .forEach(dokument -> {
                abteilungen.put(normalizeExcelValue(dokument.getString("name")), dokument);
                abteilungen.put(normalizeExcelValue(dokument.getString("_id")), dokument);
            });
        Map<String, String> standorte = new HashMap<>();
        getDatabase()
            .getCollection("standorte")
            .find()
            .forEach(dokument ->
                standorte.put(dokument.getString("_id"), dokument.getString("name"))
            );
        Map<String, List<Document>> raumSchluessel = new HashMap<>();
        getDatabase()
            .getCollection("raeume")
            .find()
            .forEach(raum -> {
                addRoomImportKey(raumSchluessel, raum.getString("_id"), raum);
                addRoomImportKey(raumSchluessel, raum.getString("name"), raum);
                addRoomImportKey(
                    raumSchluessel,
                    standorte.getOrDefault(raum.getString("standortId"), "") +
                        " · " +
                        raum.getString("name"),
                    raum
                );
            });

        int gesamt = 0;
        try (Workbook workbook = WorkbookFactory.create(datei)) {
            if (workbook.getNumberOfSheets() == 0) throw new BadRequestException(
                "Die Excel-Datei enthält kein Tabellenblatt."
            );
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) throw new BadRequestException(
                "Die Excel-Datei enthält keine Überschriften."
            );
            DataFormatter formatter = new DataFormatter(Locale.GERMAN);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Map<String, Integer> spalten = new HashMap<>();
            for (Cell zelle : header)
                spalten.put(
                    normalizeExcelValue(formatter.formatCellValue(zelle, evaluator)),
                    zelle.getColumnIndex()
                );
            String[] pflicht = { "vorname", "nachname", "email", "abteilung" };
            for (String feld : pflicht) {
                if (!spalten.containsKey(feld)) throw new BadRequestException(
                    "Pflichtspalte fehlt: " + feld + ". Bitte die Vorlage verwenden."
                );
            }
            Integer raumSpalte = spalten.get("bevorzugterraum");
            Integer aktivSpalte = spalten.get("aktiv");

            for (
                int zeilenIndex = header.getRowNum() + 1;
                zeilenIndex <= sheet.getLastRowNum();
                zeilenIndex++
            ) {
                Row zeile = sheet.getRow(zeilenIndex);
                if (zeile == null || isImportRowEmpty(zeile, formatter, evaluator)) continue;
                gesamt++;
                int excelZeile = zeilenIndex + 1;
                if (gesamt > 1000) {
                    fehler.add(
                        new MitarbeiterImportFehlerDTO(
                            excelZeile,
                            "Datei",
                            "Maximal 1.000 Mitarbeiter pro Import."
                        )
                    );
                    break;
                }
                String vorname = excelText(zeile, spalten.get("vorname"), formatter, evaluator);
                String nachname = excelText(zeile, spalten.get("nachname"), formatter, evaluator);
                String email = excelText(
                    zeile,
                    spalten.get("email"),
                    formatter,
                    evaluator
                ).toLowerCase(Locale.ROOT);
                String abteilungText = excelText(
                    zeile,
                    spalten.get("abteilung"),
                    formatter,
                    evaluator
                );
                String raumText =
                    raumSpalte == null ? "" : excelText(zeile, raumSpalte, formatter, evaluator);
                String aktivText =
                    aktivSpalte == null ? "" : excelText(zeile, aktivSpalte, formatter, evaluator);
                boolean aktiv =
                    isBlank(aktivText) ||
                    Set.of("ja", "yes", "true", "1", "aktiv").contains(
                        normalizeExcelValue(aktivText)
                    );
                boolean zeileGueltig = true;

                if (isBlank(vorname)) {
                    fehler.add(importError(excelZeile, "Vorname", "Vorname fehlt."));
                    zeileGueltig = false;
                }
                if (isBlank(nachname)) {
                    fehler.add(importError(excelZeile, "Nachname", "Nachname fehlt."));
                    zeileGueltig = false;
                }
                if (isBlank(email) || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                    fehler.add(
                        importError(
                            excelZeile,
                            "E-Mail",
                            "Bitte eine gültige E-Mail-Adresse eintragen."
                        )
                    );
                    zeileGueltig = false;
                } else if (!dateiEmails.add(email)) {
                    fehler.add(
                        importError(
                            excelZeile,
                            "E-Mail",
                            "Diese E-Mail-Adresse kommt in der Datei mehrfach vor."
                        )
                    );
                    zeileGueltig = false;
                } else if (vorhandeneEmails.contains(email)) {
                    fehler.add(
                        importError(
                            excelZeile,
                            "E-Mail",
                            "Für diese E-Mail-Adresse existiert bereits ein Konto."
                        )
                    );
                    zeileGueltig = false;
                }
                Document abteilung = abteilungen.get(normalizeExcelValue(abteilungText));
                if (abteilung == null) {
                    fehler.add(
                        importError(
                            excelZeile,
                            "Abteilung",
                            "Unbekannte Abteilung. Bitte einen Wert aus der Vorlage verwenden."
                        )
                    );
                    zeileGueltig = false;
                } else if (
                    !isSuperadmin(admin) && !admin.abteilungId().equals(abteilung.getString("_id"))
                ) {
                    fehler.add(
                        importError(
                            excelZeile,
                            "Abteilung",
                            "Als Abteilungsleitung können Sie nur Ihre eigene Abteilung importieren."
                        )
                    );
                    zeileGueltig = false;
                }
                if (
                    !isBlank(aktivText) &&
                    !Set.of(
                        "ja",
                        "yes",
                        "true",
                        "1",
                        "aktiv",
                        "nein",
                        "no",
                        "false",
                        "0",
                        "inaktiv"
                    ).contains(normalizeExcelValue(aktivText))
                ) {
                    fehler.add(importError(excelZeile, "Aktiv", "Erlaubt sind Ja oder Nein."));
                    zeileGueltig = false;
                }

                String raumId = "";
                String raumAnzeige = "";
                if (!isBlank(raumText)) {
                    List<Document> treffer = raumSchluessel.getOrDefault(
                        normalizeExcelValue(raumText),
                        List.of()
                    );
                    if (treffer.isEmpty()) {
                        fehler.add(
                            importError(
                                excelZeile,
                                "Bevorzugter Raum",
                                "Raum wurde nicht gefunden. Bitte einen Wert aus der Vorlage verwenden."
                            )
                        );
                        zeileGueltig = false;
                    } else if (treffer.size() > 1) {
                        fehler.add(
                            importError(
                                excelZeile,
                                "Bevorzugter Raum",
                                "Raumname ist nicht eindeutig. Bitte Standort · Raum verwenden."
                            )
                        );
                        zeileGueltig = false;
                    } else {
                        Document raum = treffer.get(0);
                        raumId = raum.getString("_id");
                        raumAnzeige =
                            standorte.getOrDefault(raum.getString("standortId"), "") +
                            " · " +
                            raum.getString("name");
                        if (
                            abteilung != null &&
                            !Objects.equals(
                                abteilung.getString("_id"),
                                raum.getString("abteilungId")
                            )
                        ) {
                            fehler.add(
                                importError(
                                    excelZeile,
                                    "Bevorzugter Raum",
                                    "Der Raum gehört nicht zur angegebenen Abteilung."
                                )
                            );
                            zeileGueltig = false;
                        }
                    }
                }
                String abteilungName =
                    abteilung == null ? abteilungText : abteilung.getString("name");
                vorschau.add(
                    new MitarbeiterImportZeileDTO(
                        excelZeile,
                        vorname,
                        nachname,
                        email,
                        abteilungName,
                        raumAnzeige,
                        aktiv
                    )
                );
                if (zeileGueltig) {
                    gueltigeAnfragen.add(
                        new BenutzerRequestDTO(
                            vorname,
                            nachname,
                            email,
                            "USER",
                            abteilung.getString("_id"),
                            abteilung.getString("name"),
                            raumId,
                            aktiv
                        )
                    );
                }
            }
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException(
                "Die Excel-Datei konnte nicht gelesen werden. Bitte eine unveränderte .xlsx-Datei verwenden."
            );
        }

        int importiert = 0;
        if (speichern && fehler.isEmpty() && !gueltigeAnfragen.isEmpty()) {
            long importZeit = System.currentTimeMillis();
            List<Document> dokumente = new ArrayList<>();
            for (int index = 0; index < gueltigeAnfragen.size(); index++) {
                BenutzerRequestDTO request = gueltigeAnfragen.get(index);
                dokumente.add(
                    buildUserDocument("USER-IMPORT-" + importZeit + "-" + index, request).append(
                        "passwortHash",
                        hashPassword("holter123")
                    )
                );
            }
            List<String> importEmails = dokumente
                .stream()
                .map(dokument -> dokument.getString("email"))
                .toList();
            // Bereinigt Soft-Delete-Altbestände aus früheren Versionen. Aktive oder nur
            // deaktivierte Konten wurden bereits oben als Duplikat abgewiesen.
            getDatabase()
                .getCollection("benutzer")
                .deleteMany(
                    new Document("email", new Document("$in", importEmails)).append(
                        "geloescht",
                        true
                    )
                );
            getDatabase().getCollection("benutzer").insertMany(dokumente);
            importiert = dokumente.size();
            auditRepo.recordAuditEntry(
                adminId,
                "MITARBEITER_IMPORTIERT",
                importiert + " Mitarbeiter"
            );
        }
        return new MitarbeiterImportErgebnisDTO(
            gesamt,
            gueltigeAnfragen.size(),
            importiert,
            vorschau,
            fehler
        );
    }

    private void addRoomImportKey(
        Map<String, List<Document>> schluessel,
        String wert,
        Document raum
    ) {
        if (isBlank(wert)) return;
        List<Document> treffer = schluessel.computeIfAbsent(normalizeExcelValue(wert), ignoriert ->
            new ArrayList<>()
        );
        if (
            treffer
                .stream()
                .noneMatch(eintrag ->
                    Objects.equals(eintrag.getString("_id"), raum.getString("_id"))
                )
        ) {
            treffer.add(raum);
        }
    }

    private MitarbeiterImportFehlerDTO importError(int zeile, String feld, String meldung) {
        return new MitarbeiterImportFehlerDTO(zeile, feld, meldung);
    }

    private boolean isImportRowEmpty(
        Row zeile,
        DataFormatter formatter,
        FormulaEvaluator evaluator
    ) {
        for (Cell zelle : zeile)
            if (!formatter.formatCellValue(zelle, evaluator).trim().isEmpty()) return false;
        return true;
    }

    private String excelText(
        Row zeile,
        int spalte,
        DataFormatter formatter,
        FormulaEvaluator evaluator
    ) {
        Cell zelle = zeile.getCell(spalte, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return zelle == null ? "" : formatter.formatCellValue(zelle, evaluator).trim();
    }

    private String normalizeExcelValue(String wert) {
        if (wert == null) return "";
        String normalisiert = Normalizer.normalize(
            wert.trim().toLowerCase(Locale.ROOT),
            Normalizer.Form.NFD
        ).replaceAll("\\p{M}", "");
        return normalisiert.replace("ß", "ss").replaceAll("[^a-z0-9]", "");
    }

    private void validatePreferredRoom(BenutzerRequestDTO request) {
        if (isBlank(request.bevorzugterRaumId())) return;
        Document raum = getDatabase()
            .getCollection("raeume")
            .find(new Document("_id", request.bevorzugterRaumId()))
            .first();
        if (raum == null || !Objects.equals(raum.getString("abteilungId"), request.abteilungId())) {
            throw new BadRequestException(
                "Der bevorzugte Raum muss zur ausgewählten Abteilung gehören."
            );
        }
    }
}
