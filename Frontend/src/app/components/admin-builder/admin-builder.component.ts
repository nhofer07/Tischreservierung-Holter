import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, EventEmitter, HostListener, Input, OnInit, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Abteilung, Arbeitsplatz, ArbeitsplatzRequest, AuditEintrag, Benutzer, BenutzerRequest, FirmenDesignHistorie, FirmenEinstellung, Raum, RaumAuswahl, RaumElement, RaumRequest, Reservierung, Standort, StandortRequest, Zeitraum } from '../../models';
import { ArbeitsplatzService } from '../../services/arbeitsplatz.service';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-admin-builder',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-builder.component.html',
  styleUrl: './admin-builder.component.css'
})
export class AdminBuilderComponent implements OnInit {
  @Output() nachricht = new EventEmitter<string>();
  @Output() fehler = new EventEmitter<string>();
  @Output() standorteGeaendert = new EventEmitter<void>();
  @Output() designGeaendert = new EventEmitter<FirmenEinstellung>();

  private adminWert: Benutzer | null = null;
  private standortListe: Standort[] = [];

  @Input({ required: true }) set admin(value: Benutzer) {
    this.adminWert = value;
  }

  get admin(): Benutzer {
    return this.adminWert!;
  }

  get istSuperadmin(): boolean { return this.admin.rolle === 'SUPERADMIN'; }

  @Input() set standorte(value: Standort[]) {
    this.standortListe = value;
  }

  get standorte(): Standort[] {
    return this.standortListe;
  }

  bereich: 'builder' | 'stammdaten' | 'benutzer' | 'reservierungen' | 'audit' = 'builder';
  modus: 'tisch' | 'raum' | 'grundriss' = 'tisch';
  werkzeug: RaumElement['typ'] | 'auswahl' = 'auswahl';
  raeume: RaumAuswahl[] = [];
  stammdatenRaeume: RaumAuswahl[] = [];
  raum: Raum | null = null;
  standortId = '';
  raumId = '';
  ausgewaehlterTisch: Arbeitsplatz | null = null;
  ausgewaehltesElement: RaumElement | null = null;
  abteilungen: Abteilung[] = [];
  equipmentListe: string[] = [];
  alleReservierungen: Reservierung[] = [];
  benutzerListe: Benutzer[] = [];
  auditEintraege: AuditEintrag[] = [];
  ausgewaehlterBenutzerId = '';
  benutzerForm: BenutzerRequest = this.leererBenutzer();
  standortForm: StandortRequest = { name: '', adresse: '', ort: '' };
  bearbeiteterStandortId = '';
  reservierungsSuche = '';
  reservierungsStandort = '';
  reservierungsRaum = '';
  reservierungsStatus = '';
  reservierungsDatum = '';
  stornierungsgrund = '';
  neueAbteilung = '';
  neuesEquipment = '';
  firmenEinstellung: FirmenEinstellung = { firmenname: 'HOLTER', produktname: 'DeskVision', primaerfarbe: '#a51e2d', akzentfarbe: '#343638', logoUrl: '/holter-logo.png' };
  designHistorie: FirmenDesignHistorie[] = [];
  laden = false;
  benutzerWerdenGeladen = false;
  reservierungenWerdenGeladen = false;
  rasterAktiv = true;
  zoom = 1;
  ansichtX = 0;
  ansichtY = 0;
  grundrissVollbild = false;
  ausgewaehlteElementIds: string[] = [];
  historie: RaumElement[][] = [];
  wiederholenHistorie: RaumElement[][] = [];
  private gespeicherterGrundriss = '';
  private tischWirdGezogen = false;
  private tischAktion: 'verschieben' | 'groesse' | 'drehen' | '' = '';
  private tischStart: { mausX: number; mausY: number; x: number; y: number; breite: number; hoehe: number; rotation: number } | null = null;
  private tischResizeRichtung: 'nw' | 'ne' | 'sw' | 'se' = 'se';

  raumForm: RaumRequest = this.leererRaum();
  tischForm: ArbeitsplatzRequest = this.leererTisch();

  private startpunkt: { x: number; y: number } | null = null;
  wandVorschau: { x: number; y: number; breite: number; hoehe: number } | null = null;
  private dragElementId = '';
  private dragVersatz = { x: 0, y: 0 };
  private resizeElementId = '';
  private rotateElementId = '';
  private resizeRichtung: 'nw' | 'ne' | 'sw' | 'se' = 'se';
  private resizeStart: { mausX: number; mausY: number; x: number; y: number; breite: number; hoehe: number } | null = null;

  constructor(private service: ArbeitsplatzService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.stammdatenLaden();
    this.benutzerLaden();
    this.reservierungenLaden();
    if (this.istSuperadmin) this.firmenEinstellungenLaden();
  }

  firmenEinstellungenLaden(): void {
    this.service.getFirmenEinstellungen().subscribe({
      next: (einstellung) => { this.firmenEinstellung = einstellung; this.cdr.detectChanges(); },
      error: () => this.fehler.emit('Firmendesign konnte nicht geladen werden.')
    });
    this.service.getFirmenDesignHistorie(this.admin.id).subscribe((historie) => {
      this.designHistorie = historie;
      this.cdr.detectChanges();
    });
  }

  firmenEinstellungenSpeichern(): void {
    this.service.firmenEinstellungenSpeichern(this.admin.id, this.firmenEinstellung).subscribe({
      next: (einstellung) => {
        this.firmenEinstellung = einstellung;
        this.designGeaendert.emit(einstellung);
        this.firmenEinstellungenLaden();
        this.nachricht.emit('Firmendesign wurde gespeichert.');
      },
      error: () => this.fehler.emit('Firmendesign konnte nicht gespeichert werden. Bitte Eingaben prüfen.')
    });
  }

  firmenDesignZuruecksetzen(): void {
    this.firmenEinstellung = { firmenname: 'HOLTER', produktname: 'DeskVision', primaerfarbe: '#a51e2d', akzentfarbe: '#343638', logoUrl: '/holter-logo.png' };
    this.firmenEinstellungenSpeichern();
  }

  designAusHistorieVerwenden(eintrag: FirmenDesignHistorie): void {
    this.service.firmenDesignAusHistorieVerwenden(this.admin.id, eintrag.id).subscribe({
      next: (einstellung) => {
        this.firmenEinstellung = einstellung;
        this.designGeaendert.emit(einstellung);
        this.nachricht.emit('Designvariante wurde angewendet.');
      },
      error: () => this.fehler.emit('Designvariante konnte nicht angewendet werden.')
    });
  }

  designHistorieLoeschen(eintrag: FirmenDesignHistorie, event: Event): void {
    event.stopPropagation();
    this.service.firmenDesignHistorieLoeschen(this.admin.id, eintrag.id).subscribe({
      next: () => {
        this.designHistorie = this.designHistorie.filter((item) => item.id !== eintrag.id);
        this.nachricht.emit('Designvariante wurde aus der Historie entfernt.');
      },
      error: () => this.fehler.emit('Designvariante konnte nicht entfernt werden.')
    });
  }

  logoAuswaehlen(event: Event): void {
    const input = event.target as HTMLInputElement;
    const datei = input.files?.[0];
    if (!datei) return;
    if (!datei.type.startsWith('image/')) { this.fehler.emit('Bitte eine Bilddatei auswählen.'); return; }
    if (datei.size > 2_000_000) { this.fehler.emit('Das Logo darf maximal 2 MB groß sein.'); input.value = ''; return; }
    const leser = new FileReader();
    leser.onerror = () => this.fehler.emit('Die Bilddatei konnte nicht gelesen werden.');
    leser.onload = () => {
      const logoUrl = String(leser.result);
      const bild = new Image();
      bild.onerror = () => this.fehler.emit('Die ausgewählte Datei ist kein gültiges Bild.');
      bild.onload = () => {
        this.firmenEinstellung.logoUrl = logoUrl;
        this.cdr.detectChanges();
        this.firmenEinstellungenSpeichern();
        input.value = '';
      };
      bild.src = logoUrl;
    };
    leser.readAsDataURL(datei);
  }

  bereichOeffnen(bereich: 'builder' | 'stammdaten' | 'benutzer' | 'reservierungen' | 'audit'): void {
    if (this.bereich === 'builder' && bereich !== 'builder' && !this.darfGrundrissVerlassen()) return;
    this.bereich = bereich;
    if (bereich === 'stammdaten') this.stammdatenLaden();
    if (bereich === 'reservierungen') this.reservierungenLaden();
    if (bereich === 'benutzer') this.benutzerLaden();
    if (bereich === 'audit') this.auditLaden();
  }

  standortAendern(id: string): void {
    if (!this.darfGrundrissVerlassen()) return;
    this.standortId = id;
    this.raumId = '';
    this.raum = null;
    this.raeume = [];
    this.raeumeLaden();
  }

  raumAendern(id: string): void {
    if (!this.darfGrundrissVerlassen()) return;
    this.raumId = id;
    if (this.raumId) this.raumLaden(this.raumId);
  }

  get raumGruppen(): { abteilung: string; raeume: RaumAuswahl[] }[] {
    const gruppen = new Map<string, RaumAuswahl[]>();
    for (const raum of this.raeume) {
      const liste = gruppen.get(raum.abteilungName) ?? [];
      liste.push(raum);
      gruppen.set(raum.abteilungName, liste);
    }
    return [...gruppen.entries()]
      .map(([abteilung, raeume]) => ({ abteilung, raeume }))
      .sort((a, b) => a.abteilung.localeCompare(b.abteilung));
  }

  raeumeLaden(gewuenschterRaumId = ''): void {
    if (!this.standortId) return;
    this.laden = true;
    const angefragterStandort = this.standortId;
    this.service.getRaeumeByStandort(angefragterStandort).subscribe({
      next: (raeume) => {
        if (this.standortId !== angefragterStandort) return;
        const sichtbareRaeume = this.istSuperadmin
          ? raeume
          : raeume.filter((raum) => raum.abteilungName === this.admin.abteilungName);
        this.raeume = sichtbareRaeume;
        const ziel = sichtbareRaeume.find((raum) => raum.id === gewuenschterRaumId);
        if (ziel) this.raumLaden(ziel.id);
        else { this.raumId = ''; this.raum = null; }
        this.laden = false;
        this.cdr.detectChanges();
      },
      error: () => {
        if (this.standortId !== angefragterStandort) return;
        this.raeume = [];
        this.raumId = '';
        this.raum = null;
        this.laden = false;
        this.fehler.emit('Räume konnten nicht geladen werden.');
        this.cdr.detectChanges();
      }
    });
  }

  raumLaden(id: string): void {
    this.raumId = id;
    const zeitraum: Zeitraum = { datum: this.heute(), endDatum: this.heute(), beginn: '08:00', ende: '16:00' };
    this.service.getRaum(id, zeitraum, this.admin.id).subscribe({
      next: (raum) => {
        raum.elemente = raum.elemente.map((element) => {
          if (element.typ === 'tuer') {
            const hoehe = element.breite * 16 / 9;
            return { ...element, y: element.y + element.hoehe / 2 - hoehe / 2, hoehe };
          }
          if (element.typ === 'wand' || element.typ === 'fenster') {
            const mitteX = element.x + element.breite / 2;
            const mitteY = element.y + element.hoehe / 2;
            const gedreht = Math.round((element.rotation || 0) / 90) % 2 !== 0;
            const waagrecht = gedreht ? element.breite < element.hoehe : element.breite >= element.hoehe;
            const laenge = Math.max(element.breite, element.hoehe);
            const breite = waagrecht ? laenge : 2;
            const hoehe = waagrecht ? 2 : laenge;
            return { ...element,
              x: this.begrenzen(mitteX - breite / 2, 0, 100 - breite),
              y: this.begrenzen(mitteY - hoehe / 2, 0, 100 - hoehe),
              breite, hoehe, rotation: 0 };
          }
          return element;
        });
        this.raum = raum;
        this.raumForm = { name: raum.name, stockwerk: raum.stockwerk, standortId: raum.standortId,
          abteilungId: raum.abteilungId, abteilungName: raum.abteilungName, elemente: [...raum.elemente] };
        this.gespeicherterGrundriss = JSON.stringify(raum.elemente);
        this.neuerTisch();
        this.historie = [];
        this.wiederholenHistorie = [];
        this.cdr.detectChanges();
      },
      error: () => this.fehler.emit('Der Raum konnte nicht geladen werden.')
    });
  }

  neuerRaum(): void {
    if (!this.darfGrundrissVerlassen()) return;
    this.modus = 'raum';
    this.raumId = '';
    this.raum = null;
    this.raumForm = this.leererRaum();
  }

  raumBearbeiten(): void {
    if (this.raum && this.darfGrundrissVerlassen()) this.modus = 'raum';
  }

  grundrissBearbeiten(): void {
    if (this.raum) {
      this.modus = 'grundriss';
      this.werkzeug = 'auswahl';
      this.ausgewaehltesElement = null;
      this.ausgewaehlteElementIds = [];
    }
  }

  raumSpeichern(): void {
    const warNeu = !this.raumId;
    const aufruf = this.raumId
      ? this.service.raumAendern(this.admin.id, this.raumId, this.raumForm)
      : this.service.raumAnlegen(this.admin.id, this.raumForm);
    aufruf.subscribe({
      next: (raum) => {
        this.nachricht.emit(warNeu ? 'Raum wurde angelegt und kann sofort bearbeitet werden.' : 'Raum wurde aktualisiert.');
        this.standortId = this.raumForm.standortId;
        this.modus = 'grundriss';
        this.raeumeLaden(raum.id);
      },
      error: () => this.fehler.emit('Raum konnte nicht gespeichert werden.')
    });
  }

  grundrissSpeichern(): void {
    if (!this.raum) return;
    this.raumForm.elemente = [...this.raum.elemente];
    this.service.raumAendern(this.admin.id, this.raum.id, this.raumForm).subscribe({
      next: () => { this.gespeicherterGrundriss = JSON.stringify(this.raum!.elemente); this.nachricht.emit('Grundriss wurde gespeichert und ist für Mitarbeiter sichtbar.'); this.raumLaden(this.raum!.id); },
      error: () => this.fehler.emit('Grundriss konnte nicht gespeichert werden.')
    });
  }

  raumLoeschen(): void {
    if (this.raumId) this.stammdatenRaumLoeschen(this.raumId, this.raum?.name ?? this.raumId);
  }

  stammdatenRaumLoeschen(id: string, name: string): void {
    if (!this.istSuperadmin) return;
    if (!confirm(`Raum „${name}“ wirklich löschen? Arbeitsplätze sowie vergangene und stornierte Reservierungen dieses Raums werden ebenfalls entfernt.`)) return;
    this.service.raumLoeschen(this.admin.id, id).subscribe({
      next: () => {
        if (this.raumId === id) { this.raumId = ''; this.raum = null; }
        this.nachricht.emit('Raum wurde gelöscht.');
        this.stammdatenRaeumeLaden();
        if (this.standortId) this.raeumeLaden();
      },
      error: () => this.fehler.emit('Räume mit laufenden oder zukünftigen Reservierungen können nicht gelöscht werden.')
    });
  }

  tischAuswaehlen(tisch: Arbeitsplatz): void {
    if (this.ausgewaehlterTisch && this.ausgewaehlterTisch.id !== tisch.id) {
      this.tischFormInRaumUebernehmen();
    }
    this.ausgewaehlterTisch = tisch;
    this.tischForm = { tischnr: tisch.tischnr, name: tisch.name, raumId: this.raum!.id,
      standortId: this.raum!.standortId, abteilungId: tisch.abteilungId,
      abteilungName: tisch.abteilungName, x: tisch.x, y: tisch.y,
      rotation: tisch.rotation || 0,
      breite: tisch.breite || 12,
      hoehe: tisch.hoehe || 12,
      equipment: tisch.equipment.map((item) => item.name) };
  }

  neuerTisch(): void {
    if (this.modus === 'grundriss' && !this.darfGrundrissVerlassen()) return;
    this.modus = 'tisch';
    this.ausgewaehlterTisch = null;
    this.tischForm = this.leererTisch();
    if (this.raum) {
      Object.assign(this.tischForm, { raumId: this.raum.id, standortId: this.raum.standortId,
        abteilungId: this.raum.abteilungId, abteilungName: this.raum.abteilungName,
        tischnr: this.naechsteTischnummer() });
      this.tischForm.name = `Arbeitsplatz ${this.tischForm.tischnr}`;
    }
    this.tischForm.equipment = this.equipmentListe.filter((name) => name === 'Tastatur' || name === 'Maus' || name === 'Monitor');
  }

  positionSetzen(event: MouseEvent): void {
    if (!this.raum || this.tischWirdGezogen) return;
    const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
    this.tischForm.x = Math.round(Math.max(8, Math.min(92, (event.offsetX / rect.width) * 100)));
    this.tischForm.y = Math.round(Math.max(12, Math.min(86, (event.offsetY / rect.height) * 100)));
  }

  equipmentUmschalten(name: string, aktiv: boolean): void {
    this.tischForm.equipment = aktiv
      ? [...new Set([...this.tischForm.equipment, name])]
      : this.tischForm.equipment.filter((eintrag) => eintrag !== name);
  }

  hatEquipment(name: string): boolean {
    return this.tischForm.equipment.includes(name);
  }

  tischGroesseSetzen(groesse: 'klein' | 'mittel' | 'gross'): void {
    const wert = groesse === 'klein' ? 9 : groesse === 'mittel' ? 12 : 16;
    this.tischForm.breite = wert;
    this.tischForm.hoehe = wert;
    if (!this.raum || this.raum.arbeitsplaetze.length === 0) return;

    const aenderungen = this.raum.arbeitsplaetze.map((tisch) => {
      const request: ArbeitsplatzRequest = {
        tischnr: tisch.tischnr, name: tisch.name, raumId: this.raum!.id,
        standortId: this.raum!.standortId, abteilungId: tisch.abteilungId,
        abteilungName: tisch.abteilungName, x: tisch.x, y: tisch.y,
        rotation: tisch.rotation || 0, breite: wert, hoehe: wert,
        equipment: tisch.equipment.map((item) => item.name)
      };
      return this.service.arbeitsplatzAendern(this.admin.id, tisch.id, request);
    });

    forkJoin(aenderungen).subscribe({
      next: () => {
        this.raum!.arbeitsplaetze.forEach((tisch) => { tisch.breite = wert; tisch.hoehe = wert; });
        this.nachricht.emit(`Alle Tische wurden auf ${groesse === 'gross' ? 'Groß' : groesse[0].toUpperCase() + groesse.slice(1)} gesetzt.`);
      },
      error: () => this.fehler.emit('Die Tischgrößen konnten nicht gespeichert werden.')
    });
  }

  tischSpeichern(): void {
    if (!this.raum) return;
    if (this.tischHatKollision(this.tischForm.x, this.tischForm.y, this.ausgewaehlterTisch?.id)) {
      this.fehler.emit('Der Arbeitsplatz überlappt eine Wand, Sperrzone oder einen anderen Tisch.');
      return;
    }
    if (this.ausgewaehlterTisch) {
      const aenderungen = this.raum.arbeitsplaetze.map((tisch) => {
        const request: ArbeitsplatzRequest = tisch.id === this.ausgewaehlterTisch!.id
          ? { ...this.tischForm }
          : {
              tischnr: tisch.tischnr, name: tisch.name, raumId: this.raum!.id,
              standortId: this.raum!.standortId, abteilungId: tisch.abteilungId,
              abteilungName: tisch.abteilungName, x: tisch.x, y: tisch.y,
              rotation: tisch.rotation || 0, breite: tisch.breite || 12,
              hoehe: tisch.hoehe || 12, equipment: tisch.equipment.map((item) => item.name)
            };
        return this.service.arbeitsplatzAendern(this.admin.id, tisch.id, request);
      });
      forkJoin(aenderungen).subscribe({
        next: () => { this.nachricht.emit('Arbeitsplätze wurden gespeichert.'); this.raumLaden(this.raum!.id); },
        error: () => this.fehler.emit('Die Arbeitsplätze konnten nicht gespeichert werden.')
      });
      return;
    }

    const raumId = this.raum.id;
    const neuerTischRequest: ArbeitsplatzRequest = { ...this.tischForm, equipment: [...this.tischForm.equipment] };
    const vorhandeneTischeSpeichern = this.raum.arbeitsplaetze.map((tisch) =>
      this.service.arbeitsplatzAendern(this.admin.id, tisch.id, this.requestFuerTisch(tisch)));
    const neuenTischAnlegen = () => this.service.arbeitsplatzAnlegen(this.admin.id, neuerTischRequest).subscribe({
      next: () => { this.nachricht.emit('Arbeitsplatz und aktuelle Anordnung wurden gespeichert.'); this.raumLaden(raumId); },
      error: () => this.fehler.emit('Arbeitsplatz konnte nicht gespeichert werden. Tischnummer prüfen.')
    });

    if (vorhandeneTischeSpeichern.length === 0) {
      neuenTischAnlegen();
      return;
    }
    forkJoin(vorhandeneTischeSpeichern).subscribe({
      next: () => neuenTischAnlegen(),
      error: () => this.fehler.emit('Die aktuelle Tischanordnung konnte nicht gespeichert werden.')
    });
  }

  tischLoeschen(): void {
    if (!this.ausgewaehlterTisch || !this.raum) return;
    if (!confirm(`Arbeitsplatz ${this.ausgewaehlterTisch.tischnr} wirklich löschen?`)) return;
    const geloeschteId = this.ausgewaehlterTisch.id;
    const raumId = this.raum.id;
    const uebrigeTische = this.raum.arbeitsplaetze.filter((tisch) => tisch.id !== geloeschteId);
    this.service.arbeitsplatzLoeschen(this.admin.id, geloeschteId).subscribe({
      next: () => {
        if (uebrigeTische.length === 0) {
          this.nachricht.emit('Arbeitsplatz wurde gelöscht.');
          this.raumLaden(raumId);
          return;
        }
        const positionenSpeichern = uebrigeTische.map((tisch) =>
          this.service.arbeitsplatzAendern(this.admin.id, tisch.id, this.requestFuerTisch(tisch)));
        forkJoin(positionenSpeichern).subscribe({
          next: () => {
            this.nachricht.emit('Arbeitsplatz wurde gelöscht und die Anordnung wurde gespeichert.');
            this.raumLaden(raumId);
          },
          error: () => this.fehler.emit('Der Arbeitsplatz wurde gelöscht, aber die übrige Anordnung konnte nicht vollständig gespeichert werden.')
        });
      },
      error: () => this.fehler.emit('Arbeitsplatz mit aktiver Reservierung kann nicht gelöscht werden.')
    });
  }

  private requestFuerTisch(tisch: Arbeitsplatz): ArbeitsplatzRequest {
    return {
      tischnr: tisch.tischnr, name: tisch.name, raumId: this.raum!.id,
      standortId: this.raum!.standortId, abteilungId: tisch.abteilungId,
      abteilungName: tisch.abteilungName, x: tisch.x, y: tisch.y,
      rotation: tisch.rotation || 0, breite: tisch.breite || 12,
      hoehe: tisch.hoehe || 12, equipment: tisch.equipment.map((item) => item.name)
    };
  }

  svgPointerDown(event: PointerEvent): void {
    if (!this.raum) return;
    if (this.werkzeug === 'auswahl') {
      this.auswahlLeeren();
      return;
    }
    const punkt = this.svgPunkt(event);
    if (this.rotateElementId) {
      const element = this.raum.elemente.find((eintrag) => eintrag.id === this.rotateElementId);
      if (!element) return;
      const mitteX = element.x + element.breite / 2;
      const mitteY = element.y + element.hoehe / 2;
      element.rotation = Math.round((Math.atan2(punkt.y - mitteY, punkt.x - mitteX) * 180 / Math.PI + 90 + 360) % 360);
      return;
    }
    if (this.werkzeug === 'wand') {
      this.zustandMerken();
      this.startpunkt = this.punktAnWandAndocken(punkt);
      (event.currentTarget as SVGElement).setPointerCapture(event.pointerId);
      return;
    }
    this.zustandMerken();
    const groesse = this.standardGroesse(this.werkzeug);
    const element = this.neuesElement(this.werkzeug, punkt.x, punkt.y, groesse.breite, groesse.hoehe);
    if (element.typ === 'tuer' || element.typ === 'fenster') this.anWandAndocken(element);
    this.raum.elemente.push(element);
    this.ausgewaehltesElement = element;
    this.werkzeug = 'auswahl';
  }

  svgPointerMove(event: PointerEvent): void {
    if (!this.raum) return;
    const punkt = this.svgPunkt(event);
    if (this.startpunkt && this.werkzeug === 'wand') {
      this.wandVorschau = this.wandBerechnen(this.startpunkt, punkt);
      return;
    }
    if (this.resizeElementId && this.resizeStart) {
      const element = this.raum.elemente.find((eintrag) => eintrag.id === this.resizeElementId);
      if (!element) return;
      const links = this.resizeRichtung.includes('w');
      const oben = this.resizeRichtung.includes('n');
      const deltaX = punkt.x - this.resizeStart.mausX;
      const deltaY = punkt.y - this.resizeStart.mausY;
      if (element.typ === 'tuer') {
        const deltaYAlsBreite = deltaY * 9 / 16;
        const delta = Math.abs(deltaX) >= Math.abs(deltaYAlsBreite)
          ? (links ? -deltaX : deltaX)
          : (oben ? -deltaYAlsBreite : deltaYAlsBreite);
        const seite = this.begrenzen(this.raster(this.resizeStart.breite + delta), 4, 25);
        const hoehe = seite * 16 / 9;
        element.x = links ? this.resizeStart.x + this.resizeStart.breite - seite : this.resizeStart.x;
        element.y = oben ? this.resizeStart.y + this.resizeStart.hoehe - hoehe : this.resizeStart.y;
        element.breite = seite;
        element.hoehe = hoehe;
        return;
      }
      if (links) {
        const x = this.begrenzen(this.raster(this.resizeStart.x + deltaX), 0, this.resizeStart.x + this.resizeStart.breite - 1);
        element.x = x; element.breite = this.resizeStart.breite + this.resizeStart.x - x;
      } else element.breite = this.begrenzen(this.raster(this.resizeStart.breite + deltaX), 1, 100 - element.x);
      if (oben) {
        const y = this.begrenzen(this.raster(this.resizeStart.y + deltaY), 0, this.resizeStart.y + this.resizeStart.hoehe - 1);
        element.y = y; element.hoehe = this.resizeStart.hoehe + this.resizeStart.y - y;
      } else element.hoehe = this.begrenzen(this.raster(this.resizeStart.hoehe + deltaY), 1, 100 - element.y);
      return;
    }
    if (!this.dragElementId) return;
    const element = this.raum.elemente.find((eintrag) => eintrag.id === this.dragElementId);
    if (element) {
      const neuesX = this.begrenzen(this.raster(punkt.x - this.dragVersatz.x), 0, 100 - element.breite);
      const neuesY = this.begrenzen(this.raster(punkt.y - this.dragVersatz.y), 0, 100 - element.hoehe);
      const deltaX = neuesX - element.x; const deltaY = neuesY - element.y;
      element.x = neuesX; element.y = neuesY;
      if (this.ausgewaehlteElementIds.length > 1) {
        this.raum.elemente.filter((e) => e.id !== element.id && this.ausgewaehlteElementIds.includes(e.id)).forEach((e) => {
          e.x = this.begrenzen(e.x + deltaX, 0, 100 - e.breite); e.y = this.begrenzen(e.y + deltaY, 0, 100 - e.hoehe);
        });
      }
      if (element.typ === 'tuer' || element.typ === 'fenster') this.anWandAndocken(element);
    }
  }

  svgPointerUp(event: PointerEvent): void {
    if (this.raum && this.startpunkt && this.werkzeug === 'wand') {
      const ende = this.svgPunkt(event);
      if (Math.abs(ende.x - this.startpunkt.x) < 1.5 && Math.abs(ende.y - this.startpunkt.y) < 1.5) {
        this.startpunkt = null;
        this.wandVorschau = null;
        return;
      }
      const form = this.wandBerechnen(this.startpunkt, ende);
      const wand = this.neuesElement('wand', form.x + form.breite / 2, form.y + form.hoehe / 2, form.breite, form.hoehe);
      this.wandendenVerbinden(wand);
      this.raum.elemente.push(wand);
      this.ausgewaehltesElement = wand;
      this.ausgewaehlteElementIds = [wand.id];
      this.werkzeug = 'auswahl';
    }
    if (this.raum && this.resizeElementId) {
      const element = this.raum.elemente.find((eintrag) => eintrag.id === this.resizeElementId);
      if (element?.typ === 'tuer' || element?.typ === 'fenster') this.anWandAndocken(element);
      if (element?.typ === 'wand') this.wandAndocken(element);
    }
    if (this.raum && this.dragElementId) {
      const element = this.raum.elemente.find((eintrag) => eintrag.id === this.dragElementId);
      if (element?.typ === 'wand') this.wandAndocken(element);
    }
    this.startpunkt = null;
    this.wandVorschau = null;
    this.dragElementId = '';
    this.resizeElementId = '';
    this.rotateElementId = '';
    this.resizeStart = null;
  }

  elementPointerDown(event: PointerEvent, element: RaumElement): void {
    event.stopPropagation();
    this.zustandMerken();
    this.ausgewaehltesElement = element;
    if (event.shiftKey) {
      this.ausgewaehlteElementIds = this.ausgewaehlteElementIds.includes(element.id)
        ? this.ausgewaehlteElementIds.filter((id) => id !== element.id)
        : [...this.ausgewaehlteElementIds, element.id];
    } else if (!this.ausgewaehlteElementIds.includes(element.id)) {
      this.ausgewaehlteElementIds = [element.id];
    }
    this.werkzeug = 'auswahl';
    const punkt = this.svgPunkt(event);
    this.dragElementId = element.id;
    this.dragVersatz = { x: punkt.x - element.x, y: punkt.y - element.y };
    (event.currentTarget as SVGElement).setPointerCapture(event.pointerId);
  }

  resizePointerDown(event: PointerEvent, element: RaumElement, richtung: 'nw' | 'ne' | 'sw' | 'se'): void {
    event.stopPropagation();
    this.zustandMerken();
    this.ausgewaehltesElement = element;
    this.ausgewaehlteElementIds = [element.id];
    this.resizeElementId = element.id;
    this.resizeRichtung = richtung;
    const punkt = this.svgPunkt(event);
    this.resizeStart = { mausX: punkt.x, mausY: punkt.y, x: element.x, y: element.y, breite: element.breite, hoehe: element.hoehe };
    (event.currentTarget as SVGElement).setPointerCapture(event.pointerId);
  }

  rotatePointerDown(event: PointerEvent, element: RaumElement): void {
    event.stopPropagation();
    this.zustandMerken();
    this.ausgewaehltesElement = element;
    this.ausgewaehlteElementIds = [element.id];
    this.rotateElementId = element.id;
    (event.currentTarget as SVGElement).setPointerCapture(event.pointerId);
  }

  rotatePointerMove(event: PointerEvent): void {
    if (!this.raum || !this.rotateElementId) return;
    const element = this.raum.elemente.find((eintrag) => eintrag.id === this.rotateElementId);
    if (!element) return;
    const punkt = this.svgPunkt(event);
    const mitteX = element.x + element.breite / 2;
    const mitteY = element.y + element.hoehe / 2;
    element.rotation = Math.round((Math.atan2(punkt.y - mitteY, punkt.x - mitteX) * 180 / Math.PI + 90 + 360) % 360);
  }

  elementLoeschen(): void {
    if (!this.raum || !this.ausgewaehltesElement) return;
    this.zustandMerken();
    const ids = new Set(this.ausgewaehlteElementIds.length ? this.ausgewaehlteElementIds : [this.ausgewaehltesElement.id]);
    this.raum.elemente = this.raum.elemente.filter((element) => !ids.has(element.id));
    this.ausgewaehltesElement = null;
    this.ausgewaehlteElementIds = [];
  }

  beschriftungAuswaehlen(element: RaumElement): void {
    this.ausgewaehltesElement = element;
    this.ausgewaehlteElementIds = [element.id];
    this.werkzeug = 'auswahl';
  }

  beschriftungBearbeiten(element: RaumElement): void {
    this.beschriftungAuswaehlen(element);
    const text = prompt('Beschriftung eingeben:', element.text || '');
    if (text !== null && text.trim()) element.text = text.trim();
  }

  beschriftungEntfernen(element: RaumElement): void {
    this.beschriftungAuswaehlen(element);
    this.elementLoeschen();
  }

  elementDuplizieren(): void {
    if (!this.raum || !this.ausgewaehltesElement) return;
    this.zustandMerken();
    const element = this.ausgewaehltesElement;
    const kopie = this.neuesElement(element.typ, element.x + 3, element.y + 3, element.breite, element.hoehe);
    kopie.text = element.text;
    kopie.rotation = element.rotation;
    this.raum.elemente.push(kopie);
    this.ausgewaehltesElement = kopie;
  }

  elementDrehen(): void {
    if (!this.ausgewaehltesElement) return;
    this.zustandMerken();
    if (this.ausgewaehltesElement.typ === 'wand' || this.ausgewaehltesElement.typ === 'fenster') {
      const element = this.ausgewaehltesElement;
      const mitteX = element.x + element.breite / 2;
      const mitteY = element.y + element.hoehe / 2;
      const alteBreite = element.breite;
      element.breite = element.hoehe;
      element.hoehe = alteBreite;
      element.x = this.begrenzen(mitteX - element.breite / 2, 0, 100 - element.breite);
      element.y = this.begrenzen(mitteY - element.hoehe / 2, 0, 100 - element.hoehe);
      element.rotation = 0;
      if (element.typ === 'wand') this.wandAndocken(element); else this.anWandAndocken(element);
      return;
    }
    this.ausgewaehltesElement.rotation = ((this.ausgewaehltesElement.rotation || 0) + 90) % 360;
  }

  linienLaenge(element: RaumElement): number {
    return Math.max(element.breite, element.hoehe);
  }

  linienLaengeAendern(element: RaumElement, wert: number): void {
    const laenge = this.begrenzen(Number(wert), 2, 100);
    const waagrecht = element.breite >= element.hoehe;
    if (waagrecht) {
      element.y = this.begrenzen(element.y + element.hoehe / 2 - 1, 0, 98);
      element.breite = this.begrenzen(laenge, 2, 100 - element.x);
      element.hoehe = 2;
    } else {
      element.x = this.begrenzen(element.x + element.breite / 2 - 1, 0, 98);
      element.breite = 2;
      element.hoehe = this.begrenzen(laenge, 2, 100 - element.y);
    }
    if (element.typ === 'wand') this.wandAndocken(element); else this.anWandAndocken(element);
  }

  tuerGroesseAendern(element: RaumElement, wert: number): void {
    const seite = this.begrenzen(Number(wert), 4, 25);
    element.breite = seite;
    element.hoehe = seite * 16 / 9;
    this.anWandAndocken(element);
  }

  grundrissVerwerfen(): void {
    if (this.raum) this.raumLaden(this.raum.id);
  }

  get grundrissGeaendert(): boolean {
    return !!this.raum && JSON.stringify(this.raum.elemente) !== this.gespeicherterGrundriss;
  }

  get svgViewBox(): string {
    const groesse = 100 / this.zoom;
    return `${this.ansichtX * 1.6} ${this.ansichtY * .9} ${groesse * 1.6} ${groesse * .9}`;
  }

  zoomAendern(richtung: number): void {
    this.zoom = this.begrenzen(Math.round((this.zoom + richtung) * 10) / 10, 1, 4);
    const max = 100 - 100 / this.zoom;
    this.ansichtX = this.begrenzen(this.ansichtX, 0, max);
    this.ansichtY = this.begrenzen(this.ansichtY, 0, max);
  }

  ansichtVerschieben(x: number, y: number): void {
    const max = 100 - 100 / this.zoom;
    this.ansichtX = this.begrenzen(this.ansichtX + x / this.zoom, 0, max);
    this.ansichtY = this.begrenzen(this.ansichtY + y / this.zoom, 0, max);
  }

  ansichtZuruecksetzen(): void {
    this.zoom = 1;
    this.ansichtX = 0;
    this.ansichtY = 0;
    this.auswahlLeeren();
  }

  mausradZoom(event: WheelEvent): void {
    event.preventDefault();
    this.zoomAendern(event.deltaY < 0 ? .2 : -.2);
  }

  grundrissVollbildUmschalten(): void {
    this.grundrissVollbild = !this.grundrissVollbild;
    if (!this.grundrissVollbild) this.ansichtZuruecksetzen();
  }

  allesZuruecksetzen(): void {
    if (!this.raum || !confirm('Grundriss wirklich auf die rechteckige Standardvorlage zurücksetzen?')) return;
    this.rechteckVorlage();
    this.ansichtZuruecksetzen();
    this.werkzeug = 'auswahl';
  }

  rueckgaengig(): void {
    if (!this.raum || this.historie.length === 0) return;
    this.wiederholenHistorie.push(this.elementeKopieren(this.raum.elemente));
    this.raum.elemente = this.historie.pop()!;
    this.auswahlLeeren();
  }

  wiederholen(): void {
    if (!this.raum || this.wiederholenHistorie.length === 0) return;
    this.historie.push(this.elementeKopieren(this.raum.elemente));
    this.raum.elemente = this.wiederholenHistorie.pop()!;
    this.auswahlLeeren();
  }

  elementeAusrichten(richtung: 'links' | 'oben' | 'horizontal' | 'vertikal'): void {
    if (!this.raum) return;
    const elemente = this.raum.elemente.filter((element) => this.ausgewaehlteElementIds.includes(element.id));
    if (elemente.length < 2) return;
    this.zustandMerken();
    if (richtung === 'links') { const x = Math.min(...elemente.map((e) => e.x)); elemente.forEach((e) => e.x = x); }
    if (richtung === 'oben') { const y = Math.min(...elemente.map((e) => e.y)); elemente.forEach((e) => e.y = y); }
    if (richtung === 'horizontal') { const y = elemente.reduce((summe, e) => summe + e.y, 0) / elemente.length; elemente.forEach((e) => e.y = y); }
    if (richtung === 'vertikal') { const x = elemente.reduce((summe, e) => summe + e.x, 0) / elemente.length; elemente.forEach((e) => e.x = x); }
  }

  ebeneAendern(nachVorne: boolean): void {
    if (!this.raum || !this.ausgewaehltesElement) return;
    this.zustandMerken();
    const index = this.raum.elemente.findIndex((e) => e.id === this.ausgewaehltesElement!.id);
    const [element] = this.raum.elemente.splice(index, 1);
    if (nachVorne) this.raum.elemente.push(element); else this.raum.elemente.unshift(element);
  }

  rechteckVorlage(): void {
    if (!this.raum) return;
    this.zustandMerken();
    this.raum.elemente = [
      this.neuesElement('wand', 50, 3, 94, 2), this.neuesElement('wand', 50, 97, 94, 2),
      this.neuesElement('wand', 3, 50, 2, 94), this.neuesElement('wand', 97, 50, 2, 94),
      this.neuesElement('tuer', 20, 92, 10, 10)
    ];
  }

  lFormVorlage(): void {
    if (!this.raum) return;
    this.zustandMerken();
    this.raum.elemente = [
      this.neuesElement('wand', 50, 3, 94, 2),
      this.neuesElement('wand', 3, 50, 2, 94),
      this.neuesElement('wand', 27, 97, 48, 2),
      this.neuesElement('wand', 51, 74, 2, 48),
      this.neuesElement('wand', 74, 51, 48, 2),
      this.neuesElement('wand', 97, 27, 2, 48),
      this.neuesElement('tuer', 18, 92, 10, 10)
    ];
  }

  tischDragStart(event: PointerEvent, tisch: Arbeitsplatz): void {
    event.stopPropagation();
    this.tischWirdGezogen = true;
    this.tischAktion = 'verschieben';
    this.tischAuswaehlen(tisch);
    this.tischStart = { mausX: 0, mausY: 0, x: this.tischForm.x, y: this.tischForm.y,
      breite: this.tischForm.breite, hoehe: this.tischForm.hoehe, rotation: this.tischForm.rotation };
    (event.currentTarget as HTMLElement).setPointerCapture(event.pointerId);
  }

  neuerTischDragStart(event: PointerEvent): void {
    event.stopPropagation();
    this.tischWirdGezogen = true;
    this.tischAktion = 'verschieben';
    this.tischStart = { mausX: 0, mausY: 0, x: this.tischForm.x, y: this.tischForm.y,
      breite: this.tischForm.breite, hoehe: this.tischForm.hoehe, rotation: this.tischForm.rotation };
    (event.currentTarget as HTMLElement).setPointerCapture(event.pointerId);
  }

  tischDrag(event: PointerEvent): void {
    if (!this.tischWirdGezogen || this.tischAktion !== 'verschieben') return;
    const raum = (event.currentTarget as HTMLElement).parentElement!;
    const rect = raum.getBoundingClientRect();
    const x = this.begrenzen(((event.clientX - rect.left) / rect.width) * 100, 8, 92);
    const y = this.begrenzen(((event.clientY - rect.top) / rect.height) * 100, 12, 86);
    this.tischForm.x = Math.round(x); this.tischForm.y = Math.round(y);
    if (this.ausgewaehlterTisch) {
      this.ausgewaehlterTisch.x = this.tischForm.x;
      this.ausgewaehlterTisch.y = this.tischForm.y;
    }
  }

  tischDragEnd(event: PointerEvent): void {
    if (!this.tischWirdGezogen) return;
    if (event.type === 'pointercancel') {
      this.tischStartWiederherstellen();
      return;
    }
    if (this.tischAktion === 'verschieben') this.tischDrag(event);
    this.tischFormInRaumUebernehmen();
    this.tischWirdGezogen = false;
    this.tischAktion = '';
    if (this.tischHatKollision(this.tischForm.x, this.tischForm.y, this.ausgewaehlterTisch?.id)) {
      this.fehler.emit('Der Arbeitsplatz überlappt eine Wand, Sperrzone oder einen anderen Tisch.');
      this.tischStartWiederherstellen();
      return;
    }
    this.tischStart = null;
  }

  private tischStartWiederherstellen(): void {
    if (this.tischStart) {
      this.tischForm.x = this.tischStart.x;
      this.tischForm.y = this.tischStart.y;
      this.tischForm.breite = this.tischStart.breite;
      this.tischForm.hoehe = this.tischStart.hoehe;
      this.tischForm.rotation = this.tischStart.rotation;
      this.tischFormInRaumUebernehmen();
    }
    this.tischWirdGezogen = false;
    this.tischAktion = '';
    this.tischStart = null;
  }

  tischResizeStart(event: PointerEvent, richtung: 'nw' | 'ne' | 'sw' | 'se'): void {
    event.stopPropagation();
    const punkt = this.tischPunkt(event);
    this.tischWirdGezogen = true;
    this.tischAktion = 'groesse';
    this.tischResizeRichtung = richtung;
    this.tischStart = { mausX: punkt.x, mausY: punkt.y, x: this.tischForm.x, y: this.tischForm.y,
      breite: this.tischForm.breite, hoehe: this.tischForm.hoehe, rotation: this.tischForm.rotation };
    (event.currentTarget as HTMLElement).setPointerCapture(event.pointerId);
  }

  tischDrehenStart(event: PointerEvent): void {
    event.stopPropagation();
    this.tischWirdGezogen = true;
    this.tischAktion = 'drehen';
    this.tischStart = { mausX: 0, mausY: 0, x: this.tischForm.x, y: this.tischForm.y,
      breite: this.tischForm.breite, hoehe: this.tischForm.hoehe, rotation: this.tischForm.rotation };
    (event.currentTarget as HTMLElement).setPointerCapture(event.pointerId);
  }

  tischBearbeitenPointerMove(event: PointerEvent): void {
    if (!this.tischWirdGezogen) return;
    if (this.tischAktion === 'verschieben') { this.tischDrag(event); return; }
    if (this.tischAktion === 'drehen') {
      const raum = (event.currentTarget as HTMLElement).closest('.builder-room') as HTMLElement;
      const rect = raum.getBoundingClientRect();
      const mitteX = rect.left + rect.width * this.tischForm.x / 100;
      const mitteY = rect.top + rect.height * this.tischForm.y / 100;
      const winkel = (Math.atan2(event.clientY - mitteY, event.clientX - mitteX) * 180 / Math.PI + 90 + 360) % 360;
      this.tischForm.rotation = Math.round(winkel * 10) / 10;
      this.tischFormInRaumUebernehmen();
      return;
    }
    if (this.tischAktion !== 'groesse' || !this.tischStart) return;
    const punkt = this.tischPunkt(event);
    const links = this.tischResizeRichtung.includes('w');
    const oben = this.tischResizeRichtung.includes('n');
    const deltaX = punkt.x - this.tischStart.mausX;
    const deltaY = punkt.y - this.tischStart.mausY;
    this.tischForm.breite = this.begrenzen(this.tischStart.breite + (links ? -deltaX : deltaX), 6, 24);
    this.tischForm.hoehe = this.begrenzen(this.tischStart.hoehe + (oben ? -deltaY : deltaY), 6, 24);
    this.tischFormInRaumUebernehmen();
  }

  private tischFormInRaumUebernehmen(): void {
    if (!this.ausgewaehlterTisch) return;
    this.ausgewaehlterTisch.x = this.tischForm.x;
    this.ausgewaehlterTisch.y = this.tischForm.y;
    this.ausgewaehlterTisch.breite = this.tischForm.breite;
    this.ausgewaehlterTisch.hoehe = this.tischForm.hoehe;
    this.ausgewaehlterTisch.rotation = this.tischForm.rotation;
  }

  private tischPunkt(event: PointerEvent): { x: number; y: number } {
    const raum = (event.currentTarget as HTMLElement).closest('.builder-room') as HTMLElement;
    const rect = raum.getBoundingClientRect();
    return { x: ((event.clientX - rect.left) / rect.width) * 100, y: ((event.clientY - rect.top) / rect.height) * 100 };
  }

  @HostListener('window:keydown', ['$event'])
  tastatur(event: KeyboardEvent): void {
    if (this.modus !== 'grundriss' || !this.raum) return;
    const ziel = event.target as HTMLElement;
    if (ziel.matches('input, textarea, select')) return;
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'z') { event.preventDefault(); event.shiftKey ? this.wiederholen() : this.rueckgaengig(); return; }
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'y') { event.preventDefault(); this.wiederholen(); return; }
    if (event.key === 'Escape') {
      if (this.grundrissVollbild) this.grundrissVollbild = false;
      this.auswahlLeeren(); this.werkzeug = 'auswahl'; return;
    }
    if ((event.key === 'Delete' || event.key === 'Backspace') && this.ausgewaehltesElement) { event.preventDefault(); this.elementLoeschen(); return; }
    if (this.ausgewaehltesElement && ['ArrowLeft','ArrowRight','ArrowUp','ArrowDown'].includes(event.key)) {
      event.preventDefault(); this.zustandMerken(); const schritt = event.shiftKey ? 5 : 1;
      if (event.key === 'ArrowLeft') this.ausgewaehltesElement.x -= schritt;
      if (event.key === 'ArrowRight') this.ausgewaehltesElement.x += schritt;
      if (event.key === 'ArrowUp') this.ausgewaehltesElement.y -= schritt;
      if (event.key === 'ArrowDown') this.ausgewaehltesElement.y += schritt;
      this.ausgewaehltesElement.x = this.begrenzen(this.ausgewaehltesElement.x, 0, 100 - this.ausgewaehltesElement.breite);
      this.ausgewaehltesElement.y = this.begrenzen(this.ausgewaehltesElement.y, 0, 100 - this.ausgewaehltesElement.hoehe);
      if (this.ausgewaehltesElement.typ === 'wand') this.wandAndocken(this.ausgewaehltesElement);
      if (this.ausgewaehltesElement.typ === 'tuer' || this.ausgewaehltesElement.typ === 'fenster') this.anWandAndocken(this.ausgewaehltesElement);
    }
  }

  @HostListener('window:beforeunload', ['$event'])
  verlassenWarnen(event: BeforeUnloadEvent): void {
    if (this.grundrissGeaendert) event.preventDefault();
  }

  abteilungAnlegen(): void {
    if (!this.neueAbteilung.trim()) return;
    this.service.abteilungAnlegen(this.admin.id, this.neueAbteilung).subscribe({
      next: () => { this.neueAbteilung = ''; this.stammdatenLaden(); this.nachricht.emit('Abteilung wurde angelegt.'); },
      error: () => this.fehler.emit('Abteilung existiert bereits oder ist ungültig.')
    });
  }

  abteilungLoeschen(id: string): void {
    if (!confirm('Abteilung und zugehörige Testnutzer wirklich löschen?')) return;
    this.service.abteilungLoeschen(this.admin.id, id).subscribe({
      next: () => { this.stammdatenLaden(); this.nachricht.emit('Abteilung wurde gelöscht.'); },
      error: () => this.fehler.emit('Abteilung wird noch von Räumen oder Testnutzern verwendet.')
    });
  }

  equipmentAnlegen(): void {
    if (!this.neuesEquipment.trim()) return;
    this.service.equipmentAnlegen(this.admin.id, this.neuesEquipment).subscribe({
      next: () => { this.neuesEquipment = ''; this.stammdatenLaden(); this.nachricht.emit('Equipment wurde angelegt.'); },
      error: () => this.fehler.emit('Equipment existiert bereits oder ist ungültig.')
    });
  }

  equipmentLoeschen(name: string): void {
    if (!confirm(`Equipment „${name}“ wirklich löschen?`)) return;
    this.service.equipmentLoeschen(this.admin.id, name).subscribe({
      next: () => { this.stammdatenLaden(); this.nachricht.emit('Equipment wurde gelöscht.'); },
      error: () => this.fehler.emit('Equipment wird noch von Arbeitsplätzen verwendet.')
    });
  }

  private stammdatenLaden(): void {
    this.service.getAbteilungen(this.admin.id).subscribe((daten) => { this.abteilungen = daten; this.cdr.detectChanges(); });
    this.service.getEquipmentVerwaltung(this.admin.id).subscribe((daten) => { this.equipmentListe = daten; this.cdr.detectChanges(); });
    if (this.istSuperadmin) this.stammdatenRaeumeLaden();
  }

  private stammdatenRaeumeLaden(): void {
    if (this.standorte.length === 0) { this.stammdatenRaeume = []; return; }
    forkJoin(this.standorte.map((standort) => this.service.getRaeumeByStandort(standort.id))).subscribe({
      next: (listen) => {
        this.stammdatenRaeume = listen.flat().sort((a, b) =>
          this.standortName(a.standortId).localeCompare(this.standortName(b.standortId)) || a.name.localeCompare(b.name));
        this.cdr.detectChanges();
      },
      error: () => this.fehler.emit('Räume konnten nicht geladen werden.')
    });
  }

  standortName(id: string): string {
    return this.standorte.find((standort) => standort.id === id)?.name ?? id;
  }

  reservierungenLaden(): void {
    this.reservierungenWerdenGeladen = true;
    this.service.getAlleReservierungen(this.admin.id).subscribe({
      next: (daten) => { this.alleReservierungen = daten; this.reservierungenWerdenGeladen = false; this.cdr.detectChanges(); },
      error: () => { this.reservierungenWerdenGeladen = false; this.fehler.emit('Reservierungen konnten nicht geladen werden.'); this.cdr.detectChanges(); }
    });
  }

  reservierungStornieren(reservierung: Reservierung): void {
    if (reservierung.status !== 'reserviert') return;
    if (!this.stornierungsgrund.trim()) { this.fehler.emit('Bitte zuerst einen Stornierungsgrund angeben.'); return; }
    if (!confirm(`Reservierung von ${reservierung.benutzerName} wirklich stornieren?`)) return;
    this.service.reservierungAlsAdminStornieren(this.admin.id, reservierung.id, this.stornierungsgrund).subscribe({
      next: () => { this.nachricht.emit('Reservierung wurde administrativ storniert.'); this.reservierungenLaden(); },
      error: () => this.fehler.emit('Reservierung konnte nicht storniert werden.')
    });
  }

  get gefilterteReservierungen(): Reservierung[] {
    const suche = this.reservierungsSuche.trim().toLowerCase();
    return this.alleReservierungen.filter((eintrag) =>
      (!suche || `${eintrag.benutzerName} ${eintrag.arbeitsplatzName} ${eintrag.raumName}`.toLowerCase().includes(suche)) &&
      (!this.reservierungsStandort || eintrag.standortId === this.reservierungsStandort) &&
      (!this.reservierungsRaum || eintrag.raumId === this.reservierungsRaum) &&
      (!this.reservierungsStatus || this.reservierungsStatusText(eintrag) === this.reservierungsStatus) &&
      (!this.reservierungsDatum || eintrag.reservierungAnfang.slice(0, 10) === this.reservierungsDatum));
  }

  get reservierungsRaeume(): { id: string; name: string }[] {
    const eindeutig = new Map<string,string>();
    this.alleReservierungen.filter((r) => !this.reservierungsStandort || r.standortId === this.reservierungsStandort)
      .forEach((r) => eindeutig.set(r.raumId, r.raumName));
    return [...eindeutig].map(([id,name]) => ({ id,name })).sort((a,b) => a.name.localeCompare(b.name));
  }

  csvExportieren(): void {
    const kopf = ['Mitarbeiter','Arbeitsplatz','Raum','Standort','Beginn','Ende','Status'];
    const zeilen = this.gefilterteReservierungen.map((r) => [r.benutzerName,r.arbeitsplatzName,r.raumName,r.standortName,r.reservierungAnfang,r.reservierungEnde,r.status]);
    const csv = [kopf, ...zeilen].map((zeile) => zeile.map((wert) => `"${String(wert).replaceAll('"','""')}"`).join(';')).join('\n');
    const link = document.createElement('a'); link.href = URL.createObjectURL(new Blob([csv], { type:'text/csv;charset=utf-8' }));
    link.download = 'reservierungen.csv'; link.click(); URL.revokeObjectURL(link.href);
  }

  benutzerLaden(): void {
    this.benutzerWerdenGeladen = true;
    this.service.getBenutzerVerwaltung(this.admin.id).subscribe({
      next: (daten) => {
        this.benutzerListe = this.istSuperadmin ? daten : daten.filter((benutzer) => benutzer.rolle === 'USER');
        this.benutzerWerdenGeladen = false;
        this.cdr.detectChanges();
      },
      error: () => { this.benutzerWerdenGeladen = false; this.fehler.emit('Mitarbeiter konnten nicht geladen werden.'); this.cdr.detectChanges(); }
    });
  }

  neuerBenutzer(): void { this.ausgewaehlterBenutzerId = ''; this.benutzerForm = this.leererBenutzer(); }

  benutzerAuswaehlen(benutzer: Benutzer): void {
    this.ausgewaehlterBenutzerId = benutzer.id;
    this.benutzerForm = { vorname:benutzer.vorname, nachname:benutzer.nachname, email:benutzer.email, rolle:benutzer.rolle,
      abteilungId:benutzer.abteilungId, abteilungName:benutzer.abteilungName, bevorzugterRaumId:benutzer.bevorzugterRaumId, aktiv:benutzer.aktiv };
  }

  benutzerSpeichern(): void {
    const abteilung = this.abteilungen.find((a) => a.id === this.benutzerForm.abteilungId);
    if (abteilung) this.benutzerForm.abteilungName = abteilung.name;
    const aufruf = this.ausgewaehlterBenutzerId
      ? this.service.benutzerAendern(this.admin.id, this.ausgewaehlterBenutzerId, this.benutzerForm)
      : this.service.benutzerAnlegen(this.admin.id, this.benutzerForm);
    aufruf.subscribe({ next: () => { this.nachricht.emit('Mitarbeiter wurde gespeichert.'); this.benutzerLaden(); this.neuerBenutzer(); },
      error: () => this.fehler.emit('Mitarbeiter konnte nicht gespeichert werden.') });
  }

  passwortZuruecksetzen(): void {
    if (!this.ausgewaehlterBenutzerId || !confirm('Passwort auf holter123 zurücksetzen?')) return;
    this.service.passwortZuruecksetzen(this.admin.id, this.ausgewaehlterBenutzerId, 'holter123').subscribe({
      next: () => this.nachricht.emit('Passwort wurde auf holter123 zurückgesetzt.'),
      error: () => this.fehler.emit('Passwort konnte nicht zurückgesetzt werden.') });
  }

  benutzerLoeschen(): void {
    const ziel = this.benutzerListe.find((eintrag) => eintrag.id === this.ausgewaehlterBenutzerId);
    if (!this.istSuperadmin || !ziel || ziel.rolle !== 'USER') return;
    if (!confirm(`Mitarbeiter ${ziel.vorname} ${ziel.nachname} wirklich entfernen?`)) return;
    this.service.benutzerLoeschen(this.admin.id, ziel.id).subscribe({
      next: () => { this.nachricht.emit('Mitarbeiter wurde gelöscht.'); this.neuerBenutzer(); this.benutzerLaden(); },
      error: (error) => this.fehler.emit(error?.error?.details || 'Mitarbeiter konnte nicht gelöscht werden.')
    });
  }

  standortAuswaehlen(standort: Standort): void {
    this.bearbeiteterStandortId = standort.id;
    this.standortForm = { name:standort.name, adresse:standort.adresse, ort:standort.ort };
  }

  neuerStandort(): void { this.bearbeiteterStandortId = ''; this.standortForm = { name:'', adresse:'', ort:'' }; }

  standortSpeichern(): void {
    const aufruf = this.bearbeiteterStandortId
      ? this.service.standortAendern(this.admin.id, this.bearbeiteterStandortId, this.standortForm)
      : this.service.standortAnlegen(this.admin.id, this.standortForm);
    aufruf.subscribe({ next: () => { this.nachricht.emit('Standort wurde gespeichert.'); this.standorteGeaendert.emit(); this.neuerStandort(); },
      error: () => this.fehler.emit('Standort konnte nicht gespeichert werden.') });
  }

  standortLoeschen(id: string): void {
    if (!confirm('Standort wirklich löschen?')) return;
    this.service.standortLoeschen(this.admin.id, id).subscribe({ next: () => { this.nachricht.emit('Standort wurde gelöscht.'); this.standorteGeaendert.emit(); },
      error: () => this.fehler.emit('Standort besitzt noch Räume und kann nicht gelöscht werden.') });
  }

  auditLaden(): void { this.service.getAuditLog(this.admin.id).subscribe((daten) => { this.auditEintraege = daten; this.cdr.detectChanges(); }); }

  reservierungIstAktiv(reservierung: Reservierung): boolean {
    return reservierung.status === 'reserviert' && new Date(reservierung.reservierungEnde).getTime() >= Date.now();
  }

  reservierungsStatusText(reservierung: Reservierung): string {
    if (reservierung.status === 'storniert') return 'storniert';
    return this.reservierungIstAktiv(reservierung) ? 'aktiv' : 'abgelaufen';
  }

  private svgPunkt(event: PointerEvent): { x: number; y: number } {
    const svg = (event.currentTarget as SVGElement).closest('svg') as SVGSVGElement;
    const punkt = svg.createSVGPoint();
    punkt.x = event.clientX;
    punkt.y = event.clientY;
    const umgerechnet = punkt.matrixTransform(svg.getScreenCTM()!.inverse());
    return { x: umgerechnet.x / 1.6, y: umgerechnet.y / .9 };
  }

  private wandBerechnen(start: { x: number; y: number }, ende: { x: number; y: number }, ignorierteId = ''): { x: number; y: number; breite: number; hoehe: number } {
    const deltaX = Math.abs(ende.x - start.x);
    const deltaY = Math.abs(ende.y - start.y);
    const staerke = 2;
    if (deltaX >= deltaY) {
      const linieY = this.wandLinieAndocken(true, start.y, Math.min(start.x, ende.x), Math.max(start.x, ende.x), ignorierteId);
      const startAngedockt = this.punktAnWandAndocken({ x: start.x, y: linieY }, ignorierteId);
      const endeAngedockt = this.punktAnWandAndocken({ x: ende.x, y: linieY }, ignorierteId);
      return {
        x: this.begrenzen(this.raster(Math.min(startAngedockt.x, endeAngedockt.x)), 0, 100 - staerke),
        y: this.begrenzen(this.raster(linieY - staerke / 2), 0, 100 - staerke),
        breite: Math.max(2, this.raster(Math.abs(endeAngedockt.x - startAngedockt.x))),
        hoehe: staerke
      };
    }
    const linieX = this.wandLinieAndocken(false, start.x, Math.min(start.y, ende.y), Math.max(start.y, ende.y), ignorierteId);
    const startAngedockt = this.punktAnWandAndocken({ x: linieX, y: start.y }, ignorierteId);
    const endeAngedockt = this.punktAnWandAndocken({ x: linieX, y: ende.y }, ignorierteId);
    return {
      x: this.begrenzen(this.raster(linieX - staerke / 2), 0, 100 - staerke),
      y: this.begrenzen(this.raster(Math.min(startAngedockt.y, endeAngedockt.y)), 0, 100 - staerke),
      breite: staerke,
      hoehe: Math.max(2, this.raster(Math.abs(endeAngedockt.y - startAngedockt.y)))
    };
  }

  private neuesElement(typ: RaumElement['typ'], x: number, y: number, breite: number, hoehe: number): RaumElement {
    return { id: `${typ.toUpperCase()}-${Date.now()}-${Math.round(Math.random() * 999)}`, typ,
      x: this.begrenzen(this.raster(x - breite / 2), 0, 100 - breite),
      y: this.begrenzen(this.raster(y - hoehe / 2), 0, 100 - hoehe), breite, hoehe,
      text: typ === 'beschriftung' ? 'Beschriftung' : undefined, rotation: 0 };
  }

  private standardGroesse(typ: RaumElement['typ']): { breite: number; hoehe: number } {
    if (typ === 'gesperrt') return { breite: 18, hoehe: 12 };
    if (typ === 'tuer') return { breite: 10, hoehe: 10 * 16 / 9 };
    if (typ === 'fenster') return { breite: 14, hoehe: 2 };
    if (typ === 'klima') return { breite: 10, hoehe: 5 };
    if (typ === 'saeule') return { breite: 5, hoehe: 5 * 16 / 9 };
    if (typ === 'feuerloescher') return { breite: 4, hoehe: 6 };
    if (typ === 'beschriftung') return { breite: 18, hoehe: 5 };
    return { breite: 7, hoehe: 7 * 16 / 9 };
  }

  private anWandAndocken(element: RaumElement): void {
    if (!this.raum) return;
    const waende = this.raum.elemente.filter((eintrag) => eintrag.typ === 'wand');
    let besteWand: RaumElement | undefined;
    let kleinsterAbstand = 8;
    const mitteX = element.x + element.breite / 2;
    const mitteY = element.y + element.hoehe / 2;
    for (const wand of waende) {
      const waagrecht = wand.breite >= wand.hoehe;
      const naechstesX = this.begrenzen(mitteX, wand.x, wand.x + wand.breite);
      const naechstesY = this.begrenzen(mitteY, wand.y, wand.y + wand.hoehe);
      const deltaX = waagrecht ? mitteX - naechstesX : mitteX - (wand.x + wand.breite / 2);
      const deltaY = waagrecht ? mitteY - (wand.y + wand.hoehe / 2) : mitteY - naechstesY;
      const abstand = Math.hypot(deltaX * 1.6, deltaY * .9);
      if (abstand < kleinsterAbstand) { kleinsterAbstand = abstand; besteWand = wand; }
    }
    if (!besteWand) return;
    const waagrecht = besteWand.breite >= besteWand.hoehe;
    if (element.typ === 'tuer') {
      const seite = Math.max(4, Math.min(25, element.breite));
      const hoehe = seite * 16 / 9;
      element.breite = seite;
      element.hoehe = hoehe;
      if (waagrecht) {
        const wandMitte = besteWand.y + besteWand.hoehe / 2;
        element.x = this.begrenzen(mitteX - seite / 2, besteWand.x, besteWand.x + besteWand.breite - seite);
        element.y = mitteY <= wandMitte ? wandMitte - hoehe : wandMitte;
        element.rotation = mitteY <= wandMitte ? 0 : 180;
      } else {
        const wandMitte = besteWand.x + besteWand.breite / 2;
        element.x = mitteX <= wandMitte ? wandMitte - seite : wandMitte;
        element.y = this.begrenzen(mitteY - hoehe / 2, besteWand.y, besteWand.y + besteWand.hoehe - hoehe);
        element.rotation = mitteX <= wandMitte ? 270 : 90;
      }
      return;
    }
    if (waagrecht) {
      const laenge = Math.max(element.breite, element.hoehe);
      element.breite = laenge;
      element.hoehe = Math.min(element.breite, 3);
      element.y = besteWand.y + besteWand.hoehe / 2 - element.hoehe / 2;
      element.x = this.begrenzen(element.x, besteWand.x, besteWand.x + besteWand.breite - element.breite);
    } else {
      const laenge = Math.max(element.breite, element.hoehe);
      element.breite = Math.min(element.breite, 3);
      element.hoehe = laenge;
      element.x = besteWand.x + besteWand.breite / 2 - element.breite / 2;
      element.y = this.begrenzen(element.y, besteWand.y, besteWand.y + besteWand.hoehe - element.hoehe);
    }
  }

  private wandendenVerbinden(wand: RaumElement): void {
    this.wandAndocken(wand);
  }

  private wandAndocken(wand: RaumElement): void {
    const waagrecht = wand.breite >= wand.hoehe;
    const start = waagrecht
      ? { x: wand.x, y: wand.y + wand.hoehe / 2 }
      : { x: wand.x + wand.breite / 2, y: wand.y };
    const ende = waagrecht
      ? { x: wand.x + wand.breite, y: wand.y + wand.hoehe / 2 }
      : { x: wand.x + wand.breite / 2, y: wand.y + wand.hoehe };
    const form = this.wandBerechnen(start, ende, wand.id);
    wand.x = form.x;
    wand.y = form.y;
    wand.breite = form.breite;
    wand.hoehe = form.hoehe;
    wand.rotation = 0;
  }

  private punktAnWandAndocken(punkt: { x: number; y: number }, ignorierteId = ''): { x: number; y: number } {
    if (!this.raum) return punkt;
    let ergebnis = { ...punkt };
    let kleinsterAbstand = 7;
    for (const wand of this.raum.elemente.filter((element) => element.typ === 'wand' && element.id !== ignorierteId)) {
      const waagrecht = wand.breite >= wand.hoehe;
      const x = waagrecht
        ? this.begrenzen(punkt.x, wand.x, wand.x + wand.breite)
        : wand.x + wand.breite / 2;
      const y = waagrecht
        ? wand.y + wand.hoehe / 2
        : this.begrenzen(punkt.y, wand.y, wand.y + wand.hoehe);
      const abstand = Math.hypot((punkt.x - x) * 1.6, (punkt.y - y) * .9);
      if (abstand < kleinsterAbstand) {
        kleinsterAbstand = abstand;
        ergebnis = { x, y };
      }
    }
    return ergebnis;
  }

  private wandLinieAndocken(waagrecht: boolean, wert: number, von: number, bis: number, ignorierteId: string): number {
    if (!this.raum) return wert;
    let ergebnis = wert;
    let kleinsterAbstand = 3;
    for (const wand of this.raum.elemente.filter((element) => element.typ === 'wand' && element.id !== ignorierteId)) {
      const andereWaagrecht = wand.breite >= wand.hoehe;
      if (andereWaagrecht !== waagrecht) continue;
      const andereVon = waagrecht ? wand.x : wand.y;
      const andereBis = waagrecht ? wand.x + wand.breite : wand.y + wand.hoehe;
      if (bis < andereVon - 4 || von > andereBis + 4) continue;
      const linie = waagrecht ? wand.y + wand.hoehe / 2 : wand.x + wand.breite / 2;
      const abstand = Math.abs(wert - linie);
      if (abstand < kleinsterAbstand) {
        kleinsterAbstand = abstand;
        ergebnis = linie;
      }
    }
    return ergebnis;
  }

  private raster(wert: number): number { return this.rasterAktiv ? Math.round(wert) : Math.round(wert * 10) / 10; }
  private begrenzen(wert: number, min: number, max: number): number { return Math.max(min, Math.min(max, wert)); }

  private zustandMerken(): void {
    if (!this.raum) return;
    this.historie.push(this.elementeKopieren(this.raum.elemente));
    if (this.historie.length > 30) this.historie.shift();
    this.wiederholenHistorie = [];
  }

  private elementeKopieren(elemente: RaumElement[]): RaumElement[] { return elemente.map((element) => ({ ...element })); }
  private auswahlLeeren(): void { this.ausgewaehltesElement = null; this.ausgewaehlteElementIds = []; }
  private darfGrundrissVerlassen(): boolean { return !this.grundrissGeaendert || confirm('Ungespeicherte Grundrissänderungen verwerfen?'); }

  private tischHatKollision(x: number, y: number, eigeneId?: string): boolean {
    if (!this.raum) return false;
    const eigenerTisch = this.raum.arbeitsplaetze.find((tisch) => tisch.id === eigeneId);
    const eigeneBreite = eigeneId ? this.tischForm.breite : eigenerTisch?.breite || this.tischForm.breite;
    const eigeneHoehe = eigeneId ? this.tischForm.hoehe : eigenerTisch?.hoehe || this.tischForm.hoehe;
    const eigeneRotation = eigeneId ? this.tischForm.rotation : eigenerTisch?.rotation || this.tischForm.rotation;
    const eigeneFlaeche = this.gedrehteGroesse(eigeneBreite, eigeneHoehe, eigeneRotation);
    const trifft = (links: number, oben: number, breite: number, hoehe: number) =>
      x + eigeneFlaeche.breite / 2 > links && x - eigeneFlaeche.breite / 2 < links + breite &&
      y + eigeneFlaeche.hoehe / 2 > oben && y - eigeneFlaeche.hoehe / 2 < oben + hoehe;
    const elementTreffer = this.raum.elemente.some((element) =>
      (element.typ === 'wand' || element.typ === 'gesperrt' || element.typ === 'saeule' ||
        element.typ === 'pflanze' || element.typ === 'klima' || element.typ === 'feuerloescher') &&
      trifft(element.x, element.y, element.breite, element.hoehe));
    const tischTreffer = this.raum.arbeitsplaetze.some((tisch) => {
      if (tisch.id === eigeneId) return false;
      const andereFlaeche = this.gedrehteGroesse(tisch.breite || 12, tisch.hoehe || 12, tisch.rotation || 0);
      const mindestAbstandX = Math.max(2, (eigeneFlaeche.breite + andereFlaeche.breite) / 2 - 3);
      const mindestAbstandY = Math.max(2, (eigeneFlaeche.hoehe + andereFlaeche.hoehe) / 2 - 3);
      return Math.abs(tisch.x - x) < mindestAbstandX && Math.abs(tisch.y - y) < mindestAbstandY;
    });
    return elementTreffer || tischTreffer;
  }

  private gedrehteGroesse(breite: number, hoehe: number, rotation: number): { breite: number; hoehe: number } {
    const winkel = rotation * Math.PI / 180;
    return {
      breite: Math.abs(breite * Math.cos(winkel)) + Math.abs(hoehe * Math.sin(winkel)),
      hoehe: Math.abs(breite * Math.sin(winkel)) + Math.abs(hoehe * Math.cos(winkel))
    };
  }

  private leererRaum(): RaumRequest {
    return { name: '', stockwerk: 'EG', standortId: this.standortId, abteilungId: 'ABT-IT', abteilungName: 'IT', elemente: [] };
  }

  private leererTisch(): ArbeitsplatzRequest {
    return { tischnr: '1', name: '', raumId: '', standortId: '', abteilungId: '', abteilungName: '', x: 50, y: 45, rotation: 0, breite: 12, hoehe: 11, equipment: [] };
  }

  private leererBenutzer(): BenutzerRequest {
    return { vorname:'', nachname:'', email:'', rolle:'USER', abteilungId:this.abteilungen[0]?.id ?? 'ABT-IT',
      abteilungName:this.abteilungen[0]?.name ?? 'IT', bevorzugterRaumId:'', aktiv:true };
  }

  private naechsteTischnummer(): string {
    const nummern = this.raum?.arbeitsplaetze.map((tisch) => Number(tisch.tischnr)).filter((wert) => Number.isFinite(wert)) ?? [];
    return String(Math.max(0, ...nummern) + 1);
  }

  private heute(): string { return new Date().toISOString().slice(0, 10); }
}
